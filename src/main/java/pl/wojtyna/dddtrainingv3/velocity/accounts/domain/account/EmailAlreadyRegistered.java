package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

/// An email address that already has an account can't be registered again (DE-98).
public class EmailAlreadyRegistered extends RuntimeException {

    public EmailAlreadyRegistered(String message) {
        super(message);
    }
}
