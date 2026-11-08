package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.joda.money.Money;

import java.time.Duration;
import java.time.Instant;

/// The tariff VeloCity starts with (DE-30), valid from 2026-01-01T00:00:00Z: 15 min free; PLN 3.00, 5.00, 7.00, then
/// 9.00 per hour; drop zone fee PLN 2.00; unauthorized zone return fee PLN 40.00; penalty PLN 150.00.
public final class LaunchTariff {

    public static final TariffId ID = new TariffId("LAUNCH");
    public static final Instant VALID_FROM = Instant.parse("2026-01-01T00:00:00Z");

    private LaunchTariff() {
    }

    public static Tariff tariff() {
        return new Tariff(ID, VALID_FROM, Duration.ofMinutes(15), pln(3), pln(5), pln(7), pln(9), pln(2), pln(40),
                          pln(150));
    }

    private static Money pln(int amount) {
        return Money.of(Tariff.PLN, amount);
    }
}
