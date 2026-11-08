package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.FluentLanguage.Through;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/// A bike as Fleet Maintenance sees it: broken (and why), under maintenance, or in circulation.
@AggregateRoot
public class Bike {

    private final BikeId id;
    private final Version version; // the version this bike was loaded at; commands never change it
    private BikeCondition condition; // needed by: every rule (duplicate reports, only broken reserved, returns after maintenance)
    private Optional<FaultSource> faultSource; // shown to technicians; a duplicate report keeps the first one
    private Optional<FaultDescription> faultDescription;
    private Optional<Instant> reportedAt;
    private List<IncidentId> incidents; // needed by: a fixed bike resolves its linked fleet issues
    private Optional<TechnicianReservation> reservation; // needed by: an expired / failed reservation is released
    private Optional<Maintenance> maintenance; // needed by: returns only after maintenance has started

    private Bike(BikeId id, Version version, BikeCondition condition, Optional<FaultSource> faultSource,
                 Optional<FaultDescription> faultDescription, Optional<Instant> reportedAt, List<IncidentId> incidents,
                 Optional<TechnicianReservation> reservation, Optional<Maintenance> maintenance) {
        this.id = id;
        this.version = version;
        this.condition = condition;
        this.faultSource = faultSource;
        this.faultDescription = faultDescription;
        this.reportedAt = reportedAt;
        this.incidents = List.copyOf(incidents);
        this.reservation = reservation;
        this.maintenance = maintenance;
    }

    /// A bike Fleet Maintenance never heard of: it isn't broken (Fleet Maintenance keeps no copy of the fleet).
    public static Bike unknown(BikeId id) {
        return new Bike(id, Version.initial(), BikeCondition.IN_CIRCULATION, Optional.empty(), Optional.empty(),
                        Optional.empty(), List.of(), Optional.empty(), Optional.empty());
    }

    /// A fault report. A bike that isn't broken becomes broken (and is withdrawn, unless Rentals already did it); a
    /// broken bike stays as it is, except that a new fleet-issue incident is linked to it.
    public At<Instant, DomainEvents> reportFault(FaultReport report) {
        return reportedAt -> {
            if (condition != BikeCondition.IN_CIRCULATION) {
                return linkIncidentOf(report, reportedAt);
            }
            condition = BikeCondition.BROKEN;
            faultSource = Optional.of(report.source());
            faultDescription = report.description();
            this.reportedAt = Optional.of(reportedAt);
            incidents = report.incident().stream().toList();
            reservation = Optional.empty();
            maintenance = Optional.empty();
            var events = DomainEvents.of(new BikeEvent.BikeReportedBroken(id, report.source(), report.description(),
                                                                          report.incident(), report.technician(), reportedAt));
            if (!report.source().withdrawnByRentals()) {
                events = events.append(DomainEvents.of(new BikeEvent.WithdrawalRequested(id, reportedAt)));
            }
            return events;
        };
    }

    private DomainEvents linkIncidentOf(FaultReport report, Instant linkedAt) {
        if (report.incident().isEmpty() || incidents.contains(report.incident().get())) {
            return DomainEvents.empty();
        }
        var incident = report.incident().get();
        incidents = java.util.stream.Stream.concat(incidents.stream(), java.util.stream.Stream.of(incident)).toList();
        return DomainEvents.of(new BikeEvent.IncidentLinked(id, incident, linkedAt));
    }

    /// Only a broken bike waiting for a technician is reserved, through Rentals, until the time Rentals answers.
    public Through<ServiceReservations, DomainEvents> reserveFor(TechnicianId technician) {
        return serviceReservations -> {
            requireWaitingForTechnician();
            var until = serviceReservations.reserve(id, technician);
            reservation = Optional.of(new TechnicianReservation(technician, until));
            return DomainEvents.of(new BikeEvent.BikeReservedForTechnician(id, technician, until));
        };
    }

    /// Only a broken bike waiting for a technician is unlocked, through Rentals. Maintenance starts when Rentals
    /// reports the unlock, not here.
    public Through<ServiceUnlocks, ServiceUnlocks.Outcome> unlockFor(TechnicianId technician) {
        return serviceUnlocks -> {
            requireWaitingForTechnician();
            return serviceUnlocks.unlock(id, technician);
        };
    }

    /// Rentals reported the technician's unlock: maintenance starts.
    public At<Instant, DomainEvents> startMaintenanceBy(TechnicianId technician) {
        return startedAt -> {
            if (condition != BikeCondition.BROKEN) {
                return DomainEvents.empty(); // already under maintenance (repeated report) or not broken at all
            }
            condition = BikeCondition.UNDER_MAINTENANCE;
            reservation = Optional.empty();
            maintenance = Optional.of(new Maintenance(technician, startedAt));
            return DomainEvents.of(new BikeEvent.MaintenanceStarted(id, technician, startedAt));
        };
    }

    /// Rentals reported that the technician's reservation ended without an unlock (expired, unlock failed): the
    /// bike stays broken and any technician may reserve it.
    public At<Instant, DomainEvents> releaseReservationOf(TechnicianId technician) {
        return releasedAt -> {
            if (reservation.isEmpty() || !reservation.get().technician().equals(technician)) {
                return DomainEvents.empty();
            }
            reservation = Optional.empty();
            return DomainEvents.of(new BikeEvent.TechnicianReservationReleased(id, technician, releasedAt));
        };
    }

    /// A technician returns the bike to circulation: only after maintenance started; Rentals must make it available
    /// first, then the bike is fixed.
    public Through<CirculationReturns, DomainEvents> returnToCirculation(CirculationReturn circulationReturn) {
        return circulationReturns -> {
            if (condition == BikeCondition.IN_CIRCULATION) {
                throw new NotBroken("Bike " + id.value() + " isn't broken");
            }
            if (condition != BikeCondition.UNDER_MAINTENANCE) {
                throw new MaintenanceNotStarted("Maintenance on bike " + id.value() + " hasn't started: unlock it first");
            }
            circulationReturns.makeAvailable(id, circulationReturn.place());
            condition = BikeCondition.IN_CIRCULATION;
            reservation = Optional.empty();
            maintenance = Optional.empty();
            return DomainEvents.of(new BikeEvent.BikeFixed(id, circulationReturn.technician(), circulationReturn.place(),
                                                           circulationReturn.returnedAt(), incidents));
        };
    }

    private void requireWaitingForTechnician() {
        if (condition == BikeCondition.IN_CIRCULATION) {
            throw new NotBroken("Bike " + id.value() + " isn't broken: technicians take only broken bikes");
        }
        if (condition == BikeCondition.UNDER_MAINTENANCE) {
            throw new UnderMaintenance("Bike " + id.value() + " is already under maintenance");
        }
    }

    @Identity
    public BikeId id() {
        return id;
    }

    public Version version() {
        return version;
    }

    public BikeCondition condition() {
        return condition;
    }

    public Optional<FaultSource> faultSource() {
        return faultSource;
    }

    public Optional<FaultDescription> faultDescription() {
        return faultDescription;
    }

    public Optional<Instant> reportedAt() {
        return reportedAt;
    }

    public List<IncidentId> incidents() {
        return incidents;
    }

    public Optional<TechnicianReservation> reservation() {
        return reservation;
    }

    public Optional<Maintenance> maintenance() {
        return maintenance;
    }

    // Persistence support (memento), not used by domain logic.

    public record Snapshot(BikeId id, Version version, BikeCondition condition, Optional<FaultSource> faultSource,
                           Optional<FaultDescription> faultDescription, Optional<Instant> reportedAt,
                           List<IncidentId> incidents, Optional<TechnicianReservation> reservation,
                           Optional<Maintenance> maintenance) {

        public Snapshot withVersion(Version newVersion) {
            return new Snapshot(id, newVersion, condition, faultSource, faultDescription, reportedAt, incidents,
                                reservation, maintenance);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(id, version, condition, faultSource, faultDescription, reportedAt, incidents, reservation,
                            maintenance);
    }

    public static Bike restore(Snapshot snapshot) {
        return new Bike(snapshot.id(), snapshot.version(), snapshot.condition(), snapshot.faultSource(),
                        snapshot.faultDescription(), snapshot.reportedAt(), snapshot.incidents(), snapshot.reservation(),
                        snapshot.maintenance());
    }
}
