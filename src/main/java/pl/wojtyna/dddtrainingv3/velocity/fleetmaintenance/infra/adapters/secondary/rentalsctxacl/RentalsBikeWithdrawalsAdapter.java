package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeId;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeWithdrawals;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.CollaboratorUnavailable;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.UnknownBike;

import java.util.Map;

/// *Take the bike away from riders* → Rentals' *Withdraw Bike* (`POST /rentals/bikes/{bikeId}/withdrawal`, reason
/// recorded there as `REQUESTED`).
@SecondaryAdapter
public class RentalsBikeWithdrawalsAdapter implements BikeWithdrawals {

    private final RentalsResource rentals;

    public RentalsBikeWithdrawalsAdapter(RestClient restClient, VelocityServiceUrls urls) {
        this.rentals = new RentalsResource(restClient, urls);
    }

    @Override
    public void withdraw(BikeId bike) {
        var reply = rentals.post("/rentals/bikes/" + bike.value() + "/withdrawal", Map.of(), Void.class);
        if (reply.status() == 404) {
            throw new UnknownBike("VeloCity's fleet has no bike " + bike.value());
        }
        if (!reply.ok()) {
            throw new CollaboratorUnavailable("Rentals didn't withdraw " + bike.value() + ": " + reply.status() + " " + reply.error());
        }
    }
}
