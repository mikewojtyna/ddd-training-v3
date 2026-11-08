package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;

/// Settlement as *Enforcer*: makes Accounts block a rider for outstanding debt and lift that block once the debt is
/// cleared. Accounts owns the block, its notification and "no second notification" (DE-7, DE-94).
@SecondaryPort
public interface AccountBlocks {

    /// @throws UnknownRiderAccountException if Accounts has no such rider
    void blockForOutstandingDebt(RiderId rider);

    /// @throws UnknownRiderAccountException if Accounts has no such rider
    void liftOutstandingDebtBlock(RiderId rider);
}
