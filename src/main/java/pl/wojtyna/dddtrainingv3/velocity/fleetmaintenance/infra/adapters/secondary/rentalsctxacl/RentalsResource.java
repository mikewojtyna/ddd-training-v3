package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.secondary.rentalsctxacl;

import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.CollaboratorUnavailable;

import java.io.IOException;

/// Calls Rentals' REST resources and reads either the answer or Rentals' `{error, message}` rejection.
class RentalsResource {

    record Reply<T>(int status, T body, String error, String message) {

        boolean ok() {
            return status >= 200 && status < 300;
        }
    }

    private record ErrorBody(String error, String message) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    RentalsResource(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    <T> Reply<T> post(String path, Object body, Class<T> answer) {
        try {
            return restClient.post().uri(urls.url(path)).contentType(MediaType.APPLICATION_JSON).body(body)
                             .exchange((request, response) -> read(response, answer));
        } catch (RestClientException failure) {
            throw new CollaboratorUnavailable("Rentals didn't answer " + path, failure);
        }
    }

    <T> Reply<T> get(String path, Class<T> answer) {
        try {
            return restClient.get().uri(urls.url(path)).exchange((request, response) -> read(response, answer));
        } catch (RestClientException failure) {
            throw new CollaboratorUnavailable("Rentals didn't answer " + path, failure);
        }
    }

    private static <T> Reply<T> read(RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response, Class<T> answer)
        throws IOException {
        var status = response.getStatusCode().value();
        if (status >= 200 && status < 300) {
            return new Reply<>(status, answer == Void.class ? null : response.bodyTo(answer), null, null);
        }
        ErrorBody error;
        try {
            error = response.bodyTo(ErrorBody.class);
        } catch (RuntimeException noErrorBody) {
            error = null;
        }
        return new Reply<>(status, null, error == null ? null : error.error(), error == null ? null : error.message());
    }
}
