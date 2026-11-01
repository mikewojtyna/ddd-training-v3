package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl;

import pl.wojtyna.dddtrainingv3.common.integration.IntegrationEvent;

/**
 * Placeholders for Settlement's published language. Replace them with Settlement's {@code api/events} types once the
 * Settlement context exists.
 */
public interface SettlementPublishedLanguage {

    record RentalFeeUnpaid(String riderId) implements IntegrationEvent {
    }

    record OutstandingDebtCleared(String riderId) implements IntegrationEvent {
    }
}
