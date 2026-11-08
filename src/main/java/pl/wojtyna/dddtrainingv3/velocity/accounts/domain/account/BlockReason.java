package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

/// Why a rider can't start rentals. An account can hold both reasons at once; they are placed and lifted
/// independently (DE-6).
public enum BlockReason {
    INVALID_CARD, OUTSTANDING_DEBT
}
