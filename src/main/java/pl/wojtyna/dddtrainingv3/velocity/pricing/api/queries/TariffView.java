package pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries;

/// One published tariff as the pricing manager sees it. Money as decimal strings in PLN, times as ISO-8601 instants.
public record TariffView(String tariffId,
                         String validFrom,
                         int freeMinutes,
                         String firstHourFee,
                         String secondHourFee,
                         String thirdHourFee,
                         String furtherHourFee,
                         String dropZoneFee,
                         String unauthorizedZoneFee,
                         String penalty) {
}
