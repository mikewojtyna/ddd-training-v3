package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.joda.money.Money;

import java.time.Duration;
import java.time.Instant;

/// Tariffs of the spec examples.
public final class TariffFixtures {

    public static final Instant LAUNCH_VALID_FROM = Instant.parse("2026-01-01T00:00:00Z");

    private TariffFixtures() {
    }

    public static Money pln(String amount) {
        return Money.of(Tariff.PLN, new java.math.BigDecimal(amount));
    }

    /// DE-30: 15 min free; PLN 3.00, 5.00, 7.00, then 9.00 per hour; drop zone PLN 2.00; unauthorized zone PLN 40.00;
    /// penalty PLN 150.00.
    public static Tariff launchTariff() {
        return new Tariff(new TariffId("LAUNCH"), LAUNCH_VALID_FROM, Duration.ofMinutes(15),
                          pln("3.00"), pln("5.00"), pln("7.00"), pln("9.00"), pln("2.00"), pln("40.00"), pln("150.00"));
    }

    /// The launch prices under another id and valid from.
    public static Tariff launchPrices(String id, Instant validFrom) {
        return new Tariff(new TariffId(id), validFrom, Duration.ofMinutes(15),
                          pln("3.00"), pln("5.00"), pln("7.00"), pln("9.00"), pln("2.00"), pln("40.00"), pln("150.00"));
    }

    /// pricing-manager-publishes-tariff's T-2: 15 min free; PLN 3.50, 5.50, 7.50, then 9.50; drop zone PLN 2.50;
    /// unauthorized zone PLN 45.00; penalty PLN 150.00.
    public static Tariff t2(String id, Instant validFrom) {
        return new Tariff(new TariffId(id), validFrom, Duration.ofMinutes(15),
                          pln("3.50"), pln("5.50"), pln("7.50"), pln("9.50"), pln("2.50"), pln("45.00"), pln("150.00"));
    }
}
