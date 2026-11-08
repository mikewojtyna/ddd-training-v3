package pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries.IncidentQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries.OpenIncidentsQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries.RiderIncidentsQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases.*;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.Incidents;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.primary.scheduling.UncontestedRejectionsJob;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.secondary.IntegrationEventsPublisher;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.secondary.persistence.InMemoryIncidentReadModel;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.secondary.persistence.InMemoryIncidents;

import java.time.Clock;
import java.time.ZoneId;

/// Wiring of the CustomerSupport bounded context. Domain classes have no Spring annotations.
@Configuration
public class CustomerSupportContextConfig {

    /// The city's time zone, in which the 14 calendar days of the reopening window are counted (plan
    /// rider-reopens-rejected-incident D5).
    @Bean
    public ZoneId customerSupportCityZone(@Value("${velocity.customer-support.zone:Europe/Warsaw}") String zone) {
        return ZoneId.of(zone);
    }

    @Bean
    public Incidents customerSupportIncidents() {
        return new InMemoryIncidents();
    }

    @Bean
    public InMemoryIncidentReadModel customerSupportIncidentReadModel() {
        return new InMemoryIncidentReadModel();
    }

    @Bean
    public IntegrationEventsPublisher customerSupportIntegrationEventsPublisher(ApplicationEventPublisher publisher) {
        return new IntegrationEventsPublisher(publisher);
    }

    @Bean
    public ReportIncidentUseCase customerSupportReportIncidentUseCase(Incidents customerSupportIncidents, DomainEventPublisher eventPublisher) {
        return new ReportIncidentUseCase(customerSupportIncidents, eventPublisher);
    }

    @Bean
    public PickUpIncidentUseCase customerSupportPickUpIncidentUseCase(Incidents customerSupportIncidents, DomainEventPublisher eventPublisher) {
        return new PickUpIncidentUseCase(customerSupportIncidents, eventPublisher);
    }

    @Bean
    public AcceptIncidentUseCase customerSupportAcceptIncidentUseCase(Incidents customerSupportIncidents, DomainEventPublisher eventPublisher) {
        return new AcceptIncidentUseCase(customerSupportIncidents, eventPublisher);
    }

    @Bean
    public RejectIncidentUseCase customerSupportRejectIncidentUseCase(Incidents customerSupportIncidents, DomainEventPublisher eventPublisher,
                                                                      ZoneId customerSupportCityZone) {
        return new RejectIncidentUseCase(customerSupportIncidents, eventPublisher, customerSupportCityZone);
    }

    @Bean
    public EscalateIncidentUseCase customerSupportEscalateIncidentUseCase(Incidents customerSupportIncidents, DomainEventPublisher eventPublisher) {
        return new EscalateIncidentUseCase(customerSupportIncidents, eventPublisher);
    }

    @Bean
    public ResolveIncidentUseCase customerSupportResolveIncidentUseCase(Incidents customerSupportIncidents, DomainEventPublisher eventPublisher) {
        return new ResolveIncidentUseCase(customerSupportIncidents, eventPublisher);
    }

    @Bean
    public ReopenIncidentUseCase customerSupportReopenIncidentUseCase(Incidents customerSupportIncidents, DomainEventPublisher eventPublisher) {
        return new ReopenIncidentUseCase(customerSupportIncidents, eventPublisher);
    }

    @Bean
    public ResolveFleetIssueUseCase customerSupportResolveFleetIssueUseCase(Incidents customerSupportIncidents, DomainEventPublisher eventPublisher) {
        return new ResolveFleetIssueUseCase(customerSupportIncidents, eventPublisher);
    }

    @Bean
    public ResolveUncontestedRejectionsUseCase customerSupportResolveUncontestedRejectionsUseCase(Incidents customerSupportIncidents,
                                                                                                  DomainEventPublisher eventPublisher) {
        return new ResolveUncontestedRejectionsUseCase(customerSupportIncidents, eventPublisher);
    }

    @Bean
    public IncidentQueryHandler customerSupportIncidentQueryHandler(InMemoryIncidentReadModel customerSupportIncidentReadModel) {
        return new IncidentQueryHandler(customerSupportIncidentReadModel);
    }

    @Bean
    public OpenIncidentsQueryHandler customerSupportOpenIncidentsQueryHandler(InMemoryIncidentReadModel customerSupportIncidentReadModel) {
        return new OpenIncidentsQueryHandler(customerSupportIncidentReadModel);
    }

    @Bean
    public RiderIncidentsQueryHandler customerSupportRiderIncidentsQueryHandler(InMemoryIncidentReadModel customerSupportIncidentReadModel) {
        return new RiderIncidentsQueryHandler(customerSupportIncidentReadModel);
    }

    @Bean
    public UncontestedRejectionsJob customerSupportUncontestedRejectionsJob(ResolveUncontestedRejectionsUseCase customerSupportResolveUncontestedRejectionsUseCase,
                                                                            Clock clock) {
        return new UncontestedRejectionsJob(customerSupportResolveUncontestedRejectionsUseCase, clock);
    }
}
