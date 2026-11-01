package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

public class AccountAlreadyBlockedException extends RuntimeException {

    public AccountAlreadyBlockedException(String message) {
        super(message);
    }

    public AccountAlreadyBlockedException(String message, Throwable cause) {
        super(message, cause);
    }
}
