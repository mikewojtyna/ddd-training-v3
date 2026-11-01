package pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.infra.adapters.secondary.paymentctxacl;

import pl.wojtyna.dddtrainingv3.crowdsorcery.deposit.domain.account.Deposit;
import pl.wojtyna.dddtrainingv3.crowdsorcery.payment.api.PaymentToken;

public interface PendingPayments {

    void add(PaymentToken paymentToken, Deposit deposit);

    PaymentToken getPaymentToken(Deposit deposit);
}
