package pl.wojtyna.dddtrainingv3.velocity.settlement.api.usecases;

/// Result of a collection (*Pay Off* or *Collect Outstanding Debt*): money in PLN as decimal strings.
/// `collectedAmount` is `0.00` when nothing was collected; `outstandingDebt` is what the rider still owes.
public record DebtCollectionResult(boolean collected, String collectedAmount, String outstandingDebt) {
}
