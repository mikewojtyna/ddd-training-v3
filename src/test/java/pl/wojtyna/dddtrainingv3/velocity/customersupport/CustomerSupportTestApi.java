package pl.wojtyna.dddtrainingv3.velocity.customersupport;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.PayloadApplicationEvent;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.events.FleetIssueReported;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries.IncidentView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/// Test client of Customer Support's REST resources, plus a recorder of the published `FleetIssueReported` events
/// (plan rider-reports-incident D7). The recorder is added to the shared application context once; tests filter it
/// by their own incident ids.
final class CustomerSupportTestApi {

    private static final List<FleetIssueReported> FLEET_ISSUES = Collections.synchronizedList(new ArrayList<>());
    private static final Map<ConfigurableApplicationContext, Boolean> REGISTERED = Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static final tools.jackson.databind.json.JsonMapper JSON = tools.jackson.databind.json.JsonMapper.builder().build();

    private final RestClient client;

    CustomerSupportTestApi(int port, ConfigurableApplicationContext context) {
        this.client = RestClient.builder().baseUrl("http://localhost:" + port + "/customer-support/incidents").build();
        REGISTERED.computeIfAbsent(context, ctx -> {
            ctx.addApplicationListener(new FleetIssueRecorder());
            return true;
        });
    }

    /// A Warsaw local time (as the examples state them), e.g. `warsaw("2026-11-09T10:00")`.
    static java.time.Instant warsaw(String localDateTime) {
        return java.time.LocalDateTime.parse(localDateTime).atZone(java.time.ZoneId.of("Europe/Warsaw")).toInstant();
    }

    static String unique(String label) {
        return label + "-" + UUID.randomUUID();
    }

    record Response(int status, Map<String, Object> body) {

        String error() {
            return body == null ? null : (String) body.get("error");
        }
    }

    Response post(String path, Map<String, Object> body) {
        return client.post().uri(path).contentType(MediaType.APPLICATION_JSON).body(body)
                     .exchange((request, response) -> {
                         var raw = new String(response.getBody().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
                         @SuppressWarnings("unchecked")
                         Map<String, Object> parsed = raw.isBlank() ? null : JSON.readValue(raw, Map.class);
                         return new Response(response.getStatusCode().value(), parsed);
                     });
    }

    String report(String riderId, String category, String description, String bikeId) {
        var body = new java.util.HashMap<String, Object>();
        body.put("riderId", riderId);
        body.put("category", category);
        body.put("description", description);
        body.put("bikeId", bikeId);
        var response = post("", body);
        if (response.status() != 201) {
            throw new IllegalStateException("report failed: " + response);
        }
        return (String) response.body().get("incidentId");
    }

    Response pickUp(String incidentId, String agentId) {
        return post("/" + incidentId + "/assignment", Map.of("agentId", agentId));
    }

    Response accept(String incidentId, String agentId) {
        return post("/" + incidentId + "/acceptance", Map.of("agentId", agentId));
    }

    Response reject(String incidentId, String agentId, String reason) {
        return post("/" + incidentId + "/rejection", Map.of("agentId", agentId, "reason", reason));
    }

    Response escalate(String incidentId, String agentId, String team) {
        return post("/" + incidentId + "/escalation", Map.of("agentId", agentId, "team", team));
    }

    Response resolve(String incidentId, String agentId, String resolution) {
        return post("/" + incidentId + "/resolution", Map.of("agentId", agentId, "resolution", resolution));
    }

    Response reopen(String incidentId, String riderId) {
        return post("/" + incidentId + "/reopening", Map.of("riderId", riderId));
    }

    Response resolveFleetIssue(String incidentId, String bikeId) {
        return post("/" + incidentId + "/fleet-resolution", Map.of("bikeId", bikeId));
    }

    IncidentView incident(String incidentId) {
        return client.get().uri("/" + incidentId).retrieve().body(IncidentView.class);
    }

    int incidentStatusCode(String incidentId, String riderId) {
        return client.get().uri(builder -> builder.path("/" + incidentId).queryParam("riderId", riderId).build())
                     .exchange((request, response) -> response.getStatusCode()).value();
    }

    IncidentView incidentAsRider(String incidentId, String riderId) {
        return client.get().uri(builder -> builder.path("/" + incidentId).queryParam("riderId", riderId).build())
                     .retrieve().body(IncidentView.class);
    }

    List<Map<String, Object>> openIncidents() {
        return client.get().uri(builder -> builder.queryParam("status", "OPEN").build())
                     .retrieve().body(new org.springframework.core.ParameterizedTypeReference<>() {
                     });
    }

    List<FleetIssueReported> fleetIssuesReportedFor(String incidentId) {
        synchronized (FLEET_ISSUES) {
            return FLEET_ISSUES.stream().filter(event -> event.incidentId().equals(incidentId)).toList();
        }
    }

    private static final class FleetIssueRecorder implements ApplicationListener<ApplicationEvent> {

        @Override
        public void onApplicationEvent(ApplicationEvent event) {
            if (event instanceof PayloadApplicationEvent<?> payload && payload.getPayload() instanceof FleetIssueReported fleetIssue) {
                FLEET_ISSUES.add(fleetIssue);
            }
        }
    }
}
