package pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.primary.paymentprocessoracl;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.ErrorResponse;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.InvalidatePaymentMethod;
import pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases.InvalidatePaymentMethodUseCase;

/// The processor's *Card Invalidated* notification (DE-72): `{cardToken, reason}` with reason `EXPIRED`, `CANCELLED`
/// or `REPORTED_LOST`. Always `200` for a well-formed notification, also for a card we don't know.
@PrimaryAdapter
@RestController
@RequestMapping("/payments/processor-notifications")
public class ProcessorNotificationsController {

    public record CardInvalidationNotification(String cardToken, String reason) {
    }

    private final InvalidatePaymentMethodUseCase invalidatePaymentMethod;

    public ProcessorNotificationsController(InvalidatePaymentMethodUseCase invalidatePaymentMethod) {
        this.invalidatePaymentMethod = invalidatePaymentMethod;
    }

    @PostMapping("/card-invalidations")
    public void cardInvalidated(@RequestBody CardInvalidationNotification notification) {
        invalidatePaymentMethod.handle(new InvalidatePaymentMethod(notification.cardToken(), notification.reason()));
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class})
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
