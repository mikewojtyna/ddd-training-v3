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
import pl.wojtyna.dddtrainingv3.velocity.payments.PaymentsDriver.PaymentMethod;
import pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl.SimulatedPaymentProcessor;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/// Payments' side of `docs/specs/rider-adds-payment-method.md`: Accounts' *Verify Payment Method* call, made here
/// directly over Payments' REST resource. The Accounts side (onboarding step, E1.2) is tested in Accounts'
/// `RiderAddsPaymentMethodSpecTest`.
@VelocitySpringTest
@DisplayName("Spec rider-adds-payment-method (Payments side)")
class RiderAddsPaymentMethodSpecTest {

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
    private final String anna = "anna-" + suffix;
    private final String visa4242 = "tok_visa_4242_" + suffix;
    private final String mc5555 = "tok_mc_5555_" + suffix;
    private final String declined0002 = "tok_declined_0002_" + suffix;

    @BeforeEach
    void setUp() {
        payments = new PaymentsDriver(restClient, urls, multicaster);
        clock.setTo("2026-11-08T10:10:00Z");
    }

    @AfterEach
    void tearDown() {
        payments.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna has no card,
         when Anna adds her card ending 4242 at 10:10 and the processor accepts the PLN 1.00 pre-authorization,
         then the card ending 4242 is verified and is Anna's current card, and the PLN 1.00 is released
        """
    )
    // @formatter:on
    @Test
    void e1_1_card_passes_pre_authorization_and_becomes_current() {
        // when
        var verification = payments.verify(anna, visa4242);

        // then
        assertThat(verification.verified()).isTrue();
        assertThat(verification.declineReason()).isNull();
        var methods = payments.paymentMethodsOf(anna);
        assertThat(methods.currentPaymentMethodId()).isEqualTo(verification.paymentMethodId());
        assertThat(methods.paymentMethods()).containsExactly(new PaymentMethod(verification.paymentMethodId(), visa4242, "VERIFIED"));
        assertThat(processor.authorizationsOf(visa4242)).singleElement().satisfies(authorization -> {
            assertThat(authorization.amount()).isEqualByComparingTo(BigDecimal.ONE);
            assertThat(authorization.currency()).isEqualTo("PLN");
            assertThat(processor.isVoided(authorization.authorizationId())).isTrue();
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna has no card,
         when Anna adds her card ending 0002 and the processor declines the pre-authorization,
         then the card isn't verified and Anna has no current card
        """
    )
    // @formatter:on
    @Test
    void e2_1_declined_card_keeps_rider_at_payment_step() {
        // when
        var verification = payments.verify(anna, declined0002);

        // then
        assertThat(verification.verified()).isFalse();
        assertThat(verification.declineReason()).isEqualTo("DECLINED");
        var methods = payments.paymentMethodsOf(anna);
        assertThat(methods.currentPaymentMethodId()).isNull();
        assertThat(methods.paymentMethods()).containsExactly(new PaymentMethod(verification.paymentMethodId(), declined0002, "DECLINED"));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Anna's card ending 0002 was declined at 10:10,
         when Anna adds her card ending 4242 at 10:12 and the processor accepts the pre-authorization,
         then the card ending 4242 is Anna's current card
        """
    )
    // @formatter:on
    @Test
    void e2_2_second_card_verified_after_decline() {
        // given
        payments.verify(anna, declined0002);
        clock.setTo("2026-11-08T10:12:00Z");

        // when
        var verification = payments.verify(anna, visa4242);

        // then
        assertThat(verification.verified()).isTrue();
        assertThat(payments.paymentMethodsOf(anna).currentPaymentMethodId()).isEqualTo(verification.paymentMethodId());
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's current card ends 4242,
         when on 2026-12-01 Anna adds her card ending 5555 and the processor accepts the pre-authorization,
         then the card ending 5555 is Anna's current card and the card ending 4242 is no longer charged
        """
    )
    // @formatter:on
    @Test
    void e3_1_new_verified_card_replaces_current_card() {
        // given
        var visa = payments.verify(anna, visa4242);
        clock.setTo("2026-12-01T09:00:00Z");

        // when
        var mc = payments.verify(anna, mc5555);

        // then
        var methods = payments.paymentMethodsOf(anna);
        assertThat(methods.currentPaymentMethodId()).isEqualTo(mc.paymentMethodId());
        assertThat(methods.paymentMethods()).containsExactly(new PaymentMethod(visa.paymentMethodId(), visa4242, "REPLACED"),
                                                             new PaymentMethod(mc.paymentMethodId(), mc5555, "VERIFIED"));
        var reference = "S-" + suffix;
        payments.charge(anna, "5.00", reference);
        assertThat(processor.capturesFor(reference)).singleElement()
                                                    .satisfies(capture -> assertThat(capture.cardToken()).isEqualTo(mc5555));
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Anna's current card ends 4242,
         when Anna adds her card ending 0002 and the processor declines the pre-authorization,
         then the card ending 4242 stays Anna's current card
        """
    )
    // @formatter:on
    @Test
    void e3_2_declined_card_keeps_current_card() {
        // given
        var visa = payments.verify(anna, visa4242);
        clock.setTo("2026-12-01T09:00:00Z");

        // when
        var declined = payments.verify(anna, declined0002);

        // then
        assertThat(declined.verified()).isFalse();
        assertThat(payments.paymentMethodsOf(anna).currentPaymentMethodId()).isEqualTo(visa.paymentMethodId());
    }
}
