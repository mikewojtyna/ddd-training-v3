package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery;

import org.jmolecules.ddd.annotation.ValueObject;

/// A collections agent who pauses or resumes recovery (not authenticated; DE-89: any agent may).
@ValueObject
public record AgentId(String value) {

    public AgentId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("AgentId cannot be blank");
        }
    }
}
