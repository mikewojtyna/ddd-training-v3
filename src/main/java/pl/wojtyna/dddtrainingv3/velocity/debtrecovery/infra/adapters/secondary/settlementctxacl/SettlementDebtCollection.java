package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.settlementctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.CollectionOutcome;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.DebtCollection;
import pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.RiderId;

import java.util.Optional;

import static pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recovery.Pln.pln;

/// Calls Settlement's *Collect Outstanding Debt* (`POST /settlement/riders/{riderId}/debt-collections` →
/// `{collected, collectedAmount, outstandingDebt}`) and translates the answer into a `CollectionOutcome`. Settlement
/// answers `outstandingDebt = "0.00"` when nothing is owed any more, which the domain reads as a cleared debt. A
/// request that gets no proper answer (error status, Settlement unreachable) is an unanswered, failed collection.
@SecondaryAdapter
public class SettlementDebtCollection implements DebtCollection {

    private static final Logger log = LoggerFactory.getLogger(SettlementDebtCollection.class);

    private record CollectionAnswer(Boolean collected, String collectedAmount, String outstandingDebt) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    public SettlementDebtCollection(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    @Override
    public CollectionOutcome collect(RiderId rider) {
        try {
            var answer = restClient.post()
                                   .uri(urls.url("/settlement/riders/" + rider.value() + "/debt-collections"))
                                   .retrieve()
                                   .body(CollectionAnswer.class);
            if (answer == null) {
                return CollectionOutcome.unanswered();
            }
            return new CollectionOutcome(Boolean.TRUE.equals(answer.collected()),
                                         Optional.ofNullable(answer.outstandingDebt()).map(debt -> pln(debt)));
        } catch (RestClientException failure) {
            log.warn("Settlement didn't answer the collection for rider {}", rider.value(), failure);
            return CollectionOutcome.unanswered();
        }
    }
}
