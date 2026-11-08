package pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.ErrorResponse;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.queries.PaymentMethodsQuery;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.queries.PaymentMethodsQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.queries.RiderPaymentMethodsView;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeCurrentCard;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeCurrentCardUseCase;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.ChargeResult;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.PaymentMethodVerification;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.VerifyPaymentMethod;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.VerifyPaymentMethodUseCase;

import java.math.BigDecimal;

/// Payments' Open Host Service: *Verify Payment Method* (Accounts), *Charge* (Settlement) and the rider's payment
/// methods (query). See `docs/bounded-contexts/velocity/integration-contracts.md`.
@PrimaryAdapter
@RestController
@RequestMapping("/payments")
public class PaymentsController {

    public record VerifyPaymentMethodRequest(String riderId, String cardToken) {
    }

    public record ChargeRequest(String riderId, BigDecimal amount, String reference) {
    }

    private final VerifyPaymentMethodUseCase verifyPaymentMethod;
    private final ChargeCurrentCardUseCase chargeCurrentCard;
    private final PaymentMethodsQueryHandler paymentMethods;

    public PaymentsController(VerifyPaymentMethodUseCase verifyPaymentMethod, ChargeCurrentCardUseCase chargeCurrentCard,
                              PaymentMethodsQueryHandler paymentMethods) {
        this.verifyPaymentMethod = verifyPaymentMethod;
        this.chargeCurrentCard = chargeCurrentCard;
        this.paymentMethods = paymentMethods;
    }

    @PostMapping("/payment-methods")
    public PaymentMethodVerification verify(@RequestBody VerifyPaymentMethodRequest request) {
        return verifyPaymentMethod.handle(new VerifyPaymentMethod(request.riderId(), request.cardToken()));
    }

    @PostMapping("/charges")
    public ChargeResult charge(@RequestBody ChargeRequest request) {
        return chargeCurrentCard.handle(new ChargeCurrentCard(request.riderId(), request.amount(), request.reference()));
    }

    @GetMapping("/riders/{riderId}/payment-methods")
    public RiderPaymentMethodsView paymentMethods(@PathVariable("riderId") String riderId) {
        return paymentMethods.handle(new PaymentMethodsQuery(riderId));
    }

    @ExceptionHandler({IllegalArgumentException.class, ArithmeticException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(RuntimeException exception) {
        return ErrorResponse.of(exception);
    }

    @ExceptionHandler(OptimisticLockingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse concurrentModification(OptimisticLockingException exception) {
        return ErrorResponse.of(exception);
    }
}
