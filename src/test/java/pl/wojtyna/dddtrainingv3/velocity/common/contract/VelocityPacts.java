package pl.wojtyna.dddtrainingv3.velocity.common.contract;

import org.springframework.mock.env.MockEnvironment;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import tools.jackson.databind.json.JsonMapper;

/// Shared vocabulary of VeloCity's Pact contract tests.
///
/// - Consumer tests (`*PactTest`, next to the ACL adapter or listener they cover) write the pacts into [#PACT_DIRECTORY];
///   provider tests (`*PactProviderTest`, tag [#PROVIDER_TAG]) verify them against the running application.
/// - Pacticipants are named after the CML bounded contexts. A context that publishes integration events is also a
///   message provider named `<Context>Events`, so its REST and its events are verified by separate classes.
/// - The formats follow `docs/bounded-contexts/velocity/integration-contracts.md`: money as a decimal string in PLN,
///   times as ISO-8601 instants.
public final class VelocityPacts {

    /// Where the pacts live (committed, there is no Pact Broker). Relative to the project root, as Maven and the IDE run tests.
    public static final String PACT_DIRECTORY = "src/test/resources/pacts";

    /// JUnit tag of the provider verifications; Surefire runs them after the consumer tests (see `pom.xml`).
    public static final String PROVIDER_TAG = "pact-provider";

    public static final String ACCOUNTS = "Accounts";
    public static final String PAYMENTS = "Payments";
    public static final String RENTALS = "Rentals";
    public static final String PRICING = "Pricing";
    public static final String SETTLEMENT = "Settlement";
    public static final String DEBT_RECOVERY = "DebtRecovery";
    public static final String CUSTOMER_SUPPORT = "CustomerSupport";
    public static final String FLEET_MAINTENANCE = "FleetMaintenance";

    public static final String PAYMENTS_EVENTS = "PaymentsEvents";
    public static final String RENTALS_EVENTS = "RentalsEvents";
    public static final String SETTLEMENT_EVENTS = "SettlementEvents";
    public static final String CUSTOMER_SUPPORT_EVENTS = "CustomerSupportEvents";

    /// PLN as a decimal string with two decimal places, e.g. `"5.00"`.
    public static final String MONEY = "\\d+\\.\\d{2}";

    /// An ISO-8601 instant in UTC, e.g. `"2026-11-08T10:00:00Z"`.
    public static final String INSTANT = "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d{1,9})?Z";

    /// Reads and writes integration events as JSON, the way a message broker would carry them.
    public static final JsonMapper EVENT_JSON = JsonMapper.builder().build();

    private VelocityPacts() {
    }

    /// Real `VelocityServiceUrls` pointing at the Pact mock server, so consumer tests drive the production ACL adapters.
    public static VelocityServiceUrls urlsOf(String mockServerUrl) {
        return new VelocityServiceUrls(new MockEnvironment().withProperty("velocity.services.base-url", mockServerUrl));
    }
}
