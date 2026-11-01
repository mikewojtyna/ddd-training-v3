package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

import org.jmolecules.ddd.annotation.ValueObject;

/**
 * Outcome of a {@link ChargeRider} command. A charge either succeeds or fails.
 */
public sealed interface ChargeResult {

    @ValueObject
    record Succeeded(String chargeId) implements ChargeResult {
    }

    @ValueObject
    record Failed(String reason) implements ChargeResult {
    }
}
