package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// The cinema's ticket sequence: 1, 2, 3, … across all screenings.
@SecondaryPort
public interface TicketSequence {

    long next();
}
