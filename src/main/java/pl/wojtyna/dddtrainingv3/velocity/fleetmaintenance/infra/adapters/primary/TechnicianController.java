package pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportFault;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReportFaultUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReturnBikeToCirculation;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.api.usecases.ReturnBikeToCirculationUseCase;
import pl.wojtyna.dddtrainingv3.velocity.fleetmaintenance.domain.bike.MaintenanceNotStarted;

/// REST API for the technician app: the canvas commands *Report Fault* and *Return Bike to Circulation*.
@PrimaryAdapter
@RestController
@RequestMapping("/fleet-maintenance/bikes/{bikeId}")
public class TechnicianController {

    private final ReportFaultUseCase reportFault;
    private final ReturnBikeToCirculationUseCase returnBikeToCirculation;

    public TechnicianController(ReportFaultUseCase reportFault,
                                ReturnBikeToCirculationUseCase returnBikeToCirculation) {
        this.reportFault = reportFault;
        this.returnBikeToCirculation = returnBikeToCirculation;
    }

    @PostMapping("/fault-reports")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void reportFault(@PathVariable("bikeId") String bikeId) {
        reportFault.handle(new ReportFault(bikeId));
    }

    @PostMapping("/return-to-circulation")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void returnToCirculation(@PathVariable("bikeId") String bikeId,
                                    @RequestBody ReturnToCirculationRequest request) {
        returnBikeToCirculation.handle(new ReturnBikeToCirculation(bikeId, request.technicianId()));
    }

    @ExceptionHandler(MaintenanceNotStarted.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public String maintenanceNotStarted(MaintenanceNotStarted exception) {
        return exception.getMessage();
    }
}
