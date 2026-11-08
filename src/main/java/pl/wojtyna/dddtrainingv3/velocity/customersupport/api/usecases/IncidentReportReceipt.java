package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases;

/// What the rider app learns right after reporting: the new incident's id and its detected type
/// (`FLEET_ISSUE`, `BILLING_DISPUTE`, `SAFETY_CONCERN`, `OTHER`).
public record IncidentReportReceipt(String incidentId, String type) {
}
