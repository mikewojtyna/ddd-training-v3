package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishPriceList;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishPriceListUseCase;

/// Back office: canvas inbound command "Publish Price List".
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
        publishPriceListUseCase.handle(new PublishPriceList(request.validFrom(),
                                                            request.freePeriodMinutes(),
                                                            request.firstHourFee(),
                                                            request.secondHourFee(),
                                                            request.thirdHourFee(),
                                                            request.subsequentHourlyFee(),
                                                            request.currency()));
    }
}
