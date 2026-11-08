package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

/// Who or what reported the bike broken.
@ValueObject
public enum FaultSource {
    /// Rentals withdrew the bike because the rider reported a fault when returning it.
    FAULT_REPORTED_AT_RETURN(true),
    /// Rentals withdrew the bike because its lock didn't close.
    LOCK_FAILED(true),
    /// Rentals withdrew the bike because it signalled a fault.
    FAULT_SIGNALLED(true),
    /// Rentals withdrew the bike because it wasn't returned within the maximum rental time (DE-21).
    NOT_RETURNED(true),
    /// A rider's fleet-issue incident (DE-52).
    FLEET_ISSUE(false),
    /// A technician's fault report during a routine check (DE-62).
    TECHNICIAN(false);

    private final boolean withdrawnByRentals;

    FaultSource(boolean withdrawnByRentals) {
        this.withdrawnByRentals = withdrawnByRentals;
    }

    /// Rentals withdrew the bike itself before reporting it; otherwise Fleet Maintenance asks for the withdrawal.
    public boolean withdrawnByRentals() {
        return withdrawnByRentals;
    }
}
