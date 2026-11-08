package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountUnblocked;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.BlockReason;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.messagingprovideracl.RecordingRiderNotifications;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.messagingprovideracl.RecordingRiderNotifications.SentNotification;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/// Acceptance tests of `docs/specs/finance-blocks-rider-with-invalid-card.md`: the processor's notification enters
/// Payments over REST, Payments publishes Payment Method Invalidated, and Accounts' asynchronous listener blocks the
/// rider. Every effect of that hand-off is awaited. E1.2 (a running ride continues) needs Rentals and comes later.
@VelocitySpringTest
@DisplayName("Spec finance-blocks-rider-with-invalid-card")
class FinanceBlocksRiderWithInvalidCardSpecTest {

    private static final Duration HAND_OFF = Duration.ofSeconds(10);

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;
    @Autowired
    private ApplicationEventMulticaster multicaster;
    @Autowired
    private RecordingRiderNotifications notifications;
    @Autowired
    private MutableClock clock;

    private AccountsDriver accounts;
    private PaymentsView payments;
    private final String suffix = UUID.randomUUID().toString();
    private final String annaEmail = "anna-" + suffix + "@example.com";
    private final String visa4242 = "tok_visa_4242_" + suffix;
    private final String mc5555 = "tok_mc_5555_" + suffix;

    @BeforeEach
    void setUp() {
        accounts = new AccountsDriver(restClient, urls, multicaster);
        payments = new PaymentsView(restClient, urls);
        clock.setTo("2026-11-18T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        accounts.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's account is activated, not blocked, and her current card ends 4242,
         when on 2026-11-20 at 08:00 the payment processor reports the card ending 4242 expired,
         then the card is invalidated, Anna has no current card and is blocked for invalid card
        """
    )
    // @formatter:on
    @Test
    void e1_1_current_card_invalidated_blocks_rider() {
        // given
        var anna = accounts.activated(annaEmail, visa4242);
        clock.setTo("2026-11-20T08:00:00Z");

        // when
        payments.processorReportsCardInvalidated(visa4242, "EXPIRED");

        // then
        assertThat(payments.paymentMethodsOf(anna).currentPaymentMethodId()).isNull();
        await().atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(accounts.statusOf(anna).blocks()).containsExactly("INVALID_CARD");
            assertThat(accounts.rentalEligibilityOf(anna)).isEqualTo(new AccountsDriver.Eligibility(anna, false, "BLOCKED"));
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E1.3 given Carla's card ending 7777 was verified on 2026-11-18 and she hasn't signed the user agreement,
         when on 2026-11-20 the payment processor reports the card ending 7777 expired,
         then Carla is blocked for invalid card, her account stays incomplete, and if she signs on 2026-11-21 she is activated but not eligible
        """
    )
    // @formatter:on
    @Test
    void e1_3_onboarding_rider_blocked_and_not_eligible_after_signing() {
        // given
        var visa7777 = "tok_visa_7777_" + suffix;
        var carla = accounts.cardVerified("carla-" + suffix + "@example.com", visa7777);
        clock.setTo("2026-11-20T08:00:00Z");

        // when
        payments.processorReportsCardInvalidated(visa7777, "EXPIRED");

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(accounts.statusOf(carla))
            .isEqualTo(new AccountsDriver.Status(carla, "SIGN_USER_AGREEMENT", false, List.of("INVALID_CARD"))));
        clock.setTo("2026-11-21T10:00:00Z");
        assertThat(accounts.signUserAgreement(carla)).isEqualTo(200);
        assertThat(accounts.statusOf(carla).activated()).isTrue();
        assertThat(accounts.rentalEligibilityOf(carla)).isEqualTo(new AccountsDriver.Eligibility(carla, false, "BLOCKED"));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna is not blocked,
         when she is blocked for invalid card at 08:00,
         then Anna receives one block notification saying her card is no longer usable and she should add a new card
        """
    )
    // @formatter:on
    @Test
    void e2_1_rider_gets_one_block_notification() {
        // given
        var anna = accounts.activated(annaEmail, visa4242);
        clock.setTo("2026-11-20T08:00:00Z");

        // when
        payments.processorReportsCardInvalidated(visa4242, "EXPIRED");

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(notifications.sentTo(anna)).singleElement().satisfies(sent -> {
            assertThat(sent.reason()).isEqualTo("INVALID_CARD");
            assertThat(sent.message()).contains("no longer usable").contains("Add a new card");
            assertThat(sent.sentAt()).isEqualTo(Instant.parse("2026-11-20T08:00:00Z"));
        }));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Ben is already blocked for outstanding debt,
         when at 09:00 the payment processor reports Ben's current card ending 1111 cancelled,
         then Ben is blocked for invalid card as well and receives a second block notification, for invalid card
        """
    )
    // @formatter:on
    @Test
    void e2_2_second_reason_sends_second_notification() {
        // given
        var visa1111 = "tok_visa_1111_" + suffix;
        var ben = accounts.activated("ben-" + suffix + "@example.com", visa1111);
        clock.setTo("2026-11-20T08:00:00Z");
        assertThat(accounts.blockForOutstandingDebt(ben)).isEqualTo(200);
        clock.setTo("2026-11-20T09:00:00Z");

        // when
        payments.processorReportsCardInvalidated(visa1111, "CANCELLED");

        // then
        await().atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(accounts.statusOf(ben).blocks()).containsExactly("INVALID_CARD", "OUTSTANDING_DEBT");
            assertThat(notifications.sentTo(ben)).extracting(SentNotification::reason)
                                                 .containsExactly("OUTSTANDING_DEBT", "INVALID_CARD");
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E2.3 given Anna was blocked for invalid card at 08:00 after the card ending 4242 was reported expired,
         when at 08:30 the payment processor reports the card ending 4242 expired again,
         then nothing changes and Anna gets no second notification
        """
    )
    // @formatter:on
    @Test
    void e2_3_repeated_invalidation_changes_nothing() {
        // given
        var anna = accounts.activated(annaEmail, visa4242);
        clock.setTo("2026-11-20T08:00:00Z");
        payments.processorReportsCardInvalidated(visa4242, "EXPIRED");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(notifications.sentTo(anna)).hasSize(1));
        clock.setTo("2026-11-20T08:30:00Z");

        // when
        payments.processorReportsCardInvalidated(visa4242, "EXPIRED");

        // then
        await().during(Duration.ofSeconds(1)).atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(notifications.sentTo(anna)).hasSize(1);
            assertThat(accounts.statusOf(anna).blocks()).containsExactly("INVALID_CARD");
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Anna replaced her card ending 4242 with the card ending 5555, which is now her current card,
         when the payment processor reports the card ending 4242 expired,
         then 4242 is recorded as invalid, Anna keeps 5555 as her current card, isn't blocked and gets no notification
        """
    )
    // @formatter:on
    @Test
    void e3_1_old_card_invalidated_changes_nothing_else() {
        // given
        var anna = accounts.activated(annaEmail, visa4242);
        accounts.addPaymentMethod(anna, mc5555);
        clock.setTo("2026-11-20T08:00:00Z");

        // when
        payments.processorReportsCardInvalidated(visa4242, "EXPIRED");

        // then
        var methods = payments.paymentMethodsOf(anna);
        assertThat(methods.currentCardToken()).isEqualTo(mc5555);
        assertThat(methods.paymentMethods()).extracting(PaymentsView.PaymentMethod::cardToken, PaymentsView.PaymentMethod::status)
                                            .contains(org.assertj.core.groups.Tuple.tuple(visa4242, "INVALIDATED"));
        await().during(Duration.ofSeconds(1)).atMost(HAND_OFF).untilAsserted(() -> {
            assertThat(accounts.statusOf(anna).blocks()).isEmpty();
            assertThat(notifications.sentTo(anna)).isEmpty();
            assertThat(accounts.rentalEligibilityOf(anna).eligible()).isTrue();
        });
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given Anna is blocked for invalid card only since 2026-11-20,
         when on 2026-11-21 Anna adds her card ending 5555 and it is verified,
         then 5555 is her current card, the invalid card block is lifted and Anna is eligible again (Account Unblocked, eligible: yes)
        """
    )
    // @formatter:on
    @Test
    void e4_1_verified_card_lifts_invalid_card_block() {
        // given
        var anna = blockedForInvalidCard(accounts.activated(annaEmail, visa4242), visa4242);
        clock.setTo("2026-11-21T10:00:00Z");

        // when
        var addition = accounts.addPaymentMethod(anna, mc5555);

        // then
        assertThat(addition.verified()).isTrue();
        assertThat(accounts.statusOf(anna).blocks()).isEmpty();
        assertThat(accounts.rentalEligibilityOf(anna)).isEqualTo(new AccountsDriver.Eligibility(anna, true, "ELIGIBLE"));
        assertThat(accounts.eventsOf(anna, AccountUnblocked.class)).containsExactly(
            new AccountUnblocked(new RiderId(anna), BlockReason.INVALID_CARD, true, Instant.parse("2026-11-21T10:00:00Z")));
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(payments.paymentMethodsOf(anna).currentCardToken()).isEqualTo(mc5555));
    }

    // @formatter:off
    @DisplayName(
        """
         E4.2 given Ben is blocked for invalid card and for outstanding debt,
         when Ben adds his card ending 2222 and it is verified,
         then the invalid card block is lifted and Ben stays blocked for outstanding debt (Account Unblocked, eligible: no)
        """
    )
    // @formatter:on
    @Test
    void e4_2_verified_card_keeps_debt_block() {
        // given
        var visa1111 = "tok_visa_1111_" + suffix;
        var ben = accounts.activated("ben-" + suffix + "@example.com", visa1111);
        accounts.blockForOutstandingDebt(ben);
        blockedForInvalidCard(ben, visa1111);
        clock.setTo("2026-11-21T10:00:00Z");

        // when
        var addition = accounts.addPaymentMethod(ben, "tok_visa_2222_" + suffix);

        // then
        assertThat(addition.verified()).isTrue();
        assertThat(accounts.statusOf(ben).blocks()).containsExactly("OUTSTANDING_DEBT");
        assertThat(accounts.rentalEligibilityOf(ben)).isEqualTo(new AccountsDriver.Eligibility(ben, false, "BLOCKED"));
        assertThat(accounts.eventsOf(ben, AccountUnblocked.class)).containsExactly(
            new AccountUnblocked(new RiderId(ben), BlockReason.INVALID_CARD, false, Instant.parse("2026-11-21T10:00:00Z")));
    }

    // @formatter:off
    @DisplayName(
        """
         E4.3 given Anna is blocked for invalid card,
         when Anna adds her card ending 0002 and its pre-authorization is declined,
         then Anna stays blocked for invalid card
        """
    )
    // @formatter:on
    @Test
    void e4_3_declined_card_keeps_block() {
        // given
        var anna = blockedForInvalidCard(accounts.activated(annaEmail, visa4242), visa4242);

        // when
        var addition = accounts.addPaymentMethod(anna, "tok_declined_0002_" + suffix);

        // then
        assertThat(addition.verified()).isFalse();
        assertThat(accounts.statusOf(anna).blocks()).containsExactly("INVALID_CARD");
        assertThat(accounts.eventsOf(anna, AccountUnblocked.class)).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         Contract: given Payments reports an invalidated card of a rider id Accounts doesn't know,
         when Rentals asks whether that rider may rent,
         then the answer is 200 with eligible=false and reason UNKNOWN_RIDER
        """
    )
    // @formatter:on
    @Test
    void unknown_rider_is_not_eligible() {
        // when
        var eligibility = accounts.rentalEligibilityOf("nobody-" + suffix);

        // then
        assertThat(eligibility).isEqualTo(new AccountsDriver.Eligibility("nobody-" + suffix, false, "UNKNOWN_RIDER"));
        assertThat(accounts.statusCodeOf("nobody-" + suffix)).isEqualTo(404);
    }

    private String blockedForInvalidCard(String riderId, String currentCard) {
        clock.setTo("2026-11-20T08:00:00Z");
        payments.processorReportsCardInvalidated(currentCard, "EXPIRED");
        await().atMost(HAND_OFF).untilAsserted(() -> assertThat(accounts.statusOf(riderId).blocks()).contains("INVALID_CARD"));
        return riderId;
    }
}
