package pl.wojtyna.dddtrainingv3.velocity.pricing;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// Drives Pricing over its REST resources, as Settlement (Price for Session) and a pricing manager (Publish Tariff,
/// Tariffs) do. Keeps its own records: the test only knows the published contract.
class PricingDriver {

    record SessionPrice(String tariffId, int billingHours, String hourlyFees, String returnFee, String returnFeeKind,
                        String penalty, String total) {
    }

    record Tariff(String tariffId, String validFrom, int freeMinutes, String firstHourFee, String secondHourFee,
                  String thirdHourFee, String furtherHourFee, String dropZoneFee, String unauthorizedZoneFee,
                  String penalty) {
    }

    record Error(String error, String message) {
    }

    /// Outcome of a publication: HTTP status and, for a rejection, the broken rule.
    record Publication(int status, String error) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    PricingDriver(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    /// A tariff with every price, money as decimal strings in PLN.
    static Map<String, Object> tariff(String tariffId, String validFrom, String firstHourFee, String secondHourFee,
                                      String thirdHourFee, String furtherHourFee, String dropZoneFee,
                                      String unauthorizedZoneFee, String penalty) {
        var body = new HashMap<String, Object>();
        body.put("tariffId", tariffId);
        body.put("validFrom", validFrom);
        body.put("freeMinutes", 15);
        body.put("firstHourFee", firstHourFee);
        body.put("secondHourFee", secondHourFee);
        body.put("thirdHourFee", thirdHourFee);
        body.put("furtherHourFee", furtherHourFee);
        body.put("dropZoneFee", dropZoneFee);
        body.put("unauthorizedZoneFee", unauthorizedZoneFee);
        body.put("penalty", penalty);
        body.put("publishedBy", "paula");
        return body;
    }

    /// The launch prices (DE-30) under another id and valid from: used to end a test tariff's influence (plan D8).
    static Map<String, Object> launchPrices(String tariffId, String validFrom) {
        return tariff(tariffId, validFrom, "3.00", "5.00", "7.00", "9.00", "2.00", "40.00", "150.00");
    }

    Publication publish(Map<String, Object> tariff) {
        return restClient.post()
                         .uri(urls.url("/pricing/tariffs"))
                         .contentType(MediaType.APPLICATION_JSON)
                         .body(tariff)
                         .exchange((request, response) -> {
                             var status = response.getStatusCode().value();
                             if (status == 201) {
                                 return new Publication(status, null);
                             }
                             return new Publication(status, response.bodyTo(Error.class).error());
                         });
    }

    List<Tariff> tariffs() {
        return List.of(restClient.get().uri(urls.url("/pricing/tariffs")).retrieve().body(Tariff[].class));
    }

    SessionPrice price(String startedAt, String endedAt, String ending) {
        return get("/pricing/session-price?startedAt=" + startedAt + "&endedAt=" + endedAt + "&ending=" + ending);
    }

    SessionPrice price(String startedAt, String endedAt, String ending, double latitude, double longitude) {
        return get("/pricing/session-price?startedAt=" + startedAt + "&endedAt=" + endedAt + "&ending=" + ending
                   + "&latitude=" + latitude + "&longitude=" + longitude);
    }

    /// Status of a price question that is expected to be rejected.
    int priceStatus(String query) {
        return restClient.get()
                         .uri(urls.url("/pricing/session-price?" + query))
                         .exchange((request, response) -> response.getStatusCode().value());
    }

    private SessionPrice get(String path) {
        return restClient.get().uri(urls.url(path)).retrieve().body(SessionPrice.class);
    }
}
