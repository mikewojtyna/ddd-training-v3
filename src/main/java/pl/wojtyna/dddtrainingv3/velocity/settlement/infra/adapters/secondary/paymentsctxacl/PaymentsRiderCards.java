package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctxacl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.VelocityServiceUrls;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.Charge;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeFailureReason;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.ChargeOutcome;
import pl.wojtyna.dddtrainingv3.velocity.settlement.domain.charging.RiderCards;

import java.math.BigDecimal;

/// Calls Payments' *Charge*. `status: FAILED` becomes a failed `ChargeOutcome` with Payments' `failureReason`
/// (`DECLINED`, `NO_VALID_PAYMENT_METHOD`), not an exception.
@SecondaryAdapter
public class PaymentsRiderCards implements RiderCards {

    private record ChargeRequest(String riderId, BigDecimal amount, String reference) {
    }

    private record ChargeResponse(String chargeId, String status, String failureReason) {
    }

    private final RestClient restClient;
    private final VelocityServiceUrls urls;

    public PaymentsRiderCards(RestClient restClient, VelocityServiceUrls urls) {
        this.restClient = restClient;
        this.urls = urls;
    }

    @Override
    public ChargeOutcome charge(Charge charge) {
        var response = restClient.post()
                                 .uri(urls.url("/payments/charges"))
                                 .contentType(MediaType.APPLICATION_JSON)
                                 .body(new ChargeRequest(charge.rider().value(), charge.amount().getAmount(), charge.reference()))
                                 .retrieve()
                                 .body(ChargeResponse.class);
        if (response == null) {
            throw new IllegalStateException("Payments gave no answer to charge " + charge.reference());
        }
        if ("SUCCEEDED".equals(response.status())) {
            return ChargeOutcome.success();
        }
        return ChargeOutcome.failure("NO_VALID_PAYMENT_METHOD".equals(response.failureReason())
                                         ? ChargeFailureReason.NO_VALID_PAYMENT_METHOD
                                         : ChargeFailureReason.DECLINED);
    }
}
