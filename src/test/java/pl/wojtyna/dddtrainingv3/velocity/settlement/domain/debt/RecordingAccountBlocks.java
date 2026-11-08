package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;

import java.util.ArrayList;
import java.util.List;

/// Fake of the `AccountBlocks` port: records what Settlement asked Accounts to do.
class RecordingAccountBlocks implements AccountBlocks {

    final List<String> requests = new ArrayList<>();

    @Override
    public void blockForOutstandingDebt(RiderId rider) {
        requests.add("block " + rider.value());
    }

    @Override
    public void liftOutstandingDebtBlock(RiderId rider) {
        requests.add("unblock " + rider.value());
    }
}
