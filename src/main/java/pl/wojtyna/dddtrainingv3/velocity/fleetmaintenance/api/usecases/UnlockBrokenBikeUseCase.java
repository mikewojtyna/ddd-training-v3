package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Bikes;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.ServiceUnlocks;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.TechnicianId;

/// A technician unlocks the broken bike they reserved, through Rentals. Nothing is recorded here: maintenance starts
/// when Rentals reports the unlock (plan technician-starts-maintenance D5).
@PrimaryPort
public class UnlockBrokenBikeUseCase {

    private final Bikes bikes;
    private final ServiceUnlocks serviceUnlocks;

    public UnlockBrokenBikeUseCase(Bikes bikes, ServiceUnlocks serviceUnlocks) {
        this.bikes = bikes;
        this.serviceUnlocks = serviceUnlocks;
    }

    public UnlockResult handle(UnlockBrokenBike command) {
        var bike = bikes.loadOrUnknown(new BikeId(command.bikeId()));
        var outcome = bike.unlockFor(new TechnicianId(command.technicianId())).through(serviceUnlocks);
        return new UnlockResult(command.bikeId(), outcome.name());
    }
}
