package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.zone;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

import java.time.Instant;

/// The City's zone map in Pricing's language: is a position inside a drop zone or in the unauthorized zone, on the
/// zone map valid at a given moment? A position on a drop zone's border is inside (DE-84).
@SecondaryPort
public interface ZoneMap {

    Zone zoneAt(Position position, Instant moment);
}
