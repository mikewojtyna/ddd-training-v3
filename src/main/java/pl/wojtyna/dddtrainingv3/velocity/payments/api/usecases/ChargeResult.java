package pl.wojtyna.dddtrainingv3.velocity.payments.api.usecases;

/// Result of *Charge*: `status` is `SUCCEEDED` or `FAILED`; `failureReason` (`DECLINED` or `NO_VALID_PAYMENT_METHOD`)
/// only for a failed charge.
public record ChargeResult(String chargeId, String status, String failureReason) {
}
