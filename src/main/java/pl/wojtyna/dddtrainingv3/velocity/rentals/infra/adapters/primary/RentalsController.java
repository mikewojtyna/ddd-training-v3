package pl.wojtyna.dddtrainingv3.velocity.rentals.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.ErrorResponse;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.BikeAvailability;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.BikeAvailabilityQuery;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.BikeAvailabilityQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.RidersActiveRental;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.RidersActiveRentalQuery;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.queries.RidersActiveRentalQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.AddBikeToFleet;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.AddBikeToFleetUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.BikeReservation;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndRideHere;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.EndRideHereUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.MakeBikeAvailable;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.MakeBikeAvailableUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReserveBike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReserveBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReturnBikeAtStation;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.ReturnBikeAtStationUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.SessionEnd;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.SignalFault;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.SignalFaultUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.UnlockBike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.UnlockBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.UnlockOutcome;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.WithdrawBike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.api.usecases.WithdrawBikeUseCase;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeAlreadyHeld;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeAlreadyInFleet;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeNotAvailable;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeNotFound;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeNotUnlockedForService;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.BikeNotWithdrawn;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.NoRunningSession;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.NotReservationHolder;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.OneRiderOneBike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RentalEligibilityUnknown;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.RiderNotEligible;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.TooFarFromBike;
import pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bike.UnknownStation;

import java.math.BigDecimal;

/// Rentals' Open Host Service (integration-contracts.md): the rider app, the fleet manager, the bikes' telemetry and
/// Fleet Maintenance call it. A broken rule answers `409 {error, message}` (the error names the rule), an unknown bike
/// `404`, malformed input `400`, and Accounts not answering the eligibility question `503`.
@PrimaryAdapter
@RestController
@RequestMapping("/rentals")
public class RentalsController {

    public record AddBikeRequest(String bikeId, String stationId) {
    }

    public record ReservationRequest(String holderType, String holderId, BigDecimal latitude, BigDecimal longitude) {
    }

    public record UnlockRequest(String holderType, String holderId) {
    }

    public record StationReturnRequest(String riderId, String stationId, String faultDescription) {
    }

    public record FreeFloatingReturnRequest(String riderId, BigDecimal latitude, BigDecimal longitude) {
    }

    public record FaultSignalRequest(String faultCode) {
    }

    public record AvailabilityRequest(String stationId, BigDecimal latitude, BigDecimal longitude) {
    }

    private final AddBikeToFleetUseCase addBikeToFleet;
    private final ReserveBikeUseCase reserveBike;
    private final UnlockBikeUseCase unlockBike;
    private final ReturnBikeAtStationUseCase returnBikeAtStation;
    private final EndRideHereUseCase endRideHere;
    private final SignalFaultUseCase signalFault;
    private final WithdrawBikeUseCase withdrawBike;
    private final MakeBikeAvailableUseCase makeBikeAvailable;
    private final BikeAvailabilityQueryHandler bikeAvailability;
    private final RidersActiveRentalQueryHandler ridersActiveRental;

    public RentalsController(AddBikeToFleetUseCase addBikeToFleet, ReserveBikeUseCase reserveBike, UnlockBikeUseCase unlockBike,
                             ReturnBikeAtStationUseCase returnBikeAtStation, EndRideHereUseCase endRideHere,
                             SignalFaultUseCase signalFault, WithdrawBikeUseCase withdrawBike,
                             MakeBikeAvailableUseCase makeBikeAvailable, BikeAvailabilityQueryHandler bikeAvailability,
                             RidersActiveRentalQueryHandler ridersActiveRental) {
        this.addBikeToFleet = addBikeToFleet;
        this.reserveBike = reserveBike;
        this.unlockBike = unlockBike;
        this.returnBikeAtStation = returnBikeAtStation;
        this.endRideHere = endRideHere;
        this.signalFault = signalFault;
        this.withdrawBike = withdrawBike;
        this.makeBikeAvailable = makeBikeAvailable;
        this.bikeAvailability = bikeAvailability;
        this.ridersActiveRental = ridersActiveRental;
    }

    @PostMapping("/bikes")
    @ResponseStatus(HttpStatus.CREATED)
    public void addBikeToFleet(@RequestBody AddBikeRequest request) {
        addBikeToFleet.handle(new AddBikeToFleet(request.bikeId(), request.stationId()));
    }

    @GetMapping("/bikes/{bikeId}")
    public ResponseEntity<BikeAvailability> bikeAvailability(@PathVariable("bikeId") String bikeId) {
        return ResponseEntity.of(bikeAvailability.handle(new BikeAvailabilityQuery(bikeId)));
    }

    @PostMapping("/bikes/{bikeId}/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public BikeReservation reserve(@PathVariable("bikeId") String bikeId, @RequestBody ReservationRequest request) {
        return reserveBike.handle(new ReserveBike(bikeId, request.holderType(), request.holderId(), request.latitude(),
                                                  request.longitude()));
    }

    @PostMapping("/bikes/{bikeId}/unlock")
    public UnlockOutcome unlock(@PathVariable("bikeId") String bikeId, @RequestBody UnlockRequest request) {
        return unlockBike.handle(new UnlockBike(bikeId, request.holderType(), request.holderId()));
    }

    @PostMapping("/bikes/{bikeId}/station-return")
    public SessionEnd returnAtStation(@PathVariable("bikeId") String bikeId, @RequestBody StationReturnRequest request) {
        return returnBikeAtStation.handle(new ReturnBikeAtStation(bikeId, request.riderId(), request.stationId(),
                                                                  request.faultDescription()));
    }

    @PostMapping("/bikes/{bikeId}/free-floating-return")
    public SessionEnd endRideHere(@PathVariable("bikeId") String bikeId, @RequestBody FreeFloatingReturnRequest request) {
        return endRideHere.handle(new EndRideHere(bikeId, request.riderId(), request.latitude(), request.longitude()));
    }

    @PostMapping("/bikes/{bikeId}/fault-signals")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void signalFault(@PathVariable("bikeId") String bikeId, @RequestBody FaultSignalRequest request) {
        signalFault.handle(new SignalFault(bikeId, request.faultCode()));
    }

    @PostMapping("/bikes/{bikeId}/withdrawal")
    public void withdraw(@PathVariable("bikeId") String bikeId) {
        withdrawBike.handle(new WithdrawBike(bikeId));
    }

    @PostMapping("/bikes/{bikeId}/availability")
    public void makeAvailable(@PathVariable("bikeId") String bikeId, @RequestBody AvailabilityRequest request) {
        makeBikeAvailable.handle(new MakeBikeAvailable(bikeId, request.stationId(), request.latitude(), request.longitude()));
    }

    @GetMapping("/riders/{riderId}/active-rental")
    public ResponseEntity<RidersActiveRental> activeRental(@PathVariable("riderId") String riderId) {
        return ResponseEntity.of(ridersActiveRental.handle(new RidersActiveRentalQuery(riderId)));
    }

    @ExceptionHandler({BikeAlreadyInFleet.class, UnknownStation.class, TooFarFromBike.class, RiderNotEligible.class,
        OneRiderOneBike.class, BikeNotAvailable.class, BikeNotWithdrawn.class, BikeAlreadyHeld.class,
        NotReservationHolder.class, NoRunningSession.class, BikeNotUnlockedForService.class,
        OptimisticLockingException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse brokenRule(RuntimeException exception) {
        return ErrorResponse.of(exception);
    }

    @ExceptionHandler(BikeNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFound(BikeNotFound exception) {
        return ErrorResponse.of(exception);
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(RuntimeException exception) {
        return ErrorResponse.of(exception);
    }

    @ExceptionHandler(RentalEligibilityUnknown.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse eligibilityUnknown(RentalEligibilityUnknown exception) {
        return ErrorResponse.of(exception);
    }
}
