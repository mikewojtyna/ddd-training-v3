package pl.wojtyna.dddtrainingv3.velocity.accounts;

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
import java.time.Duration;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/// Acceptance tests of `docs/specs/rider-adds-payment-method.md`: the rider adds a card in Accounts, which asks
/// Payments to verify it over REST. The hand-off is asserted on Payments' side (its payment-methods resource and the
/// simulated processor), with Awaitility as every cross-context effect.
@VelocitySpringTest
@DisplayName("Spec rider-adds-payment-method")
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

    private AccountsDriver accounts;
    private PaymentsView payments;
    private final String suffix = UUID.randomUUID().toString();
    private final String annaEmail = "anna-" + suffix + "@example.com";
    private final String visa4242 = "tok_visa_4242_" + suffix;
    private final String mc5555 = "tok_mc_5555_" + suffix;
    private final String visa1111 = "tok_visa_1111_" + suffix;
    private final String declined0002 = "tok_declined_0002_" + suffix;

    @BeforeEach
    void setUp() {
        accounts = new AccountsDriver(restClient, urls, multicaster);
        payments = new PaymentsView(restClient, urls);
        clock.setTo("2026-11-08T10:08:00Z");
    }

    @AfterEach
    void tearDown() {
        accounts.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna provided her personal data on 2026-11-08 at 10:08 and has no card,
         when Anna adds her card ending 4242 at 10:10 and the processor accepts the PLN 1.00 pre-authorization,
         then the card is verified and is Anna's current card, the PLN 1.00 is released, and her next step is signing the agreement
        """
    )
    // @formatter:on
    @Test
    void e1_1_card_passes_pre_authorization_and_becomes_current() {
        // given
        var anna = accounts.personalDataProvided(annaEmail);
        clock.setTo("2026-11-08T10:10:00Z");

        // when
        var addition = accounts.addPaymentMethod(anna, visa4242);

        // then
        assertThat(addition.status()).isEqualTo(200);
        assertThat(addition.verified()).isTrue();
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("SIGN_USER_AGREEMENT");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var methods = payments.paymentMethodsOf(anna);
            assertThat(methods.currentPaymentMethodId()).isEqualTo(addition.paymentMethodId());
            assertThat(methods.currentCardToken()).isEqualTo(visa4242);
            assertThat(processor.authorizationsOf(visa4242)).singleElement().satisfies(authorization -> {
                assertThat(authorization.amount()).isEqualByComparingTo(BigDecimal.ONE);
                assertThat(processor.isVoided(authorization.authorizationId())).isTrue();
            });
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Ben verified his phone but hasn't provided his personal data,
         when Ben adds his card ending 1111,
         then the step is rejected, no pre-authorization is made and his next onboarding step is still providing personal data
        """
    )
    // @formatter:on
    @Test
    void e1_2_card_rejected_before_personal_data() {
        // given
        var ben = accounts.phoneVerified("ben-" + suffix + "@example.com");

        // when
        var addition = accounts.addPaymentMethod(ben, visa1111);

        // then
        assertThat(addition.status()).isEqualTo(409);
        assertThat(accounts.statusOf(ben).onboardingStep()).isEqualTo("PROVIDE_PERSONAL_DATA");
        await().during(Duration.ofMillis(500)).atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            assertThat(processor.authorizationsOf(visa1111)).isEmpty();
            assertThat(payments.paymentMethodsOf(ben).paymentMethods()).isEmpty();
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna provided her personal data and has no card,
         when Anna adds her card ending 0002 and the processor declines the pre-authorization,
         then the card isn't verified, Anna has no current card and her next onboarding step is still adding a payment method
        """
    )
    // @formatter:on
    @Test
    void e2_1_declined_card_keeps_rider_at_payment_step() {
        // given
        var anna = accounts.personalDataProvided(annaEmail);

        // when
        var addition = accounts.addPaymentMethod(anna, declined0002);

        // then
        assertThat(addition.status()).isEqualTo(200);
        assertThat(addition.verified()).isFalse();
        assertThat(addition.paymentMethodId()).isNull();
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("ADD_PAYMENT_METHOD");
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var methods = payments.paymentMethodsOf(anna);
            assertThat(methods.currentPaymentMethodId()).isNull();
            assertThat(methods.paymentMethods()).extracting(PaymentsView.PaymentMethod::status).containsExactly("DECLINED");
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Anna's card ending 0002 was declined at 10:10,
         when Anna adds her card ending 4242 at 10:12 and the processor accepts the pre-authorization,
         then the card ending 4242 is Anna's current card and her next onboarding step is signing the user agreement
        """
    )
    // @formatter:on
    @Test
    void e2_2_second_card_verified_after_decline() {
        // given
        var anna = accounts.personalDataProvided(annaEmail);
        clock.setTo("2026-11-08T10:10:00Z");
        accounts.addPaymentMethod(anna, declined0002);
        clock.setTo("2026-11-08T10:12:00Z");

        // when
        var addition = accounts.addPaymentMethod(anna, visa4242);

        // then
        assertThat(addition.verified()).isTrue();
        assertThat(accounts.statusOf(anna).onboardingStep()).isEqualTo("SIGN_USER_AGREEMENT");
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(payments.paymentMethodsOf(anna).currentCardToken()).isEqualTo(visa4242));
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna's account is activated and her current card ends 4242,
         when on 2026-12-01 Anna adds her card ending 5555 and the processor accepts the pre-authorization,
         then the card ending 5555 is Anna's current card and the card ending 4242 is no longer charged
        """
    )
    // @formatter:on
    @Test
    void e3_1_new_verified_card_replaces_current_card() {
        // given
        var anna = accounts.activated(annaEmail, visa4242);
        clock.setTo("2026-12-01T10:00:00Z");

        // when
        var addition = accounts.addPaymentMethod(anna, mc5555);

        // then
        assertThat(addition.verified()).isTrue();
        assertThat(accounts.statusOf(anna).activated()).isTrue();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            var methods = payments.paymentMethodsOf(anna);
            assertThat(methods.currentCardToken()).isEqualTo(mc5555);
            assertThat(methods.paymentMethods()).extracting(PaymentsView.PaymentMethod::cardToken, PaymentsView.PaymentMethod::status)
                                                .contains(org.assertj.core.groups.Tuple.tuple(visa4242, "REPLACED"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E3.2 given Anna's account is activated and her current card ends 4242,
         when Anna adds her card ending 0002 and the processor declines the pre-authorization,
         then the card ending 4242 stays Anna's current card
        """
    )
    // @formatter:on
    @Test
    void e3_2_declined_card_keeps_current_card() {
        // given
        var anna = accounts.activated(annaEmail, visa4242);

        // when
        var addition = accounts.addPaymentMethod(anna, declined0002);

        // then
        assertThat(addition.verified()).isFalse();
        assertThat(accounts.statusOf(anna).activated()).isTrue();
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(payments.paymentMethodsOf(anna).currentCardToken()).isEqualTo(visa4242));
    }
}
