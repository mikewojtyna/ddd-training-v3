package pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.ErrorResponse;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.queries.*;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.api.usecases.*;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.domain.incident.*;
import pl.wojtyna.dddtrainingv3.velocity.customersupport.infra.adapters.primary.IncidentRequests.*;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;

/// REST resources of Customer Support's Open Host Service (integration-contracts.md, "Customer Support").
@PrimaryAdapter
@RestController
@RequestMapping("/customer-support/incidents")
public class IncidentController {

    private final ReportIncidentUseCase reportIncident;
    private final PickUpIncidentUseCase pickUpIncident;
    private final AcceptIncidentUseCase acceptIncident;
    private final RejectIncidentUseCase rejectIncident;
    private final EscalateIncidentUseCase escalateIncident;
    private final ResolveIncidentUseCase resolveIncident;
    private final ReopenIncidentUseCase reopenIncident;
    private final ResolveFleetIssueUseCase resolveFleetIssue;
    private final IncidentQueryHandler incidentQuery;
    private final OpenIncidentsQueryHandler openIncidentsQuery;
    private final RiderIncidentsQueryHandler riderIncidentsQuery;
    private final Clock clock;

    public IncidentController(ReportIncidentUseCase reportIncident,
                              PickUpIncidentUseCase pickUpIncident,
                              AcceptIncidentUseCase acceptIncident,
                              RejectIncidentUseCase rejectIncident,
                              EscalateIncidentUseCase escalateIncident,
                              ResolveIncidentUseCase resolveIncident,
                              ReopenIncidentUseCase reopenIncident,
                              ResolveFleetIssueUseCase resolveFleetIssue,
                              IncidentQueryHandler incidentQuery,
                              OpenIncidentsQueryHandler openIncidentsQuery,
                              RiderIncidentsQueryHandler riderIncidentsQuery,
                              Clock clock) {
        this.reportIncident = reportIncident;
        this.pickUpIncident = pickUpIncident;
        this.acceptIncident = acceptIncident;
        this.rejectIncident = rejectIncident;
        this.escalateIncident = escalateIncident;
        this.resolveIncident = resolveIncident;
        this.reopenIncident = reopenIncident;
        this.resolveFleetIssue = resolveFleetIssue;
        this.incidentQuery = incidentQuery;
        this.openIncidentsQuery = openIncidentsQuery;
        this.riderIncidentsQuery = riderIncidentsQuery;
        this.clock = clock;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IncidentReportReceipt report(@RequestBody ReportIncidentRequest request) {
        if (Arrays.stream(Category.values()).noneMatch(category -> category.name().equals(request.category()))) {
            throw new BadRequest("UnknownCategory", "Category must be one of " + Arrays.toString(Category.values()));
        }
        return reportIncident.handle(new ReportIncident(request.riderId(), request.category(), request.description(),
                                                        request.bikeId(), clock.instant()));
    }

    @PostMapping("/{incidentId}/assignment")
    public void pickUp(@PathVariable("incidentId") String incidentId, @RequestBody AgentRequest request) {
        pickUpIncident.handle(new PickUpIncident(incidentId, request.agentId(), clock.instant()));
    }

    @PostMapping("/{incidentId}/acceptance")
    public void accept(@PathVariable("incidentId") String incidentId, @RequestBody AgentRequest request) {
        acceptIncident.handle(new AcceptIncident(incidentId, request.agentId(), clock.instant()));
    }

    @PostMapping("/{incidentId}/rejection")
    public void reject(@PathVariable("incidentId") String incidentId, @RequestBody RejectionRequest request) {
        rejectIncident.handle(new RejectIncident(incidentId, request.agentId(), request.reason(), clock.instant()));
    }

    @PostMapping("/{incidentId}/escalation")
    public void escalate(@PathVariable("incidentId") String incidentId, @RequestBody EscalationRequest request) {
        if (Arrays.stream(Team.values()).noneMatch(team -> team.name().equals(request.team()))) {
            throw new BadRequest("UnknownTeam", "An incident is escalated only to " + Arrays.toString(Team.values()));
        }
        escalateIncident.handle(new EscalateIncident(incidentId, request.agentId(), request.team(), clock.instant()));
    }

    @PostMapping("/{incidentId}/resolution")
    public void resolve(@PathVariable("incidentId") String incidentId, @RequestBody ResolutionRequest request) {
        resolveIncident.handle(new ResolveIncident(incidentId, request.agentId(), request.resolution(), clock.instant()));
    }

    @PostMapping("/{incidentId}/reopening")
    public void reopen(@PathVariable("incidentId") String incidentId, @RequestBody ReopeningRequest request) {
        reopenIncident.handle(new ReopenIncident(incidentId, request.riderId(), clock.instant()));
    }

    @PostMapping("/{incidentId}/fleet-resolution")
    public void resolveFleetIssue(@PathVariable("incidentId") String incidentId, @RequestBody FleetResolutionRequest request) {
        resolveFleetIssue.handle(new ResolveFleetIssue(incidentId, request.bikeId(), clock.instant()));
    }

    @GetMapping("/{incidentId}")
    public ResponseEntity<IncidentView> incident(@PathVariable("incidentId") String incidentId,
                                                 @RequestParam(name = "riderId", required = false) String riderId) {
        return ResponseEntity.of(incidentQuery.handle(new IncidentQuery(incidentId, riderId)));
    }

    @GetMapping(params = "status")
    public List<OpenIncidentView> openIncidents(@RequestParam("status") String status) {
        if (!"OPEN".equals(status)) {
            throw new BadRequest("UnsupportedStatusFilter", "Only status=OPEN is supported");
        }
        return openIncidentsQuery.handle(new OpenIncidentsQuery());
    }

    @GetMapping(params = "riderId")
    public List<IncidentView> ridersIncidents(@RequestParam("riderId") String riderId) {
        return riderIncidentsQuery.handle(new RiderIncidentsQuery(riderId));
    }

    @ExceptionHandler(IncidentNotFound.class)
    public ResponseEntity<ErrorResponse> notFound(IncidentNotFound exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.of(exception));
    }

    @ExceptionHandler({IncidentAlreadyAssigned.class, IncidentAlreadyResolved.class, NotAssignedAgent.class,
                       IncidentAlreadyDecided.class, ReopenedIncidentCannotBeRejected.class, IncidentNotAccepted.class,
                       NotReportingRider.class, IncidentNotRejected.class, ReopeningWindowClosed.class,
                       IncidentNotResolvable.class, NotAFleetIssueOfBike.class, OptimisticLockingException.class})
    public ResponseEntity<ErrorResponse> ruleBroken(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(ErrorResponse.of(exception));
    }

    @ExceptionHandler(BadRequest.class)
    public ResponseEntity<ErrorResponse> badRequest(BadRequest exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.error, exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> invalidInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse("InvalidInput", exception.getMessage()));
    }

    /// Malformed input rejected by this adapter before it reaches a use case.
    static class BadRequest extends RuntimeException {

        private final String error;

        BadRequest(String error, String message) {
            super(message);
            this.error = error;
        }
    }
}
