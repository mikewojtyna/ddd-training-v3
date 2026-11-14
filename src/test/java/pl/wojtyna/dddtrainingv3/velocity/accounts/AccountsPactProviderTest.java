package pl.wojtyna.dddtrainingv3.velocity.accounts;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.HttpProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;

import java.util.Map;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.ACCOUNTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;

/// Provider side of Accounts' Open Host Service: verifies the pacts of Rentals (*Rental Eligibility*) and Settlement
/// (*Block Account* / *Unblock Account*) against the running application.
@VelocityContractTest
@Provider(ACCOUNTS)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Accounts honours its consumers' pacts")
class AccountsPactProviderTest extends HttpProviderVerification {

    @State("an onboarded rider")
    Map<String, Object> onboardedRider() {
        return Map.of("riderId", states.onboardedRider());
    }

    @State("a rider who has not finished onboarding")
    Map<String, Object> riderWhoHasNotFinishedOnboarding() {
        return Map.of("riderId", states.registeredRider());
    }

    @State("a rider blocked for outstanding debt")
    Map<String, Object> riderBlockedForOutstandingDebt() {
        var riderId = states.onboardedRider();
        states.blockForOutstandingDebt(riderId);
        return Map.of("riderId", riderId);
    }
}
