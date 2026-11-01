package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

/// Domain events of the `Account` aggregate.
///
/// DECISION D1 (2026-11-01): carries Rentals' internal `BorrowerId` (shared kernel). See the plan.
public sealed interface AccountEvent extends DomainEvent {

    record AccountBlocked(BorrowerId borrowerId) implements AccountEvent {
    }
}
