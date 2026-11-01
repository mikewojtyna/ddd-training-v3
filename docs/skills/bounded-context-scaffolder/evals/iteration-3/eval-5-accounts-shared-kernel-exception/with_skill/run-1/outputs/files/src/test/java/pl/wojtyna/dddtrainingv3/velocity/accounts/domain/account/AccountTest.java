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
         given an active account,
         when the back office blocks it,
         then the account is blocked and Account Blocked is published
        """
    )
    // @formatter:on
    @Test
    void blockActiveAccount() {
        // given
        var account = Account.active(new BorrowerId("george"));

        // when
        var events = account.block();

        // then
        assertThat(events.hasAtLeastOneEventMatching(
            event -> event.equals(new AccountEvent.AccountBlocked(new BorrowerId("george"))))).isTrue();
        assertThat(catchThrowable(account::block)).isInstanceOf(AccountAlreadyBlocked.class);
    }

    // @formatter:off
    @DisplayName(
        """
         given a blocked account,
         when the back office blocks it again,
         then the block is rejected
        """
    )
    // @formatter:on
    @Test
    void blockAlreadyBlockedAccount() {
        // given
        var account = Account.active(new BorrowerId("george"));
        account.block();

        // when
        var exception = catchThrowable(account::block);

        // then
        assertThat(exception).isInstanceOf(AccountAlreadyBlocked.class);
    }
}
