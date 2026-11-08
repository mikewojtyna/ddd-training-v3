package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.ErrorResponse;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RideSettlementQuery;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RideSettlementQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RideSettlementView;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RiderBalanceQuery;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RiderBalanceQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.queries.RiderBalanceView;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.CollectOutstandingDebt;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.CollectOutstandingDebtUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.DebtCollectionResult;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.PayOffOutstandingDebt;
import pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases.PayOffOutstandingDebtUseCase;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt.NoOutstandingDebtException;

/// Settlement's REST resources: *Collect Outstanding Debt* (Debt Recovery), *Pay Outstanding Debt* (rider app),
/// *Rider Balance* and *Ride Settlement* (queries). See `docs/bounded-contexts/velocity/integration-contracts.md`.
///
/// A declined collection is `200` with `collected=false`; paying off without debt is `409 NoOutstandingDebt`; an
/// unknown ride is `404`.
@PrimaryAdapter
@RestController
@RequestMapping("/settlement")
public class SettlementController {

    private final CollectOutstandingDebtUseCase collectOutstandingDebt;
    private final PayOffOutstandingDebtUseCase payOffOutstandingDebt;
    private final RiderBalanceQueryHandler riderBalance;
    private final RideSettlementQueryHandler rideSettlement;

    public SettlementController(CollectOutstandingDebtUseCase collectOutstandingDebt,
                                PayOffOutstandingDebtUseCase payOffOutstandingDebt,
                                RiderBalanceQueryHandler riderBalance,
                                RideSettlementQueryHandler rideSettlement) {
        this.collectOutstandingDebt = collectOutstandingDebt;
        this.payOffOutstandingDebt = payOffOutstandingDebt;
        this.riderBalance = riderBalance;
        this.rideSettlement = rideSettlement;
    }

    @PostMapping("/riders/{riderId}/debt-collections")
    public DebtCollectionResult collect(@PathVariable("riderId") String riderId) {
        return collectOutstandingDebt.handle(new CollectOutstandingDebt(riderId));
    }

    @PostMapping("/riders/{riderId}/debt-payment")
    public DebtCollectionResult payOff(@PathVariable("riderId") String riderId) {
        return payOffOutstandingDebt.handle(new PayOffOutstandingDebt(riderId));
    }

    @GetMapping("/riders/{riderId}/balance")
    public RiderBalanceView balance(@PathVariable("riderId") String riderId) {
        return riderBalance.handle(new RiderBalanceQuery(riderId));
    }

    @GetMapping("/rides/{sessionId}")
    public ResponseEntity<RideSettlementView> ride(@PathVariable("sessionId") String sessionId) {
        return ResponseEntity.of(rideSettlement.handle(new RideSettlementQuery(sessionId)));
    }

    @ExceptionHandler({NoOutstandingDebtException.class, OptimisticLockingException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse ruleBroken(RuntimeException exception) {
        var name = exception.getClass().getSimpleName();
        return new ErrorResponse(name.endsWith("Exception") ? name.substring(0, name.length() - "Exception".length()) : name,
                                 exception.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse malformed(IllegalArgumentException exception) {
        return new ErrorResponse("MalformedRequest", exception.getMessage());
    }
}
