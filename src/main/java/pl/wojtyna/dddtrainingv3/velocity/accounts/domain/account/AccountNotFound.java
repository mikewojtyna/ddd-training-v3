package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

/// There is no account for the rider.
public class AccountNotFound extends RuntimeException {

    public AccountNotFound(RiderId rider) {
        super("No account for rider " + rider.value());
    }
}
