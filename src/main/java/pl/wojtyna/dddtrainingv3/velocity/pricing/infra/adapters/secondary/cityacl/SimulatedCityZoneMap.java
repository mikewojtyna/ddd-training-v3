package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.secondary.cityacl;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/// Stand-in for the City's zone-map service, in the City's own language: approved drop-off areas, each a circle,
/// which the City may withdraw for a time window. Distances are great-circle distances (haversine, R = 6371 km);
/// a point exactly on the circle is covered.
public class SimulatedCityZoneMap {

    private static final double EARTH_RADIUS_METRES = 6_371_000;

    /// An approved drop-off area of the City.
    public record DropOffArea(String name, double centreLatitude, double centreLongitude, double radiusMetres) {
    }

    /// The City withdrew an area from `from` (inclusive) until `until` (exclusive).
    private record Withdrawal(String areaName, Instant from, Instant until) {

        boolean covers(String name, Instant moment) {
            return areaName.equals(name) && !moment.isBefore(from) && moment.isBefore(until);
        }
    }

    private final List<DropOffArea> areas;
    private final List<Withdrawal> withdrawals = new CopyOnWriteArrayList<>();

    public SimulatedCityZoneMap(List<DropOffArea> areas) {
        this.areas = List.copyOf(areas);
    }

    /// The City's configured map: Old Town Square and Riverside Park (integration-contracts.md).
    public static SimulatedCityZoneMap warsawDropOffAreas() {
        return new SimulatedCityZoneMap(List.of(new DropOffArea("Old Town Square", 52.2297, 21.0122, 150),
                                                new DropOffArea("Riverside Park", 52.2400, 21.0300, 200)));
    }

    /// Names of the approved drop-off areas covering the point at the given moment.
    public List<String> areasCovering(double latitude, double longitude, Instant moment) {
        return areas.stream()
                    .filter(area -> distanceMetres(latitude, longitude, area.centreLatitude(), area.centreLongitude())
                                    <= area.radiusMetres())
                    .map(DropOffArea::name)
                    .filter(name -> withdrawals.stream().noneMatch(withdrawal -> withdrawal.covers(name, moment)))
                    .toList();
    }

    /// Test hook: the City withdraws an area from `from` (inclusive) until `until` (exclusive).
    public void withdraw(String areaName, Instant from, Instant until) {
        withdrawals.add(new Withdrawal(areaName, from, until));
    }

    static double distanceMetres(double latitude1, double longitude1, double latitude2, double longitude2) {
        var phi1 = Math.toRadians(latitude1);
        var phi2 = Math.toRadians(latitude2);
        var deltaPhi = Math.toRadians(latitude2 - latitude1);
        var deltaLambda = Math.toRadians(longitude2 - longitude1);
        var a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2)
                + Math.cos(phi1) * Math.cos(phi2) * Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2);
        return 2 * EARTH_RADIUS_METRES * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }
}
