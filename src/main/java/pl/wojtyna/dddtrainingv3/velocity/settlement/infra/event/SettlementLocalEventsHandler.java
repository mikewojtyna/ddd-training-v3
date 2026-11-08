package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.BlockRiderWithOutstandingDebtPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtEvent;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.UnblockRiderWhenDebtClearedPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.UnpaidRideFeeBecomesOutstandingDebtPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideSettlementEvent;

/// Routes Settlement's own domain events to its policies, in-process and synchronously (in the same call that
/// recorded the fact).
///
/// A failing call to Accounts (unknown rider, Accounts unavailable) is logged and doesn't undo the debt already
/// recorded or collected (plan `finance-retries-failed-charge` D7, risks of `rider-pays-off-outstanding-debt`); the
/// next unpaid fee asks for the block again (D5).
public class SettlementLocalEventsHandler {

    private static final Logger log = LoggerFactory.getLogger(SettlementLocalEventsHandler.class);

    private final UnpaidRideFeeBecomesOutstandingDebtPolicy unpaidRideFeeBecomesOutstandingDebt;
    private final BlockRiderWithOutstandingDebtPolicy blockRiderWithOutstandingDebt;
    private final UnblockRiderWhenDebtClearedPolicy unblockRiderWhenDebtCleared;

    public SettlementLocalEventsHandler(UnpaidRideFeeBecomesOutstandingDebtPolicy unpaidRideFeeBecomesOutstandingDebt,
                                        BlockRiderWithOutstandingDebtPolicy blockRiderWithOutstandingDebt,
                                        UnblockRiderWhenDebtClearedPolicy unblockRiderWhenDebtCleared) {
        this.unpaidRideFeeBecomesOutstandingDebt = unpaidRideFeeBecomesOutstandingDebt;
        this.blockRiderWithOutstandingDebt = blockRiderWithOutstandingDebt;
        this.unblockRiderWhenDebtCleared = unblockRiderWhenDebtCleared;
    }

    @EventListener
    public void on(RideSettlementEvent.RideFeeUnpaid event) {
        unpaidRideFeeBecomesOutstandingDebt.handle(event);
    }

    @EventListener
    public void on(RiderDebtEvent.RentalFeeUnpaid event) {
        try {
            blockRiderWithOutstandingDebt.handle(event);
        } catch (RuntimeException failure) {
            log.warn("Couldn't block rider {} for outstanding debt", event.rider().value(), failure);
        }
    }

    @EventListener
    public void on(RiderDebtEvent.OutstandingDebtCleared event) {
        try {
            unblockRiderWhenDebtCleared.handle(event);
        } catch (RuntimeException failure) {
            log.warn("Couldn't lift the outstanding debt block of rider {}", event.rider().value(), failure);
        }
    }
}
