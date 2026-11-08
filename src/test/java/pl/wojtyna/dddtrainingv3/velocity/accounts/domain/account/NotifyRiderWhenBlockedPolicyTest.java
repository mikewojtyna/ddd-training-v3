package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent.AccountBlocked;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Block notification examples")
class NotifyRiderWhenBlockedPolicyTest {

    private static final RiderId ANNA = new RiderId("anna");
    private static final RiderId BEN = new RiderId("ben");

    record Sent(RiderId rider, BlockReason reason) {
    }

    private final List<Sent> sent = new ArrayList<>();
    private final NotifyRiderWhenBlockedPolicy policy = new NotifyRiderWhenBlockedPolicy(
        (rider, reason) -> sent.add(new Sent(rider, reason)));

    // @formatter:off
    @DisplayName(
        """
         E2.1 given Anna is not blocked,
         when she is blocked for invalid card at 08:00 (Account Blocked),
         then Anna receives one block notification, for invalid card
        """
    )
    // @formatter:on
    @Test
    void riderGetsOneBlockNotification() {
        // when
        policy.handle(new AccountBlocked(ANNA, BlockReason.INVALID_CARD, Instant.parse("2026-11-20T08:00:00Z")));

        // then
        assertThat(sent).containsExactly(new Sent(ANNA, BlockReason.INVALID_CARD));
    }

    // @formatter:off
    @DisplayName(
        """
         E2.2 given Ben is already blocked for outstanding debt and got its notification,
         when he is blocked for invalid card as well at 09:00 (Account Blocked),
         then Ben receives a second block notification, for invalid card
        """
    )
    // @formatter:on
    @Test
    void secondReasonSendsSecondNotification() {
        // given
        policy.handle(new AccountBlocked(BEN, BlockReason.OUTSTANDING_DEBT, Instant.parse("2026-11-20T08:00:00Z")));

        // when
        policy.handle(new AccountBlocked(BEN, BlockReason.INVALID_CARD, Instant.parse("2026-11-20T09:00:00Z")));

        // then
        assertThat(sent).containsExactly(new Sent(BEN, BlockReason.OUTSTANDING_DEBT), new Sent(BEN, BlockReason.INVALID_CARD));
    }
}
