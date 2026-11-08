package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCards;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementRepository;

/// *Retry Failed Charges*: every ride awaiting a retry whose next attempt is due is charged again (attempt 2 or 3).
/// Each ride on its own (plan `finance-retries-failed-charge` D7): a failing call to Payments or Accounts, or a
/// concurrent change, is logged and that ride is left for the next run.
@PrimaryPort
public class RetryFailedChargesUseCase {

    private static final Logger log = LoggerFactory.getLogger(RetryFailedChargesUseCase.class);

    private final RideSettlementRepository rides;
    private final RideChargeAttempt chargeAttempt;

    public RetryFailedChargesUseCase(RideSettlementRepository rides, RiderCards riderCards,
                                     DomainEventPublisher eventPublisher) {
        this.rides = rides;
        this.chargeAttempt = new RideChargeAttempt(rides, riderCards, eventPublisher);
    }

    public void handle(RetryFailedCharges command) {
        for (var ride : rides.loadAwaitingRetry()) {
            try {
                chargeAttempt.attempt(ride, command.now());
            } catch (RuntimeException failure) {
                log.warn("Retrying the charge of ride {} failed; left for the next run", ride.id().value(), failure);
            }
        }
    }
}
