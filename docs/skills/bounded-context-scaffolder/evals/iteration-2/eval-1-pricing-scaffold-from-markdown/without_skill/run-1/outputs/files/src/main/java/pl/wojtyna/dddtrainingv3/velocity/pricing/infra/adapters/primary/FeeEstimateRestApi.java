package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.EstimateFee;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.EstimateFeeQueryHandler;

import java.math.BigDecimal;
import java.time.Duration;

/**
 * Rider app: Estimate Fee for a ride of N minutes.
 */
@PrimaryAdapter
@RestController
@RequestMapping("/velocity/pricing/fee-estimates")
public class FeeEstimateRestApi {

    private final EstimateFeeQueryHandler estimateFeeQueryHandler;

    public FeeEstimateRestApi(EstimateFeeQueryHandler estimateFeeQueryHandler) {
        this.estimateFeeQueryHandler = estimateFeeQueryHandler;
    }

    @GetMapping
    public FeeEstimateResponse estimate(@RequestParam("rideMinutes") long rideMinutes) {
        var fee = estimateFeeQueryHandler.handle(new EstimateFee(Duration.ofMinutes(rideMinutes)));
        return new FeeEstimateResponse(fee.getAmount(), fee.getCurrencyUnit().getCode());
    }

    public record FeeEstimateResponse(BigDecimal amount, String currency) {
    }
}
