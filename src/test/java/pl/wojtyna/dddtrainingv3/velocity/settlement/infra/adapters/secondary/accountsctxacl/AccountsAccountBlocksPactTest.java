package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.accountsctxacl;

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
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.UnknownRiderAccountException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.ACCOUNTS;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.SETTLEMENT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.urlsOf;

/// Consumer side of `Accounts [U,OHS,PL]->[D,ACL] Settlement`: Settlement places and lifts the *outstanding debt*
/// block. Any `2xx` is success; a `404` (Accounts doesn't know the rider) becomes `UnknownRiderAccountException`.
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = ACCOUNTS, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Settlement → Accounts (block / unblock for outstanding debt)")
class AccountsAccountBlocksPactTest {

    private static final String RIDER = "rider-anna";
    private static final String UNKNOWN_RIDER = "rider-unknown-to-accounts";

    @Pact(consumer = SETTLEMENT)
    V4Pact blockIsPlaced(PactDslWithProvider builder) {
        return builder.given("an onboarded rider")
                      .uponReceiving("a block for outstanding debt")
                      .pathFromProviderState("/accounts/riders/${riderId}/blocks", "/accounts/riders/" + RIDER + "/blocks")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(new PactDslJsonBody().stringValue("reason", "OUTSTANDING_DEBT"))
                      .willRespondWith()
                      .status(200)
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = SETTLEMENT)
    V4Pact blockOfUnknownRiderIsRejected(PactDslWithProvider builder) {
        return builder.uponReceiving("a block for outstanding debt of a rider Accounts doesn't know")
                      .path("/accounts/riders/" + UNKNOWN_RIDER + "/blocks")
                      .method("POST")
                      .headers(Map.of("Content-Type", "application/json"))
                      .body(new PactDslJsonBody().stringValue("reason", "OUTSTANDING_DEBT"))
                      .willRespondWith()
                      .status(404)
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = SETTLEMENT)
    V4Pact blockIsLifted(PactDslWithProvider builder) {
        return builder.given("a rider blocked for outstanding debt")
                      .uponReceiving("lifting the outstanding debt block")
                      .pathFromProviderState("/accounts/riders/${riderId}/blocks/OUTSTANDING_DEBT",
                                             "/accounts/riders/" + RIDER + "/blocks/OUTSTANDING_DEBT")
                      .method("DELETE")
                      .willRespondWith()
                      .status(200)
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = SETTLEMENT)
    V4Pact unblockOfUnknownRiderIsRejected(PactDslWithProvider builder) {
        return builder.uponReceiving("lifting the outstanding debt block of a rider Accounts doesn't know")
                      .path("/accounts/riders/" + UNKNOWN_RIDER + "/blocks/OUTSTANDING_DEBT")
                      .method("DELETE")
                      .willRespondWith()
                      .status(404)
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "blockIsPlaced")
    void blockIsPlaced(MockServer accounts) {
        assertThatCode(() -> blocks(accounts).blockForOutstandingDebt(new RiderId(RIDER))).doesNotThrowAnyException();
    }

    @Test
    @PactTestFor(pactMethod = "blockOfUnknownRiderIsRejected")
    void blockOfUnknownRiderIsRejected(MockServer accounts) {
        assertThatThrownBy(() -> blocks(accounts).blockForOutstandingDebt(new RiderId(UNKNOWN_RIDER)))
            .isInstanceOf(UnknownRiderAccountException.class);
    }

    @Test
    @PactTestFor(pactMethod = "blockIsLifted")
    void blockIsLifted(MockServer accounts) {
        assertThatCode(() -> blocks(accounts).liftOutstandingDebtBlock(new RiderId(RIDER))).doesNotThrowAnyException();
    }

    @Test
    @PactTestFor(pactMethod = "unblockOfUnknownRiderIsRejected")
    void unblockOfUnknownRiderIsRejected(MockServer accounts) {
        assertThatThrownBy(() -> blocks(accounts).liftOutstandingDebtBlock(new RiderId(UNKNOWN_RIDER)))
            .isInstanceOf(UnknownRiderAccountException.class);
    }

    private static AccountsAccountBlocks blocks(MockServer accounts) {
        return new AccountsAccountBlocks(RestClient.builder().build(), urlsOf(accounts.getUrl()));
    }
}
