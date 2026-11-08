package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

/// Customer Support didn't resolve a linked fleet-issue incident.
public class FleetIssueNotResolved extends RuntimeException {

    public FleetIssueNotResolved(String message) {
        super(message);
    }

    public FleetIssueNotResolved(String message, Throwable cause) {
        super(message, cause);
    }
}
