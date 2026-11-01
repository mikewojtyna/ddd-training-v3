package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.EstimateFeeQuery;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.EstimateFeeQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.FeeEstimate;

/// Rider app entry point for the canvas query "Estimate Fee". The canvas doesn't describe the endpoint; the path and
/// parameter are a scaffolding choice.
@PrimaryAdapter
@RestController
@RequestMapping("/velocity/pricing/fee-estimates")
public class FeeEstimateRestApi {

    private final EstimateFeeQueryHandler estimateFee;

    public FeeEstimateRestApi(EstimateFeeQueryHandler estimateFee) {
        this.estimateFee = estimateFee;
    }

    @GetMapping
    public ResponseEntity<FeeEstimate> estimate(@RequestParam("rideMinutes") long rideMinutes) {
        return ResponseEntity.of(estimateFee.handle(new EstimateFeeQuery(rideMinutes)));
    }
}
