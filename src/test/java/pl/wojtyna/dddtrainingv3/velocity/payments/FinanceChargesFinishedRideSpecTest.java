package pl.wojtyna.dddtrainingv3.velocity.payments;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/// Payments' side of `docs/specs/finance-charges-finished-ride.md`: Settlement's *Charge* call, made here directly
/// over Payments' REST resource. Settling the ride (E2.1, E4.1, attempts) is tested in Settlement's
/// `FinanceChargesFinishedRideSpecTest`.
@VelocitySpringTest
@DisplayName("Spec finance-charges-finished-ride (Payments side)")
class FinanceChargesFinishedRideSpecTest {

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;
    @Autowired
    private ApplicationEventMulticaster multicaster;
    @Autowired
    private SimulatedPaymentProcessor processor;
    @Autowired
    private MutableClock clock;

    private PaymentsDriver payments;
    private final String suffix = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        payments = new PaymentsDriver(restClient, urls, multicaster);
        clock.setTo("2026-11-09T10:30:00Z");
    }

    @AfterEach
    void tearDown() {
        payments.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's current card ends 4242,
         when PLN 5.00 is charged for ride S-1 at 10:30 and the payment processor accepts it,
         then the charge succeeded on the card ending 4242
        """
    )
    // @formatter:on
    @Test
    void e1_1_finished_ride_charged_to_current_card_is_settled() {
        // given
        var anna = "anna-" + suffix;
        var visa4242 = "tok_visa_4242_" + suffix;
        var rideS1 = "S-1-" + suffix;
        payments.verify(anna, visa4242);

        // when
        var charge = payments.charge(anna, "5.00", rideS1);

        // then
        assertThat(charge.status()).isEqualTo("SUCCEEDED");
        assertThat(charge.failureReason()).isNull();
        assertThat(charge.chargeId()).isNotBlank();
        assertThat(processor.capturesFor(rideS1)).singleElement().satisfies(capture -> {
            assertThat(capture.cardToken()).isEqualTo(visa4242);
            assertThat(capture.amount()).isEqualByComparingTo(new BigDecimal("5.00"));
            assertThat(capture.currency()).isEqualTo("PLN");
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Ben's current card ends 0002,
         when PLN 43.00 is charged for ride S-3 at 10:30 and the payment processor declines it,
         then the charge failed with reason declined
        """
    )
    // @formatter:on
    @Test
    void e3_1_declined_first_charge_schedules_next_attempt() {
        // given
        var ben = "ben-" + suffix;
        var nofunds0002 = "tok_nofunds_0002_" + suffix;
        var rideS3 = "S-3-" + suffix;
        payments.verify(ben, nofunds0002);

        // when
        var charge = payments.charge(ben, "43.00", rideS3);

        // then
        assertThat(charge.status()).isEqualTo("FAILED");
        assertThat(charge.failureReason()).isEqualTo("DECLINED");
        assertThat(processor.capturesFor(rideS3)).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Carla's current card was invalidated and she has no current card,
         when PLN 3.00 is charged for ride S-4 at 10:30,
         then the charge fails because Carla has no valid payment method, and the processor isn't called
        """
    )
    // @formatter:on
    @Test
    void e3_2_no_current_card_fails_first_charge() {
        // given
        var carla = "carla-" + suffix;
        var visa7777 = "tok_visa_7777_" + suffix;
        var rideS4 = "S-4-" + suffix;
        payments.verify(carla, visa7777);
        payments.reportCardInvalidated(visa7777, "EXPIRED");

        // when
        var charge = payments.charge(carla, "3.00", rideS4);

        // then
        assertThat(charge.status()).isEqualTo("FAILED");
        assertThat(charge.failureReason()).isEqualTo("NO_VALID_PAYMENT_METHOD");
        assertThat(processor.capturesFor(rideS4)).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         Scripted processor: given Dan's current card ends 4242 and the processor will decline its next charge,
         when PLN 5.00 is charged, then the charge failed with reason declined; the following charge succeeds
        """
    )
    // @formatter:on
    @Test
    void scripted_processor_outcome_applies_to_the_next_charge_only() {
        // given
        var dan = "dan-" + suffix;
        var visa4242 = "tok_visa_4242_dan_" + suffix;
        payments.verify(dan, visa4242);
        processor.scriptNextCapture(visa4242, SimulatedPaymentProcessor.ResponseCode.INSUFFICIENT_FUNDS);

        // when
        var first = payments.charge(dan, "5.00", "S-5-" + suffix);
        var second = payments.charge(dan, "5.00", "S-6-" + suffix);

        // then
        assertThat(first.failureReason()).isEqualTo("DECLINED");
        assertThat(second.status()).isEqualTo("SUCCEEDED");
    }
}
