package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Port for the canvas outbound command **Charge Rider** to Payments ("Charging" relationship in the CML).
///
/// Conformist by the user's decision: there is no anti-corruption layer, and this port is meant to speak Payments'
/// published language. Any change in Payments' charging API changes this port and every caller in Settlement.
@SecondaryPort
public interface PaymentsCharging {

    ChargeResult charge(ChargeRider command);
}
