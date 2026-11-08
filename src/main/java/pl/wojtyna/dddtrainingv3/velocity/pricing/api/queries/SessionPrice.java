package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

/// Answer to *Price for Session*: the total fee and how it is made up. Money as decimal strings in PLN.
///
/// - `hourlyFees`: the sum of the fees of all billing hours (`billingHours` of them).
/// - `returnFeeKind`: `NONE`, `DROP_ZONE` or `UNAUTHORIZED_ZONE`.
/// - `total` = `hourlyFees` + `returnFee` + `penalty`.
public record SessionPrice(String tariffId,
                           int billingHours,
                           String hourlyFees,
                           String returnFee,
                           String returnFeeKind,
                           String penalty,
                           String total) {
}
