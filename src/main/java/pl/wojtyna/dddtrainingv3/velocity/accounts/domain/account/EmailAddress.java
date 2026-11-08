package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

import java.util.Locale;
import java.util.regex.Pattern;

/// The address a person registers with; one account per email address (DE-98). Compared case-insensitively.
@ValueObject
public record EmailAddress(String value) {

    private static final Pattern WELL_FORMED = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public EmailAddress {
        if (value == null || !WELL_FORMED.matcher(value.trim()).matches()) {
            throw new IllegalArgumentException("'" + value + "' is not an email address");
        }
        value = value.trim().toLowerCase(Locale.ROOT);
    }
}
