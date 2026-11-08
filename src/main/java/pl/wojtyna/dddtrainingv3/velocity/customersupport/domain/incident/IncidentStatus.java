package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

/// Where an incident is in its life.
public enum IncidentStatus {
    OPEN, PICKED_UP, ACCEPTED, REJECTED, ESCALATED, REOPENED, RESOLVED
}
