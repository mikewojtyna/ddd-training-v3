package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.jmolecules.ddd.annotation.ValueObject;

/// Identity of the tariff schedule. VeloCity has one ({@link TariffSchedule#VELOCITY}).
@ValueObject
public record TariffScheduleId(String value) {
}
