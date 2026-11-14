package pl.wojtyna.dddtrainingv3.velocity.settlement;

import au.com.dius.pact.provider.PactVerifyProvider;
import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.MessageProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.OutstandingDebtCleared;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.events.RentalFeeUnpaid;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.unique;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.SETTLEMENT_EVENTS;

/// Provider side of Settlement's integration events: verifies Debt Recovery's pacts for `RentalFeeUnpaid` and
/// `OutstandingDebtCleared` with the events Settlement really publishes after three failed charges and a pay-off.
@VelocityContractTest
@Provider(SETTLEMENT_EVENTS)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Settlement publishes the events its consumers expect")
class SettlementEventsPactProviderTest extends MessageProviderVerification {

    @PactVerifyProvider("a ride's fee became outstanding debt")
    String rentalFeeUnpaid() {
        var riderId = (String) states.riderWithUnpaidRide().get("riderId");
        return asMessage(events.awaitFirst(RentalFeeUnpaid.class, event -> event.riderId().equals(riderId)));
    }

    @PactVerifyProvider("a rider's outstanding debt was cleared")
    String outstandingDebtCleared() {
        var riderId = (String) states.riderWithUnpaidRide().get("riderId");
        states.addCard(riderId, unique("tok_visa"));
        states.payOffDebt(riderId);
        return asMessage(events.awaitFirst(OutstandingDebtCleared.class, event -> event.riderId().equals(riderId)));
    }
}
