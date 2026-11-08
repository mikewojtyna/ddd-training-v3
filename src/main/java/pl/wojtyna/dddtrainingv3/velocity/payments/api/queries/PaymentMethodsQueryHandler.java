package pl.wojtyna.dddtrainingv3.velocity.payments.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.List;

/// Answers the rider's payment methods; an unknown rider has none.
@PrimaryPort
public class PaymentMethodsQueryHandler {

    private final PaymentMethodsReadModel readModel;

    public PaymentMethodsQueryHandler(PaymentMethodsReadModel readModel) {
        this.readModel = readModel;
    }

    public RiderPaymentMethodsView handle(PaymentMethodsQuery query) {
        return readModel.paymentMethodsOf(query.riderId())
                        .orElseGet(() -> new RiderPaymentMethodsView(query.riderId(), null, List.of()));
    }
}
