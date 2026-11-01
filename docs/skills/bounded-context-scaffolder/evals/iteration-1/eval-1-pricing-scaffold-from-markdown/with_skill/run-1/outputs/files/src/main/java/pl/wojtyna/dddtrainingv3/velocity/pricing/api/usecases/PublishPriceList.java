package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import java.math.BigDecimal;
import java.time.Instant;

/// Canvas inbound command: "Publish Price List" from the back office. A new price list valid from a given moment.
public record PublishPriceList(String priceListId, Instant validFrom, long freePeriodMinutes,
                               BigDecimal firstHourFee, BigDecimal secondHourFee, BigDecimal thirdHourFee,
                               BigDecimal subsequentHourlyFee, String currency) {
}
