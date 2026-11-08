package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.FluentLanguage.On;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.FluentLanguage.Using;

import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Set;

/// A rider's account. Guards: onboarding steps are completed in order (with the age limit, DE-2), signing activates
/// the account (DE-4), one block per reason (DE-94), and rental eligibility = activated and not blocked.
@AggregateRoot
public class Account {

    private static final int MINIMUM_AGE = 16; // DE-2

    private final RiderId riderId;
    private final Version version; // the version this account was registered or loaded at
    private final EmailAddress email; // carried by Email Confirmed
    private boolean emailConfirmed;   // needed by: onboarding steps are completed in order
    private PhoneNumber phone;        // needed by: onboarding steps are completed in order (null until verified)
    private boolean personalDataProvided; // needed by: onboarding steps are completed in order
    private PaymentMethodId paymentMethod; // needed by: onboarding order (null until a card was verified); latest verified card's reference (D7)
    private boolean activated;        // needed by: onboarding order, rental eligibility
    private final EnumSet<BlockReason> blocks = EnumSet.noneOf(BlockReason.class); // needed by: one block per reason, rental eligibility

    private Account(RiderId riderId, Version version, EmailAddress email) {
        this.riderId = riderId;
        this.version = version;
        this.email = email;
    }

    static Account registered(RiderId riderId, EmailAddress email) {
        return new Account(riderId, Version.initial(), email);
    }

    public OnboardingStep nextOnboardingStep() {
        if (!emailConfirmed) {
            return OnboardingStep.CONFIRM_EMAIL;
        }
        if (phone == null) {
            return OnboardingStep.VERIFY_PHONE;
        }
        if (!personalDataProvided) {
            return OnboardingStep.PROVIDE_PERSONAL_DATA;
        }
        if (paymentMethod == null) {
            return OnboardingStep.ADD_PAYMENT_METHOD;
        }
        return activated ? OnboardingStep.NONE : OnboardingStep.SIGN_USER_AGREEMENT;
    }

    public boolean isActivated() {
        return activated;
    }

    /// Following the emailed link again changes nothing (DE-98).
    public DomainEvents confirmEmail(Instant now) {
        if (emailConfirmed) {
            return DomainEvents.empty();
        }
        emailConfirmed = true;
        return DomainEvents.of(new AccountEvent.EmailConfirmed(riderId, email, now));
    }

    public At<Instant, DomainEvents> verifyPhone(PhoneNumber phone) {
        return now -> {
            requireNextStep(OnboardingStep.VERIFY_PHONE);
            this.phone = phone;
            return DomainEvents.of(new AccountEvent.PhoneVerified(riderId, phone, now));
        };
    }

    public On<LocalDate, At<Instant, DomainEvents>> providePersonalData(PersonalData data) {
        return today -> now -> {
            requireNextStep(OnboardingStep.PROVIDE_PERSONAL_DATA);
            if (sixteenthBirthday(data.dateOfBirth()).isAfter(today)) {
                throw new RiderTooYoung("A rider must be at least " + MINIMUM_AGE + " years old on " + today);
            }
            personalDataProvided = true;
            return DomainEvents.of(new AccountEvent.PersonalDataProvided(riderId, data.fullName(), data.dateOfBirth(),
                                                                         data.countryOfResidency(), now));
        };
    }

    /// Someone born on 29 February turns 16 on 1 March in a non-leap year (DE-98).
    private static LocalDate sixteenthBirthday(LocalDate dateOfBirth) {
        var birthday = dateOfBirth.plusYears(MINIMUM_AGE);
        return birthday.getDayOfMonth() < dateOfBirth.getDayOfMonth() ? birthday.plusDays(1) : birthday;
    }

    public At<Instant, Using<PaymentMethodVerifier, DomainEvents>> addPaymentMethod(CardToken card) {
        return now -> verifier -> {
            if (!personalDataProvided) { // a card can be added (or replaced) any time after the personal data (DE-5, plan D7)
                throw new OnboardingStepOutOfOrder("Rider " + riderId.value() + " can't add a payment method now, the next onboarding step is " + nextOnboardingStep());
            }
            var verification = verifier.verify(riderId, card);
            if (!verification.verified()) {
                return DomainEvents.of(new AccountEvent.PaymentMethodDeclined(riderId, card, now));
            }
            paymentMethod = verification.paymentMethod().orElseThrow();
            return DomainEvents.of(new AccountEvent.PaymentMethodAdded(riderId, paymentMethod, now))
                               .append(liftBlock(BlockReason.INVALID_CARD).at(now)); // a newly verified card lifts the card block (DE-6)
        };
    }

    /// Signing completes the last onboarding step and activates the account at once (DE-4).
    public DomainEvents signUserAgreement(Instant now) {
        requireNextStep(OnboardingStep.SIGN_USER_AGREEMENT);
        activated = true;
        return DomainEvents.of(new AccountEvent.AccountActivated(riderId, now, now));
    }

    /// Places a block; a reason already in place changes nothing (DE-94). A block is placed during onboarding too (DE-93).
    public At<Instant, DomainEvents> placeBlock(BlockReason reason) {
        return now -> blocks.add(reason)
            ? DomainEvents.of(new AccountEvent.AccountBlocked(riderId, reason, now))
            : DomainEvents.empty();
    }

    /// Lifts one block reason and only that one (DE-6); a reason not in place changes nothing.
    public At<Instant, DomainEvents> liftBlock(BlockReason reason) {
        return now -> blocks.remove(reason)
            ? DomainEvents.of(new AccountEvent.AccountUnblocked(riderId, reason, rentalEligibility().eligible(), now))
            : DomainEvents.empty();
    }

    public Set<BlockReason> blocks() {
        return Set.copyOf(blocks);
    }

    public RentalEligibility rentalEligibility() {
        if (!activated) {
            return RentalEligibility.ONBOARDING_INCOMPLETE;
        }
        return blocks.isEmpty() ? RentalEligibility.ELIGIBLE : RentalEligibility.BLOCKED;
    }

    private void requireNextStep(OnboardingStep step) {
        var next = nextOnboardingStep();
        if (next != step) {
            throw new OnboardingStepOutOfOrder("Rider " + riderId.value() + " can't " + step + " now, the next onboarding step is " + next);
        }
    }

    @Identity
    public RiderId riderId() {
        return riderId;
    }

    public Version version() {
        return version;
    }

    // --- persistence plumbing (memento): used by repositories only, never by domain logic ---

    public record Snapshot(RiderId riderId, Version version, EmailAddress email, boolean emailConfirmed,
                           PhoneNumber phone, boolean personalDataProvided, PaymentMethodId paymentMethod,
                           boolean activated, Set<BlockReason> blocks) {

        public Snapshot {
            blocks = Set.copyOf(blocks);
        }

        public Snapshot withVersion(Version newVersion) {
            return new Snapshot(riderId, newVersion, email, emailConfirmed, phone, personalDataProvided, paymentMethod,
                                activated, blocks);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(riderId, version, email, emailConfirmed, phone, personalDataProvided, paymentMethod, activated,
                            blocks);
    }

    public static Account restore(Snapshot snapshot) {
        var account = new Account(snapshot.riderId(), snapshot.version(), snapshot.email());
        account.emailConfirmed = snapshot.emailConfirmed();
        account.phone = snapshot.phone();
        account.personalDataProvided = snapshot.personalDataProvided();
        account.paymentMethod = snapshot.paymentMethod();
        account.activated = snapshot.activated();
        account.blocks.addAll(snapshot.blocks());
        return account;
    }
}
