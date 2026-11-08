package pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries;

/// Query *Rider's incidents* (rider app): every incident a rider reported.
public record RiderIncidentsQuery(String riderId) {
}
