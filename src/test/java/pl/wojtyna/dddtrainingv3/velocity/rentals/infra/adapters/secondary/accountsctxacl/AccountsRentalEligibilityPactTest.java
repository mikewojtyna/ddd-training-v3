package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.accountsctxacl;

import au.com.dius.pact.consumer.MockServer;
import au.com.dius.pact.consumer.dsl.PactDslJsonBody;
import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactConsumerTestExt;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.PactSpecVersion;
import au.com.dius.pact.core.model.V4Pact;
import au.com.dius.pact.core.model.annotations.Pact;
import au.com.dius.pact.core.model.annotations.PactDirectory;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.ACCOUNTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.RENTALS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.urlsOf;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility.Eligibility.ELIGIBLE;
import static pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility.Eligibility.NOT_ELIGIBLE;

/// Consumer side of `Accounts [U,OHS,PL]->[D,ACL] Rentals`: Rentals asks Accounts' *Rental Eligibility* and reads only
/// `eligible`. An unknown rider is a `200` with `eligible=false`, not a `404` (integration-contracts.md).
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = ACCOUNTS, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Rentals → Accounts (rental eligibility)")
class AccountsRentalEligibilityPactTest {

    private static final String RIDER = "rider-anna";
    private static final String UNKNOWN_RIDER = "rider-unknown-to-accounts";

    @Pact(consumer = RENTALS)
    V4Pact onboardedRiderIsEligible(PactDslWithProvider builder) {
        return builder.given("an onboarded rider")
                      .uponReceiving("a rental eligibility query for an onboarded rider")
                      .pathFromProviderState("/accounts/riders/${riderId}/rental-eligibility", "/accounts/riders/" + RIDER + "/rental-eligibility")
                      .method("GET")
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().booleanValue("eligible", true))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = RENTALS)
    V4Pact riderWithIncompleteOnboardingIsNotEligible(PactDslWithProvider builder) {
        return builder.given("a rider who has not finished onboarding")
                      .uponReceiving("a rental eligibility query for a rider who has not finished onboarding")
                      .pathFromProviderState("/accounts/riders/${riderId}/rental-eligibility", "/accounts/riders/" + RIDER + "/rental-eligibility")
                      .method("GET")
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().booleanValue("eligible", false))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = RENTALS)
    V4Pact unknownRiderIsNotEligible(PactDslWithProvider builder) {
        return builder.uponReceiving("a rental eligibility query for a rider Accounts doesn't know")
                      .path("/accounts/riders/" + UNKNOWN_RIDER + "/rental-eligibility")
                      .method("GET")
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().booleanValue("eligible", false))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "onboardedRiderIsEligible")
    void onboardedRiderIsEligible(MockServer accounts) {
        assertThat(adapter(accounts).of(Holder.rider(RIDER))).isEqualTo(ELIGIBLE);
    }

    @Test
    @PactTestFor(pactMethod = "riderWithIncompleteOnboardingIsNotEligible")
    void riderWithIncompleteOnboardingIsNotEligible(MockServer accounts) {
        assertThat(adapter(accounts).of(Holder.rider(RIDER))).isEqualTo(NOT_ELIGIBLE);
    }

    @Test
    @PactTestFor(pactMethod = "unknownRiderIsNotEligible")
    void unknownRiderIsNotEligible(MockServer accounts) {
        assertThat(adapter(accounts).of(Holder.rider(UNKNOWN_RIDER))).isEqualTo(NOT_ELIGIBLE);
    }

    private static AccountsRentalEligibilityAdapter adapter(MockServer accounts) {
        return new AccountsRentalEligibilityAdapter(RestClient.builder().build(), urlsOf(accounts.getUrl()));
    }
}
