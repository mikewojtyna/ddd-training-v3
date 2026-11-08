package pl.wojtyna.dddtrainingv3.velocity.pricing.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.ErrorResponse;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.PriceForSession;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.PriceForSessionQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.SessionPrice;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.TariffView;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.TariffsQuery;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.queries.TariffsQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishTariff;
import pl.wojtyna.dddtrainingv3.velocity.pricing.api.usecases.PublishTariffUseCase;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.DuplicateTariffIdException;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.DuplicateValidFromException;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.IncompleteTariffException;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.NegativeFeeException;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.NoTariffValidException;
import pl.wojtyna.dddtrainingv3.velocity.pricing.domain.tariff.ValidFromNotInFutureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;

/// Pricing's Open Host Service: *Price for Session* (Settlement), *Publish Tariff* and *Tariffs* (pricing manager).
///
/// Rejections: `409` with the broken rule's name (`ValidFromNotInFuture`, `DuplicateValidFrom`, `DuplicateTariffId`,
/// `IncompleteTariff`, `NegativeFee`, `NoTariffValid`), `400` for malformed input.
@PrimaryAdapter
@RestController
@RequestMapping("/pricing")
public class PricingController {

    private final PriceForSessionQueryHandler priceForSession;
    private final PublishTariffUseCase publishTariff;
    private final TariffsQueryHandler tariffs;

    public PricingController(PriceForSessionQueryHandler priceForSession,
                             PublishTariffUseCase publishTariff,
                             TariffsQueryHandler tariffs) {
        this.priceForSession = priceForSession;
        this.publishTariff = publishTariff;
        this.tariffs = tariffs;
    }

    @GetMapping("/session-price")
    public SessionPrice sessionPrice(@RequestParam("startedAt") String startedAt,
                                     @RequestParam("endedAt") String endedAt,
                                     @RequestParam("ending") String ending,
                                     @RequestParam(name = "latitude", required = false) Double latitude,
                                     @RequestParam(name = "longitude", required = false) Double longitude) {
        return priceForSession.handle(new PriceForSession(Instant.parse(startedAt), Instant.parse(endedAt), ending,
                                                          latitude, longitude));
    }

    @PostMapping("/tariffs")
    @ResponseStatus(HttpStatus.CREATED)
    public void publishTariff(@RequestBody PublishTariffRequest request) {
        publishTariff.handle(new PublishTariff(request.tariffId(),
                                               request.validFrom() == null ? null : Instant.parse(request.validFrom()),
                                               request.freeMinutes(),
                                               decimal(request.firstHourFee()),
                                               decimal(request.secondHourFee()),
                                               decimal(request.thirdHourFee()),
                                               decimal(request.furtherHourFee()),
                                               decimal(request.dropZoneFee()),
                                               decimal(request.unauthorizedZoneFee()),
                                               decimal(request.penalty()),
                                               request.publishedBy()));
    }

    @GetMapping("/tariffs")
    public List<TariffView> tariffs() {
        return tariffs.handle(new TariffsQuery());
    }

    private static BigDecimal decimal(String amount) {
        return amount == null ? null : new BigDecimal(amount);
    }

    @ExceptionHandler({ValidFromNotInFutureException.class, DuplicateValidFromException.class,
                       DuplicateTariffIdException.class, IncompleteTariffException.class, NegativeFeeException.class,
                       NoTariffValidException.class, OptimisticLockingException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse ruleBroken(RuntimeException exception) {
        return rule(exception);
    }

    @ExceptionHandler({IllegalArgumentException.class, DateTimeParseException.class, ArithmeticException.class,
                       HttpMessageNotReadableException.class, MissingServletRequestParameterException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse malformed(Exception exception) {
        return new ErrorResponse("MalformedRequest", exception.getMessage());
    }

    private static ErrorResponse rule(RuntimeException exception) {
        var name = exception.getClass().getSimpleName();
        return new ErrorResponse(name.endsWith("Exception") ? name.substring(0, name.length() - "Exception".length())
                                                            : name,
                                 exception.getMessage());
    }
}
