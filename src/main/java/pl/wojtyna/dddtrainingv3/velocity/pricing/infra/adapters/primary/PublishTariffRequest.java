package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary;

/// Body of `POST /pricing/tariffs`: money as decimal strings in PLN, `validFrom` as an ISO-8601 instant.
public record PublishTariffRequest(String tariffId,
                                   String validFrom,
                                   Integer freeMinutes,
                                   String firstHourFee,
                                   String secondHourFee,
                                   String thirdHourFee,
                                   String furtherHourFee,
                                   String dropZoneFee,
                                   String unauthorizedZoneFee,
                                   String penalty,
                                   String publishedBy) {
}
