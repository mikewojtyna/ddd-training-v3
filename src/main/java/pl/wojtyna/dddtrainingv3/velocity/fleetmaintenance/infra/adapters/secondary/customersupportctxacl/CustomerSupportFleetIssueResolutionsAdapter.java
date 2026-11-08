package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.customersupportctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.FleetIssueNotResolved;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.IncidentId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.FleetIssueResolutions;

import java.util.Map;

/// *The incident's bike is fixed* → Customer Support's *Resolve Fleet Issue*. `200` also for an already resolved
/// incident (idempotent there, DE-55); `404 IncidentNotFound` and `409 NotAFleetIssueOfBike` → `FleetIssueNotResolved`.
@SecondaryAdapter
public class CustomerSupportFleetIssueResolutionsAdapter implements FleetIssueResolutions {

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    public CustomerSupportFleetIssueResolutionsAdapter(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    @Override
    public void resolve(IncidentId incident, BikeId bike) {
        int status;
        try {
            status = restClient.post()
                               .uri(urls.url("/customer-support/incidents/{incidentId}/fleet-resolution"), incident.value())
                               .contentType(MediaType.APPLICATION_JSON)
                               .body(Map.of("bikeId", bike.value()))
                               .exchange((request, response) -> response.getStatusCode().value());
        } catch (RestClientException failure) {
            throw new FleetIssueNotResolved("Customer Support didn't answer for incident " + incident.value(), failure);
        }
        if (status < 200 || status >= 300) {
            throw new FleetIssueNotResolved("Customer Support didn't resolve incident " + incident.value() + ": " + status);
        }
    }
}
