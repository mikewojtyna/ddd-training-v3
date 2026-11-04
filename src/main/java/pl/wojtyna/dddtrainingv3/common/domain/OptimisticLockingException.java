package pl.wojtyna.dddtrainingv3.common.domain;

/// Thrown by repositories when an aggregate was modified by someone else after it was loaded.
public class OptimisticLockingException extends RuntimeException {

    public OptimisticLockingException(String message) {
        super(message);
    }
}
