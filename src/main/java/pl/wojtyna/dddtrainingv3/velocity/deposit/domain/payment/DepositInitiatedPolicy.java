package pl.wojtyna.dddtrainingv3.velocity.deposit.domain.payment;

import pl.wojtyna.dddtrainingv3.velocity.common.domain.DomainPolicy;
import pl.wojtyna.dddtrainingv3.velocity.deposit.domain.account.DepositInitiated;

public class DepositInitiatedPolicy implements DomainPolicy {

    private final PaymentProcessor paymentProcessor;

    public DepositInitiatedPolicy(PaymentProcessor paymentProcessor) {this.paymentProcessor = paymentProcessor;}

    public void handle(DepositInitiated event) {
        paymentProcessor.process(event.deposit());
    }
}
