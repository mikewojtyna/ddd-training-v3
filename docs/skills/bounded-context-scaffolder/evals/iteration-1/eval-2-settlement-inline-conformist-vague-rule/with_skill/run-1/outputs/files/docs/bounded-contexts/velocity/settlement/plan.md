# Settlement bounded context: implementation plan

- **System / package**: `velocity.settlement`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/settlement/canvas.md` (saved from the user's inline description, 2026-11-01)
- **Last updated**: 2026-11-01

## Canvas summary
Settlement charges the rider through their payment method once a ride is priced, so every ride eventually gets paid. Failed charges are retried and then turned into Outstanding Debt for DebtRecovery. The canvas gives no strategic classification and no domain roles (see the `docs/model-traits-worksheet.md` traits; Execution Model is the likely candidate, not applied without confirmation).

## Decisions
| Decision | Choice | Reason |
|---|---|---|
| System | `velocity` | The user asked for "a Settlement context to velocity"; Settlement is in the VeloCity CML. |
| Name | `Settlement` | Matches `BoundedContext Settlement` in `src/main/cml/VeloCity-*.cml`. |
| Canvas | Saved the inline text to `canvas.md` | The skill recommends a file; non-interactive run, so saved without asking. Unfilled sections are marked TODO. |
| Modules | 1 module = 1 context | default |
| ACL towards Payments | **no** (conformist, user insisted) | The user said explicitly to skip the ACL and use Payments' API directly. Pushback given once (see the final report). Risk: every change in Payments' charging API ripples straight into `PaymentsCharging` and its callers, and Payments' terms (payment method, pre-authorization, ...) mix into Settlement's language. The adapter lives in `infra/adapters/secondary/paymentsctx/` **without** `@AntiCorruptionLayer`; the package javadoc records the decision and risk. Matches the reference map (`Payments [U,OHS,PL]->[D,CF] Settlement`). |
| Aggregates | **none yet** | The only business rule is prose ("retry a few times shortly after the ride, then record debt"). No strict GWT rule exists, so ports and adapters are scaffolded without an aggregate, without guessing a retry count or timing. |
| Charge in the use case | First Charge Attempt only | Undisputed part of the purpose ("once a ride is priced we charge the rider"). The failure branch waits for the rule. |
| Payments types | Placeholders `ChargeRider` and `ChargeResult` in `domain/charge` | Payments doesn't exist in code, so there is no published API to conform to. Don't invent Payments' API: these mirror only the canvas message, and should be replaced by Payments' own types once they exist. |
| Pricing listener | Not created | Pricing doesn't exist in code; only our own `RidePriced` record and the use case exist. |

## Mapping: canvas → code
### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Pricing | Event | Ride Priced | `api/usecases/RidePriced` (our record) + `api/usecases/SettleRideUseCase`. Listener in `infra/adapters/primary/pricingctxacl`: follow-up (Pricing not in code). |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Charge Rider | Command | Payments | `domain/charge/PaymentsCharging` (+ `ChargeRider`, `ChargeResult`) | `infra/adapters/secondary/paymentsctx/PaymentsChargingAdapter` (no ACL, conformist; stub) |
| Debt Recorded | Event | DebtRecovery | n/a | `api/events/DebtRecorded` (published-language record). Not emitted yet: needs the Settlement aggregate's domain event and an `IntegrationEventsPublisher`. |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Settlement | none yet (`SettleRideUseCase` uses the verb) | javadoc only; planned aggregate root `Settlement`, one per priced ride |
| Charge Attempt | none yet | javadoc only; planned entity/record inside `Settlement` |
| Outstanding Debt | field `outstandingDebt` of `api/events/DebtRecorded` | javadoc + event field; planned value object |

### Business rules → aggregates
| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
| none strict yet | | | |

### Rules awaiting GWT
- **"If a charge fails we retry a few times shortly after the ride, then record debt."** Vague: "a few times" and "shortly" have no values. Suggested strict GWT (values are a suggestion, please confirm or correct):
  1. Given a Settlement for ride R-1 priced at 12.00 PLN and no Charge Attempts yet, when the first Charge Attempt fails, then a retry is scheduled for 5 minutes after the ride ended and no debt is recorded.
  2. Given a Settlement for ride R-1 priced at 12.00 PLN with 2 failed Charge Attempts, when the 3rd Charge Attempt fails, then Debt Recorded with Outstanding Debt 12.00 PLN and no further Charge Attempt is scheduled.
  3. Given a Settlement for ride R-1 priced at 12.00 PLN with 1 failed Charge Attempt, when the 2nd Charge Attempt succeeds, then the Settlement is settled and no debt is recorded.
  4. Given a Settlement for ride R-1 with Debt Recorded, when another Charge Attempt is requested, then it is rejected (Settlement no longer charges; DebtRecovery owns the debt).

  Status: awaiting confirmation. Proposed design once confirmed: aggregate `Settlement` (one per ride) holding its Charge Attempts; consistency boundary = the attempts of one ride, because every Given above only touches that state.

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Inbound **Ride Priced** event from Pricing | Rentals → Settlement **Rental Session Ended**, and Settlement *calls* Pricing for the price (`Pricing [U,OHS]->[D,CF] Settlement`) | Yes: "Pricing reacting to Rental Session Ended instead of being called by Settlement". The Rentals → Settlement relationship then disappears from Settlement's side. |
| Outbound **Debt Recorded** event to DebtRecovery | **Rental Fee Unpaid** event to DebtRecovery; DebtRecovery asks Settlement to "attempt collection" | No. Different message name; the "attempt collection" inbound command is absent from the canvas. |
| No outbound to Accounts | Settlement → Accounts: **Rental Fee Unpaid** (block), **Outstanding Debt Cleared** (unblock) | No. The canvas doesn't say who blocks a rider with debt. |
| UL: Settlement, Charge Attempt, Outstanding Debt; term "ride" | Key language: charge, settled, outstanding debt, rider's balance; term "rental session" | No. "Rider's balance" is missing; "ride" vs. "rental session" naming. |
| Charge Rider to Payments, conformist | `Payments [U,OHS,PL]->[D,CF] Settlement` (Charging) | Consistent with the map. |

## Files
```
docs/bounded-contexts/velocity/settlement/
  canvas.md
  plan.md
src/main/java/pl/wojtyna/dddtrainingv3/velocity/settlement/
  package-info.java
  api/usecases/RidePriced.java
  api/usecases/SettleRideUseCase.java
  api/events/DebtRecorded.java
  domain/charge/ChargeRider.java
  domain/charge/ChargeResult.java
  domain/charge/PaymentsCharging.java
  infra/adapters/secondary/paymentsctx/package-info.java
  infra/adapters/secondary/paymentsctx/PaymentsChargingAdapter.java
  infra/config/SettlementContextConfig.java
```
No tests: there is no strict GWT rule to test yet.

## Stubs and follow-ups
- `PaymentsChargingAdapter` is a stub (`UnsupportedOperationException("implement this")`): Payments isn't in code yet.
- `ChargeRider` / `ChargeResult` are placeholders for Payments' published types (conformist).
- Pricing listener (`infra/adapters/primary/pricingctxacl`) translating Pricing's Ride Priced event: once Pricing exists.
- `DebtRecorded` isn't emitted until the retry rule is confirmed and the `Settlement` aggregate exists.
- What does Settlement publish when a ride is paid (e.g. Ride Settled / Outstanding Debt Cleared)? Not in the canvas.
- Can a charge be partial? "Outstanding Debt = unpaid amount left" suggests it can.
- Strategic classification, domain roles, assumptions, verification metrics, open questions: empty in the canvas.
- CML (`src/main/cml/VeloCity-*.cml`) and `velocity/velocity-context-map.md` differ from the canvas (see above); not changed.

## Changelog
### 2026-11-01: scaffolded
- Added: canvas (from inline text), plan, Settlement module (use case, inbound record, Charge Rider port and conformist stub adapter, Debt Recorded published event, Spring config).
- Changed: none
- Removed (confirmed by user): none
- Proposed removals (not applied): none
- Triggered by: initial inline canvas from the user
