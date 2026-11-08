package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

/// Inbound command *Unblock Account* (Settlement, reason `OUTSTANDING_DEBT`). The *invalid card* block is lifted
/// only by a newly verified card (DE-6), never by this command from outside.
public record UnblockAccount(String riderId, String reason) {
}
