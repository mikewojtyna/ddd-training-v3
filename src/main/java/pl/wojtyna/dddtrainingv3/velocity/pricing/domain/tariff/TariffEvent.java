package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

import java.time.Instant;

/// Facts about the tariff schedule.
public sealed interface TariffEvent extends DomainEvent {

    /// A tariff was accepted for publication; it applies to sessions started at or after its valid from.
    record TariffPublished(Tariff tariff, PricingManagerId publishedBy, Instant publishedAt) implements TariffEvent {
    }
}
