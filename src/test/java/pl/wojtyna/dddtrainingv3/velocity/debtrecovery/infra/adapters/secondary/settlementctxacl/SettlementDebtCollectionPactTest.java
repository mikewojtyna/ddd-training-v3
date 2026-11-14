package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl;

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
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

import static org.assertj.core.api.Assertions.assertThat;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.DEBT_RECOVERY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.MONEY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.PACT_DIRECTORY;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.SETTLEMENT;
import static pl.wojtyna.dddtrainingv3.velocity.common.contract.VelocityPacts.urlsOf;

/// Consumer side of `Settlement [U,S,PL]->[D,C] DebtRecovery`: Debt Recovery asks Settlement to *Collect Outstanding Debt*
/// and reads `collected` and `outstandingDebt` (a PLN decimal string; `"0.00"` means the debt is cleared).
@ExtendWith(PactConsumerTestExt.class)
@PactTestFor(providerName = SETTLEMENT, pactVersion = PactSpecVersion.V4)
@PactDirectory(PACT_DIRECTORY)
@DisplayName("Contract: Debt Recovery → Settlement (collect outstanding debt)")
class SettlementDebtCollectionPactTest {

    private static final String RIDER = "rider-debtor";

    @Pact(consumer = DEBT_RECOVERY)
    V4Pact debtIsCollected(PactDslWithProvider builder) {
        return builder.given("a rider owing for an unpaid ride who now has a card that works")
                      .uponReceiving("a debt collection that succeeds")
                      .pathFromProviderState("/settlement/riders/${riderId}/debt-collections",
                                             "/settlement/riders/" + RIDER + "/debt-collections")
                      .method("POST")
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().booleanValue("collected", true).stringMatcher("outstandingDebt", MONEY, "0.00"))
                      .toPact(V4Pact.class);
    }

    @Pact(consumer = DEBT_RECOVERY)
    V4Pact collectionFails(PactDslWithProvider builder) {
        return builder.given("a rider owing for an unpaid ride whose card has no funds")
                      .uponReceiving("a debt collection that fails")
                      .pathFromProviderState("/settlement/riders/${riderId}/debt-collections",
                                             "/settlement/riders/" + RIDER + "/debt-collections")
                      .method("POST")
                      .willRespondWith()
                      .status(200)
                      .body(new PactDslJsonBody().booleanValue("collected", false).stringMatcher("outstandingDebt", MONEY, "3.00"))
                      .toPact(V4Pact.class);
    }

    @Test
    @PactTestFor(pactMethod = "debtIsCollected")
    void debtIsCollected(MockServer settlement) {
        var outcome = collection(settlement).collect(new RiderId(RIDER));

        assertThat(outcome.collected()).isTrue();
        assertThat(outcome.debtCleared()).isTrue();
    }

    @Test
    @PactTestFor(pactMethod = "collectionFails")
    void collectionFails(MockServer settlement) {
        var outcome = collection(settlement).collect(new RiderId(RIDER));

        assertThat(outcome.collected()).isFalse();
        assertThat(outcome.debtCleared()).isFalse();
        assertThat(outcome.outstandingDebt()).hasValueSatisfying(debt -> assertThat(debt.getAmount()).isEqualByComparingTo("3.00"));
    }

    private static SettlementDebtCollection collection(MockServer settlement) {
        return new SettlementDebtCollection(RestClient.builder().build(), urlsOf(settlement.getUrl()));
    }
}
