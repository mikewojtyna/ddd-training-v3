I wrote the intent for investor withdrawals in `docs/intents/crowdsorcery-investor-withdrawal.md`. Nothing is committed.

**What it says:**
- **Why:** the vision promises investors can withdraw their money at any time, but money can only go into an account today.
- **Where:** in the Deposit context, which owns the account and its balance. Payment carries out the payout.
- **Rules:**
  - Only the available balance can be withdrawn.
  - The money is set aside as soon as a request is accepted, so two requests at once can't overdraw.
  - Payouts go only to the investor's verified bank account, and the investor can't enter a different one.
  - A failed payout puts the money back.
- **Lifecycle:** a withdrawal goes from Requested to either PaidOut or Failed, with an event at each step.
- **Acceptance scenarios** are written as Given/When/Then.
- **Your three technical items** are in a "Proposed technical shape" section rather than presented as the requirement.

**What I found in the code:**
- **A withdrawal endpoint already exists.** `WithdrawRestApi` answers `POST /accounts/{accountId}/withdrawals`, and `DepositAccount.withdraw` already refuses when the balance is too low.
- **There is no Kafka or Postgres in the project.** It runs on H2, and events only go through Spring inside the app.
- **Nothing models a bank account or its verification yet.**

**Calls I made without you:**
- **"Cap at the available balance":** a request above it is rejected and the investor is told how much is available. The amount is not quietly lowered.
- **"Available balance":** confirmed balance minus pending withdrawals and money committed to investments.
- **Response:** the endpoint returns 202 Accepted, because the payout finishes later.
- **Kafka event:** `WithdrawalRequested` carries only ids, amount and currency, never bank details.
- **Out of scope:** fees, limits, cancelling a withdrawal, and adding or verifying a bank account.

**Questions for you:**
1. Should a request above the available balance be rejected, or paid out at the available balance instead (a "withdraw all" option)?
2. Which context owns the verified bank account (Profile, a new KYC context, or Payment), and what counts as verified?
3. Should `POST /api/withdrawals` replace the existing endpoint? If so, the account has to come from the logged-in investor, and the app has no authentication yet.
4. Should this training repo actually get Kafka and Postgres, or should they stay behind ports with in-memory stand-ins?
5. Deposit doesn't know how much money is committed to investments. Who should tell it?
6. Who consumes `WithdrawalRequested`, and should `WithdrawalPaidOut` and `WithdrawalFailed` be published to Kafka too?
