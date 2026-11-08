package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.Optional;

@Repository
@SecondaryPort
public interface AccountRepository {

    Optional<Account> load(RiderId riderId);

    /// @throws OptimisticLockingException if the stored version differs from `account.version()`, or a new account's
    ///                                    email address is already taken by another account
    void save(Account account);
}
