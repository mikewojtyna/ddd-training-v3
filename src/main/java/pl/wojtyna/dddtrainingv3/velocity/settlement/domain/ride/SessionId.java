package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride;

import org.jmolecules.ddd.annotation.ValueObject;

/// A ride is identified by the rental session it settles (Rentals' session id).
@ValueObject
public record SessionId(String value) {

    public SessionId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("SessionId cannot be blank");
        }
    }
}
