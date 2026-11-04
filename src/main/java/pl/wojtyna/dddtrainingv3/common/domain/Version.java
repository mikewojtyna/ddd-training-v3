package pl.wojtyna.dddtrainingv3.common.domain;

import org.jmolecules.ddd.annotation.ValueObject;

/// Version of an aggregate as it was loaded. Repositories compare it on save to detect concurrent modification.
@ValueObject
public record Version(long value) {

    public Version {
        if (value < 0) {
            throw new IllegalArgumentException("Version cannot be negative");
        }
    }

    public static Version initial() {
        return new Version(0);
    }

    public Version next() {
        return new Version(value + 1);
    }

    public Version plus(long changes) {
        return new Version(value + changes);
    }
}
