package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Objects;

/// An official dock where bikes are locked. Its id is its name (`Central`, `Old Town`, `Riverside`). It docks at most
/// `capacity` bikes; most stations hold 20.
@ValueObject
public record Station(StationId id, Position position, int capacity) {

    public static final int USUAL_CAPACITY = 20;

    public Station {
        Objects.requireNonNull(id, "station id");
        Objects.requireNonNull(position, "station position");
    }

    public Station(StationId id, Position position) {
        this(id, position, USUAL_CAPACITY);
    }

    /// Admits a returned bike when the station has room for it, given how many bikes already dock here.
    ///
    /// @throws StationFull when `bikesDocked` already fills the station
    public void admitReturn(int bikesDocked) {
        if (bikesDocked >= capacity) {
            throw new StationFull(id.value() + " is full");
        }
    }

    @ValueObject
    public record StationId(String value) {

        public StationId {
            Objects.requireNonNull(value, "station id");
        }
    }
}
