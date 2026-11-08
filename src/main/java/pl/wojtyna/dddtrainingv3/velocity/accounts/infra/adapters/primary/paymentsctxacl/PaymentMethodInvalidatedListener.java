package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.primary.paymentsctxacl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccount;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccountUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountNotFound;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.events.PaymentMethodInvalidated;

/// Policy across contexts: whenever Payments reports a rider's payment method invalidated, the account is blocked
/// for *invalid card* (DE-72, DE-93). Asynchronous, as from a message broker; a repeated report changes nothing
/// (the block is idempotent per reason, DE-94).
@PrimaryAdapter
public class PaymentMethodInvalidatedListener {

    private static final int MAX_ATTEMPTS = 3; // a concurrent change of the same account (e.g. the rider adding a card) is retried

    private static final Logger log = LoggerFactory.getLogger(PaymentMethodInvalidatedListener.class);

    private final BlockAccountUseCase blockAccount;

    public PaymentMethodInvalidatedListener(BlockAccountUseCase blockAccount) {
        this.blockAccount = blockAccount;
    }

    @Async
    @EventListener
    public void on(PaymentMethodInvalidated event) {
        for (var attempt = 1; ; attempt++) {
            try {
                blockAccount.handle(new BlockAccount(event.riderId(), "INVALID_CARD"));
                return;
            } catch (AccountNotFound unknownRider) {
                log.warn("Payment method {} invalidated for rider {} who has no account; nothing to block",
                         event.paymentMethodId(), event.riderId());
                return;
            } catch (OptimisticLockingException concurrentChange) {
                if (attempt == MAX_ATTEMPTS) {
                    throw concurrentChange;
                }
            }
        }
    }
}
