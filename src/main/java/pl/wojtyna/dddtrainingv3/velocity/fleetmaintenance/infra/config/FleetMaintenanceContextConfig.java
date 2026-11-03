package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportFaultUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReturnBikeToCirculationUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.StartMaintenanceUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.InMemoryBikeRepository;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.customersupportctxacl.CustomerSupportFixNotificationsAdapter;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl.RentalsCirculationAdapter;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.event.FleetMaintenanceLocalEventsHandler;

@Configuration
public class FleetMaintenanceContextConfig {

    @Bean
    public BikeRepository fleetMaintenanceBikeRepository() {
        return new InMemoryBikeRepository();
    }

    @Bean
    public Circulation fleetMaintenanceCirculation() {
        return new RentalsCirculationAdapter();
    }

    @Bean
    public FixNotifications fleetMaintenanceFixNotifications() {
        return new CustomerSupportFixNotificationsAdapter();
    }

    @Bean
    public ReportFaultUseCase fleetMaintenanceReportFaultUseCase(BikeRepository fleetMaintenanceBikeRepository,
                                                                 DomainEventPublisher eventPublisher) {
        return new ReportFaultUseCase(fleetMaintenanceBikeRepository, eventPublisher);
    }

    @Bean
    public StartMaintenanceUseCase fleetMaintenanceStartMaintenanceUseCase(BikeRepository fleetMaintenanceBikeRepository,
                                                                           DomainEventPublisher eventPublisher) {
        return new StartMaintenanceUseCase(fleetMaintenanceBikeRepository, eventPublisher);
    }

    @Bean
    public ReturnBikeToCirculationUseCase fleetMaintenanceReturnBikeToCirculationUseCase(
        BikeRepository fleetMaintenanceBikeRepository, DomainEventPublisher eventPublisher) {
        return new ReturnBikeToCirculationUseCase(fleetMaintenanceBikeRepository, eventPublisher);
    }

    @Bean
    public ReturnFixedBikeToCirculationPolicy fleetMaintenanceReturnFixedBikeToCirculationPolicy(
        Circulation fleetMaintenanceCirculation) {
        return new ReturnFixedBikeToCirculationPolicy(fleetMaintenanceCirculation);
    }

    @Bean
    public NotifySupportOfFixedBikePolicy fleetMaintenanceNotifySupportOfFixedBikePolicy(
        FixNotifications fleetMaintenanceFixNotifications) {
        return new NotifySupportOfFixedBikePolicy(fleetMaintenanceFixNotifications);
    }

    @Bean
    public FleetMaintenanceLocalEventsHandler fleetMaintenanceLocalEventsHandler(
        ReturnFixedBikeToCirculationPolicy fleetMaintenanceReturnFixedBikeToCirculationPolicy,
        NotifySupportOfFixedBikePolicy fleetMaintenanceNotifySupportOfFixedBikePolicy) {
        return new FleetMaintenanceLocalEventsHandler(fleetMaintenanceReturnFixedBikeToCirculationPolicy,
                                                      fleetMaintenanceNotifySupportOfFixedBikePolicy);
    }
}
