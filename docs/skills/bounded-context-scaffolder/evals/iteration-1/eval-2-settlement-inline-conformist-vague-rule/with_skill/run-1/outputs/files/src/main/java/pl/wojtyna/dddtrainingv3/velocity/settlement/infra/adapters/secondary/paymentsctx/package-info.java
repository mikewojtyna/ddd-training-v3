/// Integration with the Payments bounded context for the **Charge Rider** command.
///
/// **Not an anti-corruption layer, on purpose.** Settlement is conformist to Payments (the user's decision, which
/// matches the reference context map: `Payments [U,OHS,PL]->[D,CF] Settlement`). Settlement uses Payments' API
/// directly, without translating it.
///
/// Risk accepted with this decision: every change to Payments' charging API ripples straight into Settlement's port
/// (`domain.charge.PaymentsCharging`) and its callers, and Payments' terms (payment method, pre-authorization, ...)
/// may end up mixed into Settlement's ubiquitous language. If that starts to hurt, turn this package into
/// `paymentsctxacl` annotated with `@AntiCorruptionLayer` and rename the port to Settlement's own terms.
package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctx;
