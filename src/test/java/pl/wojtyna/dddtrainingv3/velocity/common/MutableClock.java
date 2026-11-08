package pl.wojtyna.dddtrainingv3.velocity.common;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/// A clock tests can set and move. It replaces `velocityClock` in Spring tests (see `VelocityTestConfig`).
public class MutableClock extends Clock {

    private volatile Instant now;

    public MutableClock(Instant now) {
        this.now = now;
    }

    public void setTo(Instant instant) {
        this.now = instant;
    }

    public void setTo(String isoInstant) {
        setTo(Instant.parse(isoInstant));
    }

    public void advanceBy(Duration duration) {
        this.now = now.plus(duration);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return now;
    }
}
