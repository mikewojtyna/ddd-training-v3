package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.Account;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountRepository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemoryAccountRepository implements AccountRepository {

    private final Map<BorrowerId, Account> accounts = new ConcurrentHashMap<>();

    @Override
    public Optional<Account> load(BorrowerId id) {
        return Optional.ofNullable(accounts.get(id));
    }

    @Override
    public void save(Account account) {
        accounts.put(account.id(), account);
    }
}
