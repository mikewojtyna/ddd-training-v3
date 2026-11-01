package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId;

/// Rule "Block an already blocked account": the block is rejected.
public class AccountAlreadyBlocked extends RuntimeException {

    public AccountAlreadyBlocked(BorrowerId id) {
        super("Account " + id.value() + " is already blocked");
    }
}
