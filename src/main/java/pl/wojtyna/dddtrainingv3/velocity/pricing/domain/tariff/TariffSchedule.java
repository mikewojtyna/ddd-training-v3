package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;
import pl.wojtyna.dddtrainingv3.common.domain.Version;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.FluentLanguage.At;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.FluentLanguage.By;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/// All tariffs of VeloCity. A state-based aggregate: the publication rules span every tariff (no two share a valid
/// from), so the whole schedule is one consistency boundary.
///
/// Invariants:
/// - a tariff's valid from lies after the moment it is published (DE-96);
/// - no two tariffs share a valid from (DE-96);
/// - no two tariffs share an id.
@AggregateRoot
public class TariffSchedule {

    /// VeloCity has exactly one tariff schedule.
    public static final TariffScheduleId VELOCITY = new TariffScheduleId("velocity");

    private final TariffScheduleId id;
    private final Version version; // the version the schedule was created or loaded at; commands never change it
    private final List<Tariff> tariffs; // needed by: unique valid from, unique id; read by: tariff valid at

    private TariffSchedule(TariffScheduleId id, Version version, List<Tariff> tariffs) {
        this.id = id;
        this.version = version;
        this.tariffs = new ArrayList<>(tariffs);
    }

    /// Establishes the schedule with its first tariff, the launch tariff (DE-30). This is not a publication: the
    /// launch tariff's valid from may lie in the past.
    public static TariffSchedule startingWith(Tariff firstTariff) {
        return new TariffSchedule(VELOCITY, Version.initial(), List.of(firstTariff));
    }

    public By<PricingManagerId, At<Instant, DomainEvents>> publish(Tariff tariff) {
        return publisher -> now -> {
            if (!tariff.validFrom().isAfter(now)) {
                throw new ValidFromNotInFutureException("Tariff %s must be valid from a future moment, not %s"
                                                            .formatted(tariff.id().value(), tariff.validFrom()));
            }
            if (tariffs.stream().anyMatch(published -> published.validFrom().equals(tariff.validFrom()))) {
                throw new DuplicateValidFromException("A tariff valid from %s is already published"
                                                          .formatted(tariff.validFrom()));
            }
            if (tariffs.stream().anyMatch(published -> published.id().equals(tariff.id()))) {
                throw new DuplicateTariffIdException("Tariff %s is already published".formatted(tariff.id().value()));
            }
            tariffs.add(tariff);
            return DomainEvents.of(new TariffEvent.TariffPublished(tariff, publisher, now));
        };
    }

    /// The published tariff with the latest valid from that isn't after the given moment.
    public Tariff tariffValidAt(Instant moment) {
        return tariffs.stream()
                      .filter(tariff -> !tariff.validFrom().isAfter(moment))
                      .max(Comparator.comparing(Tariff::validFrom))
                      .orElseThrow(() -> new NoTariffValidException("No tariff is valid at " + moment));
    }

    /// Every tariff, ordered by valid from.
    public List<Tariff> tariffs() {
        return tariffs.stream().sorted(Comparator.comparing(Tariff::validFrom)).toList();
    }

    @Identity
    public TariffScheduleId id() {
        return id;
    }

    public Version version() {
        return version;
    }

    // --- persistence support (memento), not used by domain logic ---

    public record Snapshot(TariffScheduleId id, Version version, List<Tariff> tariffs) {

        public Snapshot {
            tariffs = List.copyOf(tariffs);
        }
    }

    public Snapshot snapshot() {
        return new Snapshot(id, version, tariffs);
    }

    public static TariffSchedule restore(Snapshot snapshot) {
        return new TariffSchedule(snapshot.id(), snapshot.version(), snapshot.tariffs());
    }
}
