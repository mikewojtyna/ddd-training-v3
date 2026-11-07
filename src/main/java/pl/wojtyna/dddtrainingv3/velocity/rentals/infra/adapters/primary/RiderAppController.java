package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikes;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStation;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.AvailableBikesAtStationHandler;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.RentBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReserveBike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReserveBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.*;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.AlreadyHoldsReservationException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.AlreadyRidingException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.BorrowerNotEligibleException;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.borrowing.HoldsReservationForAnotherBikeException;

import java.time.Duration;

/// REST API for the rider app: *Reserve bike*, *Unlock bike* (rent) and *Available bikes at station*.
/// Translates the spec's *rider* into Rentals' *borrower*. Rule rejections answer 409 Conflict.
@PrimaryAdapter
@RestController
@RequestMapping("/rentals/stations/{stationId}")
public class RiderAppController {

    private final ReserveBikeUseCase reserveBike;
    private final RentBikeUseCase rentBike;
    private final AvailableBikesAtStationHandler availableBikes;

    public RiderAppController(ReserveBikeUseCase reserveBike, RentBikeUseCase rentBike,
                              AvailableBikesAtStationHandler availableBikes) {
        this.reserveBike = reserveBike;
        this.rentBike = rentBike;
        this.availableBikes = availableBikes;
    }

    @PostMapping("/bikes/{bikeId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public void reserve(@PathVariable("stationId") String stationId, @PathVariable("bikeId") String bikeId,
                        @RequestBody ReservationRequest request) {
        reserveBike.handle(new ReserveBike(stationId, bikeId, request.riderId()));
    }

    @PostMapping("/bikes/{bikeId}/rentals")
    @ResponseStatus(HttpStatus.CREATED)
    public void rent(@PathVariable("stationId") String stationId, @PathVariable("bikeId") String bikeId,
                     @RequestBody RentalRequest request) {
        rentBike.rent(new BikeStationId(stationId), new BikeId(bikeId), new BorrowerId(request.riderId()),
                      new RentalDuration(Duration.parse(request.duration())));
    }

    @GetMapping("/available-bikes")
    public AvailableBikes availableBikes(@PathVariable("stationId") String stationId) {
        return availableBikes.handle(new AvailableBikesAtStation(stationId));
    }

    @ExceptionHandler({BikeNotAvailableException.class, AlreadyHoldsReservationException.class,
                       AlreadyRidingException.class, HoldsReservationForAnotherBikeException.class,
                       BorrowerNotEligibleException.class, MaximumRentalDurationExceededException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public String rejected(RuntimeException exception) {
        return exception.getMessage();
    }

    @ExceptionHandler(BikeStationNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String notFound(BikeStationNotFoundException exception) {
        return exception.getMessage();
    }
}
