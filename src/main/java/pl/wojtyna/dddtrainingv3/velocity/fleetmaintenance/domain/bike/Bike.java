package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike;

import org.jmolecules.ddd.annotation.AggregateRoot;
import org.jmolecules.ddd.annotation.Identity;
import pl.wojtyna.dddtrainingv3.common.domain.DomainEvents;

import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeMaintenanceEvent.BikeFixed;
import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeMaintenanceEvent.BikeReportedBroken;
import static pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.BikeMaintenanceEvent.MaintenanceStarted;

/// The condition of one bike, as Fleet Maintenance sees it: in circulation, broken, or under maintenance.
/// One bike is the consistency boundary because every business rule is about the condition of a single bike
/// (plan D5). Reservations are Rentals' state and aren't tracked here (plan D6).
@AggregateRoot
public class Bike {

    private enum Condition {
        IN_CIRCULATION,
        BROKEN,
        UNDER_MAINTENANCE
    }

    private final BikeId id;
    private Condition condition;

    private Bike(BikeId id, Condition condition) {
        this.id = id;
        this.condition = condition;
    }

    /// A bike Fleet Maintenance hasn't heard of yet is in circulation (plan D8).
    public static Bike inCirculation(BikeId id) {
        return new Bike(id, Condition.IN_CIRCULATION);
    }

    /// U1: a fault report makes a bike in circulation broken.
    /// R5: a fault report on a bike that is already broken changes nothing. A bike under maintenance is still
    /// broken, so a report during maintenance is ignored too (Q5, suggested default).
    public DomainEvents reportFault() {
        if (condition != Condition.IN_CIRCULATION) {
            return DomainEvents.empty();
        }
        condition = Condition.BROKEN;
        return DomainEvents.of(new BikeReportedBroken(id));
    }

    /// U2: a technician's unlock of a broken bike starts maintenance, whichever technician it is (R4).
    /// R3: a technician's unlock of a bike that isn't broken is ignored.
    /// Not modelled yet (Q6): an unlock of a bike already under maintenance is ignored for now.
    public DomainEvents startMaintenance(TechnicianId technician) {
        if (condition != Condition.BROKEN) {
            return DomainEvents.empty();
        }
        condition = Condition.UNDER_MAINTENANCE;
        return DomainEvents.of(new MaintenanceStarted(id, technician));
    }

    /// R2: returning a bike under maintenance to circulation means it is fixed.
    /// R1: a bike whose maintenance hasn't started can't be returned; it stays withdrawn.
    /// Not modelled yet (Q7): any technician may return the bike, not only the one who started maintenance.
    public DomainEvents returnToCirculation(TechnicianId technician) {
        if (condition != Condition.UNDER_MAINTENANCE) {
            throw new MaintenanceNotStarted(id);
        }
        condition = Condition.IN_CIRCULATION;
        return DomainEvents.of(new BikeFixed(id, technician));
    }

    @Identity
    public BikeId id() {
        return id;
    }
}
