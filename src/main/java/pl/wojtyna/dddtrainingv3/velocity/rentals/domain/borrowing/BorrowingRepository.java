package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

import java.util.Optional;

@Repository
@SecondaryPort
public interface BorrowingRepository {

    Optional<Borrowing> load(BorrowerId borrower);

    /// Saves the borrowing. Throws `OptimisticLockingException` if it was saved by someone else since it was loaded
    /// (its `version()` no longer matches the stored one).
    void save(Borrowing borrowing);
}
