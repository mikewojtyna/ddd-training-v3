/// Anti-corruption layer for what Payments publishes: `payments.api.events.PaymentMethodInvalidated` is translated
/// here into Accounts' own command, *Block Account* with reason *invalid card*. The only place Payments' published
/// language enters Accounts.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.primary.paymentsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
