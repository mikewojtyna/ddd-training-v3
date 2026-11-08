package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// A rejected incident can be reopened only until the end of the 14th calendar day (DE-54, DE-56).
public class ReopeningWindowClosed extends RuntimeException {

    public ReopeningWindowClosed(String message) {
        super(message);
    }

    public ReopeningWindowClosed(String message, Throwable cause) {
        super(message, cause);
    }
}
