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
import pl.wojtyna.dddtrainingv3.velocity.payments.api.events.PaymentMethodInvalidated;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/// Payments' side of `docs/specs/finance-blocks-rider-with-invalid-card.md`: the processor's notification and the
/// published `PaymentMethodInvalidated`, as a downstream listener sees it. The block itself (Accounts) is tested in
/// Accounts' `FinanceBlocksRiderWithInvalidCardSpecTest`.
@VelocitySpringTest
@DisplayName("Spec finance-blocks-rider-with-invalid-card (Payments side)")
class FinanceBlocksRiderWithInvalidCardSpecTest {

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;
    @Autowired
    private ApplicationEventMulticaster multicaster;
    @Autowired
    private MutableClock clock;

    private PaymentsDriver payments;
    private final String suffix = UUID.randomUUID().toString();
    private final String anna = "anna-" + suffix;
    private final String visa4242 = "tok_visa_4242_" + suffix;
    private final String mc5555 = "tok_mc_5555_" + suffix;

    @BeforeEach
    void setUp() {
        payments = new PaymentsDriver(restClient, urls, multicaster);
        clock.setTo("2026-11-18T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        payments.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's current card ends 4242,
         when on 2026-11-20 at 08:00 the payment processor reports the card ending 4242 expired,
         then the card is invalidated, Anna has no current card and Payment Method Invalidated is published
        """
    )
    // @formatter:on
    @Test
    void e1_1_current_card_invalidated_blocks_rider() {
        // given
        var visa = payments.verify(anna, visa4242);
        clock.setTo("2026-11-20T08:00:00Z");

        // when
        payments.reportCardInvalidated(visa4242, "EXPIRED");

        // then
        var methods = payments.paymentMethodsOf(anna);
        assertThat(methods.currentPaymentMethodId()).isNull();
        assertThat(methods.paymentMethods()).containsExactly(new PaymentMethod(visa.paymentMethodId(), visa4242, "INVALIDATED"));
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(payments.invalidationsOf(anna)).containsExactly(
            new PaymentMethodInvalidated(anna, visa.paymentMethodId(), "EXPIRED", Instant.parse("2026-11-20T08:00:00Z"))));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.3 given the card ending 4242 was reported expired at 08:00,
         when at 08:30 the payment processor reports the card ending 4242 expired again,
         then nothing changes and Payment Method Invalidated isn't published again
        """
    )
    // @formatter:on
    @Test
    void e2_3_repeated_invalidation_changes_nothing() {
        // given
        payments.verify(anna, visa4242);
        clock.setTo("2026-11-20T08:00:00Z");
        payments.reportCardInvalidated(visa4242, "EXPIRED");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> assertThat(payments.invalidationsOf(anna)).hasSize(1));
        clock.setTo("2026-11-20T08:30:00Z");

        // when
        payments.reportCardInvalidated(visa4242, "EXPIRED");

        // then
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(payments.invalidationsOf(anna)).hasSize(1));
        assertThat(payments.paymentMethodsOf(anna).paymentMethods()).extracting(PaymentMethod::status).containsExactly("INVALIDATED");
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna replaced her card ending 4242 with the card ending 5555, which is now her current card,
         when the payment processor reports the card ending 4242 expired,
         then the card ending 4242 is recorded as invalid, Anna keeps the card ending 5555, and nothing is published
        """
    )
    // @formatter:on
    @Test
    void e3_1_old_card_invalidated_changes_nothing_else() {
        // given
        var visa = payments.verify(anna, visa4242);
        var mc = payments.verify(anna, mc5555);
        clock.setTo("2026-11-20T08:00:00Z");

        // when
        payments.reportCardInvalidated(visa4242, "EXPIRED");

        // then
        var methods = payments.paymentMethodsOf(anna);
        assertThat(methods.currentPaymentMethodId()).isEqualTo(mc.paymentMethodId());
        assertThat(methods.paymentMethods()).containsExactly(new PaymentMethod(visa.paymentMethodId(), visa4242, "INVALIDATED"),
                                                             new PaymentMethod(mc.paymentMethodId(), mc5555, "VERIFIED"));
        await().during(Duration.ofSeconds(1)).atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(payments.invalidationsOf(anna)).isEmpty());
    }

    // @formatter:off
    @DisplayName(
        """
         Contract: given no rider added the card tok_unknown,
         when the payment processor reports it cancelled,
         then Payments answers 200 and nothing happens
        """
    )
    // @formatter:on
    @Test
    void unknown_card_notification_is_accepted_and_changes_nothing() {
        // when
        payments.reportCardInvalidated("tok_unknown_" + suffix, "CANCELLED");

        // then
        assertThat(payments.paymentMethodsOf(anna).paymentMethods()).isEmpty();
    }
}
