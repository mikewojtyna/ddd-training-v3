package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccountUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountAlreadyBlockedException;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountNotFoundException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

/**
 * Back-office entry point for the Block Account command.
 */
@PrimaryAdapter
@RestController
@RequestMapping("/velocity/back-office/accounts/{borrowerId}/block")
public class BackOfficeAccountBlockRestApi {

    private final BlockAccountUseCase blockAccountUseCase;

    public BackOfficeAccountBlockRestApi(BlockAccountUseCase blockAccountUseCase) {
        this.blockAccountUseCase = blockAccountUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void block(@PathVariable("borrowerId") String borrowerId) {
        blockAccountUseCase.block(new BorrowerId(borrowerId));
    }

    @ExceptionHandler(AccountAlreadyBlockedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String handle(AccountAlreadyBlockedException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(AccountNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handle(AccountNotFoundException exception) {
        return exception.getMessage();
    }
}
