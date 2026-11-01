package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceList;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListRepository;

import java.time.Clock;
import java.time.Duration;
import java.util.Optional;

/// Canvas inbound query: "Estimate Fee" (rider app). Uses the price list valid now.
///
/// The estimate is a calculation, not a lookup, so the handler uses the domain's `PriceList` instead of a separate
/// read model.
@PrimaryPort
public class EstimateFeeQueryHandler {

    private final PriceListRepository priceLists;
    private final Clock clock;

    public EstimateFeeQueryHandler(PriceListRepository priceLists, Clock clock) {
        this.priceLists = priceLists;
        this.clock = clock;
    }

    public Optional<FeeEstimate> handle(EstimateFeeQuery query) {
        return PriceList.validAt(priceLists.all(), clock.instant())
                        .map(priceList -> {
                            var fee = priceList.estimate(Duration.ofMinutes(query.rideMinutes())).amount();
                            return new FeeEstimate(priceList.id().value(), fee.getAmount(),
                                                   fee.getCurrencyUnit().getCode());
                        });
    }
}
