package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.api.usecases;

/// The pause reason isn't one of `DISPUTE`, `PAYMENT_PLAN`, `REGULATORY_HOLD` (DE-89). Malformed input rather than a
/// broken rule, because the domain's closed list can't even represent it (plan `collections-agent-pauses-debt-recovery`
/// D5): answered `400`.
public class UnknownPauseReason extends IllegalArgumentException {

    public UnknownPauseReason(String message) {
        super(message);
    }
}
