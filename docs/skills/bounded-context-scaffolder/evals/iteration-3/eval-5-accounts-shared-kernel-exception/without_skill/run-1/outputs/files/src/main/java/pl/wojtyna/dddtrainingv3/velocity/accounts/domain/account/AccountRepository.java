package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

import java.util.Optional;

@Repository
@SecondaryPort
public interface AccountRepository {

    Optional<Account> load(BorrowerId id);

    void save(Account account);
}
