package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.accountsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.AccountBlocks;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.UnknownRiderAccountException;

/// Calls Accounts' *Block Account* / *Unblock Account* with reason `OUTSTANDING_DEBT`. Accounts' `404` (unknown rider)
/// becomes `UnknownRiderAccountException`.
@SecondaryAdapter
public class AccountsAccountBlocks implements AccountBlocks {

    private static final String OUTSTANDING_DEBT = "OUTSTANDING_DEBT";

    private record BlockRequest(String reason) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    public AccountsAccountBlocks(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    @Override
    public void blockForOutstandingDebt(RiderId rider) {
        restClient.post()
                  .uri(urls.url("/accounts/riders/" + rider.value() + "/blocks"))
                  .contentType(MediaType.APPLICATION_JSON)
                  .body(new BlockRequest(OUTSTANDING_DEBT))
                  .retrieve()
                  .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                      throw new UnknownRiderAccountException("Accounts doesn't know rider " + rider.value());
                  })
                  .toBodilessEntity();
    }

    @Override
    public void liftOutstandingDebtBlock(RiderId rider) {
        restClient.delete()
                  .uri(urls.url("/accounts/riders/" + rider.value() + "/blocks/" + OUTSTANDING_DEBT))
                  .retrieve()
                  .onStatus(status -> status.value() == HttpStatus.NOT_FOUND.value(), (request, response) -> {
                      throw new UnknownRiderAccountException("Accounts doesn't know rider " + rider.value());
                  })
                  .toBodilessEntity();
    }
}
