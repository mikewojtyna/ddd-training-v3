package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.Account;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountAlreadyBlockedException;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountEvent;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountNotFoundException;
import pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.repository.InMemoryAccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("Block Account use case examples")
class BlockAccountUseCaseTest {

    private final InMemoryAccountRepository repository = new InMemoryAccountRepository();
    private final RecordingPublisher publisher = new RecordingPublisher();
    private final BlockAccountUseCase useCase = new BlockAccountUseCase(repository, publisher);

    // @formatter:off
    @DisplayName(
        """
         given George's active account,
         when the back office blocks it,
         then the blocked account is saved
         and Account Blocked is published
        """
    )
    // @formatter:on
    @Test
    void test0() {
        // given
        var george = new BorrowerId("george");
        repository.save(Account.active(george));

        // when
        useCase.block(george);

        // then
        assertThat(repository.load(george)).hasValueSatisfying(account -> assertThat(account.isBlocked()).isTrue());
        assertThat(publisher.published).containsExactly(new AccountEvent.AccountBlocked(george));
    }

    // @formatter:off
    @DisplayName(
        """
         given George's blocked account,
         when the back office blocks it again,
         then the block is rejected
         and nothing is published
        """
    )
    // @formatter:on
    @Test
    void test1() {
        // given
        var george = new BorrowerId("george");
        var account = Account.active(george);
        account.block();
        repository.save(account);

        // when
        var exception = catchThrowable(() -> useCase.block(george));

        // then
        assertThat(exception).isInstanceOf(AccountAlreadyBlockedException.class);
        assertThat(publisher.published).isEmpty();
    }

    // @formatter:off
    @DisplayName(
        """
         given no account for George,
         when the back office blocks it,
         then the block is rejected
        """
    )
    // @formatter:on
    @Test
    void test2() {
        // when
        var exception = catchThrowable(() -> useCase.block(new BorrowerId("george")));

        // then
        assertThat(exception).isInstanceOf(AccountNotFoundException.class);
        assertThat(publisher.published).isEmpty();
    }

    private static class RecordingPublisher implements DomainEventPublisher {

        private final List<DomainEvent> published = new ArrayList<>();

        @Override
        public void publish(DomainEvents events) {
            events.stream().forEach(published::add);
        }

        @Override
        public void publish(DomainEvent event) {
            published.add(event);
        }
    }
}
