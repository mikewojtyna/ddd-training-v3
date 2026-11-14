package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.ticket;

/// Fake of the ticket sequence, starting at a given number.
class TicketSequenceAt implements TicketSequence {

    private long current;

    TicketSequenceAt(long current) {
        this.current = current;
    }

    @Override
    public long next() {
        return ++current;
    }
}
