package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountUnblocked;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.BlockReason;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.common.MutableClock;
import pl.wojtyna.dddtrainingv3.velocity.common.VelocitySpringTest;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/// Accounts' side of `docs/specs/rider-pays-off-outstanding-debt.md`: Settlement's *Unblock Account* for outstanding
/// debt (`DELETE /accounts/riders/{riderId}/blocks/OUTSTANDING_DEBT`), called here directly as Settlement's ACL will
/// call it. The payoff itself is tested in Settlement's `RiderPaysOffOutstandingDebtSpecTest`.
@VelocitySpringTest
@DisplayName("Spec rider-pays-off-outstanding-debt (Accounts side)")
class RiderPaysOffOutstandingDebtSpecTest {

    @Autowired
    private RestClient restClient;
    @Autowired
    private VelocityServiceUrls urls;
    @Autowired
    private ApplicationEventMulticaster multicaster;
    @Autowired
    private MutableClock clock;

    private AccountsDriver accounts;
    private PaymentsView payments;
    private final String suffix = UUID.randomUUID().toString();

    @BeforeEach
    void setUp() {
        accounts = new AccountsDriver(restClient, urls, multicaster);
        payments = new PaymentsView(restClient, urls);
        clock.setTo("2026-11-09T10:00:00Z");
    }

    @AfterEach
    void tearDown() {
        accounts.stop();
    }

    // @formatter:off
    @DisplayName(
        """
         E1.1 given Anna is blocked for outstanding debt only,
         when at 18:00 Settlement unblocks her for outstanding debt,
         then the block is lifted and Anna is eligible to rent (Account Unblocked naming outstanding debt, eligible: yes)
        """
    )
    // @formatter:on
    @Test
    void e1_1_cleared_debt_lifts_debt_block() {
        // given
        var anna = accounts.activated("anna-" + suffix + "@example.com", "tok_visa_4242_" + suffix);
        accounts.blockForOutstandingDebt(anna);
        clock.setTo("2026-11-09T18:00:00Z");

        // when
        var status = accounts.unblockOutstandingDebt(anna);

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(anna).blocks()).isEmpty();
        assertThat(accounts.rentalEligibilityOf(anna)).isEqualTo(new AccountsDriver.Eligibility(anna, true, "ELIGIBLE"));
        assertThat(accounts.eventsOf(anna, AccountUnblocked.class)).containsExactly(
            new AccountUnblocked(new RiderId(anna), BlockReason.OUTSTANDING_DEBT, true, Instant.parse("2026-11-09T18:00:00Z")));
    }

    // @formatter:off
    @DisplayName(
        """
         Canvas (DE-6) given Carla is blocked for outstanding debt and for an invalid card,
         when Settlement unblocks her for outstanding debt,
         then Carla is still blocked for invalid card and isn't eligible to rent (Account Unblocked, eligible: no)
        """
    )
    // @formatter:on
    @Test
    void debt_unblock_does_not_lift_card_block() {
        // given
        var visa = "tok_visa_9999_" + suffix;
        var carla = accounts.activated("carla-" + suffix + "@example.com", visa);
        accounts.blockForOutstandingDebt(carla);
        payments.processorReportsCardInvalidated(visa, "CANCELLED");
        await().atMost(Duration.ofSeconds(10))
               .untilAsserted(() -> assertThat(accounts.statusOf(carla).blocks()).containsExactly("INVALID_CARD", "OUTSTANDING_DEBT"));

        // when
        var status = accounts.unblockOutstandingDebt(carla);

        // then
        assertThat(status).isEqualTo(200);
        assertThat(accounts.statusOf(carla).blocks()).containsExactly("INVALID_CARD");
        assertThat(accounts.rentalEligibilityOf(carla).reason()).isEqualTo("BLOCKED");
        assertThat(accounts.eventsOf(carla, AccountUnblocked.class)).singleElement()
                                                                  .extracting(AccountUnblocked::eligibleNow)
                                                                  .isEqualTo(false);
    }

    // @formatter:off
    @DisplayName(
        """
         Contract: unblocking an unknown rider answers 404; unblocking a rider without a debt block changes nothing
        """
    )
    // @formatter:on
    @Test
    void unblock_contract() {
        // given
        var eva = accounts.activated("eva-" + suffix + "@example.com", "tok_visa_4242_" + suffix);

        // when
        var unknown = accounts.unblockOutstandingDebt("nobody-" + suffix);
        var notBlocked = accounts.unblockOutstandingDebt(eva);

        // then
        assertThat(unknown).isEqualTo(404);
        assertThat(notBlocked).isEqualTo(200);
        assertThat(accounts.eventsOf(eva, AccountUnblocked.class)).isEmpty();
    }
}
