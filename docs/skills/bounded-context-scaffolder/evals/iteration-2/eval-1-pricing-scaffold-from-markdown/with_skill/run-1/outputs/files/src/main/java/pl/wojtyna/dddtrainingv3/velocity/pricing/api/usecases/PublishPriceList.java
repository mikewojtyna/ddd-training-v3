package pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases;

import java.math.BigDecimal;
import java.time.Instant;

/// Canvas inbound command "Publish Price List" (from Back office). Fees are in PLN.
public record PublishPriceList(String priceListId, Instant validFrom, int freePeriodMinutes, BigDecimal firstHourFee,
                               BigDecimal secondHourFee, BigDecimal thirdHourFee, BigDecimal subsequentHourlyFee) {
}
