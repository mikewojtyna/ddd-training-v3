package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.Optional;

/// A report that a bike is broken: from Rentals (it withdrew the bike), from a fleet-issue incident, or from a
/// technician.
@ValueObject
public record FaultReport(FaultSource source, Optional<FaultDescription> description, Optional<IncidentId> incident,
                          Optional<TechnicianId> technician) {

    public FaultReport {
        Objects.requireNonNull(source, "Fault source is required");
        Objects.requireNonNull(description);
        Objects.requireNonNull(incident);
        Objects.requireNonNull(technician);
        if (source == FaultSource.FLEET_ISSUE && incident.isEmpty()) {
            throw new IllegalArgumentException("A fleet-issue report names its incident");
        }
        if (source == FaultSource.TECHNICIAN && (technician.isEmpty() || description.isEmpty())) {
            throw new IllegalArgumentException("A technician's report names the technician and describes the fault");
        }
    }

    public static FaultReport fromRentals(FaultSource source, Optional<FaultDescription> description) {
        if (!source.withdrawnByRentals()) {
            throw new IllegalArgumentException(source + " is not a Rentals withdrawal reason");
        }
        return new FaultReport(source, description, Optional.empty(), Optional.empty());
    }

    public static FaultReport fromFleetIssue(IncidentId incident, FaultDescription description) {
        return new FaultReport(FaultSource.FLEET_ISSUE, Optional.of(description), Optional.of(incident), Optional.empty());
    }

    public static FaultReport fromTechnician(TechnicianId technician, FaultDescription description) {
        return new FaultReport(FaultSource.TECHNICIAN, Optional.of(description), Optional.empty(), Optional.of(technician));
    }
}
