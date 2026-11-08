package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.PriceForSessionQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.TariffsQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishTariffUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee.FeeCalculator;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.LaunchTariff;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffSchedule;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.TariffScheduleRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.ZoneMap;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.InMemoryTariffScheduleRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.cityacl.CityZoneMapAdapter;
import pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.cityacl.SimulatedCityZoneMap;

import java.time.Clock;

/// Wiring of the Pricing bounded context. The tariff schedule is established at start-up with the launch tariff
/// (DE-30, valid from 2026-01-01T00:00:00Z); later tariffs come through Publish Tariff.
@Configuration
public class PricingContextConfig {

    @Bean
    public TariffScheduleRepository pricingTariffScheduleRepository() {
        var repository = new InMemoryTariffScheduleRepository();
        repository.save(TariffSchedule.startingWith(LaunchTariff.tariff()));
        return repository;
    }

    @Bean
    public SimulatedCityZoneMap pricingSimulatedCityZoneMap() {
        return SimulatedCityZoneMap.warsawDropOffAreas();
    }

    @Bean
    public ZoneMap pricingZoneMap(SimulatedCityZoneMap pricingSimulatedCityZoneMap) {
        return new CityZoneMapAdapter(pricingSimulatedCityZoneMap);
    }

    @Bean
    public FeeCalculator pricingFeeCalculator() {
        return new FeeCalculator();
    }

    @Bean
    public PublishTariffUseCase pricingPublishTariffUseCase(TariffScheduleRepository pricingTariffScheduleRepository,
                                                            DomainEventPublisher eventPublisher,
                                                            Clock clock) {
        return new PublishTariffUseCase(pricingTariffScheduleRepository, eventPublisher, clock);
    }

    @Bean
    public TariffsQueryHandler pricingTariffsQueryHandler(TariffScheduleRepository pricingTariffScheduleRepository) {
        return new TariffsQueryHandler(pricingTariffScheduleRepository);
    }

    @Bean
    public PriceForSessionQueryHandler pricingPriceForSessionQueryHandler(
        TariffScheduleRepository pricingTariffScheduleRepository,
        ZoneMap pricingZoneMap,
        FeeCalculator pricingFeeCalculator) {
        return new PriceForSessionQueryHandler(pricingTariffScheduleRepository, pricingZoneMap, pricingFeeCalculator);
    }
}
