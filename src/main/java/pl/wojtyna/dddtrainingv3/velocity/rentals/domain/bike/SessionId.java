package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;
import java.util.UUID;

/// Identity of a rental session; a new one is drawn when a rider's unlock starts a session.
@ValueObject
public record SessionId(String value) {

    public SessionId {
        Objects.requireNonNull(value, "session id");
    }

    static SessionId newSession() {
        return new SessionId(UUID.randomUUID().toString());
    }
}
