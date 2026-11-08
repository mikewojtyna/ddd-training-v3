package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// Whoever reserved or unlocked a bike: a *rider* (for a ride) or a *technician* (for service).
@ValueObject
public record Holder(HolderType type, String id) {

    public enum HolderType {
        RIDER,
        TECHNICIAN
    }

    public Holder {
        Objects.requireNonNull(type, "holder type");
        Objects.requireNonNull(id, "holder id");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Holder id cannot be blank");
        }
    }

    public static Holder rider(String riderId) {
        return new Holder(HolderType.RIDER, riderId);
    }

    public static Holder technician(String technicianId) {
        return new Holder(HolderType.TECHNICIAN, technicianId);
    }

    public boolean isRider() {
        return type == HolderType.RIDER;
    }
}
