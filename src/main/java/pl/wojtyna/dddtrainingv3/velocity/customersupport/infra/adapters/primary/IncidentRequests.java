package pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.primary;

/// REST bodies of the incident resources (integration-contracts.md, Customer Support).
public final class IncidentRequests {

    private IncidentRequests() {
    }

    public record ReportIncidentRequest(String riderId, String category, String description, String bikeId) {
    }

    public record AgentRequest(String agentId) {
    }

    public record RejectionRequest(String agentId, String reason) {
    }

    public record EscalationRequest(String agentId, String team) {
    }

    public record ResolutionRequest(String agentId, String resolution) {
    }

    public record ReopeningRequest(String riderId) {
    }

    public record FleetResolutionRequest(String bikeId) {
    }
}
