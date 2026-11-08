package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountActivated;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountBlocked;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountUnblocked;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.PaymentMethodAdded;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.PaymentMethodDeclined;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.BlockReason.INVALID_CARD;
import static pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.BlockReason.OUTSTANDING_DEBT;

/// Block examples of `docs/specs/finance-blocks-rider-with-invalid-card.md`, `finance-retries-failed-charge.md` and
/// `rider-pays-off-outstanding-debt.md` (Accounts' part), plus the canvas decision on rental eligibility.
@DisplayName("Account block and eligibility examples")
class AccountBlocksTest {

    private static final RiderId ANNA = new RiderId("anna");
    private static final CardToken VISA_4242 = new CardToken("tok_visa_4242");
    private static final CardToken MC_5555 = new CardToken("tok_mc_5555");
    private static final CardToken DECLINED_0002 = new CardToken("tok_declined_0002");
    private static final Instant NOV_18 = Instant.parse("2026-11-18T10:00:00Z");
    private static final Instant NOV_20_0800 = Instant.parse("2026-11-20T08:00:00Z");
    private static final Instant NOV_20_0830 = Instant.parse("2026-11-20T08:30:00Z");
    private static final Instant NOV_20_0900 = Instant.parse("2026-11-20T09:00:00Z");
    private static final Instant NOV_21 = Instant.parse("2026-11-21T10:00:00Z");

    private final FakePaymentMethodVerifier payments = new FakePaymentMethodVerifier();

    // @formatter:off
    @DisplayName(
        """
         Canvas: given Ben registered but stopped before signing the agreement,
         when Rentals asks whether Ben may rent,
         then the answer is no, onboarding incomplete
        """
    )
    // @formatter:on
    @Test
    void incompleteAccountIsNotEligible() {
        // given
        var ben = paymentStepCompleted();

        // when
        var eligibility = ben.rentalEligibility();

        // then
        assertThat(eligibility).isEqualTo(RentalEligibility.ONBOARDING_INCOMPLETE);
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's account is activated and not blocked,
         when on 2026-11-20 at 08:00 Payments reports her current card invalidated,
         then Anna is blocked for invalid card (Account Blocked) and isn't eligible to rent
        """
    )
    // @formatter:on
    @Test
    void invalidCardBlocksActivatedRider() {
        // given
        var anna = activated();

        // when
        var events = anna.placeBlock(INVALID_CARD).at(NOV_20_0800);

        // then
        assertThat(events.stream()).containsExactly(new AccountBlocked(ANNA, INVALID_CARD, NOV_20_0800));
        assertThat(anna.blocks()).containsExactly(INVALID_CARD);
        assertThat(anna.rentalEligibility()).isEqualTo(RentalEligibility.BLOCKED);
    }

    // @formatter:off
    @DisplayName(
        """
         E1.3 given Carla's card was verified on 2026-11-18 and she hasn't signed the user agreement,
         when on 2026-11-20 her payment method is invalidated,
         then Carla is blocked for invalid card, stays incomplete, and once she signs on 2026-11-21 she is activated but not eligible
        """
    )
    // @formatter:on
    @Test
    void onboardingRiderBlockedAndNotEligibleAfterSigning() {
        // given
        var carla = paymentStepCompleted();

        // when
        var blocked = carla.placeBlock(INVALID_CARD).at(NOV_20_0800);
        var signed = carla.signUserAgreement(NOV_21);

        // then
        assertThat(blocked.stream()).containsExactly(new AccountBlocked(ANNA, INVALID_CARD, NOV_20_0800));
        assertThat(signed.stream()).containsExactly(new AccountActivated(ANNA, NOV_21, NOV_21));
        assertThat(carla.isActivated()).isTrue();
        assertThat(carla.rentalEligibility()).isEqualTo(RentalEligibility.BLOCKED);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Ben is already blocked for outstanding debt,
         when at 09:00 his current card is invalidated,
         then Ben is blocked for invalid card as well (Account Blocked)
        """
    )
    // @formatter:on
    @Test
    void secondReasonIsPlacedNextToTheFirst() {
        // given
        var ben = activated();
        ben.placeBlock(OUTSTANDING_DEBT).at(NOV_20_0800);

        // when
        var events = ben.placeBlock(INVALID_CARD).at(NOV_20_0900);

        // then
        assertThat(events.stream()).containsExactly(new AccountBlocked(ANNA, INVALID_CARD, NOV_20_0900));
        assertThat(ben.blocks()).containsExactlyInAnyOrder(INVALID_CARD, OUTSTANDING_DEBT);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.3 (and finance-retries-failed-charge E3.1) given Anna was blocked for invalid card at 08:00,
         when the same block is placed again at 08:30,
         then nothing changes and no Account Blocked is recorded (DE-94)
        """
    )
    // @formatter:on
    @Test
    void sameReasonTwiceChangesNothing() {
        // given
        var anna = activated();
        anna.placeBlock(INVALID_CARD).at(NOV_20_0800);

        // when
        var events = anna.placeBlock(INVALID_CARD).at(NOV_20_0830);

        // then
        assertThat(events.stream()).isEmpty();
        assertThat(anna.blocks()).containsExactly(INVALID_CARD);
    }

    // @formatter:off
    @DisplayName(
        """
         E4.1 given Anna is blocked for invalid card only since 2026-11-20,
         when on 2026-11-21 Anna adds her card ending 5555 and Payments verifies it,
         then the invalid card block is lifted and Anna is eligible again (Account Unblocked naming invalid card, eligible: yes)
        """
    )
    // @formatter:on
    @Test
    void verifiedCardLiftsInvalidCardBlock() {
        // given
        var anna = activated();
        anna.placeBlock(INVALID_CARD).at(NOV_20_0800);

        // when
        var events = anna.addPaymentMethod(MC_5555).at(NOV_21).using(payments);

        // then
        assertThat(events.stream()).containsExactly(
            new PaymentMethodAdded(ANNA, FakePaymentMethodVerifier.referenceOf(MC_5555), NOV_21),
            new AccountUnblocked(ANNA, INVALID_CARD, true, NOV_21));
        assertThat(anna.rentalEligibility()).isEqualTo(RentalEligibility.ELIGIBLE);
    }

    // @formatter:off
    @DisplayName(
        """
         E4.2 given Ben is blocked for invalid card and for outstanding debt,
         when Ben adds his card ending 2222 and Payments verifies it,
         then the invalid card block is lifted and Ben stays blocked for outstanding debt (Account Unblocked, eligible: no)
        """
    )
    // @formatter:on
    @Test
    void verifiedCardKeepsDebtBlock() {
        // given
        var ben = activated();
        ben.placeBlock(INVALID_CARD).at(NOV_20_0800);
        ben.placeBlock(OUTSTANDING_DEBT).at(NOV_20_0900);
        var visa2222 = new CardToken("tok_visa_2222");

        // when
        var events = ben.addPaymentMethod(visa2222).at(NOV_21).using(payments);

        // then
        assertThat(events.hasAtLeastOneEventMatching(new AccountUnblocked(ANNA, INVALID_CARD, false, NOV_21)::equals)).isTrue();
        assertThat(ben.blocks()).containsExactly(OUTSTANDING_DEBT);
        assertThat(ben.rentalEligibility()).isEqualTo(RentalEligibility.BLOCKED);
    }

    // @formatter:off
    @DisplayName(
        """
         E4.3 given Anna is blocked for invalid card,
         when Anna adds her card ending 0002 and Payments declines it,
         then Anna stays blocked for invalid card
        """
    )
    // @formatter:on
    @Test
    void declinedCardKeepsBlock() {
        // given
        var anna = activated();
        anna.placeBlock(INVALID_CARD).at(NOV_20_0800);
        payments.declines(DECLINED_0002);

        // when
        var events = anna.addPaymentMethod(DECLINED_0002).at(NOV_21).using(payments);

        // then
        assertThat(events.stream()).containsExactly(new PaymentMethodDeclined(ANNA, DECLINED_0002, NOV_21));
        assertThat(anna.blocks()).containsExactly(INVALID_CARD);
    }

    // @formatter:off
    @DisplayName(
        """
         rider-pays-off-outstanding-debt E1.1 given Anna is blocked for outstanding debt only,
         when Settlement unblocks her for outstanding debt,
         then the block is lifted and Anna is eligible to rent (Account Unblocked naming outstanding debt, eligible: yes)
        """
    )
    // @formatter:on
    @Test
    void clearedDebtLiftsDebtBlock() {
        // given
        var anna = activated();
        anna.placeBlock(OUTSTANDING_DEBT).at(NOV_20_0800);

        // when
        var events = anna.liftBlock(OUTSTANDING_DEBT).at(NOV_21);

        // then
        assertThat(events.stream()).containsExactly(new AccountUnblocked(ANNA, OUTSTANDING_DEBT, true, NOV_21));
        assertThat(anna.rentalEligibility()).isEqualTo(RentalEligibility.ELIGIBLE);
    }

    // @formatter:off
    @DisplayName(
        """
         Canvas (DE-6) given Anna is blocked for invalid card and for outstanding debt,
         when Settlement unblocks Anna for outstanding debt,
         then Anna is still blocked for invalid card and isn't eligible to rent
        """
    )
    // @formatter:on
    @Test
    void debtUnblockDoesNotLiftCardBlock() {
        // given
        var anna = activated();
        anna.placeBlock(INVALID_CARD).at(NOV_20_0800);
        anna.placeBlock(OUTSTANDING_DEBT).at(NOV_20_0900);

        // when
        var events = anna.liftBlock(OUTSTANDING_DEBT).at(NOV_21);

        // then
        assertThat(events.stream()).containsExactly(new AccountUnblocked(ANNA, OUTSTANDING_DEBT, false, NOV_21));
        assertThat(anna.blocks()).containsExactly(INVALID_CARD);
        assertThat(anna.rentalEligibility()).isEqualTo(RentalEligibility.BLOCKED);
    }

    // @formatter:off
    @DisplayName(
        """
         rider-pays-off-outstanding-debt S4 given Anna isn't blocked for outstanding debt,
         when Settlement unblocks her for outstanding debt,
         then nothing changes and no Account Unblocked is recorded
        """
    )
    // @formatter:on
    @Test
    void liftingReasonNotInPlaceChangesNothing() {
        // given
        var anna = activated();

        // when
        var events = anna.liftBlock(OUTSTANDING_DEBT).at(NOV_21);

        // then
        assertThat(events.stream()).isEmpty();
        assertThat(anna.rentalEligibility()).isEqualTo(RentalEligibility.ELIGIBLE);
    }

    // --- arrangement helpers ---

    private Account paymentStepCompleted() {
        var account = RiderRegistrationFixture.register(ANNA, new EmailAddress("anna@example.com"), NOV_18);
        account.confirmEmail(NOV_18);
        account.verifyPhone(new PhoneNumber("+48 600 100 200")).at(NOV_18);
        account.providePersonalData(new PersonalData("Anna Nowak", LocalDate.parse("1995-04-12"), "Poland"))
               .on(LocalDate.parse("2026-11-18")).at(NOV_18);
        account.addPaymentMethod(VISA_4242).at(NOV_18).using(payments);
        return account;
    }

    private Account activated() {
        var account = paymentStepCompleted();
        account.signUserAgreement(NOV_18);
        return account;
    }
}
