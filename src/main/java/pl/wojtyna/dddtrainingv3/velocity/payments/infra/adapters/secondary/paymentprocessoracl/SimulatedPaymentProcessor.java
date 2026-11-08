package pl.wojtyna.dddtrainingv3.velocity.payments.infra.adapters.secondary.paymentprocessoracl;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;

/// Stand-in for the external payment processor, speaking its own language: authorizations (holds) that are voided,
/// captures (charges), and response codes.
///
/// Default answers: an authorization is declined (`DO_NOT_HONOR`) for tokens starting with `tok_declined`, a capture
/// is declined (`INSUFFICIENT_FUNDS`) for tokens starting with `tok_nofunds`, everything else is `APPROVED`. Tests
/// can script the next answer for a token with `scriptNextAuthorization` / `scriptNextCapture`, and read what the
/// processor received with `authorizations()`, `voids()` and `captures()`.
public class SimulatedPaymentProcessor {

    public enum ResponseCode {
        APPROVED, DO_NOT_HONOR, INSUFFICIENT_FUNDS
    }

    public record Authorization(String authorizationId, String cardToken, BigDecimal amount, String currency,
                                ResponseCode responseCode) {
    }

    public record Capture(String cardToken, BigDecimal amount, String currency, String merchantReference,
                          ResponseCode responseCode) {
    }

    private final List<Authorization> authorizations = new CopyOnWriteArrayList<>();
    private final List<String> voids = new CopyOnWriteArrayList<>();
    private final List<Capture> captures = new CopyOnWriteArrayList<>();
    private final Map<String, Queue<ResponseCode>> scriptedAuthorizations = new ConcurrentHashMap<>();
    private final Map<String, Queue<ResponseCode>> scriptedCaptures = new ConcurrentHashMap<>();

    public Authorization authorize(String cardToken, BigDecimal amount, String currency) {
        var responseCode = next(scriptedAuthorizations, cardToken)
            .orElse(cardToken.startsWith("tok_declined") ? ResponseCode.DO_NOT_HONOR : ResponseCode.APPROVED);
        var authorization = new Authorization(UUID.randomUUID().toString(), cardToken, amount, currency, responseCode);
        authorizations.add(authorization);
        return authorization;
    }

    public void voidAuthorization(String authorizationId) {
        voids.add(authorizationId);
    }

    public Capture capture(String cardToken, BigDecimal amount, String currency, String merchantReference) {
        var responseCode = next(scriptedCaptures, cardToken)
            .orElse(cardToken.startsWith("tok_nofunds") ? ResponseCode.INSUFFICIENT_FUNDS : ResponseCode.APPROVED);
        var capture = new Capture(cardToken, amount, currency, merchantReference, responseCode);
        captures.add(capture);
        return capture;
    }

    /// The next authorization of `cardToken` gets `responseCode`, whatever the token's prefix.
    public void scriptNextAuthorization(String cardToken, ResponseCode responseCode) {
        scriptedAuthorizations.computeIfAbsent(cardToken, token -> new ConcurrentLinkedQueue<>()).add(responseCode);
    }

    /// The next capture of `cardToken` gets `responseCode`, whatever the token's prefix.
    public void scriptNextCapture(String cardToken, ResponseCode responseCode) {
        scriptedCaptures.computeIfAbsent(cardToken, token -> new ConcurrentLinkedQueue<>()).add(responseCode);
    }

    public List<Authorization> authorizations() {
        return List.copyOf(authorizations);
    }

    public List<Authorization> authorizationsOf(String cardToken) {
        return authorizations.stream().filter(authorization -> authorization.cardToken().equals(cardToken)).toList();
    }

    public List<String> voids() {
        return List.copyOf(voids);
    }

    public boolean isVoided(String authorizationId) {
        return voids.contains(authorizationId);
    }

    public List<Capture> captures() {
        return List.copyOf(captures);
    }

    public List<Capture> capturesOf(String cardToken) {
        return captures.stream().filter(capture -> capture.cardToken().equals(cardToken)).toList();
    }

    public List<Capture> capturesFor(String merchantReference) {
        return captures.stream().filter(capture -> capture.merchantReference().equals(merchantReference)).toList();
    }

    private static java.util.Optional<ResponseCode> next(Map<String, Queue<ResponseCode>> scripted, String cardToken) {
        var queue = scripted.get(cardToken);
        return java.util.Optional.ofNullable(queue == null ? null : queue.poll());
    }
}
