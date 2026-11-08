package pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases;

import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Holder;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.Position;

import java.math.BigDecimal;

/// Turns the api records' strings and decimals into Rentals' value objects. Bad input is an `IllegalArgumentException`.
final class Translation {

    private Translation() {
    }

    static Holder holder(String holderType, String holderId) {
        if (holderType == null || holderId == null) {
            throw new IllegalArgumentException("holderType and holderId are required");
        }
        return new Holder(Holder.HolderType.valueOf(holderType), holderId);
    }

    static Position position(BigDecimal latitude, BigDecimal longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("latitude and longitude are required");
        }
        return new Position(latitude.doubleValue(), longitude.doubleValue());
    }
}
