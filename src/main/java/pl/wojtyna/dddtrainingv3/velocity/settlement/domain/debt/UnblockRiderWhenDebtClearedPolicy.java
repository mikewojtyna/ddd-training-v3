package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import pl.wojtyna.dddtrainingv3.common.domain.DomainPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtEvent.OutstandingDebtCleared;

/// Whenever a rider's outstanding debt is cleared, Accounts is asked to lift the *outstanding debt* block; a block
/// for an invalid card stays (DE-6, Accounts' rule).
public class UnblockRiderWhenDebtClearedPolicy implements DomainPolicy {

    private final AccountBlocks accountBlocks;

    public UnblockRiderWhenDebtClearedPolicy(AccountBlocks accountBlocks) {
        this.accountBlocks = accountBlocks;
    }

    public void handle(OutstandingDebtCleared outstandingDebtCleared) {
        accountBlocks.liftOutstandingDebtBlock(outstandingDebtCleared.rider());
    }
}
