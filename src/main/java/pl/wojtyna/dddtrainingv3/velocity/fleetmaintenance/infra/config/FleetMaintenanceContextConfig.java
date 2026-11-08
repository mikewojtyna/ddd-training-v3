package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BikeWhereabouts;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikeQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikesQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.*;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.customersupportctxacl.FleetIssueReportedListener;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.rentalsctxacl.BikeUnlockedListener;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.rentalsctxacl.BikeWithdrawnListener;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.rentalsctxacl.ReservationExpiredListener;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary.rentalsctxacl.UnlockFailedListener;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.InMemoryBikes;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.customersupportctxacl.CustomerSupportFleetIssueResolutionsAdapter;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl.*;

/// Wiring of the FleetMaintenance bounded context. Domain classes have no Spring annotations.
@Configuration
public class FleetMaintenanceContextConfig {

    // --- persistence and read side ---

    @Bean
    public InMemoryBikes fleetMaintenanceBikes() {
        return new InMemoryBikes();
    }

    // --- ACLs to Rentals and Customer Support ---

    @Bean
    public BikeWithdrawals fleetMaintenanceBikeWithdrawals(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new RentalsBikeWithdrawalsAdapter(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public ServiceReservations fleetMaintenanceServiceReservations(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new RentalsServiceReservationsAdapter(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public ServiceUnlocks fleetMaintenanceServiceUnlocks(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new RentalsServiceUnlocksAdapter(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public CirculationReturns fleetMaintenanceCirculationReturns(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new RentalsCirculationReturnsAdapter(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public BikeWhereabouts fleetMaintenanceBikeWhereabouts(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new RentalsBikeWhereaboutsAdapter(velocityRestClient, velocityServiceUrls);
    }

    @Bean
    public FleetIssueResolutions fleetMaintenanceFleetIssueResolutions(RestClient velocityRestClient, VelocityServiceUrls velocityServiceUrls) {
        return new CustomerSupportFleetIssueResolutionsAdapter(velocityRestClient, velocityServiceUrls);
    }

    // --- use cases and queries ---

    @Bean
    public ReportBikeBrokenUseCase fleetMaintenanceReportBikeBrokenUseCase(InMemoryBikes fleetMaintenanceBikes,
                                                                           BikeWithdrawals fleetMaintenanceBikeWithdrawals,
                                                                           DomainEventPublisher eventPublisher) {
        return new ReportBikeBrokenUseCase(fleetMaintenanceBikes, fleetMaintenanceBikeWithdrawals, eventPublisher);
    }

    @Bean
    public ReportFaultUseCase fleetMaintenanceReportFaultUseCase(InMemoryBikes fleetMaintenanceBikes,
                                                                 BikeWithdrawals fleetMaintenanceBikeWithdrawals,
                                                                 DomainEventPublisher eventPublisher) {
        return new ReportFaultUseCase(fleetMaintenanceBikes, fleetMaintenanceBikeWithdrawals, eventPublisher);
    }

    @Bean
    public ReserveBrokenBikeUseCase fleetMaintenanceReserveBrokenBikeUseCase(InMemoryBikes fleetMaintenanceBikes,
                                                                             ServiceReservations fleetMaintenanceServiceReservations,
                                                                             DomainEventPublisher eventPublisher) {
        return new ReserveBrokenBikeUseCase(fleetMaintenanceBikes, fleetMaintenanceServiceReservations, eventPublisher);
    }

    @Bean
    public UnlockBrokenBikeUseCase fleetMaintenanceUnlockBrokenBikeUseCase(InMemoryBikes fleetMaintenanceBikes,
                                                                           ServiceUnlocks fleetMaintenanceServiceUnlocks) {
        return new UnlockBrokenBikeUseCase(fleetMaintenanceBikes, fleetMaintenanceServiceUnlocks);
    }

    @Bean
    public StartMaintenanceUseCase fleetMaintenanceStartMaintenanceUseCase(InMemoryBikes fleetMaintenanceBikes,
                                                                           DomainEventPublisher eventPublisher) {
        return new StartMaintenanceUseCase(fleetMaintenanceBikes, eventPublisher);
    }

    @Bean
    public ReleaseTechnicianReservationUseCase fleetMaintenanceReleaseTechnicianReservationUseCase(InMemoryBikes fleetMaintenanceBikes,
                                                                                                   DomainEventPublisher eventPublisher) {
        return new ReleaseTechnicianReservationUseCase(fleetMaintenanceBikes, eventPublisher);
    }

    @Bean
    public ReturnBikeToCirculationUseCase fleetMaintenanceReturnBikeToCirculationUseCase(InMemoryBikes fleetMaintenanceBikes,
                                                                                         CirculationReturns fleetMaintenanceCirculationReturns,
                                                                                         FleetIssueResolutions fleetMaintenanceFleetIssueResolutions,
                                                                                         DomainEventPublisher eventPublisher) {
        return new ReturnBikeToCirculationUseCase(fleetMaintenanceBikes, fleetMaintenanceCirculationReturns,
                                                  fleetMaintenanceFleetIssueResolutions, eventPublisher);
    }

    @Bean
    public BrokenBikeQueryHandler fleetMaintenanceBrokenBikeQueryHandler(InMemoryBikes fleetMaintenanceBikes,
                                                                         BikeWhereabouts fleetMaintenanceBikeWhereabouts) {
        return new BrokenBikeQueryHandler(fleetMaintenanceBikes, fleetMaintenanceBikeWhereabouts);
    }

    @Bean
    public BrokenBikesQueryHandler fleetMaintenanceBrokenBikesQueryHandler(InMemoryBikes fleetMaintenanceBikes,
                                                                           BikeWhereabouts fleetMaintenanceBikeWhereabouts) {
        return new BrokenBikesQueryHandler(fleetMaintenanceBikes, fleetMaintenanceBikeWhereabouts);
    }

    // --- inbound ACL listeners ---

    @Bean
    public BikeWithdrawnListener fleetMaintenanceBikeWithdrawnListener(ReportBikeBrokenUseCase fleetMaintenanceReportBikeBrokenUseCase) {
        return new BikeWithdrawnListener(fleetMaintenanceReportBikeBrokenUseCase);
    }

    @Bean
    public BikeUnlockedListener fleetMaintenanceBikeUnlockedListener(StartMaintenanceUseCase fleetMaintenanceStartMaintenanceUseCase) {
        return new BikeUnlockedListener(fleetMaintenanceStartMaintenanceUseCase);
    }

    @Bean
    public ReservationExpiredListener fleetMaintenanceReservationExpiredListener(
        ReleaseTechnicianReservationUseCase fleetMaintenanceReleaseTechnicianReservationUseCase) {
        return new ReservationExpiredListener(fleetMaintenanceReleaseTechnicianReservationUseCase);
    }

    @Bean
    public UnlockFailedListener fleetMaintenanceUnlockFailedListener(
        ReleaseTechnicianReservationUseCase fleetMaintenanceReleaseTechnicianReservationUseCase) {
        return new UnlockFailedListener(fleetMaintenanceReleaseTechnicianReservationUseCase);
    }

    @Bean
    public FleetIssueReportedListener fleetMaintenanceFleetIssueReportedListener(ReportBikeBrokenUseCase fleetMaintenanceReportBikeBrokenUseCase) {
        return new FleetIssueReportedListener(fleetMaintenanceReportBikeBrokenUseCase);
    }
}
