/**
 * Inbound ACL to the Rentals bounded context. Translates Rentals' published language (Rental Session Ended) into
 * Pricing's model. This is the only package of the primary adapters where Rentals classes can be used.
 */
@AntiCorruptionLayer
package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary.rentalsctxacl;

import pl.wojtyna.dddtrainingv3.common.arch.AntiCorruptionLayer;
