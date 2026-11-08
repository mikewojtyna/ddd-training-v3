package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import java.util.ArrayList;
import java.util.List;

/// Locks that always work or always fail, recording what they were asked to do.
final class FakeBikeLocks implements BikeLocks {

    private final LockResult result;
    private final List<String> calls = new ArrayList<>();

    private FakeBikeLocks(LockResult result) {
        this.result = result;
    }

    static FakeBikeLocks working() {
        return new FakeBikeLocks(LockResult.SUCCEEDED);
    }

    static FakeBikeLocks failing() {
        return new FakeBikeLocks(LockResult.FAILED);
    }

    @Override
    public LockResult open(BikeId bike) {
        calls.add("open " + bike.value());
        return result;
    }

    @Override
    public LockResult close(BikeId bike) {
        calls.add("close " + bike.value());
        return result;
    }

    List<String> calls() {
        return calls;
    }
}
