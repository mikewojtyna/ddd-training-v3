package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

/// Inbound command *Block Account*: `reason` is `INVALID_CARD` (from Payments' Payment Method Invalidated) or
/// `OUTSTANDING_DEBT` (from Settlement).
public record BlockAccount(String riderId, String reason) {
}
