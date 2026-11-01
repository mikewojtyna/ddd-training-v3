package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishPriceListUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.FreePeriod;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.HourlyFees;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceList;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricecatalog.PriceListId;

import java.math.BigDecimal;

/**
 * Back office: Publish Price List.
 */
@PrimaryAdapter
@RestController
@RequestMapping("/velocity/pricing/price-lists")
public class PriceListRestApi {

    private final PublishPriceListUseCase publishPriceListUseCase;

    public PriceListRestApi(PublishPriceListUseCase publishPriceListUseCase) {
        this.publishPriceListUseCase = publishPriceListUseCase;
    }

    @PostMapping
    public void publish(@RequestBody PublishPriceListRequest request) {
        publishPriceListUseCase.publish(toPriceList(request));
    }

    private PriceList toPriceList(PublishPriceListRequest request) {
        var currency = CurrencyUnit.of(request.currency());
        return new PriceList(new PriceListId(request.priceListId()),
                             request.validFrom(),
                             FreePeriod.ofMinutes(request.freePeriodMinutes()),
                             new HourlyFees(money(request.firstHourFee(), currency),
                                            money(request.secondHourFee(), currency),
                                            money(request.thirdHourFee(), currency),
                                            money(request.subsequentHourlyFee(), currency)));
    }

    private Money money(BigDecimal amount, CurrencyUnit currency) {
        return Money.of(currency, amount);
    }
}
