package pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike;

import org.jmolecules.ddd.annotation.ValueObject;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Station.StationId;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/// The stations Rentals knows: reference data (integration-contracts.md). A name that isn't here is not a station.
@ValueObject
public final class Stations {

    private static final Stations REFERENCE = new Stations(List.of(
        new Station(new StationId("Central"), new Position(52.2300, 21.0110)),
        new Station(new StationId("Old Town"), new Position(52.2490, 21.0122)),
        new Station(new StationId("Riverside"), new Position(52.2400, 21.0300))));

    private final Map<StationId, Station> stations;

    public Stations(List<Station> stations) {
        this.stations = stations.stream().collect(Collectors.toUnmodifiableMap(Station::id, Function.identity()));
    }

    public static Stations reference() {
        return REFERENCE;
    }

    public Optional<Station> find(StationId id) {
        return Optional.ofNullable(stations.get(id));
    }

    /// @throws UnknownStation if `id` names no station
    public Station named(StationId id) {
        return find(id).orElseThrow(() -> new UnknownStation(id.value() + " is not a station"));
    }
}
