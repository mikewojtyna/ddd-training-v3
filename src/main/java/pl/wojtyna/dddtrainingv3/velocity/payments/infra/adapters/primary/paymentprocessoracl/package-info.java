/// Anti-corruption layer for what the external payment processor sends us: its notifications arrive here, in its
/// language, and are translated into Payments' commands (CML `PaymentProcessor [U,OHS,PL]->[D,ACL] Payments`).
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.primary.paymentprocessoracl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
