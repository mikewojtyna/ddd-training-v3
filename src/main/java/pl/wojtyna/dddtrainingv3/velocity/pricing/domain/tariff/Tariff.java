package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.jmolecules.ddd.annotation.ValueObject;
import org.joda.money.CurrencyUnit;
import org.joda.money.Money;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.stream.Stream;

/// The prices valid from a moment on: free period, the fees of billing hours 1, 2 and 3, the fee of every further
/// billing hour, the drop zone fee, the unauthorized zone return fee and the penalty. Money is in PLN.
@ValueObject
public record Tariff(TariffId id,
                     Instant validFrom,
                     Duration freePeriod,
                     Money firstHourFee,
                     Money secondHourFee,
                     Money thirdHourFee,
                     Money furtherHourFee,
                     Money dropZoneFee,
                     Money unauthorizedZoneFee,
                     Money penalty) {

    public static final CurrencyUnit PLN = CurrencyUnit.of("PLN");

    public Tariff {
        if (id == null || validFrom == null) {
            throw new IllegalArgumentException("A tariff needs an id and a valid from");
        }
        if (Stream.of(freePeriod, firstHourFee, secondHourFee, thirdHourFee, furtherHourFee, dropZoneFee,
                      unauthorizedZoneFee, penalty).anyMatch(Objects::isNull)) {
            throw new IncompleteTariffException("Tariff %s must state every price".formatted(id.value()));
        }
        if (Stream.of(firstHourFee, secondHourFee, thirdHourFee, furtherHourFee, dropZoneFee, unauthorizedZoneFee,
                      penalty).anyMatch(Money::isNegative)) {
            throw new NegativeFeeException("Tariff %s can't have negative fees".formatted(id.value()));
        }
    }

    /// The fee of the given billing hour (1-based).
    public Money hourlyFeeOf(int billingHour) {
        return switch (billingHour) {
            case 1 -> firstHourFee;
            case 2 -> secondHourFee;
            case 3 -> thirdHourFee;
            default -> furtherHourFee;
        };
    }
}
