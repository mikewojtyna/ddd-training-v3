package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;

import java.util.Map;

/// *Unlock a broken bike for its technician* → Rentals' *Unlock Bike* for a `TECHNICIAN` holder
/// (`POST /rentals/bikes/{bikeId}/unlock`). `409 NotReservationHolder` → `NotYourReservation`; any other `409` →
/// `UnlockRefused`; `404` → `UnknownBike`.
@SecondaryAdapter
public class RentalsServiceUnlocksAdapter implements ServiceUnlocks {

    private record UnlockResponse(String bikeId, String outcome, String sessionId) {
    }

    private final RentalsResource rentals;

    public RentalsServiceUnlocksAdapter(RestClient restClient, VelocityServiceUrls urls) {
        this.rentals = new RentalsResource(restClient, urls);
    }

    @Override
    public Outcome unlock(BikeId bike, TechnicianId technician) {
        var reply = rentals.post("/rentals/bikes/" + bike.value() + "/unlock",
                                 Map.of("holderType", "TECHNICIAN", "holderId", technician.value()), UnlockResponse.class);
        if (reply.ok()) {
            return "UNLOCKED".equals(reply.body().outcome()) ? Outcome.UNLOCKED : Outcome.UNLOCK_FAILED;
        }
        if (reply.status() == 404) {
            throw new UnknownBike("VeloCity's fleet has no bike " + bike.value());
        }
        if (reply.status() == 409 && "NotReservationHolder".equals(reply.error())) {
            throw new NotYourReservation("Technician " + technician.value() + " doesn't hold a reservation of " + bike.value());
        }
        if (reply.status() == 409) {
            throw new UnlockRefused("Rentals refused to unlock " + bike.value() + ": " + reply.error() + " " + reply.message());
        }
        throw new CollaboratorUnavailable("Rentals didn't unlock " + bike.value() + ": " + reply.status());
    }
}
