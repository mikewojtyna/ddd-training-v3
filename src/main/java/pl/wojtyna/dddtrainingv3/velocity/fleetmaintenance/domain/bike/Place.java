package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;

import java.math.BigDecimal;
import java.util.Objects;

/// Where a technician leaves a fixed bike: a station or a position (e.g. in a drop zone); trusted as given (DE-91).
@ValueObject
public sealed interface Place {

    static Place station(String stationId) {
        return new AtStation(stationId);
    }

    static Place position(BigDecimal latitude, BigDecimal longitude) {
        return new AtPosition(latitude, longitude);
    }

    record AtStation(String stationId) implements Place {

        public AtStation {
            if (stationId == null || stationId.isBlank()) {
                throw new IllegalArgumentException("Station id is required");
            }
        }
    }

    record AtPosition(BigDecimal latitude, BigDecimal longitude) implements Place {

        public AtPosition {
            Objects.requireNonNull(latitude, "Latitude is required");
            Objects.requireNonNull(longitude, "Longitude is required");
        }
    }
}
