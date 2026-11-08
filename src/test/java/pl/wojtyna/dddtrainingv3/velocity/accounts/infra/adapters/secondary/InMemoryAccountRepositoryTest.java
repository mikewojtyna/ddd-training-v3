package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.EmailAddress;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.OnboardingStep;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderRegistration;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

@DisplayName("In-memory account repository")
class InMemoryAccountRepositoryTest {

    private static final RiderId ANNA = new RiderId("anna");
    private static final EmailAddress ANNA_EMAIL = new EmailAddress("anna@example.com");
    private static final Instant NOW = Instant.parse("2026-11-08T10:00:00Z");

    private final InMemoryAccountRepository repository = new InMemoryAccountRepository();
    private final RiderRegistration registration = new RiderRegistration(repository);

    @Test
    @DisplayName("a saved account is loaded with its state, and its email counts as registered")
    void savesAndLoads() {
        // given
        var account = registration.register(ANNA_EMAIL).as(ANNA).at(NOW).created();
        account.confirmEmail(NOW);

        // when
        repository.save(account);

        // then
        assertThat(repository.load(ANNA)).hasValueSatisfying(
            loaded -> assertThat(loaded.nextOnboardingStep()).isEqualTo(OnboardingStep.VERIFY_PHONE));
        assertThat(repository.isRegistered(ANNA_EMAIL)).isTrue();
    }

    @Test
    @DisplayName("saving a stale copy of an account is rejected")
    void staleCopyIsRejected() {
        // given
        repository.save(registration.register(ANNA_EMAIL).as(ANNA).at(NOW).created());
        var first = repository.load(ANNA).orElseThrow();
        var second = repository.load(ANNA).orElseThrow();
        first.confirmEmail(NOW);
        repository.save(first);
        second.confirmEmail(NOW);

        // when
        var thrown = catchThrowable(() -> repository.save(second));

        // then
        assertThat(thrown).isInstanceOf(OptimisticLockingException.class);
    }

    @Test
    @DisplayName("a second new account with an email that was registered in between is rejected")
    void concurrentRegistrationOfSameEmailIsRejected() {
        // given
        var anna = registration.register(ANNA_EMAIL).as(ANNA).at(NOW).created();
        var other = registration.register(ANNA_EMAIL).as(new RiderId("other")).at(NOW).created();
        repository.save(anna);

        // when
        var thrown = catchThrowable(() -> repository.save(other));

        // then
        assertThat(thrown).isInstanceOf(OptimisticLockingException.class);
        assertThat(repository.load(new RiderId("other"))).isEmpty();
    }
}
