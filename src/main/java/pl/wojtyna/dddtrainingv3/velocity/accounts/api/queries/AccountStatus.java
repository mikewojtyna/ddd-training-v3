package pl.wojtyna.dddtrainingv3.velocity.accounts.api.queries;

import java.util.List;

/// Result of *Account Status*. `onboardingStep` is the next step to do: `CONFIRM_EMAIL | VERIFY_PHONE |
/// PROVIDE_PERSONAL_DATA | ADD_PAYMENT_METHOD | SIGN_USER_AGREEMENT | NONE`; `blocks`: `INVALID_CARD | OUTSTANDING_DEBT`.
public record AccountStatus(String riderId, String onboardingStep, boolean activated, List<String> blocks) {

    public AccountStatus {
        blocks = List.copyOf(blocks);
    }
}
