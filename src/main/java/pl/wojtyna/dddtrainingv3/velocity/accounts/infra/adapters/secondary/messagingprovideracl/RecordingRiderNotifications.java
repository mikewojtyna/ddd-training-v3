package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.secondary.messagingprovideracl;

import org.jmolecules.architecture.hexagonal.SecondaryAdapter;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.BlockReason;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderId;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderNotifications;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/// Simulated messaging provider: turns a block notification into the message the rider would read and records it
/// instead of sending it. Tests (of any context) read what was sent with `sentTo(riderId)`.
@SecondaryAdapter
public class RecordingRiderNotifications implements RiderNotifications {

    /// A message as the provider would deliver it. `reason` is `INVALID_CARD` or `OUTSTANDING_DEBT`.
    public record SentNotification(String riderId, String reason, String message, Instant sentAt) {
    }

    private final Clock clock;
    private final List<SentNotification> sent = new CopyOnWriteArrayList<>();

    public RecordingRiderNotifications(Clock clock) {
        this.clock = clock;
    }

    @Override
    public void sendBlockNotification(RiderId rider, BlockReason reason) {
        sent.add(new SentNotification(rider.value(), reason.name(), messageFor(reason), clock.instant()));
    }

    public List<SentNotification> sentTo(String riderId) {
        return sent.stream().filter(notification -> notification.riderId().equals(riderId)).toList();
    }

    private static String messageFor(BlockReason reason) {
        return switch (reason) {
            case INVALID_CARD -> "Your card is no longer usable, so you can't start new rentals. Add a new card to ride again.";
            case OUTSTANDING_DEBT -> "You have an outstanding debt, so you can't start new rentals. Pay it off in the app to ride again.";
        };
    }
}
