package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Tells Customer Support that the bike of a fleet-issue incident is fixed (Customer Support's *Resolve Fleet Issue*).
@SecondaryPort
public interface FleetIssueResolutions {

    /// Idempotent on Customer Support's side: an already resolved incident keeps its resolution (DE-55).
    ///
    /// @throws FleetIssueNotResolved if Customer Support didn't resolve it
    void resolve(IncidentId incident, BikeId bike);
}
