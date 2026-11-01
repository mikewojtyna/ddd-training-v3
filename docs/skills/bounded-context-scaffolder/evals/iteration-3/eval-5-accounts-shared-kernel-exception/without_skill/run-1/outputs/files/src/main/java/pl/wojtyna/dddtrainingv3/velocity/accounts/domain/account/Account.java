package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
// Shared kernel with Rentals: BorrowerId is used directly, on purpose. See this context's package-info.java.
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

import java.util.Objects;

/**
 * A rider's VeloCity account, identified by the rider's borrower id.
 */
@AggregateRoot
public class Account {

    private final BorrowerId id;
    private AccountStatus status;

    private Account(BorrowerId id, AccountStatus status) {
        this.id = Objects.requireNonNull(id, "Account must be identified by a borrower id");
        this.status = Objects.requireNonNull(status, "Account must have a status");
    }

    public static Account active(BorrowerId id) {
        return new Account(id, AccountStatus.ACTIVE);
    }

    /**
     * Puts an Account Block on the account, which prevents the rider from renting.
     */
    public DomainEvents block() {
        if (isBlocked()) {
            throw new AccountAlreadyBlockedException("Account %s is already blocked".formatted(id.value()));
        }
        status = AccountStatus.BLOCKED;
        return DomainEvents.of(new AccountEvent.AccountBlocked(id));
    }

    public boolean isBlocked() {
        return status == AccountStatus.BLOCKED;
    }

    @Identity
    public BorrowerId id() {
        return id;
    }

    public AccountStatus status() {
        return status;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Account other && Objects.equals(id, other.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
