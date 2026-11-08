package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;

import java.util.Map;

/// *Offer a fixed bike to riders again* → Rentals' *Make Bike Available* (`POST /rentals/bikes/{bikeId}/availability`
/// with `{stationId}` or `{latitude, longitude}`). `409 UnknownStation` → `UnknownStation`; any other `409` (e.g.
/// `BikeNotUnlockedForService`) → `CirculationReturnRefused`; `404` → `UnknownBike`.
@SecondaryAdapter
public class RentalsCirculationReturnsAdapter implements CirculationReturns {

    private final RentalsResource rentals;

    public RentalsCirculationReturnsAdapter(RestClient restClient, VelocityServiceUrls urls) {
        this.rentals = new RentalsResource(restClient, urls);
    }

    @Override
    public void makeAvailable(BikeId bike, Place place) {
        Map<String, Object> body = switch (place) {
            case Place.AtStation station -> Map.of("stationId", station.stationId());
            case Place.AtPosition position -> Map.of("latitude", position.latitude(), "longitude", position.longitude());
        };
        var reply = rentals.post("/rentals/bikes/" + bike.value() + "/availability", body, Void.class);
        if (reply.ok()) {
            return;
        }
        if (reply.status() == 404) {
            throw new UnknownBike("VeloCity's fleet has no bike " + bike.value());
        }
        if (reply.status() == 409 && "UnknownStation".equals(reply.error())) {
            throw new UnknownStation(reply.message());
        }
        if (reply.status() == 409) {
            throw new CirculationReturnRefused("Rentals refused to make " + bike.value() + " available: " + reply.error() + " " + reply.message());
        }
        throw new CollaboratorUnavailable("Rentals didn't make " + bike.value() + " available: " + reply.status());
    }
}
