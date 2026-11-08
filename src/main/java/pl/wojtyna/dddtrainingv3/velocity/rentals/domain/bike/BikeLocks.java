package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// The bikes' locks (hardware). Opening and closing either works or fails; a failure is a business outcome, not an
/// error.
@SecondaryPort
public interface BikeLocks {

    enum LockResult {
        SUCCEEDED,
        FAILED
    }

    LockResult open(BikeId bike);

    LockResult close(BikeId bike);
}
