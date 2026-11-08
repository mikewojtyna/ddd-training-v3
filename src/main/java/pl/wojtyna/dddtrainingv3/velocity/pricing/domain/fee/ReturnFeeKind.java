package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.fee;

import org.jmolecules.ddd.annotation.ValueObject;

/// Which return fee a session pays: none (every ending except free-floating), the drop zone fee or the
/// unauthorized zone return fee.
@ValueObject
public enum ReturnFeeKind {
    NONE,
    DROP_ZONE,
    UNAUTHORIZED_ZONE
}
