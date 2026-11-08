package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

/// *Collect Outstanding Debt*: Debt Recovery asks to collect the rider's whole outstanding debt.
public record CollectOutstandingDebt(String riderId) {
}
