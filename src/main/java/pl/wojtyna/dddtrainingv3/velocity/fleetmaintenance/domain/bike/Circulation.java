package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Taking bikes out of circulation and putting them back, in Fleet Maintenance's language.
/// Rentals carries this out; the translation to its *withdraw / make available* lives in `rentalsctxacl`.
@SecondaryPort
public interface Circulation {

    /// Canvas outbound command *Withdraw Bike*. Not called yet: when it's sent is open question Q3 in the plan.
    void takeOut(BikeId bike);

    /// Canvas outbound command *Make Bike Available (back in circulation)*.
    void putBack(BikeId bike);
}
