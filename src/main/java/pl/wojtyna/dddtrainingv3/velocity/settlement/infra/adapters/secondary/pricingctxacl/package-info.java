/// Anti-corruption layer towards Pricing for *Price for Session* (`GET /pricing/session-price`). Settlement conforms to
/// Pricing's price (CML `Pricing [U,OHS,PL]->[D,CF] Settlement`), but this is the only package where Pricing's REST
/// resource, its *ending* vocabulary and its payloads appear; it uses its own response record, no Pricing classes.
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.pricingctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
