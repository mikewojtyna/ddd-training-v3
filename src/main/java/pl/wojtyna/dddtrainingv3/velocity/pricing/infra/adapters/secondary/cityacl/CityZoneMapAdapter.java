package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.cityacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.Position;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.Zone;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone.ZoneMap;

import java.time.Instant;

/// Translates the City's approved drop-off areas into Pricing's zones: covered by any area → drop zone, otherwise the
/// unauthorized zone.
@SecondaryAdapter
public class CityZoneMapAdapter implements ZoneMap {

    private final SimulatedCityZoneMap city;

    public CityZoneMapAdapter(SimulatedCityZoneMap city) {
        this.city = city;
    }

    @Override
    public Zone zoneAt(Position position, Instant moment) {
        return city.areasCovering(position.latitude(), position.longitude(), moment).isEmpty()
               ? Zone.UNAUTHORIZED_ZONE
               : Zone.DROP_ZONE;
    }
}
