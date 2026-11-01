package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.SettleRideUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charge.PaymentsCharging;
import pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctx.PaymentsChargingAdapter;

@Configuration
public class SettlementContextConfig {

    @Bean
    public PaymentsCharging settlementPaymentsCharging() {
        return new PaymentsChargingAdapter();
    }

    @Bean
    public SettleRideUseCase settlementSettleRideUseCase(PaymentsCharging settlementPaymentsCharging) {
        return new SettleRideUseCase(settlementPaymentsCharging);
    }
}
