package pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

import java.util.Optional;

@PrimaryPort
public class AccountStatusQueryHandler {

    private final AccountsReadModel readModel;

    public AccountStatusQueryHandler(AccountsReadModel readModel) {
        this.readModel = readModel;
    }

    public Optional<AccountStatus> handle(AccountStatusQuery query) {
        return readModel.statusOf(query.riderId());
    }
}
