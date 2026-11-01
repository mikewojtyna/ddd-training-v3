package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;

import java.util.Objects;

/// The canvas outbound command **Charge Rider** to Payments.
///
/// Settlement is conformist to Payments, so this record is meant to mirror Payments' published command. Payments
/// doesn't exist in code yet, so this is a placeholder: when Payments publishes its API, replace this record with
/// Payments' own type instead of translating.
@ValueObject
public record ChargeRider(String rideId, String riderId, Money amount) {

    public ChargeRider {
        Objects.requireNonNull(rideId, "rideId");
        Objects.requireNonNull(riderId, "riderId");
        Objects.requireNonNull(amount, "amount");
    }
}
