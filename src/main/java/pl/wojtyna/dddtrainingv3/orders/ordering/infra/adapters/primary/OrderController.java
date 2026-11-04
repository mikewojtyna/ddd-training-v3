package pl.wojtyna.dddtrainingv3.orders.ordering.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.orders.ordering.api.usecases.AddItemToOrder;
import pl.wojtyna.dddtrainingv3.orders.ordering.api.usecases.AddItemToOrderUseCase;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.ItemTypeLimitExceededException;
import pl.wojtyna.dddtrainingv3.orders.ordering.domain.order.OrderTotalLimitExceededException;

/// REST API for the canvas command *Add item to order*.
@PrimaryAdapter
@RestController
@RequestMapping("/ordering/orders/{orderId}")
public class OrderController {

    private final AddItemToOrderUseCase addItemToOrder;

    public OrderController(AddItemToOrderUseCase addItemToOrder) {
        this.addItemToOrder = addItemToOrder;
    }

    @PostMapping("/items")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void addItem(@PathVariable("orderId") String orderId, @RequestBody AddItemRequest request) {
        addItemToOrder.handle(new AddItemToOrder(orderId, request.itemType(), request.pricePln()));
    }

    @ExceptionHandler({OrderTotalLimitExceededException.class, ItemTypeLimitExceededException.class})
    @ResponseStatus(HttpStatus.UNPROCESSABLE_CONTENT)
    public String limitExceeded(RuntimeException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(OptimisticLockingException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String concurrentModification(OptimisticLockingException exception) {
        return exception.getMessage();
    }
}
