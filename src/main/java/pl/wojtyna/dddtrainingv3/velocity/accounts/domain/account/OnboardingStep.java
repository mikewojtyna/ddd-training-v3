package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

/// The next onboarding step an account has to complete, in the fixed onboarding order; `NONE` once activated.
/// Registering creates the account, and "payment method added" and "verified" are one step for the rider.
public enum OnboardingStep {
    CONFIRM_EMAIL, VERIFY_PHONE, PROVIDE_PERSONAL_DATA, ADD_PAYMENT_METHOD, SIGN_USER_AGREEMENT, NONE
}
