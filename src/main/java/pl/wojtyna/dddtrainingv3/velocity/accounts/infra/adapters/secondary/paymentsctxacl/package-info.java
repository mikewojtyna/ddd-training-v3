/// Anti-corruption layer towards Payments for *Verify Payment Method* (`POST /payments/payment-methods`). Accounts
/// conforms to Payments' *verified* (CML `Payments [U,OHS,PL]->[D,CF] Accounts`), but this is still the only package
/// where Payments' REST resource and payloads appear; it uses its own request/response records, no Payments classes.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.paymentsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
