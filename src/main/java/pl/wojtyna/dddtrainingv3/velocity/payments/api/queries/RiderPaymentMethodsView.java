package pl.wojtyna.dddtrainingv3.velocity.payments.api.queries;

import java.util.List;

/// Answer to `PaymentMethodsQuery`. `currentPaymentMethodId` is null when the rider has no current card; an unknown
/// rider has an empty list.
public record RiderPaymentMethodsView(String riderId, String currentPaymentMethodId, List<PaymentMethodView> paymentMethods) {

    /// One card; `status` is `VERIFIED` (the current card), `DECLINED`, `REPLACED` or `INVALIDATED`.
    public record PaymentMethodView(String paymentMethodId, String cardToken, String status) {
    }
}
