package pl.wojtyna.dddtrainingv3.velocity.deposit.domain.payment;

import org.jmolecules.architecture.hexagonal.SecondaryPort;
import pl.wojtyna.dddtrainingv3.velocity.deposit.domain.account.Deposit;

@SecondaryPort
public interface PaymentProcessor {

    void process(Deposit deposit);
}
