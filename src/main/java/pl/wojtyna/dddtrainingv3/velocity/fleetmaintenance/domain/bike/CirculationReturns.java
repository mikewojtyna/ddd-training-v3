package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Asks Rentals to offer a fixed bike to riders again where the technician left it (Rentals' *Make Bike Available*).
@SecondaryPort
public interface CirculationReturns {

    /// @throws CirculationReturnRefused if Rentals won't make the bike available
    /// @throws UnknownStation if the station doesn't exist
    void makeAvailable(BikeId bike, Place place);
}
