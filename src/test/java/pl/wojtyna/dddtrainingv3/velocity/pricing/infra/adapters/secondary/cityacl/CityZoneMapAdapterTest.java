package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.cityacl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.Position;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.Zone;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

@DisplayName("City zone map examples")
class CityZoneMapAdapterTest {

    private static final Instant TEN_THIRTY = Instant.parse("2026-11-09T10:30:00Z");

    private final SimulatedCityZoneMap city = SimulatedCityZoneMap.warsawDropOffAreas();
    private final CityZoneMapAdapter zoneMap = new CityZoneMapAdapter(city);

    @Test
    @DisplayName("(52.2298, 21.0123) lies in the Old Town Square drop zone")
    void positionNearOldTownSquareCentreIsInDropZone() {
        assertThat(zoneMap.zoneAt(new Position(52.2298, 21.0123), TEN_THIRTY)).isEqualTo(Zone.DROP_ZONE);
    }

    @Test
    @DisplayName("(52.2400, 21.0300), the centre of Riverside Park, lies in a drop zone")
    void riversideParkIsDropZone() {
        assertThat(zoneMap.zoneAt(new Position(52.2400, 21.0300), TEN_THIRTY)).isEqualTo(Zone.DROP_ZONE);
    }

    @Test
    @DisplayName("(52.23104, 21.0122) is 149 m from the Old Town Square centre (radius 150 m), so inside (DE-84)")
    void positionAtBorderIsInside() {
        assertThat(SimulatedCityZoneMap.distanceMetres(52.23104, 21.0122, 52.2297, 21.0122)).isCloseTo(149.0, within(0.1));
        assertThat(zoneMap.zoneAt(new Position(52.23104, 21.0122), TEN_THIRTY)).isEqualTo(Zone.DROP_ZONE);
    }

    @Test
    @DisplayName("(52.2311, 21.0122) is about 155.7 m from the Old Town Square centre, so outside")
    void positionJustBeyondBorderIsOutside() {
        assertThat(zoneMap.zoneAt(new Position(52.2311, 21.0122), TEN_THIRTY)).isEqualTo(Zone.UNAUTHORIZED_ZONE);
    }

    @Test
    @DisplayName("(52.2500, 21.1000) lies outside every drop zone")
    void farPositionIsUnauthorizedZone() {
        assertThat(zoneMap.zoneAt(new Position(52.2500, 21.1000), TEN_THIRTY)).isEqualTo(Zone.UNAUTHORIZED_ZONE);
    }

    @Test
    @DisplayName("Old Town Square withdrawn from 10:20 until midnight: unauthorized at 10:30, a drop zone at 10:10 and after midnight")
    void withdrawnZoneCountsOnlyDuringItsWindow() {
        // given
        city.withdraw("Old Town Square", Instant.parse("2026-11-09T10:20:00Z"), Instant.parse("2026-11-10T00:00:00Z"));
        var position = new Position(52.2298, 21.0123);

        // then
        assertThat(zoneMap.zoneAt(position, TEN_THIRTY)).isEqualTo(Zone.UNAUTHORIZED_ZONE);
        assertThat(zoneMap.zoneAt(position, Instant.parse("2026-11-09T10:10:00Z"))).isEqualTo(Zone.DROP_ZONE);
        assertThat(zoneMap.zoneAt(position, Instant.parse("2026-11-10T00:00:00Z"))).isEqualTo(Zone.DROP_ZONE);
    }
}
