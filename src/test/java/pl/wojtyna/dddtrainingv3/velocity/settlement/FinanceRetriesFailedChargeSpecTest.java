package pl.wojtyna.dddtrainingv3.velocity.settlement;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.messagingprovideracl.RecordingRiderNotifications;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor.ResponseCode;
import pl.wojtyna.dddtrainingv3.velocity.settlement.SettlementDriver.UnpaidRide;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.RetryFailedChargesUseCase;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static pl.wojtyna.dddtrainingv3.velocity.settlement.SettlementDriver.at;

/// Acceptance tests of `docs/specs/finance-retries-failed-charge.md` (plan D9): attempt 1 happens for real (the test
/// publishes Rentals' `RentalSessionEnded`), then the test sets the clock and runs *Retry Failed Charges* directly
/// (scheduling is off in tests). The Settlement → Accounts block is asserted with Awaitility on Accounts' Account
/// Status and its recording notification adapter.
@VelocitySpringTest
@DisplayName("Spec: Finance wants a failed ride charge retried before it becomes outstanding debt")
class FinanceRetriesFailedChargeSpecTest {

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
    @Autowired
    private RecordingRiderNotifications notifications;

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
         E1.1 given Ben's ride S-3 is priced at PLN 43.00 and attempt 1 failed at 10:30,
         when failed charges are retried at 10:35 and the payment processor accepts PLN 43.00 on Ben's current card,
         then S-3 is settled (Ride Settled)
        """
    )
    // @formatter:on
    @Test
    void e1_1_retry_five_minutes_later_settles_ride() {
        // given
        var ben = settlement.registeredRider("ben");
        var visa = "tok_visa_4242_" + suffix;
        settlement.verifyCard(ben, visa);
        processor.scriptNextCapture(visa, ResponseCode.INSUFFICIENT_FUNDS);
        var s3 = "S-3-" + suffix;
        settlement.ride43PlnEnded(s3, ben, "10:00", "10:30");
        settlement.awaitRide(s3, "PENDING", 1);

        // when
        settlement.retryFailedChargesAt("10:35");

        // then
        settlement.awaitRide(s3, "SETTLED", 1);
        assertThat(processor.capturesFor(s3)).hasSize(2)
                                             .allSatisfy(capture -> assertThat(capture.cardToken()).isEqualTo(visa));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given attempt 1 for S-3 failed at 10:30,
         when failed charges are retried at 10:34,
         then no attempt is made for S-3
        """
    )
    // @formatter:on
    @Test
    void e1_2_no_retry_before_five_minutes() {
        // given
        var ben = settlement.registeredRider("ben");
        settlement.verifyCard(ben, "tok_nofunds_0002_" + suffix);
        var s3 = "S-3-" + suffix;
        settlement.ride43PlnEnded(s3, ben, "10:00", "10:30");
        settlement.awaitRide(s3, "PENDING", 1);

        // when
        settlement.retryFailedChargesAt("10:34");

        // then
        assertThat(processor.capturesFor(s3)).hasSize(1);
        var ride = settlement.awaitRide(s3, "PENDING", 1);
        assertThat(ride.nextAttemptDueAt()).isEqualTo(at("10:35"));
    }

    // @formatter:off
    @DisplayName(
        """
         E1.3 given Carla had no current card and attempt 1 for her ride S-4 (PLN 3.00) failed at 10:30,
         and at 10:33 Carla's new card ending 5555 was verified,
         when failed charges are retried at 10:35 and the payment processor accepts PLN 3.00,
         then the card ending 5555 is charged and S-4 is settled (Ride Settled) (DE-5)
        """
    )
    // @formatter:on
    @Test
    void e1_3_retry_charges_newly_verified_card() {
        // given
        var carla = settlement.registeredRider("carla");
        var s4 = "S-4-" + suffix;
        settlement.stationRideEnded(s4, carla, "10:00", "10:30");
        assertThat(settlement.awaitRide(s4, "PENDING", 1).lastFailureReason()).isEqualTo("NO_VALID_PAYMENT_METHOD");
        clock.setTo(at("10:33"));
        var mc5555 = "tok_mc_5555_" + suffix;
        settlement.verifyCard(carla, mc5555);

        // when
        settlement.retryFailedChargesAt("10:35");

        // then
        settlement.awaitRide(s4, "SETTLED", 1);
        assertThat(processor.capturesFor(s4)).singleElement().satisfies(capture -> {
            assertThat(capture.cardToken()).isEqualTo(mc5555);
            assertThat(capture.amount()).isEqualByComparingTo(new BigDecimal("3.00"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given attempt 1 for Ben's ride S-3 (PLN 43.00) failed at 10:30,
         when failed charges are retried at 10:35 and the charge fails again,
         then this was attempt 2 of 3, the next attempt is due at 10:40 and Ben has no outstanding debt yet
         (Charge Attempt Failed)
        """
    )
    // @formatter:on
    @Test
    void e2_1_second_failure_schedules_third_attempt_without_debt() {
        // given
        var ben = settlement.registeredRider("ben");
        settlement.verifyCard(ben, "tok_nofunds_0002_" + suffix);
        var s3 = "S-3-" + suffix;
        settlement.ride43PlnEnded(s3, ben, "10:00", "10:30");
        settlement.awaitRide(s3, "PENDING", 1);

        // when
        settlement.retryFailedChargesAt("10:35");

        // then
        var ride = settlement.awaitRide(s3, "PENDING", 2);
        assertThat(ride.nextAttemptDueAt()).isEqualTo(at("10:40"));
        assertThat(settlement.balanceOf(ben).outstandingDebt()).isEqualTo("0.00");
        assertThat(settlement.accountOf(ben).blocks()).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given attempts 1 and 2 for S-3 failed at 10:30 and 10:35, and Ben owes nothing and isn't blocked,
         when failed charges are retried at 10:40 and the charge fails again,
         then S-3's fee is unpaid, Ben's outstanding debt is PLN 43.00 (Rental Fee Unpaid),
         and Ben is blocked for outstanding debt and notified (Account Blocked) (DE-7)
        """
    )
    // @formatter:on
    @Test
    void e2_2_third_failure_makes_fee_outstanding_debt_and_blocks_rider() {
        // given
        var ben = settlement.registeredRider("ben");
        settlement.verifyCard(ben, "tok_nofunds_0002_" + suffix);
        var s3 = "S-3-" + suffix;
        settlement.ride43PlnEnded(s3, ben, "10:00", "10:30");
        settlement.awaitRide(s3, "PENDING", 1);
        settlement.retryFailedChargesAt("10:35");
        settlement.awaitRide(s3, "PENDING", 2);

        // when
        settlement.retryFailedChargesAt("10:40");

        // then
        settlement.awaitRide(s3, "UNPAID", 3);
        var balance = settlement.balanceOf(ben);
        assertThat(balance.outstandingDebt()).isEqualTo("43.00");
        assertThat(balance.unpaidRides()).containsExactly(new UnpaidRide(s3, "43.00"));
        assertThat(settlement.rentalFeesUnpaidOf(ben)).singleElement().satisfies(unpaid -> {
            assertThat(unpaid.sessionId()).isEqualTo(s3);
            assertThat(unpaid.amount()).isEqualByComparingTo("43.00");
            assertThat(unpaid.outstandingDebt()).isEqualByComparingTo("43.00");
            assertThat(unpaid.occurredAt()).isEqualTo(at("10:40"));
        });
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(settlement.accountOf(ben).blocks()).containsExactly("OUTSTANDING_DEBT");
            assertThat(notifications.sentTo(ben)).singleElement()
                                                 .satisfies(sent -> assertThat(sent.reason()).isEqualTo("OUTSTANDING_DEBT"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Ben owes PLN 43.00 from S-3 and has been blocked for outstanding debt since 10:40,
         and his ride S-5 started at 10:32 and ended at 10:50, priced at PLN 3.00, with attempts 1 and 2 failed at 10:50 and 10:55,
         when failed charges are retried at 11:00 and the charge fails again,
         then S-5's fee is unpaid, Ben's outstanding debt is PLN 46.00 (Rental Fee Unpaid)
         and Ben stays blocked for outstanding debt without a new block notification (DE-94)
        """
    )
    // @formatter:on
    @Test
    void e3_1_further_unpaid_fee_adds_to_debt_without_new_notification() {
        // given
        var ben = settlement.registeredRider("ben");
        settlement.verifyCard(ben, "tok_nofunds_0002_" + suffix);
        var s3 = "S-3-" + suffix;
        settlement.ridesUnpaid(ben, true, s3, "10:00", "10:30");
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(notifications.sentTo(ben)).hasSize(1));
        var s5 = "S-5-" + suffix;
        clock.setTo(at("10:50"));
        settlement.stationRideEnded(s5, ben, "10:32", "10:50");
        settlement.awaitRide(s5, "PENDING", 1);
        settlement.retryFailedChargesAt("10:55");
        settlement.awaitRide(s5, "PENDING", 2);

        // when
        settlement.retryFailedChargesAt("11:00");

        // then
        settlement.awaitRide(s5, "UNPAID", 3);
        var balance = settlement.balanceOf(ben);
        assertThat(balance.outstandingDebt()).isEqualTo("46.00");
        assertThat(balance.unpaidRides()).containsExactly(new UnpaidRide(s3, "43.00"), new UnpaidRide(s5, "3.00"));
        assertThat(settlement.rentalFeesUnpaidOf(ben)).last().satisfies(unpaid -> {
            assertThat(unpaid.sessionId()).isEqualTo(s5);
            assertThat(unpaid.amount()).isEqualByComparingTo("3.00");
            assertThat(unpaid.outstandingDebt()).isEqualByComparingTo("46.00");
        });
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(settlement.accountOf(ben).blocks()).containsExactly("OUTSTANDING_DEBT");
            assertThat(notifications.sentTo(ben)).hasSize(1);
        });
    }
}
