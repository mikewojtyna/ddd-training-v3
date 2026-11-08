package pl.wojtyna.dddtrainingv3.velocity.settlement.domain.debt;

/// Only a rider with outstanding debt can pay it off (rider-pays-off-outstanding-debt R3).
public class NoOutstandingDebtException extends RuntimeException {

    public NoOutstandingDebtException(String message) {
        super(message);
    }

    public NoOutstandingDebtException(String message, Throwable cause) {
        super(message, cause);
    }
}
