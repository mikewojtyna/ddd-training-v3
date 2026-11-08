package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.FluentLanguage.Because;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.FluentLanguage.To;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.FluentLanguage.With;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.FluentLanguage.Within;

import java.time.Instant;
import java.util.Optional;

/// A rider's reported problem, worked by one support agent until it is resolved.
@AggregateRoot
public class Incident {

    private final IncidentId id;
    private final Version version; // the version this incident was reported or loaded at; commands never change it
    private final RiderId rider; // needed by: only the reporting rider reopens
    private final IncidentType type; // needed by: only a fleet issue is resolved because its bike is fixed
    private final Optional<BikeId> bike; // needed by: only a fleet issue of the fixed bike is resolved
    private IncidentStatus status; // needed by: every state rule (picked up once, decided once, escalated once, ...)
    private Optional<AgentId> assignedAgent; // needed by: only the assigned agent works it; reopened stays with them
    private Optional<ReopeningWindow> reopeningWindow; // needed by: reopen within 14 days; uncontested rejections
    private Optional<Team> team; // carried by Incident Resolved of an escalated incident
    private Optional<Resolution> resolution; // needed by: a resolved incident keeps its first resolution

    private Incident(IncidentId id, Version version, RiderId rider, IncidentType type, Optional<BikeId> bike,
                     IncidentStatus status, Optional<AgentId> assignedAgent, Optional<ReopeningWindow> reopeningWindow,
                     Optional<Team> team, Optional<Resolution> resolution) {
        this.id = id;
        this.version = version;
        this.rider = rider;
        this.type = type;
        this.bike = bike;
        this.status = status;
        this.assignedAgent = assignedAgent;
        this.reopeningWindow = reopeningWindow;
        this.team = team;
        this.resolution = resolution;
    }

    /// A rider reports an incident; its type is detected at once (DE-50).
    public static ReportedIncident report(IncidentId id, IncidentReport report, Instant reportedAt) {
        var type = IncidentType.detect(report.category(), report.bike());
        var incident = new Incident(id, Version.initial(), report.rider(), type, report.bike(), IncidentStatus.OPEN,
                                    Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        var events = DomainEvents.of(new IncidentEvent.IncidentReported(id, report.rider(), report.category(), type,
                                                                        report.description(), report.bike(), reportedAt));
        if (type == IncidentType.FLEET_ISSUE) {
            events = events.append(DomainEvents.of(
                new IncidentEvent.FleetIssueReported(id, report.bike().orElseThrow(), report.description(), reportedAt)));
        }
        return new ReportedIncident(incident, events);
    }

    /// A support agent takes an open incident; from then on it is assigned to them (DE-51).
    public At<Instant, DomainEvents> pickUpBy(AgentId agent) {
        return pickedUpAt -> {
            if (status == IncidentStatus.RESOLVED) {
                throw new IncidentAlreadyResolved("Incident " + id.value() + " is already resolved");
            }
            if (assignedAgent.isPresent()) {
                throw new IncidentAlreadyAssigned("Incident " + id.value() + " is already assigned to " + assignedAgent.get().value());
            }
            assignedAgent = Optional.of(agent);
            status = IncidentStatus.PICKED_UP;
            return DomainEvents.of(new IncidentEvent.IncidentPickedUp(id, agent, pickedUpAt));
        };
    }

    /// The assigned agent agrees the report needs action.
    public At<Instant, DomainEvents> acceptBy(AgentId agent) {
        return acceptedAt -> {
            requireAssigned(agent);
            if (status != IncidentStatus.PICKED_UP && status != IncidentStatus.REOPENED) {
                throw new IncidentAlreadyDecided("Incident " + id.value() + " is already " + status);
            }
            status = IncidentStatus.ACCEPTED;
            return DomainEvents.of(new IncidentEvent.IncidentAccepted(id, agent, acceptedAt));
        };
    }

    /// The assigned agent turns the report down, with a reason (DE-97); the rider may reopen it within the window.
    public Because<RejectionReason, Within<ReopeningWindow, DomainEvents>> rejectBy(AgentId agent) {
        return reason -> window -> {
            requireAssigned(agent);
            if (status == IncidentStatus.REOPENED) {
                throw new ReopenedIncidentCannotBeRejected("Incident " + id.value() + " was reopened: accept or resolve it");
            }
            if (status != IncidentStatus.PICKED_UP) {
                throw new IncidentAlreadyDecided("Incident " + id.value() + " is already " + status);
            }
            status = IncidentStatus.REJECTED;
            reopeningWindow = Optional.of(window);
            return DomainEvents.of(new IncidentEvent.IncidentRejected(id, rider, agent, reason, window.rejectedAt(), window.closesAt()));
        };
    }

    /// The assigned agent hands an accepted incident to the billing, fleet or safety team, at most once (DE-53, DE-97).
    public To<Team, At<Instant, DomainEvents>> escalateBy(AgentId agent) {
        return team -> escalatedAt -> {
            requireAssigned(agent);
            if (status != IncidentStatus.ACCEPTED) {
                throw new IncidentNotAccepted("Only an accepted incident is escalated; " + id.value() + " is " + status);
            }
            status = IncidentStatus.ESCALATED;
            this.team = Optional.of(team);
            return DomainEvents.of(new IncidentEvent.IncidentEscalated(id, agent, team, escalatedAt));
        };
    }

    /// The assigned agent resolves an accepted, escalated or reopened incident, recording the outcome.
    public With<Resolution, At<Instant, DomainEvents>> resolveBy(AgentId agent) {
        return resolution -> resolvedAt -> {
            requireAssigned(agent);
            if (status != IncidentStatus.ACCEPTED && status != IncidentStatus.ESCALATED && status != IncidentStatus.REOPENED) {
                throw new IncidentNotResolvable("Only an accepted, escalated or reopened incident is resolved; "
                                                + id.value() + " is " + status);
            }
            return resolve(Optional.of(agent), resolution, resolvedAt);
        };
    }

    /// DE-87: a rejection nobody contested is resolved once its reopening window has closed; otherwise nothing happens.
    public DomainEvents resolveIfUncontestedAt(Instant now) {
        if (status != IncidentStatus.REJECTED || reopeningWindow.orElseThrow().isOpenAt(now)) {
            return DomainEvents.empty();
        }
        return resolve(Optional.empty(), Resolution.REJECTION_NOT_CONTESTED, now);
    }

    private DomainEvents resolve(Optional<AgentId> agent, Resolution resolution, Instant resolvedAt) {
        status = IncidentStatus.RESOLVED;
        this.resolution = Optional.of(resolution);
        return DomainEvents.of(new IncidentEvent.IncidentResolved(id, rider, agent, resolution, team, resolvedAt));
    }

    /// DE-55: Fleet Maintenance fixed the incident's bike, so a fleet issue is resolved with "bike fixed" whatever
    /// state it is in; an already resolved incident keeps its resolution and nothing happens.
    public At<Instant, DomainEvents> resolveFleetIssueOf(BikeId fixedBike) {
        return resolvedAt -> {
            if (type != IncidentType.FLEET_ISSUE || !bike.equals(Optional.of(fixedBike))) {
                throw new NotAFleetIssueOfBike("Incident " + id.value() + " is not a fleet issue of bike " + fixedBike.value());
            }
            if (status == IncidentStatus.RESOLVED) {
                return DomainEvents.empty();
            }
            return resolve(Optional.empty(), Resolution.BIKE_FIXED, resolvedAt);
        };
    }

    /// The rider pushes back on a rejection; the incident stays with the agent who rejected it (DE-56).
    public At<Instant, DomainEvents> reopenBy(RiderId rider) {
        return reopenedAt -> {
            if (!this.rider.equals(rider)) {
                throw new NotReportingRider("Only the rider who reported incident " + id.value() + " can reopen it");
            }
            if (status != IncidentStatus.REJECTED) {
                throw new IncidentNotRejected("Incident " + id.value() + " is " + status + ", not rejected");
            }
            if (!reopeningWindow.orElseThrow().isOpenAt(reopenedAt)) {
                throw new ReopeningWindowClosed("Incident " + id.value() + " could be reopened only until "
                                                + reopeningWindow.get().closesAt());
            }
            status = IncidentStatus.REOPENED;
            return DomainEvents.of(new IncidentEvent.IncidentReopened(id, rider, assignedAgent.orElseThrow(), reopenedAt));
        };
    }

    private void requireAssigned(AgentId agent) {
        if (!assignedAgent.equals(Optional.of(agent))) {
            throw new NotAssignedAgent("Only the agent assigned to incident " + id.value() + " works it, not " + agent.value());
        }
    }

    @Identity
    public IncidentId id() {
        return id;
    }

    public Version version() {
        return version;
    }

    public IncidentType type() {
        return type;
    }

    public IncidentStatus status() {
        return status;
    }

    public Optional<AgentId> assignedAgent() {
        return assignedAgent;
    }

    public Optional<Team> team() {
        return team;
    }

    public Optional<Resolution> resolution() {
        return resolution;
    }

    /// A newly reported incident together with what happened when it was reported.
    public record ReportedIncident(Incident incident, DomainEvents events) {
    }

    // Persistence support (memento), not used by domain logic.

    public record Snapshot(IncidentId id, Version version, RiderId rider, IncidentType type, Optional<BikeId> bike,
                           IncidentStatus status, Optional<AgentId> assignedAgent,
                           Optional<ReopeningWindow> reopeningWindow, Optional<Team> team,
                           Optional<Resolution> resolution) {

        public Snapshot withVersion(Version newVersion) {
            return new Snapshot(id, newVersion, rider, type, bike, status, assignedAgent, reopeningWindow, team, resolution);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(id, version, rider, type, bike, status, assignedAgent, reopeningWindow, team, resolution);
    }

    public static Incident restore(Snapshot snapshot) {
        return new Incident(snapshot.id(), snapshot.version(), snapshot.rider(), snapshot.type(), snapshot.bike(),
                            snapshot.status(), snapshot.assignedAgent(), snapshot.reopeningWindow(), snapshot.team(),
                            snapshot.resolution());
    }
}
