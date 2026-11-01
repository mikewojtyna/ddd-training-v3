package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEventPublisher;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelists.*;

import java.math.BigDecimal;

@PrimaryPort
public class PublishPriceListUseCase {

    private final PriceListsRepository priceListsRepository;
    private final DomainEventPublisher eventPublisher;

    public PublishPriceListUseCase(PriceListsRepository priceListsRepository, DomainEventPublisher eventPublisher) {
        this.priceListsRepository = priceListsRepository;
        this.eventPublisher = eventPublisher;
    }

    public void handle(PublishPriceList command) {
        var currency = CurrencyUnit.of(command.currency());
        var priceList = new PriceList(command.validFrom(),
                                      FreePeriod.ofMinutes(command.freePeriodMinutes()),
                                      money(command.firstHourFee(), currency),
                                      money(command.secondHourFee(), currency),
                                      money(command.thirdHourFee(), currency),
                                      money(command.subsequentHourlyFee(), currency));
        var priceLists = priceListsRepository.load(PriceListsId.VELOCITY)
                                             .orElseGet(() -> new PriceLists(PriceListsId.VELOCITY));
        var events = priceLists.publish(priceList);
        priceListsRepository.save(priceLists);
        eventPublisher.publish(events);
    }

    private static Money money(BigDecimal amount, CurrencyUnit currency) {
        return Money.of(currency, amount);
    }
}
