/// Adapter towards Payments for the canvas outbound command "Charge Rider".
///
/// **Conformist, no anti-corruption layer, by conscious decision (plan, D1).** Settlement takes Payments' published
/// language as-is, as the reference map also draws it (`Payments [U,OHS,PL]->[D,CF] Settlement`). The risk, accepted on
/// purpose: every change to Payments' published language (renamed fields, new charge statuses, new error codes)
/// ripples straight into Settlement's domain code and tests, and Payments' terms mix with Settlement's ubiquitous
/// language. That's why this package is deliberately not annotated with `@AntiCorruptionLayer`.
package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctx;
