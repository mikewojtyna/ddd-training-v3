package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Account examples")
class AccountTest {

    // @formatter:off
    @DisplayName(
        """
         given George's active account,
         when the back office blocks it,
         then the account is blocked
         and Account Blocked is published
        """
    )
    // @formatter:on
    @Test
    void test0() {
        // given
        var george = new BorrowerId("george");
        var account = Account.active(george);

        // when
        var events = account.block();

        // then
        assertThat(account.isBlocked()).isTrue();
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event instanceof AccountEvent.AccountBlocked(var blocked) && blocked.equals(george))).isTrue();
    }

    // @formatter:off
    @DisplayName(
        """
         given George's blocked account,
         when the back office blocks it again,
         then the block is rejected
        """
    )
    // @formatter:on
    @Test
    void test1() {
        // given
        var account = Account.active(new BorrowerId("george"));
        account.block();

        // when
        var exception = catchThrowable(account::block);

        // then
        assertThat(exception).isInstanceOf(AccountAlreadyBlockedException.class);
        assertThat(account.isBlocked()).isTrue();
    }
}
