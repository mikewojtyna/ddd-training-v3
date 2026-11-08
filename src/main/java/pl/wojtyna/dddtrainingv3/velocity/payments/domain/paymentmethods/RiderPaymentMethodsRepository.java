package pl.wojtyna.dddtrainingv3.velocity.payments.domain.paymentmethods;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import org.jmolecules.ddd.annotation.Repository;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;

import java.util.List;
import java.util.Optional;

@Repository
@SecondaryPort
public interface RiderPaymentMethodsRepository {

    Optional<RiderPaymentMethods> load(RiderId riderId);

    /// Every rider who ever added the card (normally one: a token belongs to one card at the processor).
    List<RiderPaymentMethods> loadHolding(CardToken card);

    /// @throws OptimisticLockingException if the stored version differs from `paymentMethods.version()`
    void save(RiderPaymentMethods paymentMethods);
}
