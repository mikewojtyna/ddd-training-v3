package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.pricingctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.joda.money.Money;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Pln;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.EndReason;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.FinishedRide;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.ride.RideFees;

/// Asks Pricing for the fee of a finished ride. Translates Rentals' end reason (Settlement's record of it) into
/// Pricing's *ending* (Settlement canvas UL "Ending (when asking Pricing)") and takes Pricing's `total` as the fee.
@SecondaryAdapter
public class PricingRideFees implements RideFees {

    private record SessionPriceResponse(String tariffId, int billingHours, String hourlyFees, String returnFee,
                                        String returnFeeKind, String penalty, String total) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    public PricingRideFees(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    @Override
    public Money feeOf(FinishedRide ride) {
        var uri = UriComponentsBuilder.fromUriString(urls.url("/pricing/session-price"))
                                      .queryParam("startedAt", ride.startedAt().toString())
                                      .queryParam("endedAt", ride.endedAt().toString())
                                      .queryParam("ending", endingOf(ride.endReason()));
        ride.position().ifPresent(position -> uri.queryParam("latitude", position.latitude().toPlainString())
                                                 .queryParam("longitude", position.longitude().toPlainString()));
        var price = restClient.get().uri(uri.build().toUri()).retrieve().body(SessionPriceResponse.class);
        if (price == null || price.total() == null) {
            throw new IllegalStateException("Pricing gave no price for session " + ride.session().value());
        }
        return Pln.pln(price.total());
    }

    static String endingOf(EndReason endReason) {
        return switch (endReason) {
            case STATION_RETURN -> "STATION";
            case FREE_FLOATING_RETURN -> "FREE_FLOATING";
            case LOCK_FAILURE -> "LOCK_FAILURE";
            case MAX_RENTAL_TIME_EXCEEDED -> "MAX_RENTAL_TIME";
        };
    }
}
