package pl.wojtyna.dddtrainingv3.velocity.settlement;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.HttpProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;

import java.util.Map;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.ProviderStates.unique;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.SETTLEMENT;

/// Provider side of Settlement's REST resources: verifies Debt Recovery's pact (*Collect Outstanding Debt*). The debt
/// is made the real way: a 30-minute ride whose charge fails three times (Rentals → Settlement → Pricing, Payments).
@VelocityContractTest
@Provider(SETTLEMENT)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Settlement honours its consumers' pacts")
class SettlementPactProviderTest extends HttpProviderVerification {

    @State("a rider owing for an unpaid ride who now has a card that works")
    Map<String, Object> riderOwingWithWorkingCard() {
        var state = states.riderWithUnpaidRide();
        states.addCard((String) state.get("riderId"), unique("tok_visa"));
        return state;
    }

    @State("a rider owing for an unpaid ride whose card has no funds")
    Map<String, Object> riderOwingWithCardWithoutFunds() {
        return states.riderWithUnpaidRide();
    }
}
