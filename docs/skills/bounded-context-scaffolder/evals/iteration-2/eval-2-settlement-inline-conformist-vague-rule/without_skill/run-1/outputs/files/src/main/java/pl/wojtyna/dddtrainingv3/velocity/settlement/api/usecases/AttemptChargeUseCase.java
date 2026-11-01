package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeRiderUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement.SettlementRepository;

import java.time.Clock;

/**
 * Makes one charge attempt for a settlement, if one is due. Settlement conforms to Payments, so it calls
 * Payments' {@link ChargeRiderUseCase} directly, without an anti-corruption layer.
 */
@PrimaryPort
public class AttemptChargeUseCase {

    private final SettlementRepository settlementRepository;
    private final ChargeRiderUseCase payments;
    private final DomainEventPublisher eventPublisher;
    private final Clock clock;

    public AttemptChargeUseCase(SettlementRepository settlementRepository, ChargeRiderUseCase payments,
                                DomainEventPublisher eventPublisher, Clock clock) {
        this.settlementRepository = settlementRepository;
        this.payments = payments;
        this.eventPublisher = eventPublisher;
        this.clock = clock;
    }

    public void attempt(SettlementId settlementId) {
        settlementRepository.load(settlementId).ifPresent(settlement -> {
            var now = clock.instant();
            if (!settlement.isDueForChargeAttempt(now)) {
                return;
            }
            var result = payments.charge(settlement.nextChargeAttempt());
            var events = settlement.record(result).at(now);
            settlementRepository.save(settlement);
            eventPublisher.publish(events);
        });
    }
}
