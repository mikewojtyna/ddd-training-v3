package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.FreePeriod;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceList;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListRepository;

import java.math.BigDecimal;

/// Canvas inbound command: "Publish Price List" (back office).
///
/// Publishing creates a new price list and emits no domain event, because the canvas lists no outbound message for it.
@PrimaryPort
public class PublishPriceListUseCase {

    private final PriceListRepository priceLists;

    public PublishPriceListUseCase(PriceListRepository priceLists) {
        this.priceLists = priceLists;
    }

    public void handle(PublishPriceList command) {
        var currency = CurrencyUnit.of(command.currency());
        var priceList = new PriceList(new PriceListId(command.priceListId()),
                                      command.validFrom(),
                                      FreePeriod.ofMinutes(command.freePeriodMinutes()),
                                      money(currency, command.firstHourFee()),
                                      money(currency, command.secondHourFee()),
                                      money(currency, command.thirdHourFee()),
                                      money(currency, command.subsequentHourlyFee()));
        priceLists.save(priceList);
    }

    private static Money money(CurrencyUnit currency, BigDecimal amount) {
        return Money.of(currency, amount);
    }
}
