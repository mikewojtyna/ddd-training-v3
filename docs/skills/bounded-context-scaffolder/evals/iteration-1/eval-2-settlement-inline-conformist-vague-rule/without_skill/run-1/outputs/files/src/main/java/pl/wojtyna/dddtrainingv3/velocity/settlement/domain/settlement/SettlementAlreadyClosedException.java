package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.settlement;

public class SettlementAlreadyClosedException extends RuntimeException {

    public SettlementAlreadyClosedException(String message) {
        super(message);
    }
}
