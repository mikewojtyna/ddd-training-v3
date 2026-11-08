package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import org.jmolecules.ddd.annotation.ValueObject;

/// Why the assigned agent rejected an incident; required (DE-97).
@ValueObject
public record RejectionReason(String value) {

    public RejectionReason {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }
    }
}
