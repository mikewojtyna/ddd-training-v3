package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// An official dock where bikes are locked. Its id is its name (`Central`, `Old Town`, `Riverside`).
@ValueObject
public record Station(StationId id, Position position) {

    public Station {
        Objects.requireNonNull(id, "station id");
        Objects.requireNonNull(position, "station position");
    }

    @ValueObject
    public record StationId(String value) {

        public StationId {
            Objects.requireNonNull(value, "station id");
        }
    }
}
