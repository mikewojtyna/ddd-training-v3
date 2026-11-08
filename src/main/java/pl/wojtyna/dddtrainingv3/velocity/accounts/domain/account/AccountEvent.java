package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

import java.time.Instant;
import java.time.LocalDate;

/// What happens to a rider's account. None of these leaves Accounts yet: no context listens to them
/// (integration-contracts.md); Rentals asks the Rental Eligibility query instead.
public sealed interface AccountEvent extends DomainEvent {

    RiderId riderId();

    /// A person registered; the account is incomplete and waits for the email confirmation.
    record AccountRegistered(RiderId riderId, EmailAddress email, Instant registeredAt) implements AccountEvent {
    }

    record EmailConfirmed(RiderId riderId, EmailAddress email, Instant confirmedAt) implements AccountEvent {
    }

    record PhoneVerified(RiderId riderId, PhoneNumber phoneNumber, Instant verifiedAt) implements AccountEvent {
    }

    record PersonalDataProvided(RiderId riderId, String fullName, LocalDate dateOfBirth, String countryOfResidency,
                                Instant providedAt) implements AccountEvent {
    }

    /// Payments verified the card; the payment step is complete.
    record PaymentMethodAdded(RiderId riderId, PaymentMethodId paymentMethodId, Instant addedAt) implements AccountEvent {
    }

    /// Payments declined the card; the payment step stays open (DE-3).
    record PaymentMethodDeclined(RiderId riderId, CardToken cardToken, Instant declinedAt) implements AccountEvent {
    }

    /// The agreement was signed, which activates the account at once (DE-4).
    record AccountActivated(RiderId riderId, Instant agreementSignedAt, Instant activatedAt) implements AccountEvent {
    }

    record AccountBlocked(RiderId riderId, BlockReason reason, Instant blockedAt) implements AccountEvent {
    }

    /// One block reason was lifted; `eligibleNow` tells whether the rider may rent now (DE-94).
    record AccountUnblocked(RiderId riderId, BlockReason liftedReason, boolean eligibleNow,
                            Instant unblockedAt) implements AccountEvent {
    }
}
