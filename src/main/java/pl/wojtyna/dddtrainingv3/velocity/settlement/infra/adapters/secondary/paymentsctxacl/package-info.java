/// Anti-corruption layer towards Payments for *Charge* (`POST /payments/charges`). Settlement conforms to Payments'
/// succeeded / failed (CML `Payments [U,OHS,PL]->[D,CF] Settlement`), but this is the only package where Payments'
/// REST resource and payloads appear; it uses its own request/response records, no Payments classes.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
