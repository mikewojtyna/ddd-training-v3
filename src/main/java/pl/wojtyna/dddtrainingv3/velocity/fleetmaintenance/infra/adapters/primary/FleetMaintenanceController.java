package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary;

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
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikeQuery;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikeQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikeView;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikesQuery;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.queries.BrokenBikesQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.*;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.*;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;

/// Fleet Maintenance's REST resources for the technician app (integration-contracts.md, "Fleet Maintenance"). A
/// broken rule answers `409 {error, message}`, a bike unknown to the fleet (or to Fleet Maintenance, for the query)
/// `404`, malformed input `400`, another context not answering `503`.
@PrimaryAdapter
@RestController
@RequestMapping("/fleet-maintenance")
public class FleetMaintenanceController {

    public record FaultReportRequest(String technicianId, String description) {
    }

    public record TechnicianRequest(String technicianId) {
    }

    public record CirculationReturnRequest(String technicianId, String stationId, BigDecimal latitude, BigDecimal longitude) {
    }

    private final ReportFaultUseCase reportFault;
    private final ReserveBrokenBikeUseCase reserveBrokenBike;
    private final UnlockBrokenBikeUseCase unlockBrokenBike;
    private final ReturnBikeToCirculationUseCase returnBikeToCirculation;
    private final BrokenBikeQueryHandler brokenBike;
    private final BrokenBikesQueryHandler brokenBikes;
    private final Clock clock;

    public FleetMaintenanceController(ReportFaultUseCase reportFault, ReserveBrokenBikeUseCase reserveBrokenBike,
                                      UnlockBrokenBikeUseCase unlockBrokenBike,
                                      ReturnBikeToCirculationUseCase returnBikeToCirculation,
                                      BrokenBikeQueryHandler brokenBike, BrokenBikesQueryHandler brokenBikes, Clock clock) {
        this.reportFault = reportFault;
        this.reserveBrokenBike = reserveBrokenBike;
        this.unlockBrokenBike = unlockBrokenBike;
        this.returnBikeToCirculation = returnBikeToCirculation;
        this.brokenBike = brokenBike;
        this.brokenBikes = brokenBikes;
        this.clock = clock;
    }

    @PostMapping("/bikes/{bikeId}/fault-reports")
    public void reportFault(@PathVariable("bikeId") String bikeId, @RequestBody FaultReportRequest request) {
        reportFault.handle(new ReportFault(bikeId, request.technicianId(), request.description(), clock.instant()));
    }

    @PostMapping("/bikes/{bikeId}/reservation")
    public TechnicianReservationReceipt reserve(@PathVariable("bikeId") String bikeId, @RequestBody TechnicianRequest request) {
        return reserveBrokenBike.handle(new ReserveBrokenBike(bikeId, request.technicianId()));
    }

    @PostMapping("/bikes/{bikeId}/unlock")
    public UnlockResult unlock(@PathVariable("bikeId") String bikeId, @RequestBody TechnicianRequest request) {
        return unlockBrokenBike.handle(new UnlockBrokenBike(bikeId, request.technicianId()));
    }

    @PostMapping("/bikes/{bikeId}/return-to-circulation")
    public void returnToCirculation(@PathVariable("bikeId") String bikeId, @RequestBody CirculationReturnRequest request) {
        returnBikeToCirculation.handle(new ReturnBikeToCirculation(bikeId, request.technicianId(), request.stationId(),
                                                                   request.latitude(), request.longitude(), clock.instant()));
    }

    @GetMapping("/bikes/{bikeId}")
    public ResponseEntity<BrokenBikeView> bike(@PathVariable("bikeId") String bikeId) {
        return ResponseEntity.of(brokenBike.handle(new BrokenBikeQuery(bikeId)));
    }

    @GetMapping("/broken-bikes")
    public List<BrokenBikeView> brokenBikes() {
        return brokenBikes.handle(new BrokenBikesQuery());
    }

    @ExceptionHandler({NotBroken.class, UnderMaintenance.class, MaintenanceNotStarted.class, AlreadyReserved.class,
        NotYourReservation.class, ReservationRefused.class, UnlockRefused.class, CirculationReturnRefused.class,
        UnknownStation.class, OptimisticLockingException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse brokenRule(RuntimeException exception) {
        return ErrorResponse.of(exception);
    }

    @ExceptionHandler(UnknownBike.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse unknownBike(UnknownBike exception) {
        return ErrorResponse.of(exception);
    }

    @ExceptionHandler({IllegalArgumentException.class, NullPointerException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(RuntimeException exception) {
        return ErrorResponse.of(exception);
    }

    @ExceptionHandler(CollaboratorUnavailable.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse collaboratorUnavailable(CollaboratorUnavailable exception) {
        return ErrorResponse.of(exception);
    }
}
