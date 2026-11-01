package pl.wojtyna.dddtrainingv3.velocity.deposit.infra.adapters.secondary.paymentctxacl;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.velocity.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.deposit.domain.account.Deposit;
import pl.wojtyna.dddtrainingv3.velocity.deposit.domain.payment.PaymentProcessor;
import pl.wojtyna.dddtrainingv3.velocity.payment.api.PaymentGateway;
import pl.wojtyna.dddtrainingv3.velocity.payment.api.PaymentToken;

@Configuration
public class PaymentContextAclConfig {

    @Bean
    public PaymentProcessor paymentProcessor(DomainEventPublisher eventPublisher) {
        return new PaymentGatewayBoundedContextIntegrationProcessor(PaymentGateway.defaultGateway(),
                                                                    new PendingPayments() {
                                                                        @Override
                                                                        public void add(PaymentToken paymentToken,
                                                                                        Deposit deposit) {

                                                                        }

                                                                        @Override
                                                                        public PaymentToken getPaymentToken(Deposit deposit) {
                                                                            return null;
                                                                        }
                                                                    }, eventPublisher);
    }
}
