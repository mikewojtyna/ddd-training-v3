package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

import java.time.Instant;

/// What happens to a rider's payment methods and charges.
public sealed interface RiderPaymentMethodsEvent extends DomainEvent {

    RiderId riderId();

    /// The pre-authorization succeeded; the card is the rider's current card.
    record PaymentMethodVerified(RiderId riderId, PaymentMethodId paymentMethodId, CardToken cardToken,
                                 Instant verifiedAt) implements RiderPaymentMethodsEvent {
    }

    /// The processor declined the pre-authorization; the card isn't verified.
    record PaymentMethodDeclined(RiderId riderId, PaymentMethodId paymentMethodId, CardToken cardToken,
                                 Instant declinedAt) implements RiderPaymentMethodsEvent {
    }

    /// A newly verified card took over from the current one, which is no longer charged (DE-5).
    record PaymentMethodReplaced(RiderId riderId, PaymentMethodId replaced, PaymentMethodId replacedBy,
                                 Instant replacedAt) implements RiderPaymentMethodsEvent {
    }

    /// The rider's current card became unusable; the rider has no current card (DE-72).
    record PaymentMethodInvalidated(RiderId riderId, PaymentMethodId paymentMethodId, CardToken cardToken,
                                    InvalidationReason reason, Instant invalidatedAt) implements RiderPaymentMethodsEvent {
    }

    /// A card that isn't the current one became unusable; it is recorded and changes nothing else (DE-73).
    record FormerPaymentMethodInvalidated(RiderId riderId, PaymentMethodId paymentMethodId, CardToken cardToken,
                                          InvalidationReason reason, Instant invalidatedAt) implements RiderPaymentMethodsEvent {
    }

    /// The amount was taken from the rider's current card.
    record ChargeSucceeded(RiderId riderId, ChargeId chargeId, PaymentMethodId paymentMethodId, Money amount,
                           ChargeReference reference, Instant chargedAt) implements RiderPaymentMethodsEvent {
    }

    /// The charge failed, see `reason`.
    record ChargeFailed(RiderId riderId, ChargeId chargeId, Money amount, ChargeReference reference,
                        ChargeFailureReason reason, Instant failedAt) implements RiderPaymentMethodsEvent {
    }
}
