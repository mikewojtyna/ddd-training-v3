package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Tells the rider about their account through the messaging provider.
@SecondaryPort
public interface RiderNotifications {

    /// The block notification: why the rider can't start rentals and what to do next (DE-7).
    void sendBlockNotification(RiderId rider, BlockReason reason);
}
