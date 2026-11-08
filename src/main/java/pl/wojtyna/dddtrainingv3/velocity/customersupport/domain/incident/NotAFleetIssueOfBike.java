package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// Only a fleet-issue incident about the fixed bike is resolved because the bike is fixed (DE-55).
public class NotAFleetIssueOfBike extends RuntimeException {

    public NotAFleetIssueOfBike(String message) {
        super(message);
    }

    public NotAFleetIssueOfBike(String message, Throwable cause) {
        super(message, cause);
    }
}
