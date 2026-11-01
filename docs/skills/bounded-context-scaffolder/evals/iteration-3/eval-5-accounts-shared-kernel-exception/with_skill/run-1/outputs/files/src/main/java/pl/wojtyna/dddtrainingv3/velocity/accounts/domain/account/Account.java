package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

import java.util.Objects;

/// A rider's VeloCity account, identified by the rider's borrower id. It carries at most one Account Block, a flag
/// that prevents renting.
///
/// DECISION D1 (2026-11-01): the identity is Rentals' internal `BorrowerId`, used as a shared kernel instead of a type
/// from Rentals' api. Deliberate, and excluded from `AccountsArchitectureTest`. See the plan.
@AggregateRoot
public class Account {

    private final BorrowerId id;
    private boolean blocked;

    private Account(BorrowerId id, boolean blocked) {
        this.id = Objects.requireNonNull(id, "id");
        this.blocked = blocked;
    }

    /// An active account: one without an Account Block.
    public static Account active(BorrowerId id) {
        return new Account(id, false);
    }

    /// Block Account: sets the Account Block and records Account Blocked.
    ///
    /// @throws AccountAlreadyBlocked if the account is already blocked
    public DomainEvents block() {
        if (blocked) {
            throw new AccountAlreadyBlocked(id);
        }
        blocked = true;
        return DomainEvents.of(new AccountEvent.AccountBlocked(id));
    }

    @Identity
    public BorrowerId id() {
        return id;
    }
}
