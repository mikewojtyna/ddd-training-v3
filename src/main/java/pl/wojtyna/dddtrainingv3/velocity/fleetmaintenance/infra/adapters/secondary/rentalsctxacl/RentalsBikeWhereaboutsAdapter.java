package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BikeWhereabouts;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikeView.Whereabouts;

import java.math.BigDecimal;
import java.util.Optional;

/// *Where is the bike?* → Rentals' *Bike Availability* (`GET /rentals/bikes/{bikeId}`); only the station and the
/// position are taken. Unknown to Rentals, or no answer → empty.
@SecondaryAdapter
public class RentalsBikeWhereaboutsAdapter implements BikeWhereabouts {

    private record BikeAvailabilityResponse(String bikeId, String availability, String hold, String holderType, String holderId,
                                            BigDecimal latitude, BigDecimal longitude, String stationId) {
    }

    private final RentalsResource rentals;

    public RentalsBikeWhereaboutsAdapter(RestClient restClient, VelocityServiceUrls urls) {
        this.rentals = new RentalsResource(restClient, urls);
    }

    @Override
    public Optional<Whereabouts> of(String bikeId) {
        try {
            var reply = rentals.get("/rentals/bikes/" + bikeId, BikeAvailabilityResponse.class);
            if (!reply.ok() || reply.body() == null) {
                return Optional.empty();
            }
            var bike = reply.body();
            return Optional.of(new Whereabouts(bike.stationId(), bike.latitude(), bike.longitude()));
        } catch (RuntimeException noAnswer) {
            return Optional.empty();
        }
    }
}
