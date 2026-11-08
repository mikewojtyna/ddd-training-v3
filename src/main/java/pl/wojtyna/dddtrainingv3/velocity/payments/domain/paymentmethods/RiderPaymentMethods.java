package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.FluentLanguage.As;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.FluentLanguage.Because;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.FluentLanguage.ForReference;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.FluentLanguage.Using;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.ChargeFailed;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.ChargeSucceeded;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.FormerPaymentMethodInvalidated;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.PaymentMethodDeclined;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.PaymentMethodInvalidated;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.PaymentMethodReplaced;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsEvent.PaymentMethodVerified;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/// A rider's payment methods. Guards: a rider has at most one current card, the newest verified one (DE-5).
@AggregateRoot
public class RiderPaymentMethods {

    private static final Money PRE_AUTHORIZATION_AMOUNT = Money.of(CurrencyUnit.of("PLN"), 1); // DE-70

    private final RiderId riderId;
    private final Version version; // the version these payment methods were created or loaded at
    private final List<PaymentMethod> paymentMethods; // needed by: one current card, the newest verified one

    private RiderPaymentMethods(RiderId riderId, Version version, List<PaymentMethod> paymentMethods) {
        this.riderId = riderId;
        this.version = version;
        this.paymentMethods = new ArrayList<>(paymentMethods);
    }

    public static RiderPaymentMethods none(RiderId riderId) {
        return new RiderPaymentMethods(riderId, Version.initial(), List.of());
    }

    /// Verifies the card with a PLN 1.00 pre-authorization, released at once (DE-70).
    public As<PaymentMethodId, At<Instant, Using<PaymentProcessor, DomainEvents>>> verify(CardToken card) {
        return id -> now -> processor -> {
            var preAuthorization = processor.preAuthorize(card, PRE_AUTHORIZATION_AMOUNT);
            if (!preAuthorization.accepted()) {
                paymentMethods.add(new PaymentMethod(id, card, PaymentMethodStatus.DECLINED));
                return DomainEvents.of(new PaymentMethodDeclined(riderId, id, card, now));
            }
            processor.release(preAuthorization);
            var events = DomainEvents.of(new PaymentMethodVerified(riderId, id, card, now));
            var replaced = currentCard();
            if (replaced.isPresent()) {
                replaced.get().status = PaymentMethodStatus.REPLACED;
                events = events.append(DomainEvents.of(new PaymentMethodReplaced(riderId, replaced.get().id, id, now)));
            }
            paymentMethods.add(new PaymentMethod(id, card, PaymentMethodStatus.VERIFIED));
            return events;
        };
    }

    /// Charges the amount to the rider's current card (DE-71).
    public ForReference<ChargeReference, As<ChargeId, At<Instant, Using<PaymentProcessor, DomainEvents>>>> charge(Money amount) {
        return reference -> chargeId -> now -> processor -> {
            var currentCard = currentCard();
            if (currentCard.isEmpty()) {
                return DomainEvents.of(new ChargeFailed(riderId, chargeId, amount, reference, ChargeFailureReason.NO_VALID_PAYMENT_METHOD, now));
            }
            var current = currentCard.get();
            if (processor.charge(current.card, amount, reference) == ProcessorAnswer.DECLINED) {
                return DomainEvents.of(new ChargeFailed(riderId, chargeId, amount, reference, ChargeFailureReason.DECLINED, now));
            }
            return DomainEvents.of(new ChargeSucceeded(riderId, chargeId, current.id, amount, reference, now));
        };
    }

    /// The processor reported the card as no longer usable (DE-72).
    public Because<InvalidationReason, At<Instant, DomainEvents>> invalidate(CardToken card) {
        return reason -> now -> {
            var events = DomainEvents.empty();
            for (var method : paymentMethods) {
                if (method.card.equals(card) && method.status != PaymentMethodStatus.INVALIDATED) { // DE-94
                    events = events.append(method.invalidate(riderId, reason, now));
                }
            }
            return events;
        };
    }

    private Optional<PaymentMethod> currentCard() {
        return paymentMethods.stream().filter(method -> method.status == PaymentMethodStatus.VERIFIED).findFirst();
    }

    @Identity
    public RiderId riderId() {
        return riderId;
    }

    public Version version() {
        return version;
    }

    /// A card of the rider. Its status changes, so it is an entity inside the aggregate.
    @org.jmolecules.ddd.annotation.Entity
    private static final class PaymentMethod {

        private final PaymentMethodId id;
        private final CardToken card;
        private PaymentMethodStatus status;

        private PaymentMethod(PaymentMethodId id, CardToken card, PaymentMethodStatus status) {
            this.id = id;
            this.card = card;
            this.status = status;
        }

        private DomainEvents invalidate(RiderId riderId, InvalidationReason reason, Instant now) {
            var wasCurrent = status == PaymentMethodStatus.VERIFIED;
            status = PaymentMethodStatus.INVALIDATED;
            return wasCurrent
                ? DomainEvents.of(new PaymentMethodInvalidated(riderId, id, card, reason, now))
                : DomainEvents.of(new FormerPaymentMethodInvalidated(riderId, id, card, reason, now));
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof PaymentMethod that && id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }
    }

    // --- persistence plumbing (memento): used by repositories only, never by domain logic ---

    /// State of the rider's payment methods, in the order they were added.
    public record Snapshot(RiderId riderId, Version version, List<PaymentMethodSnapshot> paymentMethods) {

        public Snapshot {
            paymentMethods = List.copyOf(paymentMethods);
        }

        public Optional<PaymentMethodId> currentPaymentMethod() {
            return paymentMethods.stream()
                                 .filter(method -> method.status() == PaymentMethodStatus.VERIFIED)
                                 .map(PaymentMethodSnapshot::id)
                                 .findFirst();
        }

        public boolean holds(CardToken card) {
            return paymentMethods.stream().anyMatch(method -> method.card().equals(card));
        }
    }

    public record PaymentMethodSnapshot(PaymentMethodId id, CardToken card, PaymentMethodStatus status) {
    }

    public Snapshot snapshot() {
        return new Snapshot(riderId, version, paymentMethods.stream()
                                                            .map(method -> new PaymentMethodSnapshot(method.id, method.card, method.status))
                                                            .toList());
    }

    public static RiderPaymentMethods restore(Snapshot snapshot) {
        return new RiderPaymentMethods(snapshot.riderId(), snapshot.version(),
                                       snapshot.paymentMethods().stream()
                                               .map(method -> new PaymentMethod(method.id(), method.card(), method.status()))
                                               .toList());
    }
}
