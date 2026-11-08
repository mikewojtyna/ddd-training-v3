package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.messagingprovideracl.RecordingRiderNotifications;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.messagingprovideracl.RecordingRiderNotifications.SentNotification;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/// Accounts' side of `docs/specs/finance-retries-failed-charge.md`: Settlement's *Block Account* for outstanding debt
/// (`POST /accounts/riders/{riderId}/blocks`), called here directly as Settlement's ACL will call it. The end-to-end
/// tests (unpaid fee → block) are Settlement's `FinanceRetriesFailedChargeSpecTest`.
@VelocitySpringTest
@DisplayName("Spec finance-retries-failed-charge (Accounts side)")
class FinanceRetriesFailedChargeSpecTest {

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;
    @Autowired
    private ApplicationEventMulticaster multicaster;
    @Autowired
    private RecordingRiderNotifications notifications;
    @Autowired
    private MutableClock clock;

    private AccountsDriver accounts;
    private final String suffix = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        accounts = new AccountsDriver(restClient, urls, multicaster);
        clock.setTo("2026-11-08T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        accounts.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Ben is activated and not blocked,
         when at 10:40 Settlement blocks Ben for outstanding debt,
         then Ben is blocked for outstanding debt (Account Blocked) and notified
        """
    )
    // @formatter:on
    @Test
    void e2_2_outstanding_debt_blocks_the_account() {
        // given
        var ben = accounts.activated("ben-" + suffix + "@example.com", "tok_visa_4242_" + suffix);
        clock.setTo("2026-11-08T10:40:00Z");

        // when
        var status = accounts.blockForOutstandingDebt(ben);

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(ben).blocks()).containsExactly("OUTSTANDING_DEBT");
        assertThat(accounts.rentalEligibilityOf(ben).reason()).isEqualTo("BLOCKED");
        assertThat(notifications.sentTo(ben)).extracting(SentNotification::reason).containsExactly("OUTSTANDING_DEBT");
    }

    // @formatter:off
    @DisplayName(
        """
         E3.1 given Ben has been blocked for outstanding debt since 10:40,
         when at 11:00 Settlement blocks Ben for outstanding debt again,
         then Ben stays blocked without a new block notification
        """
    )
    // @formatter:on
    @Test
    void e3_1_debt_block_already_in_place_sends_no_second_notification() {
        // given
        var ben = accounts.activated("ben-" + suffix + "@example.com", "tok_visa_4242_" + suffix);
        clock.setTo("2026-11-08T10:40:00Z");
        accounts.blockForOutstandingDebt(ben);
        clock.setTo("2026-11-08T11:00:00Z");

        // when
        var status = accounts.blockForOutstandingDebt(ben);

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(ben).blocks()).containsExactly("OUTSTANDING_DEBT");
        assertThat(notifications.sentTo(ben)).hasSize(1);
    }

    // @formatter:off
    @DisplayName(
        """
         Contract: blocking an unknown rider answers 404, and a reason other than OUTSTANDING_DEBT answers 400
        """
    )
    // @formatter:on
    @Test
    void block_contract_rejections() {
        // given
        var ben = accounts.activated("ben-" + suffix + "@example.com", "tok_visa_4242_" + suffix);

        // when
        var unknown = accounts.blockForOutstandingDebt("nobody-" + suffix);
        var cardBlock = restClient.post().uri(urls.url("/accounts/riders/" + ben + "/blocks"))
                                  .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                                  .body(java.util.Map.of("reason", "INVALID_CARD"))
                                  .exchange((request, response) -> response.getStatusCode().value());

        // then
        assertThat(unknown).isEqualTo(404);
        assertThat(cardBlock).isEqualTo(400);
        assertThat(accounts.statusOf(ben).blocks()).isEmpty();
    }
}
