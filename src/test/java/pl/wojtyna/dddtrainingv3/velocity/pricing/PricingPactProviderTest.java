package pl.wojtyna.dddtrainingv3.velocity.pricing;

import au.com.dius.pact.provider.junitsupport.Provider;
import au.com.dius.pact.provider.junitsupport.State;
import au.com.dius.pact.provider.junitsupport.loader.PactFolder;
import org.junit.jupiter.api.DisplayName;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.HttpProviderVerification;
import pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityContractTest;

import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PRICING;

/// Provider side of Pricing's Open Host Service: verifies Settlement's pact (*Price for Session*) against the running
/// application, priced by the launch tariff and the configured City zone map.
@VelocityContractTest
@Provider(PRICING)
@PactFolder(PACT_DIRECTORY)
@DisplayName("Contract: Pricing honours its consumers' pacts")
class PricingPactProviderTest extends HttpProviderVerification {

    /// The launch tariff (DE-30) is published at start-up, valid from 2026-01-01.
    @State("the launch tariff is in force")
    void launchTariffIsInForce() {
    }
}
