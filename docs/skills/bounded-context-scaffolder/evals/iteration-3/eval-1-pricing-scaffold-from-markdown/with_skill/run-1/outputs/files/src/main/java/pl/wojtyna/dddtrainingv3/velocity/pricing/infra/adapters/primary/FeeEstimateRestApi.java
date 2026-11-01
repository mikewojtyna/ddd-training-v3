package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.EstimateFee;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.EstimateFeeQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.FeeEstimate;

/// Rider app: canvas inbound query "Estimate Fee".
@PrimaryAdapter
@RestController
@RequestMapping("/velocity/pricing/fee-estimate")
public class FeeEstimateRestApi {

    private final EstimateFeeQueryHandler estimateFeeQueryHandler;

    public FeeEstimateRestApi(EstimateFeeQueryHandler estimateFeeQueryHandler) {
        this.estimateFeeQueryHandler = estimateFeeQueryHandler;
    }

    @GetMapping
    public FeeEstimate estimate(@RequestParam("minutes") long minutes) {
        return estimateFeeQueryHandler.handle(new EstimateFee(minutes));
    }
}
