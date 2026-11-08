package pl.wojtyna.dddtrainingv3.velocity.common.infra;

/// Body of a rejected REST request: `error` names the broken rule (usually the exception's simple class name),
/// `message` explains it. `409` for a broken business rule, `404` for an unknown resource, `400` for bad input.
public record ErrorResponse(String error, String message) {

    public static ErrorResponse of(RuntimeException exception) {
        return new ErrorResponse(exception.getClass().getSimpleName(), exception.getMessage());
    }
}
