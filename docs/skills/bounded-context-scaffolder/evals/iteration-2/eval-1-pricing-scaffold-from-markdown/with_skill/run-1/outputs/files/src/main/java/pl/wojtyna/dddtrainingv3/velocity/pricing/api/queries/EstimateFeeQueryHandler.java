package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListRepository;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.ReturnCharges;

import java.time.Clock;
import java.time.Duration;

/// Canvas inbound query "Estimate Fee" (from Rider app). Prices with the price list valid now and without return
/// charges, because where the ride ends isn't known yet (plan decision, Q4).
@PrimaryPort
public class EstimateFeeQueryHandler {

    private final PriceListRepository priceLists;
    private final Clock clock;

    public EstimateFeeQueryHandler(PriceListRepository priceLists, Clock clock) {
        this.priceLists = priceLists;
        this.clock = clock;
    }

    public FeeEstimate handle(EstimateFee query) {
        var priceList = priceLists.all().validAt(clock.instant());
        var totalFee = priceList.totalFeeFor(Duration.ofMinutes(query.rideMinutes()), ReturnCharges.none());
        return new FeeEstimate(totalFee.amount().getAmount(),
                               totalFee.amount().getCurrencyUnit().getCode(),
                               priceList.id().value());
    }
}
