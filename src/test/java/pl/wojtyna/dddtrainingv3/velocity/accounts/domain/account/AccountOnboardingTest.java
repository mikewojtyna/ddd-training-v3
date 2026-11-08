package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountActivated;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.EmailConfirmed;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.PaymentMethodAdded;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.PaymentMethodDeclined;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.PersonalDataProvided;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.PhoneVerified;

import java.time.Instant;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

/// Onboarding examples of `docs/specs/person-registers-with-velocity.md`, `rider-confirms-contact-details.md`,
/// `rider-provides-personal-data.md`, `rider-adds-payment-method.md` and `rider-signs-user-agreement.md`.
@DisplayName("Account onboarding examples")
class AccountOnboardingTest {

    private static final RiderId ANNA = new RiderId("anna");
    private static final EmailAddress ANNA_EMAIL = new EmailAddress("anna@example.com");
    private static final PhoneNumber ANNA_PHONE = new PhoneNumber("+48 600 100 200");
    private static final PersonalData ANNA_DATA = new PersonalData("Anna Nowak", LocalDate.parse("1995-04-12"), "Poland");
    private static final LocalDate NOV_8 = LocalDate.parse("2026-11-08");
    private static final CardToken VISA_4242 = new CardToken("tok_visa_4242");
    private static final CardToken MC_5555 = new CardToken("tok_mc_5555");
    private static final CardToken DECLINED_0002 = new CardToken("tok_declined_0002");
    private static final CardToken VISA_1111 = new CardToken("tok_visa_1111");
    private static final Instant AT_10_00 = Instant.parse("2026-11-08T10:00:00Z");
    private static final Instant AT_10_01 = Instant.parse("2026-11-08T10:01:00Z");
    private static final Instant AT_10_03 = Instant.parse("2026-11-08T10:03:00Z");
    private static final Instant AT_10_05 = Instant.parse("2026-11-08T10:05:00Z");
    private static final Instant AT_10_08 = Instant.parse("2026-11-08T10:08:00Z");
    private static final Instant AT_10_10 = Instant.parse("2026-11-08T10:10:00Z");
    private static final Instant AT_10_12 = Instant.parse("2026-11-08T10:12:00Z");
    private static final Instant AT_10_20 = Instant.parse("2026-11-08T10:20:00Z");

    private final FakePaymentMethodVerifier payments = new FakePaymentMethodVerifier();

    // --- person-registers-with-velocity ---

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna registered on 2026-11-08 at 10:00,
         when Anna looks at her onboarding progress at 10:01,
         then her account is incomplete and her next onboarding step is confirming her email
        """
    )
    // @formatter:on
    @Test
    void newAccountIsIncompleteAndWaitsForEmailConfirmation() {
        // given
        var account = registered();

        // when
        var nextStep = account.nextOnboardingStep();

        // then
        assertThat(nextStep).isEqualTo(OnboardingStep.CONFIRM_EMAIL);
        assertThat(account.isActivated()).isFalse();
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Anna registered on 2026-11-08 at 10:00 and hasn't confirmed her email,
         when Anna tries to provide her personal data at 10:05,
         then the step is rejected and her next onboarding step is still confirming her email
        """
    )
    // @formatter:on
    @Test
    void personalDataRejectedBeforeEmailConfirmed() {
        // given
        var account = registered();

        // when
        var thrown = catchThrowable(() -> account.providePersonalData(ANNA_DATA).on(NOV_8).at(AT_10_05));

        // then
        assertThat(thrown).isInstanceOf(OnboardingStepOutOfOrder.class);
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.CONFIRM_EMAIL);
    }

    // --- rider-confirms-contact-details ---

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna registered on 2026-11-08 at 10:00 with anna@example.com,
         when Anna follows the confirmation link at 10:03,
         then Anna's email is confirmed and her next onboarding step is verifying her phone (Email Confirmed)
        """
    )
    // @formatter:on
    @Test
    void riderConfirmsEmailWithLink() {
        // given
        var account = registered();

        // when
        var events = account.confirmEmail(AT_10_03);

        // then
        assertThat(events.stream()).containsExactly(new EmailConfirmed(ANNA, ANNA_EMAIL, AT_10_03));
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.VERIFY_PHONE);
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Anna confirmed anna@example.com at 10:03,
         when Anna follows the same confirmation link again at 10:20,
         then nothing changes and no second Email Confirmed is recorded
        """
    )
    // @formatter:on
    @Test
    void secondEmailConfirmationChangesNothing() {
        // given
        var account = registered();
        account.confirmEmail(AT_10_03);

        // when
        var events = account.confirmEmail(AT_10_20);

        // then
        assertThat(events.stream()).isEmpty();
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.VERIFY_PHONE);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna confirmed anna@example.com at 10:03,
         when Anna gives +48 600 100 200 and types the SMS code at 10:05,
         then Anna's phone is verified and her next onboarding step is providing personal data (Phone Verified)
        """
    )
    // @formatter:on
    @Test
    void riderVerifiesPhoneAfterEmail() {
        // given
        var account = registered();
        account.confirmEmail(AT_10_03);

        // when
        var events = account.verifyPhone(ANNA_PHONE).at(AT_10_05);

        // then
        assertThat(events.stream()).containsExactly(new PhoneVerified(ANNA, ANNA_PHONE, AT_10_05));
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.PROVIDE_PERSONAL_DATA);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Ben registered with ben@example.com and hasn't confirmed his email,
         when Ben gives +48 600 300 400 and types the SMS code,
         then the step is rejected and Ben's next onboarding step is still confirming his email
        """
    )
    // @formatter:on
    @Test
    void phoneVerificationRejectedBeforeEmailConfirmed() {
        // given
        var ben = RiderRegistrationFixture.register(new RiderId("ben"), new EmailAddress("ben@example.com"), AT_10_00);

        // when
        var thrown = catchThrowable(() -> ben.verifyPhone(new PhoneNumber("+48 600 300 400")).at(AT_10_05));

        // then
        assertThat(thrown).isInstanceOf(OnboardingStepOutOfOrder.class);
        assertThat(ben.nextOnboardingStep()).isEqualTo(OnboardingStep.CONFIRM_EMAIL);
    }

    // --- rider-provides-personal-data ---

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna verified her phone on 2026-11-08 at 10:05,
         when Anna provides "Anna Nowak", born 1995-04-12, residing in Poland, at 10:08,
         then her personal data is recorded and her next onboarding step is adding a payment method (Personal Data Provided)
        """
    )
    // @formatter:on
    @Test
    void riderWithVerifiedPhoneProvidesPersonalData() {
        // given
        var account = phoneVerified();

        // when
        var events = account.providePersonalData(ANNA_DATA).on(NOV_8).at(AT_10_08);

        // then
        assertThat(events.stream()).containsExactly(
            new PersonalDataProvided(ANNA, "Anna Nowak", LocalDate.parse("1995-04-12"), "Poland", AT_10_08));
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.ADD_PAYMENT_METHOD);
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Anna registered and confirmed her email but hasn't verified her phone,
         when Anna provides her personal data,
         then the step is rejected and her next onboarding step is still verifying her phone
        """
    )
    // @formatter:on
    @Test
    void personalDataRejectedBeforePhoneVerified() {
        // given
        var account = registered();
        account.confirmEmail(AT_10_03);

        // when
        var thrown = catchThrowable(() -> account.providePersonalData(ANNA_DATA).on(NOV_8).at(AT_10_08));

        // then
        assertThat(thrown).isInstanceOf(OnboardingStepOutOfOrder.class);
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.VERIFY_PHONE);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Ben verified his phone,
         when on 2026-11-08 Ben provides "Ben Kowalski", born 2010-11-09, residing in Poland,
         then the step is rejected because Ben is 15, and his next onboarding step is still providing personal data
        """
    )
    // @formatter:on
    @Test
    void fifteenYearOldIsRejected() {
        // given
        var ben = phoneVerified();

        // when
        var thrown = catchThrowable(() -> ben.providePersonalData(
            new PersonalData("Ben Kowalski", LocalDate.parse("2010-11-09"), "Poland")).on(NOV_8).at(AT_10_08));

        // then
        assertThat(thrown).isInstanceOf(RiderTooYoung.class);
        assertThat(ben.nextOnboardingStep()).isEqualTo(OnboardingStep.PROVIDE_PERSONAL_DATA);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Carla verified her phone,
         when on 2026-11-08 Carla provides "Carla Rossi", born 2010-11-08, residing in Poland,
         then her personal data is recorded because she turns 16 that day (Personal Data Provided)
        """
    )
    // @formatter:on
    @Test
    void riderTurningSixteenTodayIsAccepted() {
        // given
        var carla = phoneVerified();

        // when
        var events = carla.providePersonalData(
            new PersonalData("Carla Rossi", LocalDate.parse("2010-11-08"), "Poland")).on(NOV_8).at(AT_10_08);

        // then
        assertThat(events.hasOccurredEventOfType(PersonalDataProvided.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         DE-98 given Dan verified his phone and was born on 2084-02-29,
         when on 2100-02-28 (not a leap year) Dan provides his personal data,
         then the step is rejected: someone born on 29 February turns 16 on 1 March
        """
    )
    // @formatter:on
    @Test
    void riderBornOnLeapDayTurnsSixteenOnFirstOfMarch() {
        // given
        var dan = phoneVerified();
        var data = new PersonalData("Dan Leap", LocalDate.parse("2084-02-29"), "Poland");

        // when
        var onFebruary28 = catchThrowable(() -> dan.providePersonalData(data).on(LocalDate.parse("2100-02-28")).at(AT_10_08));
        var onMarch1 = dan.providePersonalData(data).on(LocalDate.parse("2100-03-01")).at(AT_10_08);

        // then
        assertThat(onFebruary28).isInstanceOf(RiderTooYoung.class);
        assertThat(onMarch1.hasOccurredEventOfType(PersonalDataProvided.class)).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Dieter verified his phone,
         when on 2026-11-08 Dieter provides "Dieter Weber", born 1988-02-03, residing in Germany,
         then his personal data is recorded (Personal Data Provided)
        """
    )
    // @formatter:on
    @Test
    void anyCountryOfResidencyIsAccepted() {
        // given
        var dieter = phoneVerified();

        // when
        var events = dieter.providePersonalData(
            new PersonalData("Dieter Weber", LocalDate.parse("1988-02-03"), "Germany")).on(NOV_8).at(AT_10_08);

        // then
        assertThat(events.hasOccurredEventOfType(PersonalDataProvided.class)).isTrue();
        assertThat(dieter.nextOnboardingStep()).isEqualTo(OnboardingStep.ADD_PAYMENT_METHOD);
    }

    // --- rider-adds-payment-method ---

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna provided her personal data at 10:08 and has no card,
         when Anna adds her card ending 4242 at 10:10 and Payments verifies it,
         then her payment step is complete and her next onboarding step is signing the user agreement
        """
    )
    // @formatter:on
    @Test
    void verifiedCardCompletesPaymentStep() {
        // given
        var account = personalDataProvided();

        // when
        var events = account.addPaymentMethod(VISA_4242).at(AT_10_10).using(payments);

        // then
        assertThat(events.stream()).containsExactly(
            new PaymentMethodAdded(ANNA, FakePaymentMethodVerifier.referenceOf(VISA_4242), AT_10_10));
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.SIGN_USER_AGREEMENT);
    }

    // @formatter:off
    @DisplayName(
        """
         E1.2 given Ben verified his phone but hasn't provided his personal data,
         when Ben adds his card ending 1111,
         then the step is rejected, Payments isn't asked and his next onboarding step is still providing personal data
        """
    )
    // @formatter:on
    @Test
    void cardRejectedBeforePersonalData() {
        // given
        var ben = phoneVerified();

        // when
        var thrown = catchThrowable(() -> ben.addPaymentMethod(VISA_1111).at(AT_10_10).using(payments));

        // then
        assertThat(thrown).isInstanceOf(OnboardingStepOutOfOrder.class);
        assertThat(payments.asked).isEmpty();
        assertThat(ben.nextOnboardingStep()).isEqualTo(OnboardingStep.PROVIDE_PERSONAL_DATA);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna provided her personal data and has no card,
         when Anna adds her card ending 0002 and Payments declines it,
         then the card isn't verified and her next onboarding step is still adding a payment method
        """
    )
    // @formatter:on
    @Test
    void declinedCardKeepsRiderAtPaymentStep() {
        // given
        var account = personalDataProvided();
        payments.declines(DECLINED_0002);

        // when
        var events = account.addPaymentMethod(DECLINED_0002).at(AT_10_10).using(payments);

        // then
        assertThat(events.stream()).containsExactly(new PaymentMethodDeclined(ANNA, DECLINED_0002, AT_10_10));
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.ADD_PAYMENT_METHOD);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Anna's card ending 0002 was declined at 10:10,
         when Anna adds her card ending 4242 at 10:12 and Payments verifies it,
         then her next onboarding step is signing the user agreement
        """
    )
    // @formatter:on
    @Test
    void secondCardVerifiedAfterDecline() {
        // given
        var account = personalDataProvided();
        payments.declines(DECLINED_0002);
        account.addPaymentMethod(DECLINED_0002).at(AT_10_10).using(payments);

        // when
        var events = account.addPaymentMethod(VISA_4242).at(AT_10_12).using(payments);

        // then
        assertThat(events.hasOccurredEventOfType(PaymentMethodAdded.class)).isTrue();
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.SIGN_USER_AGREEMENT);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1/E3.2 given Anna's account is activated and her current card ends 4242,
         when Anna adds her card ending 5555 (verified) or 0002 (declined),
         then her account stays activated (DE-5)
        """
    )
    // @formatter:on
    @Test
    void activatedRiderCanAddNewCard() {
        // given
        var account = activated();
        payments.declines(DECLINED_0002);

        // when
        var verified = account.addPaymentMethod(MC_5555).at(AT_10_20).using(payments);
        var declined = account.addPaymentMethod(DECLINED_0002).at(AT_10_20).using(payments);

        // then
        assertThat(verified.hasOccurredEventOfType(PaymentMethodAdded.class)).isTrue();
        assertThat(declined.hasOccurredEventOfType(PaymentMethodDeclined.class)).isTrue();
        assertThat(account.isActivated()).isTrue();
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.NONE);
    }

    // --- rider-signs-user-agreement ---

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna's card ending 4242 was verified on 2026-11-08 at 10:10,
         when Anna signs the user agreement at 10:12,
         then the signature is recorded and Anna's account is activated at 10:12 (Account Activated)
        """
    )
    // @formatter:on
    @Test
    void signingAfterVerifiedCardActivatesAccount() {
        // given
        var account = paymentStepCompleted();

        // when
        var events = account.signUserAgreement(AT_10_12);

        // then
        assertThat(events.stream()).containsExactly(new AccountActivated(ANNA, AT_10_12, AT_10_12));
        assertThat(account.isActivated()).isTrue();
        assertThat(account.nextOnboardingStep()).isEqualTo(OnboardingStep.NONE);
        assertThat(account.rentalEligibility()).isEqualTo(RentalEligibility.ELIGIBLE);
    }

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Ben provided his personal data and his only card ending 0002 was declined,
         when Ben signs the user agreement,
         then the step is rejected, Ben's account stays incomplete and his next step is still adding a payment method
        """
    )
    // @formatter:on
    @Test
    void signingRejectedWithoutVerifiedCard() {
        // given
        var ben = personalDataProvided();
        payments.declines(DECLINED_0002);
        ben.addPaymentMethod(DECLINED_0002).at(AT_10_10).using(payments);

        // when
        var thrown = catchThrowable(() -> ben.signUserAgreement(AT_10_12));

        // then
        assertThat(thrown).isInstanceOf(OnboardingStepOutOfOrder.class);
        assertThat(ben.isActivated()).isFalse();
        assertThat(ben.nextOnboardingStep()).isEqualTo(OnboardingStep.ADD_PAYMENT_METHOD);
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Carla's card ending 7777 was verified on 2026-11-01 and she stopped there,
         when Carla signs the user agreement on 2026-11-15,
         then Carla's account is activated on 2026-11-15 (Account Activated)
        """
    )
    // @formatter:on
    @Test
    void signingTwoWeeksLaterActivatesAccount() {
        // given
        var carla = paymentStepCompleted();
        var nov15 = Instant.parse("2026-11-15T09:00:00Z");

        // when
        var events = carla.signUserAgreement(nov15);

        // then
        assertThat(events.stream()).containsExactly(new AccountActivated(ANNA, nov15, nov15));
    }

    // @formatter:off
    @DisplayName(
        """
         Plan Q3 given Anna's account is activated,
         when Anna signs the user agreement again,
         then the step is rejected (no onboarding step is left)
        """
    )
    // @formatter:on
    @Test
    void signingTwiceIsRejected() {
        // given
        var account = activated();

        // when
        var thrown = catchThrowable(() -> account.signUserAgreement(AT_10_20));

        // then
        assertThat(thrown).isInstanceOf(OnboardingStepOutOfOrder.class);
    }

    // --- arrangement helpers: the account after each onboarding step ---

    private static Account registered() {
        return RiderRegistrationFixture.register(ANNA, ANNA_EMAIL, AT_10_00);
    }

    private static Account phoneVerified() {
        var account = registered();
        account.confirmEmail(AT_10_01);
        account.verifyPhone(ANNA_PHONE).at(AT_10_05);
        return account;
    }

    private static Account personalDataProvided() {
        var account = phoneVerified();
        account.providePersonalData(ANNA_DATA).on(NOV_8).at(AT_10_08);
        return account;
    }

    private Account paymentStepCompleted() {
        var account = personalDataProvided();
        account.addPaymentMethod(VISA_4242).at(AT_10_10).using(payments);
        return account;
    }

    private Account activated() {
        var account = paymentStepCompleted();
        account.signUserAgreement(AT_10_12);
        return account;
    }
}
