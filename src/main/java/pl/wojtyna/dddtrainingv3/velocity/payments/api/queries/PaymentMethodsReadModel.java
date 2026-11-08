package pl.wojtyna.dddtrainingv3.velocity.payments.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

/// Read model behind `PaymentMethodsQueryHandler`.
@SecondaryPort
public interface PaymentMethodsReadModel {

    Optional<RiderPaymentMethodsView> paymentMethodsOf(String riderId);
}
