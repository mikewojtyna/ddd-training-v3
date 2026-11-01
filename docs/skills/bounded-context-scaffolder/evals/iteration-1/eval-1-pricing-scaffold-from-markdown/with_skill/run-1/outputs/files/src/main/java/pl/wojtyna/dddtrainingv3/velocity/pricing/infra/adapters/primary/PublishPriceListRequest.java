package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary;

import java.math.BigDecimal;
import java.time.Instant;

public record PublishPriceListRequest(String priceListId, Instant validFrom, long freePeriodMinutes,
                                      BigDecimal firstHourFee, BigDecimal secondHourFee, BigDecimal thirdHourFee,
                                      BigDecimal subsequentHourlyFee, String currency) {
}
