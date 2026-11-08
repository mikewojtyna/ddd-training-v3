package pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.util.Optional;

@SecondaryPort
public interface RideSettlementsReadModel {

    Optional<RideSettlementView> rideSettlementOf(String sessionId);
}
