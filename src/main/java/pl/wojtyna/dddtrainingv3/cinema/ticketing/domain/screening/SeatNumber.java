package pl.wojtyna.dddtrainingv3.cinema.ticketing.domain.screening;

import org.jmolecules.ddd.annotation.ValueObject;

/// A row letter and a seat in that row, e.g. A7. Halls have rows A–J.
@ValueObject
public record SeatNumber(char row, int seat) {

    private static final char LAST_ROW = 'J';

    public SeatNumber {
        if (row < 'A' || row > LAST_ROW) {
            throw new IllegalArgumentException("There is no row " + row + ": halls have rows A-" + LAST_ROW);
        }
    }

    public static SeatNumber of(String text) {
        return new SeatNumber(text.charAt(0), Integer.parseInt(text.substring(1)));
    }
}
