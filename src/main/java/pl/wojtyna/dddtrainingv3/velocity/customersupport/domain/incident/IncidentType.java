package pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident;

import java.util.Optional;

/// How an incident is routed.
public enum IncidentType {
    FLEET_ISSUE, BILLING_DISPUTE, SAFETY_CONCERN, OTHER;

    /// DE-50 (and DE-85): detected from the category the rider picked and whether they named a bike.
    public static IncidentType detect(Category category, Optional<BikeId> bike) {
        return switch (category) {
            case BROKEN_BIKE, CANT_UNLOCK -> bike.isPresent() ? FLEET_ISSUE : OTHER;
            case WRONGFUL_CHARGE -> BILLING_DISPUTE;
            case SAFETY -> SAFETY_CONCERN;
            case OTHER -> OTHER;
        };
    }
}
