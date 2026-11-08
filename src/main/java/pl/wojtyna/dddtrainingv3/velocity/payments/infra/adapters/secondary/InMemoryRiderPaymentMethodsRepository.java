package pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.queries.PaymentMethodsReadModel;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.queries.RiderPaymentMethodsView;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.queries.RiderPaymentMethodsView.PaymentMethodView;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.CardToken;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.PaymentMethodId;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethods;
import pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods.RiderPaymentMethodsRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Riders' payment methods in memory, with optimistic locking on `Version`. Also serves the payment-methods query.
@SecondaryAdapter
public class InMemoryRiderPaymentMethodsRepository implements RiderPaymentMethodsRepository, PaymentMethodsReadModel {

    private final Map<RiderId, RiderPaymentMethods.Snapshot> riders = new ConcurrentHashMap<>();

    @Override
    public Optional<RiderPaymentMethods> load(RiderId riderId) {
        return Optional.ofNullable(riders.get(riderId)).map(RiderPaymentMethods::restore);
    }

    @Override
    public List<RiderPaymentMethods> loadHolding(CardToken card) {
        return riders.values().stream()
                     .filter(snapshot -> snapshot.holds(card))
                     .map(RiderPaymentMethods::restore)
                     .toList();
    }

    @Override
    public void save(RiderPaymentMethods paymentMethods) {
        var snapshot = paymentMethods.snapshot();
        riders.compute(snapshot.riderId(), (riderId, stored) -> {
            var storedVersion = stored == null ? Version.initial() : stored.version();
            if (!storedVersion.equals(snapshot.version())) {
                throw new OptimisticLockingException("Payment methods of rider " + riderId.value() + " were modified concurrently");
            }
            return new RiderPaymentMethods.Snapshot(riderId, snapshot.version().next(), snapshot.paymentMethods());
        });
    }

    @Override
    public Optional<RiderPaymentMethodsView> paymentMethodsOf(String riderId) {
        return Optional.ofNullable(riders.get(new RiderId(riderId)))
                       .map(snapshot -> new RiderPaymentMethodsView(
                           riderId,
                           snapshot.currentPaymentMethod().map(PaymentMethodId::value).orElse(null),
                           snapshot.paymentMethods().stream()
                                   .map(method -> new PaymentMethodView(method.id().value(), method.card().value(), method.status().name()))
                                   .toList()));
    }
}
