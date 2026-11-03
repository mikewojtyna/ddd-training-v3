package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.Circulation;

/// Stub: Rentals has no withdraw / make-available use case in its `api` yet.
@SecondaryAdapter
public class RentalsCirculationAdapter implements Circulation {

    @Override
    public void takeOut(BikeId bike) {
        // Canvas outbound command: "Withdraw Bike" -> Rentals
        throw new UnsupportedOperationException("implement this");
    }

    @Override
    public void putBack(BikeId bike) {
        // Canvas outbound command: "Make Bike Available (back in circulation)" -> Rentals
        throw new UnsupportedOperationException("implement this");
    }
}
