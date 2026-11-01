package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceCatalogRepository;

import java.time.Clock;

@PrimaryPort
public class EstimateFeeQueryHandler {

    private final PriceCatalogRepository priceCatalogRepository;
    private final Clock clock;

    public EstimateFeeQueryHandler(PriceCatalogRepository priceCatalogRepository, Clock clock) {
        this.priceCatalogRepository = priceCatalogRepository;
        this.clock = clock;
    }

    /**
     * @return fee of the ride under the price list valid now, without return charges
     */
    public Money handle(EstimateFee query) {
        return priceCatalogRepository.load().estimateFee(query.rideDuration(), clock.instant());
    }
}
