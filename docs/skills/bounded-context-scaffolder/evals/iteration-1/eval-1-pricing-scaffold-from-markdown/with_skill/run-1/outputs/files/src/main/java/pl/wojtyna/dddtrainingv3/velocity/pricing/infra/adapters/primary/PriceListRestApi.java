package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishPriceList;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishPriceListUseCase;

/// Back office entry point for the canvas command "Publish Price List". The canvas doesn't describe the endpoint;
/// the path and body shape are a scaffolding choice.
@PrimaryAdapter
@RestController
@RequestMapping("/velocity/pricing/price-lists")
public class PriceListRestApi {

    private final PublishPriceListUseCase publishPriceList;

    public PriceListRestApi(PublishPriceListUseCase publishPriceList) {
        this.publishPriceList = publishPriceList;
    }

    @PostMapping
    public void publish(@RequestBody PublishPriceListRequest request) {
        publishPriceList.handle(new PublishPriceList(request.priceListId(), request.validFrom(),
                                                     request.freePeriodMinutes(), request.firstHourFee(),
                                                     request.secondHourFee(), request.thirdHourFee(),
                                                     request.subsequentHourlyFee(), request.currency()));
    }
}
