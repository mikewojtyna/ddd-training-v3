package pl.wojtyna.dddtrainingv3.velocity.settlement.api.events;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

import java.time.Instant;

/**
 * Published for DebtRecovery when all charge attempts for a ride have failed and the unpaid amount became outstanding
 * debt.
 */
@ValueObject
public record DebtRecorded(String settlementId, String rideId, String riderId, Money outstandingDebt,
                           Instant recordedAt) implements IntegrationEvent {
}
