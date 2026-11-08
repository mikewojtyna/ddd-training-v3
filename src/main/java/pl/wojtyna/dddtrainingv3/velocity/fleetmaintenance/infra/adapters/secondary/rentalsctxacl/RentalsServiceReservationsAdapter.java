package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;

import java.time.Instant;
import java.util.Map;

/// *Reserve a broken bike for a technician* → Rentals' *Reserve Bike* for a `TECHNICIAN` holder
/// (`POST /rentals/bikes/{bikeId}/reservations`). `409 BikeAlreadyHeld` → `AlreadyReserved`; any other `409` (e.g.
/// `BikeNotWithdrawn`) → `ReservationRefused`; `404` → `UnknownBike`.
@SecondaryAdapter
public class RentalsServiceReservationsAdapter implements ServiceReservations {

    private record ReservationResponse(String bikeId, String holderType, String holderId, Instant reservedAt, Instant expiresAt) {
    }

    private final RentalsResource rentals;

    public RentalsServiceReservationsAdapter(RestClient restClient, VelocityServiceUrls urls) {
        this.rentals = new RentalsResource(restClient, urls);
    }

    @Override
    public Instant reserve(BikeId bike, TechnicianId technician) {
        var reply = rentals.post("/rentals/bikes/" + bike.value() + "/reservations",
                                 Map.of("holderType", "TECHNICIAN", "holderId", technician.value()), ReservationResponse.class);
        if (reply.ok()) {
            return reply.body().expiresAt();
        }
        if (reply.status() == 404) {
            throw new UnknownBike("VeloCity's fleet has no bike " + bike.value());
        }
        if (reply.status() == 409 && "BikeAlreadyHeld".equals(reply.error())) {
            throw new AlreadyReserved("Bike " + bike.value() + " is already held: " + reply.message());
        }
        if (reply.status() == 409) {
            throw new ReservationRefused("Rentals refused to reserve " + bike.value() + ": " + reply.error() + " " + reply.message());
        }
        throw new CollaboratorUnavailable("Rentals didn't reserve " + bike.value() + ": " + reply.status());
    }
}
