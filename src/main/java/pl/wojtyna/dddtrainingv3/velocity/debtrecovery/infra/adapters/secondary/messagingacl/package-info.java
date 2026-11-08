/// Anti-corruption layer towards the messaging provider that delivers payment reminders to riders (an external party,
/// not a bounded context; integration-contracts.md "Additions": port `PaymentReminders`). Simulated: reminders are
/// recorded in memory. The only package where the provider's language may appear.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.infra.adapters.secondary.messagingacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
