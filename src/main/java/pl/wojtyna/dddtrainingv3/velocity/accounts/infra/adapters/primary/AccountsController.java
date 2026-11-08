package pl.wojtyna.dddtrainingv3.velocity.accounts.infra.adapters.primary;

import org.jmolecules.architecture.hexagonal.PrimaryAdapter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.wojtyna.dddtrainingv3.common.domain.OptimisticLockingException;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.AccountStatus;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.AccountStatusQuery;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.AccountStatusQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.RentalEligibilityQuery;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.RentalEligibilityQueryHandler;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries.RentalEligibilityView;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.AddPaymentMethod;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.AddPaymentMethodUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccount;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.BlockAccountUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.ConfirmEmail;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.ConfirmEmailUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.PaymentMethodAddition;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.ProvidePersonalData;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.ProvidePersonalDataUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.RegisterRider;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.RegisterRiderUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.SignUserAgreement;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.SignUserAgreementUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.UnblockAccount;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.UnblockAccountUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.VerifyPhone;
import pl.wojtyna.dddtrainingv3.velocity.accounts.api.usecases.VerifyPhoneUseCase;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.AccountNotFound;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.EmailAlreadyRegistered;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.OnboardingStepOutOfOrder;
import pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account.RiderTooYoung;
import pl.wojtyna.dddtrainingv3.velocity.common.infra.ErrorResponse;

import java.time.LocalDate;

/// Accounts' Open Host Service: onboarding for the rider app, Rental Eligibility for Rentals, Block / Unblock for
/// Settlement. See `docs/bounded-contexts/velocity/integration-contracts.md`.
@PrimaryAdapter
@RestController
@RequestMapping("/accounts/riders")
public class AccountsController {

    private static final String OUTSTANDING_DEBT = "OUTSTANDING_DEBT";

    public record RegisterRequest(String email) {
    }

    public record Registered(String riderId) {
    }

    public record VerifyPhoneRequest(String phoneNumber) {
    }

    public record PersonalDataRequest(String fullName, LocalDate dateOfBirth, String countryOfResidency) {
    }

    public record AddPaymentMethodRequest(String cardToken) {
    }

    public record BlockRequest(String reason) {
    }

    private final RegisterRiderUseCase registerRider;
    private final ConfirmEmailUseCase confirmEmail;
    private final VerifyPhoneUseCase verifyPhone;
    private final ProvidePersonalDataUseCase providePersonalData;
    private final AddPaymentMethodUseCase addPaymentMethod;
    private final SignUserAgreementUseCase signUserAgreement;
    private final BlockAccountUseCase blockAccount;
    private final UnblockAccountUseCase unblockAccount;
    private final AccountStatusQueryHandler accountStatus;
    private final RentalEligibilityQueryHandler rentalEligibility;

    public AccountsController(RegisterRiderUseCase registerRider, ConfirmEmailUseCase confirmEmail,
                              VerifyPhoneUseCase verifyPhone, ProvidePersonalDataUseCase providePersonalData,
                              AddPaymentMethodUseCase addPaymentMethod, SignUserAgreementUseCase signUserAgreement,
                              BlockAccountUseCase blockAccount, UnblockAccountUseCase unblockAccount,
                              AccountStatusQueryHandler accountStatus, RentalEligibilityQueryHandler rentalEligibility) {
        this.registerRider = registerRider;
        this.confirmEmail = confirmEmail;
        this.verifyPhone = verifyPhone;
        this.providePersonalData = providePersonalData;
        this.addPaymentMethod = addPaymentMethod;
        this.signUserAgreement = signUserAgreement;
        this.blockAccount = blockAccount;
        this.unblockAccount = unblockAccount;
        this.accountStatus = accountStatus;
        this.rentalEligibility = rentalEligibility;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Registered register(@RequestBody RegisterRequest request) {
        return new Registered(registerRider.handle(new RegisterRider(request.email())));
    }

    @PostMapping("/{riderId}/email-confirmation")
    public void confirmEmail(@PathVariable("riderId") String riderId) {
        confirmEmail.handle(new ConfirmEmail(riderId));
    }

    @PostMapping("/{riderId}/phone-verification")
    public void verifyPhone(@PathVariable("riderId") String riderId, @RequestBody VerifyPhoneRequest request) {
        verifyPhone.handle(new VerifyPhone(riderId, request.phoneNumber()));
    }

    @PostMapping("/{riderId}/personal-data")
    public void providePersonalData(@PathVariable("riderId") String riderId, @RequestBody PersonalDataRequest request) {
        providePersonalData.handle(new ProvidePersonalData(riderId, request.fullName(), request.dateOfBirth(),
                                                           request.countryOfResidency()));
    }

    @PostMapping("/{riderId}/payment-methods")
    public PaymentMethodAddition addPaymentMethod(@PathVariable("riderId") String riderId,
                                                  @RequestBody AddPaymentMethodRequest request) {
        return addPaymentMethod.handle(new AddPaymentMethod(riderId, request.cardToken()));
    }

    @PostMapping("/{riderId}/agreement-signature")
    public void signUserAgreement(@PathVariable("riderId") String riderId) {
        signUserAgreement.handle(new SignUserAgreement(riderId));
    }

    @GetMapping("/{riderId}")
    public ResponseEntity<AccountStatus> accountStatus(@PathVariable("riderId") String riderId) {
        return ResponseEntity.of(accountStatus.handle(new AccountStatusQuery(riderId)));
    }

    @GetMapping("/{riderId}/rental-eligibility")
    public RentalEligibilityView rentalEligibility(@PathVariable("riderId") String riderId) {
        return rentalEligibility.handle(new RentalEligibilityQuery(riderId));
    }

    /// Settlement's *Block Account*. Only `OUTSTANDING_DEBT` is accepted over REST: the *invalid card* block comes
    /// from Payments' event.
    @PostMapping("/{riderId}/blocks")
    public void block(@PathVariable("riderId") String riderId, @RequestBody BlockRequest request) {
        if (!OUTSTANDING_DEBT.equals(request.reason())) {
            throw new IllegalArgumentException("Only " + OUTSTANDING_DEBT + " can be placed over REST, got " + request.reason());
        }
        blockAccount.handle(new BlockAccount(riderId, OUTSTANDING_DEBT));
    }

    /// Settlement's *Unblock Account*. The *invalid card* block is lifted only by a newly verified card (DE-6).
    @DeleteMapping("/{riderId}/blocks/" + OUTSTANDING_DEBT)
    public void unblock(@PathVariable("riderId") String riderId) {
        unblockAccount.handle(new UnblockAccount(riderId, OUTSTANDING_DEBT));
    }

    @ExceptionHandler({OnboardingStepOutOfOrder.class, RiderTooYoung.class, EmailAlreadyRegistered.class,
        OptimisticLockingException.class})
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse brokenRule(RuntimeException exception) {
        return ErrorResponse.of(exception);
    }

    @ExceptionHandler(AccountNotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse notFound(AccountNotFound exception) {
        return ErrorResponse.of(exception);
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse badRequest(RuntimeException exception) {
        return ErrorResponse.of(exception);
    }
}
