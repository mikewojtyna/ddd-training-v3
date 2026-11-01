package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.NoPriceListValidAt;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.PriceListsId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.PriceListsRepository;

import java.time.Clock;
import java.time.Duration;

/// Estimates with the price list valid now and without return charges (review default, 2026-11-01). It calculates
/// through the `PriceLists` aggregate instead of a read model, so the fee rules live in one place (plan D8).
@PrimaryPort
public class EstimateFeeQueryHandler {

    private final PriceListsRepository priceListsRepository;
    private final Clock clock;

    public EstimateFeeQueryHandler(PriceListsRepository priceListsRepository, Clock clock) {
        this.priceListsRepository = priceListsRepository;
        this.clock = clock;
    }

    public FeeEstimate handle(EstimateFee query) {
        var now = clock.instant();
        var priceLists = priceListsRepository.load(PriceListsId.VELOCITY)
                                             .orElseThrow(() -> new NoPriceListValidAt(now));
        var totalFee = priceLists.estimate(Duration.ofMinutes(query.minutes()), now);
        return new FeeEstimate(totalFee.amount().getAmount(), totalFee.amount().getCurrencyUnit().getCode());
    }
}
