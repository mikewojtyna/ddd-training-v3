package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

import java.time.Instant;

/// Canvas command *Resolve Uncontested Rejections* (support clock, DE-87): resolves every rejected incident whose
/// reopening window has closed by `now`.
public record ResolveUncontestedRejections(Instant now) {
}
