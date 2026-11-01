# Intent: Investors withdraw money to their verified bank account

- **System:** CrowdSorcery
- **Bounded context:** Deposit (owns the investor's account and balance), with Payment carrying out the payout
- **Status:** Draft. Open questions are at the end.

## Why

The vision promises that an investor "can withdraw his money at any time" (`crowdsorcery/CrowdSorcery.md`, *Investing*). Today money can only go into an account. Until it can come out, investors can't trust the platform with their funds. When the investor asks for money back, it has to arrive quickly, go only to an account that belongs to the investor, and never exceed what the investor actually owns.

## Outcome

An investor asks to withdraw an amount from their CrowdSorcery account. The money leaves the account and is paid out to the investor's verified bank account. The rest of the platform learns that a withdrawal was requested, so it can react. For example, Investment stops counting that money as available for automated strategies.

## Business rules

1. **Only the available balance can be withdrawn.** Available balance is the confirmed balance minus money already reserved: other pending withdrawals and money committed to investments. Pending, unconfirmed deposits are not available.
2. **Requests above the available balance are rejected, not reduced.** The investor is told how much is available and can submit a new request. The money is never silently lowered to a different amount. *(Decision taken in place of the user. See Q1.)*
3. **Payouts go only to the investor's verified bank account.** If the investor has no verified bank account, the request is rejected. The investor can't type in a destination account when withdrawing.
4. **The amount must be positive and in the account's currency.** No currency conversion during a withdrawal.
5. **Money is reserved when the request is accepted, not when the payout settles.** Two requests made at the same time can't together exceed the available balance.
6. **A failed payout returns the money.** If Payment can't complete the transfer, the reserved amount goes back to the available balance and the withdrawal is marked as failed.
7. **A withdrawal is recorded for good.** Every request, whether accepted, paid out or failed, stays on the account's history for audit.

## Withdrawal lifecycle

`Requested` -> `PaidOut` or `Failed`

| Step | What happens | Event |
|------|--------------|-------|
| Investor requests withdrawal | Rules 1-5 checked, amount reserved | `WithdrawalRequested` |
| Payment completes the transfer | Reserved amount leaves the account | `WithdrawalPaidOut` |
| Payment fails | Reservation released | `WithdrawalFailed` |

## Acceptance scenarios

- **Given** an available balance of 500 USD and a verified bank account, **when** the investor withdraws 200 USD, **then** the withdrawal is requested, 200 USD is reserved, the available balance is 300 USD, and `WithdrawalRequested` is published.
- **Given** an available balance of 500 USD, **when** the investor withdraws 600 USD, **then** the request is rejected with "insufficient available balance (500 USD)" and nothing changes.
- **Given** an available balance of 500 USD with 400 USD already in a pending withdrawal, **when** the investor withdraws 200 USD, **then** the request is rejected.
- **Given** no verified bank account, **when** the investor withdraws any amount, **then** the request is rejected and no money is reserved.
- **Given** a requested withdrawal of 200 USD, **when** Payment reports the payout failed, **then** 200 USD is available again and `WithdrawalFailed` is published.
- **Given** a requested withdrawal, **when** Payment confirms the payout, **then** the withdrawal is `PaidOut` and `WithdrawalPaidOut` is published.
- **Given** an amount of 0 or less, or a currency other than the account's, **when** the investor withdraws, **then** the request is rejected.

## Proposed technical shape

The request named three technical deliverables. Here is how they fit the repo's conventions (root `CLAUDE.md`, `crowdsorcery/CLAUDE.md`) and the code that already exists.

- **`POST /api/withdrawals`:** a primary adapter in `deposit/infra/adapters/primary`.
  - A withdrawal endpoint already exists: `WithdrawRestApi` at `POST /accounts/{accountId}/withdrawals`, backed by `WithdrawUseCase` and `DepositAccount.withdraw`.
  - Either change the path to `/api/withdrawals` and take the account from the authenticated investor, or keep the existing path. See Q3.
  - The body carries only the amount and currency, never a bank account (rule 3).
  - Response: `202 Accepted` with the withdrawal id, because the payout is asynchronous. Rejections: `422` for business-rule failures, `400` for malformed input.
- **Domain:** extend the `DepositAccount` aggregate.
  - `DepositAccount.withdraw` already throws when `balance < amount`. That is the starting point for rule 1.
  - It needs a reservation (pending withdrawals), plus `markPaidOut` and `markFailed` transitions. Each returns `DomainEvents`.
  - The use case publishes them through `DomainEventPublisher`.
  - The internal `WithdrawalMade` domain event becomes, or is joined by, `WithdrawalRequested`, `WithdrawalPaidOut` and `WithdrawalFailed`.
- **Verified bank account:** nothing in the codebase models one yet.
  - Deposit should ask for it through a secondary port, for example `VerifiedBankAccounts`, implemented by an ACL to whichever context owns verification. That is probably Profile or a new KYC context. See Q2.
  - The payout itself goes through the existing `paymentctxacl` ACL to `PaymentGateway`. The `wire` provider is the natural fit for bank transfers.
- **`WithdrawalRequested` Kafka event:** a published integration event in `deposit/api/events`.
  - Model it like `DepositMade`: an `IntegrationEvent` carrying only primitives: `withdrawalId`, `accountId`, `amount`, `currency`, `requestedAt`. No bank account details on the topic.
  - Kafka is not in the project yet: there is no dependency and no config, and events today go through Spring's `ApplicationEventPublisher`. Bridge the integration event to Kafka in a secondary adapter, ideally through an outbox, so the event and the database change can't diverge. See Q4.
- **`withdrawals` table in Postgres:** a secondary adapter (repository) in `deposit/infra/adapters/secondary`.
  - Suggested columns: `id`, `account_id`, `amount`, `currency`, `status`, `requested_at`, `completed_at`, `failure_reason`, `payment_reference`, `version` (optimistic locking for rule 5).
  - The project runs on H2 and Deposit uses `InMemoryAccountRepository`. Postgres is a new runtime dependency. See Q4.

## Out of scope

- Withdrawing money that is invested in assets or projects. Exiting an investment is a separate capability.
- Adding or verifying a bank account. This intent only uses an account that is already verified.
- Fees, limits per day, cooling-off periods, fraud checks and currency conversion.
- Cancelling a withdrawal after it is requested.

## Open questions

1. **What does "cap at the available balance" mean: reject or clamp?** This draft rejects requests above the available balance (rule 2), because silently paying out a different amount surprises the investor. Should it instead pay out the available balance, or offer "withdraw all"?
2. **Who owns "verified bank account"?** Profile (today only first and last name), a new KYC/Onboarding context, or Payment? What does "verified" mean (micro-deposit, open banking, manual review)?
3. **Endpoint path.** Should `POST /api/withdrawals` replace the existing `POST /accounts/{accountId}/withdrawals`? If it does, the account comes from the authenticated investor, and there is no authentication in the app yet.
4. **Infrastructure.** Are Kafka and Postgres real targets for this training repo? Today it is H2 plus in-process Spring events. Or should they stay as adapters behind ports, with in-memory stand-ins?
5. **Available balance vs. committed money.** Deposit doesn't track money committed to investments today. Who tells Deposit what is reserved: Investment, through a transfer, or Deposit's own bookkeeping?
6. **Who consumes `WithdrawalRequested`?** Investment only (to stop automated strategies spending the money), or also notifications and reporting? Do they also need `WithdrawalPaidOut` and `WithdrawalFailed` on the topic?
