package pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries;

import org.jmolecules.architecture.hexagonal.PrimaryPort;

/// Answers Rentals' question; an unknown rider is an answer too (`UNKNOWN_RIDER`, not eligible), not an error.
@PrimaryPort
public class RentalEligibilityQueryHandler {

    private final AccountsReadModel readModel;

    public RentalEligibilityQueryHandler(AccountsReadModel readModel) {
        this.readModel = readModel;
    }

    public RentalEligibilityView handle(RentalEligibilityQuery query) {
        return readModel.rentalEligibilityOf(query.riderId())
                        .orElseGet(() -> RentalEligibilityView.unknownRider(query.riderId()));
    }
}
