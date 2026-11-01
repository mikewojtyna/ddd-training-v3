package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

public class AccountNotFound extends RuntimeException {

    public AccountNotFound(BorrowerId id) {
        super("Account " + id.value() + " not found");
    }
}
