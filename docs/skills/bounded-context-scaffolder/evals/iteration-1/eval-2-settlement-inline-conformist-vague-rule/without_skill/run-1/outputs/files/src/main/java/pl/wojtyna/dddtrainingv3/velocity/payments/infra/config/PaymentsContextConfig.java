package pl.wojtyna.dddtrainingv3.velocity.payments.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeRiderUseCase;

@Configuration
public class PaymentsContextConfig {

    @Bean
    public ChargeRiderUseCase chargeRiderUseCase() {
        // workshop stub: the payment processor integration is not implemented yet
        return command -> {
            throw new UnsupportedOperationException("Implement charging in the Payments context");
        };
    }
}
