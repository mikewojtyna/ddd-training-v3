package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;

public sealed interface ScreeningEvent extends DomainEvent {

    record SeatReserved(ScreeningId screeningId, SeatNumber seatNumber, CustomerId customerId) implements ScreeningEvent {
    }
}
