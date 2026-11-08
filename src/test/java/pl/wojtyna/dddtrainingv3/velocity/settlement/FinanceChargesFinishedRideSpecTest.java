package pl.wojtyna.dddtrainingv3.velocity.settlement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryFailedChargesUseCase;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.SettlementDriver.at;

/// Acceptance tests of `docs/specs/finance-charges-finished-ride.md` (plan D8): the test publishes Rentals'
/// `RentalSessionEnded` as Rentals would; Settlement asks Pricing for the fee and Payments for the charge over REST.
/// The hand-off is asserted with Awaitility on Settlement's Ride Settlement query and the simulated processor.
@VelocitySpringTest
@DisplayName("Spec: Finance wants every finished ride charged to the rider's card")
class FinanceChargesFinishedRideSpecTest {

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;
    @Autowired
    private ApplicationEventPublisher eventBus;
    @Autowired
    private ApplicationEventMulticaster multicaster;
    @Autowired
    private MutableClock clock;
    @Autowired
    private RetryFailedChargesUseCase retryFailedCharges;
    @Autowired
    private SimulatedPaymentProcessor processor;

    private SettlementDriver settlement;
    private final String suffix = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        settlement = new SettlementDriver(restClient, urls, eventBus, multicaster, clock, retryFailedCharges);
        clock.setTo(at("10:30"));
    }

    @AfterEach
    void tearDown() {
        settlement.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's ride S-1 ended at 10:30 and was priced at PLN 5.00 (Ride Priced), and Anna's current card ends 4242,
         when S-1 is charged at 10:30 and the payment processor accepts PLN 5.00,
         then S-1 is settled (Ride Settled)
        """
    )
    // @formatter:on
    @Test
    void e1_1_finished_ride_charged_to_current_card_is_settled() {
        // given
        var anna = settlement.registeredRider("anna");
        var visa4242 = "tok_visa_4242_" + suffix;
        settlement.verifyCard(anna, visa4242);
        var s1 = "S-1-" + suffix;

        // when
        settlement.ride5PlnEnded(s1, anna, "10:00", "10:30");

        // then
        var ride = settlement.awaitRide(s1, "SETTLED", 0);
        assertThat(ride.fee()).isEqualTo("5.00");
        assertThat(ride.riderId()).isEqualTo(anna);
        assertThat(processor.capturesFor(s1)).singleElement().satisfies(capture -> {
            assertThat(capture.cardToken()).isEqualTo(visa4242);
            assertThat(capture.amount()).isEqualByComparingTo(new BigDecimal("5.00"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna's ride S-2 started at 10:00, ended at 10:12 and was priced at PLN 0.00,
         when S-2 is charged,
         then S-2 is settled at once and Anna's card isn't charged (Ride Settled)
        """
    )
    // @formatter:on
    @Test
    void e2_1_free_ride_settled_without_charging() {
        // given
        clock.setTo(at("10:12"));
        var anna = settlement.registeredRider("anna");
        var visa4242 = "tok_visa_4242_" + suffix;
        settlement.verifyCard(anna, visa4242);
        var s2 = "S-2-" + suffix;

        // when
        settlement.stationRideEnded(s2, anna, "10:00", "10:12");

        // then
        var ride = settlement.awaitRide(s2, "SETTLED", 0);
        assertThat(ride.fee()).isEqualTo("0.00");
        assertThat(processor.capturesFor(s2)).isEmpty();
        assertThat(processor.capturesOf(visa4242)).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Ben's ride S-3 ended at 10:30 and was priced at PLN 43.00, and Ben's current card ends 0002,
         when S-3 is charged at 10:30 and the payment processor declines it,
         then S-3 isn't settled, this was attempt 1 of 3 and the next attempt is due at 10:35 (Charge Attempt Failed)
        """
    )
    // @formatter:on
    @Test
    void e3_1_declined_first_charge_schedules_next_attempt() {
        // given
        var ben = settlement.registeredRider("ben");
        var nofunds0002 = "tok_nofunds_0002_" + suffix;
        settlement.verifyCard(ben, nofunds0002);
        var s3 = "S-3-" + suffix;

        // when
        settlement.ride43PlnEnded(s3, ben, "10:00", "10:30");

        // then
        var ride = settlement.awaitRide(s3, "PENDING", 1);
        assertThat(ride.fee()).isEqualTo("43.00");
        assertThat(ride.nextAttemptDueAt()).isEqualTo(at("10:35"));
        assertThat(ride.lastFailureReason()).isEqualTo("DECLINED");
        assertThat(processor.capturesFor(s3)).singleElement()
                                             .satisfies(capture -> assertThat(capture.cardToken()).isEqualTo(nofunds0002));
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Carla's current card was invalidated and she has no current card, and her ride S-4 ended at 10:30
         and was priced at PLN 3.00,
         when S-4 is charged at 10:30,
         then the charge fails because Carla has no valid payment method, this was attempt 1 of 3 and the next attempt
         is due at 10:35 (Charge Attempt Failed)
        """
    )
    // @formatter:on
    @Test
    void e3_2_no_current_card_fails_first_charge() {
        // given
        var carla = settlement.registeredRider("carla");
        var visa = "tok_visa_1111_" + suffix;
        settlement.verifyCard(carla, visa);
        settlement.processorReportsCardInvalidated(visa);
        var s4 = "S-4-" + suffix;

        // when
        settlement.stationRideEnded(s4, carla, "10:00", "10:30");

        // then
        var ride = settlement.awaitRide(s4, "PENDING", 1);
        assertThat(ride.fee()).isEqualTo("3.00");
        assertThat(ride.nextAttemptDueAt()).isEqualTo(at("10:35"));
        assertThat(ride.lastFailureReason()).isEqualTo("NO_VALID_PAYMENT_METHOD");
        assertThat(processor.capturesFor(s4)).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given Anna's ride S-1 is settled,
         when the news that S-1 ended arrives a second time,
         then Anna's card isn't charged again and S-1 stays settled
        """
    )
    // @formatter:on
    @Test
    void e4_1_repeated_session_ended_does_not_charge_again() {
        // given
        var anna = settlement.registeredRider("anna");
        settlement.verifyCard(anna, "tok_visa_4242_" + suffix);
        var s1 = "S-1-" + suffix;
        settlement.ride5PlnEnded(s1, anna, "10:00", "10:30");
        settlement.awaitRide(s1, "SETTLED", 0);

        // when
        settlement.ride5PlnEnded(s1, anna, "10:00", "10:30");

        // then
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(processor.capturesFor(s1)).hasSize(1));
        assertThat(settlement.ride(s1)).hasValueSatisfying(ride -> assertThat(ride.status()).isEqualTo("SETTLED"));
    }
}
