package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.secondary.accountsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibility;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibilityUnknown;

/// Asks Accounts whether a rider may rent. Any failure to get an answer is `RentalEligibilityUnknown` (fail closed).
@SecondaryAdapter
public class AccountsRentalEligibilityAdapter implements RentalEligibility {

    private record RentalEligibilityResponse(String riderId, boolean eligible, String reason) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    public AccountsRentalEligibilityAdapter(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    @Override
    public Eligibility of(Holder rider) {
        try {
            var response = restClient.get()
                                     .uri(urls.url("/accounts/riders/{riderId}/rental-eligibility"), rider.id())
                                     .retrieve()
                                     .body(RentalEligibilityResponse.class);
            return response != null && response.eligible() ? Eligibility.ELIGIBLE : Eligibility.NOT_ELIGIBLE;
        } catch (RestClientException exception) {
            throw new RentalEligibilityUnknown("Accounts didn't answer whether rider " + rider.id() + " is eligible", exception);
        }
    }
}
