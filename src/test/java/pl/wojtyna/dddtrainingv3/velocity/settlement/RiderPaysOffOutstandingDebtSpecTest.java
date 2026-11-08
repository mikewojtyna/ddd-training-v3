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

/// Acceptance tests of `docs/specs/rider-pays-off-outstanding-debt.md` (plan D8): real debt is made by riders whose
/// `tok_nofunds_*` card is declined three times (`SettlementDriver#ridesUnpaid`), so Accounts has blocked them for
/// outstanding debt; a payoff that must succeed first verifies a new valid card at Payments (DE-5). The rider pays
/// through `POST /settlement/riders/{riderId}/debt-payment`; the Settlement → Accounts unblock is asserted with
/// Awaitility on Accounts' Account Status.
@VelocitySpringTest
@DisplayName("Spec: a rider wants to pay off their outstanding debt")
class RiderPaysOffOutstandingDebtSpecTest {

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
        clock.setTo(at("10:00"));
    }

    @AfterEach
    void tearDown() {
        settlement.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna owes PLN 43.00 from ride S-3, is blocked for outstanding debt and her current card ends 4242,
         when Anna pays off her debt at 18:00 and the payment processor accepts PLN 43.00,
         then Anna owes PLN 0.00 (Outstanding Debt Cleared) and her block for outstanding debt is lifted (Account Unblocked)
        """
    )
    // @formatter:on
    @Test
    void e1_1_rider_pays_off_debt_and_debt_block_is_lifted() {
        // given
        var anna = settlement.registeredRider("anna");
        settlement.verifyCard(anna, "tok_nofunds_" + suffix);
        settlement.ridesUnpaid(anna, true, "S-3-" + suffix, "10:00", "10:30");
        awaitBlocks(anna, "OUTSTANDING_DEBT");
        var visa4242 = "tok_visa_4242_" + suffix;
        settlement.verifyCard(anna, visa4242);
        clock.setTo(at("18:00"));

        // when
        var answer = settlement.payOff(anna);

        // then
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.collected()).isTrue();
        assertThat(answer.collectedAmount()).isEqualTo("43.00");
        assertThat(answer.outstandingDebt()).isEqualTo("0.00");
        assertThat(settlement.balanceOf(anna).outstandingDebt()).isEqualTo("0.00");
        assertThat(settlement.balanceOf(anna).unpaidRides()).isEmpty();
        assertThat(processor.capturesOf(visa4242)).singleElement()
                                                  .satisfies(capture -> assertThat(capture.amount()).isEqualByComparingTo(new BigDecimal("43.00")));
        assertThat(settlement.debtsClearedOf(anna)).singleElement()
                                                   .satisfies(cleared -> assertThat(cleared.occurredAt()).isEqualTo(at("18:00")));
        awaitBlocks(anna);
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Ben owes PLN 46.00 from rides S-3 (PLN 43.00) and S-5 (PLN 3.00) and is blocked for outstanding debt,
         when Ben pays off his debt and the payment processor accepts PLN 46.00,
         then one charge of PLN 46.00 is made, Ben owes PLN 0.00 (Outstanding Debt Cleared)
         and his block for outstanding debt is lifted (Account Unblocked)
        """
    )
    // @formatter:on
    @Test
    void e1_2_debt_from_two_rides_paid_in_one_charge() {
        // given
        var ben = settlement.registeredRider("ben");
        settlement.verifyCard(ben, "tok_nofunds_" + suffix);
        settlement.ridesUnpaid(ben, true, "S-3-" + suffix, "10:00", "10:30");
        settlement.ridesUnpaid(ben, false, "S-5-" + suffix, "10:32", "10:50");
        assertThat(settlement.balanceOf(ben).outstandingDebt()).isEqualTo("46.00");
        awaitBlocks(ben, "OUTSTANDING_DEBT");
        var visa = "tok_visa_4242_" + suffix;
        settlement.verifyCard(ben, visa);
        clock.setTo(at("18:00"));

        // when
        var answer = settlement.payOff(ben);

        // then
        assertThat(answer.collected()).isTrue();
        assertThat(answer.collectedAmount()).isEqualTo("46.00");
        assertThat(answer.outstandingDebt()).isEqualTo("0.00");
        assertThat(processor.capturesOf(visa)).singleElement()
                                              .satisfies(capture -> assertThat(capture.amount()).isEqualByComparingTo(new BigDecimal("46.00")));
        assertThat(settlement.debtsClearedOf(ben)).hasSize(1);
        awaitBlocks(ben);
    }

    // @formatter:off
    @DisplayName(
        """
         E1.3 given Ben owes PLN 43.00 from ride S-3, and attempt 1 for his ride S-5 (PLN 3.00) failed at 10:50,
         when Ben pays off his debt at 10:52 and the payment processor accepts PLN 43.00,
         then only PLN 43.00 is charged, Ben owes PLN 0.00 (Outstanding Debt Cleared),
         his block for outstanding debt is lifted (Account Unblocked) and S-5 stays in its charge retries (DE-95)
        """
    )
    // @formatter:on
    @Test
    void e1_3_ride_still_in_retries_is_not_part_of_payoff() {
        // given
        var ben = settlement.registeredRider("ben");
        var nofunds = "tok_nofunds_" + suffix;
        settlement.verifyCard(ben, nofunds);
        settlement.ridesUnpaid(ben, true, "S-3-" + suffix, "10:00", "10:30");
        var s5 = "S-5-" + suffix;
        clock.setTo(at("10:50"));
        settlement.stationRideEnded(s5, ben, "10:32", "10:50");
        settlement.awaitRide(s5, "PENDING", 1);
        awaitBlocks(ben, "OUTSTANDING_DEBT");
        var visa = "tok_visa_4242_" + suffix;
        settlement.verifyCard(ben, visa);
        clock.setTo(at("10:52"));

        // when
        var answer = settlement.payOff(ben);

        // then
        assertThat(answer.collectedAmount()).isEqualTo("43.00");
        assertThat(answer.outstandingDebt()).isEqualTo("0.00");
        assertThat(processor.capturesOf(visa)).singleElement()
                                              .satisfies(capture -> assertThat(capture.amount()).isEqualByComparingTo(new BigDecimal("43.00")));
        assertThat(settlement.debtsClearedOf(ben)).hasSize(1);
        var s5Settlement = settlement.awaitRide(s5, "PENDING", 1);
        assertThat(s5Settlement.nextAttemptDueAt()).isEqualTo(at("10:55"));
        awaitBlocks(ben);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna owes PLN 43.00 and is blocked for outstanding debt,
         when Anna pays off her debt and the payment processor declines PLN 43.00,
         then Anna still owes PLN 43.00 and stays blocked
        """
    )
    // @formatter:on
    @Test
    void e2_1_declined_payment_leaves_debt_and_block() {
        // given
        var anna = settlement.registeredRider("anna");
        var nofunds = "tok_nofunds_" + suffix;
        settlement.verifyCard(anna, nofunds);
        settlement.ridesUnpaid(anna, true, "S-3-" + suffix, "10:00", "10:30");
        awaitBlocks(anna, "OUTSTANDING_DEBT");
        clock.setTo(at("18:00"));

        // when
        var answer = settlement.payOff(anna);

        // then
        assertThat(answer.status()).isEqualTo(200);
        assertThat(answer.collected()).isFalse();
        assertThat(answer.collectedAmount()).isEqualTo("0.00");
        assertThat(answer.outstandingDebt()).isEqualTo("43.00");
        assertThat(settlement.balanceOf(anna).outstandingDebt()).isEqualTo("43.00");
        assertThat(settlement.debtsClearedOf(anna)).isEmpty();
        assertThat(processor.capturesOf(nofunds)).last()
                                                 .satisfies(capture -> assertThat(capture.amount()).isEqualByComparingTo(new BigDecimal("43.00")));
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(settlement.accountOf(anna).blocks()).containsExactly("OUTSTANDING_DEBT"));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Carla owes PLN 9.00, is blocked for outstanding debt and for an invalid card, and has no current card,
         when Carla pays off her debt,
         then the payment fails, Carla still owes PLN 9.00 and both blocks stay (DE-6, DE-71)
        """
    )
    // @formatter:on
    @Test
    void e2_2_no_current_card_leaves_debt_and_both_blocks() {
        // given
        var carla = settlement.registeredRider("carla");
        var nofunds = "tok_nofunds_" + suffix;
        settlement.verifyCard(carla, nofunds);
        settlement.ridesUnpaid(carla, false, "S-6-" + suffix, "10:00", "10:30");
        settlement.ridesUnpaid(carla, false, "S-7-" + suffix, "11:00", "11:30");
        settlement.ridesUnpaid(carla, false, "S-8-" + suffix, "12:00", "12:30");
        settlement.processorReportsCardInvalidated(nofunds);
        await().atMost(Duration.ofSeconds(10)).untilAsserted(
            () -> assertThat(settlement.accountOf(carla).blocks()).containsExactlyInAnyOrder("OUTSTANDING_DEBT", "INVALID_CARD"));
        var capturesBefore = processor.capturesOf(nofunds).size();
        clock.setTo(at("18:00"));

        // when
        var answer = settlement.payOff(carla);

        // then
        assertThat(answer.collected()).isFalse();
        assertThat(answer.outstandingDebt()).isEqualTo("9.00");
        assertThat(settlement.balanceOf(carla).outstandingDebt()).isEqualTo("9.00");
        assertThat(processor.capturesOf(nofunds)).hasSize(capturesBefore);
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(10)).untilAsserted(
            () -> assertThat(settlement.accountOf(carla).blocks()).containsExactlyInAnyOrder("OUTSTANDING_DEBT", "INVALID_CARD"));
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Eva owes PLN 0.00,
         when Eva pays off her debt,
         then the payment is rejected and Eva's card isn't charged
        """
    )
    // @formatter:on
    @Test
    void e3_1_rider_without_debt_cannot_pay_off() {
        // given
        var eva = settlement.registeredRider("eva");
        var visa = "tok_visa_" + suffix;
        settlement.verifyCard(eva, visa);

        // when
        var answer = settlement.payOff(eva);

        // then
        assertThat(answer.status()).isEqualTo(409);
        assertThat(answer.error()).isEqualTo("NoOutstandingDebt");
        assertThat(processor.capturesOf(visa)).isEmpty();
    }

    private void awaitBlocks(String riderId, String... blocks) {
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(settlement.accountOf(riderId).blocks()).containsExactlyInAnyOrder(blocks));
    }
}
