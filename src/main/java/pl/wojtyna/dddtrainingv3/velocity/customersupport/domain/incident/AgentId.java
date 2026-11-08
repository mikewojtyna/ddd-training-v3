package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.ValueObject;

/// A support agent.
@ValueObject
public record AgentId(String value) {

    public AgentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Agent id is required");
        }
    }
}
