package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/// A bike as technicians see it (integration-contracts.md, Fleet Maintenance + "Additions"). `status`:
/// `BROKEN`|`UNDER_MAINTENANCE`|`IN_CIRCULATION`; `faultSource`: `LOCK_FAILED`|`FAULT_REPORTED_AT_RETURN`|
/// `FAULT_SIGNALLED`|`NOT_RETURNED`|`FLEET_ISSUE`|`TECHNICIAN`; `technicianId` is the technician holding the
/// reservation or doing the maintenance. Where the bike is (`stationId` or `latitude`/`longitude`) comes from Rentals
/// and is null when unknown (a bike that wasn't returned, DE-26).
public record BrokenBikeView(String bikeId, String status, String faultSource, Instant reportedAt, String faultDescription,
                             String technicianId, Instant reservedUntil, List<String> incidentIds, String stationId,
                             BigDecimal latitude, BigDecimal longitude) {

    public BrokenBikeView at(Whereabouts whereabouts) {
        return new BrokenBikeView(bikeId, status, faultSource, reportedAt, faultDescription, technicianId, reservedUntil,
                                  incidentIds, whereabouts.stationId(), whereabouts.latitude(), whereabouts.longitude());
    }

    /// Where a bike is, as Rentals knows it.
    public record Whereabouts(String stationId, BigDecimal latitude, BigDecimal longitude) {
    }
}
