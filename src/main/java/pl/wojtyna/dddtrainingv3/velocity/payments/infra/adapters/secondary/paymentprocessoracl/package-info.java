/// Anti-corruption layer towards the external payment processor (CML `PaymentProcessor [U,OHS,PL]->[D,ACL] Payments`).
/// This is the only package where the processor's language (authorizations, voids, captures, response codes)
/// may appear; `ProcessorPaymentProcessorAdapter` translates it into Payments' `PaymentProcessor` port.
///
/// Until a real processor is connected, `SimulatedPaymentProcessor` stands in for it: it declines authorizations of
/// tokens starting with `tok_declined`, declines captures of tokens starting with `tok_nofunds`, records everything
/// it received and lets tests script the next answer for a card token.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
