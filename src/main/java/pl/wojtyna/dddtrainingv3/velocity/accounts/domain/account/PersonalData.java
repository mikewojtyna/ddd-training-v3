package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.LocalDate;

/// Full name, date of birth and country of residency, all required (DE-2). Any country is accepted.
@ValueObject
public record PersonalData(String fullName, LocalDate dateOfBirth, String countryOfResidency) {

    public PersonalData {
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Full name is required");
        }
        if (dateOfBirth == null) {
            throw new IllegalArgumentException("Date of birth is required");
        }
        if (countryOfResidency == null || countryOfResidency.isBlank()) {
            throw new IllegalArgumentException("Country of residency is required");
        }
    }
}
