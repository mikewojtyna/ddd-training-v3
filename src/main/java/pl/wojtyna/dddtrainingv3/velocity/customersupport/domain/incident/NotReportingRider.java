package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// Only the rider who reported an incident can reopen it.
public class NotReportingRider extends RuntimeException {

    public NotReportingRider(String message) {
        super(message);
    }

    public NotReportingRider(String message, Throwable cause) {
        super(message, cause);
    }
}
