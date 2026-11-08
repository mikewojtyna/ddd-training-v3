package pl.wojtyna.dddtrainingv3.velocity.payments.api.queries;

/// Query: the rider's payment methods, with the current card.
public record PaymentMethodsQuery(String riderId) {
}
