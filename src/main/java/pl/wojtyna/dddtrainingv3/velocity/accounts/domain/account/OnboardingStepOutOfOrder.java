package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

/// Onboarding steps are completed in order: a step is accepted only when it is the account's next onboarding step.
public class OnboardingStepOutOfOrder extends RuntimeException {

    public OnboardingStepOutOfOrder(String message) {
        super(message);
    }
}
