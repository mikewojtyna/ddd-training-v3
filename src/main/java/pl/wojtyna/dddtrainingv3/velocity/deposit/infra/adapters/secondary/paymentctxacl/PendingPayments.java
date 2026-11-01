package pl.wojtyna.dddtrainingv3.velocity.deposit.infra.adapters.secondary.paymentctxacl;

import pl.wojtyna.dddtrainingv3.velocity.deposit.domain.account.Deposit;
import pl.wojtyna.dddtrainingv3.velocity.payment.api.PaymentToken;

public interface PendingPayments {

    void add(PaymentToken paymentToken, Deposit deposit);

    PaymentToken getPaymentToken(Deposit deposit);
}
