package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.AccountStatus;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.AccountsReadModel;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.RentalEligibilityView;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.Account;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.EmailAddress;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RegisteredEmails;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/// Accounts in memory, with optimistic locking on `Version`. Also answers which emails are registered and serves the
/// read model; the views are computed by the restored `Account` itself, so no rule is repeated here.
@SecondaryAdapter
public class InMemoryAccountRepository implements AccountRepository, RegisteredEmails, AccountsReadModel {

    private final Map<RiderId, Account.Snapshot> accounts = new ConcurrentHashMap<>();
    private final Map<EmailAddress, RiderId> emails = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> load(RiderId riderId) {
        return Optional.ofNullable(accounts.get(riderId)).map(Account::restore);
    }

    @Override
    public void save(Account account) {
        var snapshot = account.snapshot();
        accounts.compute(snapshot.riderId(), (riderId, stored) -> {
            var storedVersion = stored == null ? Version.initial() : stored.version();
            if (!storedVersion.equals(snapshot.version())) {
                throw new OptimisticLockingException("Account of rider " + riderId.value() + " was modified concurrently");
            }
            if (stored == null && emails.putIfAbsent(snapshot.email(), riderId) != null) {
                throw new OptimisticLockingException(snapshot.email().value() + " was registered concurrently");
            }
            return snapshot.withVersion(snapshot.version().next());
        });
    }

    @Override
    public boolean isRegistered(EmailAddress email) {
        return emails.containsKey(email);
    }

    @Override
    public Optional<AccountStatus> statusOf(String riderId) {
        return load(new RiderId(riderId)).map(account -> new AccountStatus(
            riderId,
            account.nextOnboardingStep().name(),
            account.isActivated(),
            account.blocks().stream().sorted().map(Enum::name).toList()));
    }

    @Override
    public Optional<RentalEligibilityView> rentalEligibilityOf(String riderId) {
        return load(new RiderId(riderId)).map(Account::rentalEligibility)
                                         .map(eligibility -> new RentalEligibilityView(riderId, eligibility.eligible(), eligibility.name()));
    }
}
