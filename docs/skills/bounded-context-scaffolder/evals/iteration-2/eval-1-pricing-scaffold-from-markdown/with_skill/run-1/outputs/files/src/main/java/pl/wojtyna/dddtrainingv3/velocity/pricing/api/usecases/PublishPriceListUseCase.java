package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.FreePeriod;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceList;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListId;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist.PriceListRepository;

/// Canvas inbound command "Publish Price List" (from Back office): a new price list valid from a given moment.
@PrimaryPort
public class PublishPriceListUseCase {

    private final PriceListRepository priceLists;

    public PublishPriceListUseCase(PriceListRepository priceLists) {
        this.priceLists = priceLists;
    }

    public void handle(PublishPriceList command) {
        var priceList = new PriceList(new PriceListId(command.priceListId()),
                                      command.validFrom(),
                                      FreePeriod.ofMinutes(command.freePeriodMinutes()),
                                      Money.of(PriceList.CURRENCY, command.firstHourFee()),
                                      Money.of(PriceList.CURRENCY, command.secondHourFee()),
                                      Money.of(PriceList.CURRENCY, command.thirdHourFee()),
                                      Money.of(PriceList.CURRENCY, command.subsequentHourlyFee()));
        priceLists.save(priceList);
    }
}
