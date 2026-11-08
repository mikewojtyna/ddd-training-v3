package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import java.math.BigDecimal;
import java.time.Instant;

/// Command *Publish Tariff* (pricing manager). Prices in PLN; a missing price is `null` and rejected by the domain
/// (rule "a tariff states every price"), not here. `publishedBy` is optional: there are no identities yet.
public record PublishTariff(String tariffId,
                            Instant validFrom,
                            Integer freeMinutes,
                            BigDecimal firstHourFee,
                            BigDecimal secondHourFee,
                            BigDecimal thirdHourFee,
                            BigDecimal furtherHourFee,
                            BigDecimal dropZoneFee,
                            BigDecimal unauthorizedZoneFee,
                            BigDecimal penalty,
                            String publishedBy) {
}
