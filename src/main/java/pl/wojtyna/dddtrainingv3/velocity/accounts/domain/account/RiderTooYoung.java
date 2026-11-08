package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

/// A rider must be at least 16 years old on the day they provide their personal data (DE-2).
public class RiderTooYoung extends RuntimeException {

    public RiderTooYoung(String message) {
        super(message);
    }
}
