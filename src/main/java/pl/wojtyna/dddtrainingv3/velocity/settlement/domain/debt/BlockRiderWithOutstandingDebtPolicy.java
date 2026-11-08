package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import pl.wojtyna.dddtrainingv3.common.domain.DomainPolicy;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.RiderDebtEvent.RentalFeeUnpaid;

/// Whenever a rental fee becomes outstanding debt, Accounts is asked to block the rider for *outstanding debt*: on
/// every unpaid fee, not only the first (plan `finance-retries-failed-charge` D5); Accounts ignores a block already in
/// place and sends no second notification (DE-94).
public class BlockRiderWithOutstandingDebtPolicy implements DomainPolicy {

    private final AccountBlocks accountBlocks;

    public BlockRiderWithOutstandingDebtPolicy(AccountBlocks accountBlocks) {
        this.accountBlocks = accountBlocks;
    }

    public void handle(RentalFeeUnpaid rentalFeeUnpaid) {
        accountBlocks.blockForOutstandingDebt(rentalFeeUnpaid.rider());
    }
}
