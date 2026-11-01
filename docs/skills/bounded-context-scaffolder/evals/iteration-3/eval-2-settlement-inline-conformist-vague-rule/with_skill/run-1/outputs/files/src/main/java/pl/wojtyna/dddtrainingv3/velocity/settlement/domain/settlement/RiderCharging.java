package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Outbound canvas command "Charge Rider" to Payments.
///
/// Conformist to Payments (plan decision D1): `ChargeRider` and `ChargeResult` stand for Payments' published types.
/// Payments isn't in code yet, so they are temporary local records shaped after the canvas message (plan Q3, option b).
/// When Payments publishes its `api`, they are replaced by its types, which is a breaking change inside Settlement by design.
@SecondaryPort
public interface RiderCharging {

    ChargeResult charge(ChargeRider command);
}
