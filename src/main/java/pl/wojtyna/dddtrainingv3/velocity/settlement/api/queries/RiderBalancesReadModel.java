package pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

@SecondaryPort
public interface RiderBalancesReadModel {

    Optional<RiderBalanceView> balanceOf(String riderId);
}
