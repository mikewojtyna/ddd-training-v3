package pl.wojtyna.dddtrainingv3.velocity.debtrecovery.domain.recoveryprocess;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.UUID;

@ValueObject
public record RecoveryProcessId(String value) {

    public static RecoveryProcessId generate() {
        return new RecoveryProcessId(UUID.randomUUID().toString());
    }
}
