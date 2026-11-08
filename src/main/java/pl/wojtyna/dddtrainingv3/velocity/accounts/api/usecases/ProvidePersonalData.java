package pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases;

import java.time.LocalDate;

/// Inbound command *Provide Personal Data* (rider app).
public record ProvidePersonalData(String riderId, String fullName, LocalDate dateOfBirth, String countryOfResidency) {
}
