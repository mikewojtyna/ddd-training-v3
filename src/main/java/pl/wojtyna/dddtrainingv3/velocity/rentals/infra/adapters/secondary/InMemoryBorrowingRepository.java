package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.Borrowing;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.BorrowingRepository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SecondaryAdapter
public class InMemoryBorrowingRepository implements BorrowingRepository {

    private final Map<BorrowerId, Borrowing.Snapshot> borrowings = new ConcurrentHashMap<>();

    @Override
    public Optional<Borrowing> load(BorrowerId borrower) {
        return Optional.ofNullable(borrowings.get(borrower)).map(Borrowing::restore);
    }

    @Override
    public synchronized void save(Borrowing borrowing) {
        var stored = borrowings.get(borrowing.borrower());
        var storedVersion = stored == null ? Version.initial() : stored.version();
        if (!storedVersion.equals(borrowing.version())) {
            throw new OptimisticLockingException("Borrowing of " + borrowing.borrower().value() + " was modified concurrently");
        }
        var s = borrowing.snapshot();
        borrowings.put(borrowing.borrower(),
                       new Borrowing.Snapshot(s.borrower(), borrowing.version().next(), s.reservedBike(), s.reservedAt(), s.riding()));
    }
}
