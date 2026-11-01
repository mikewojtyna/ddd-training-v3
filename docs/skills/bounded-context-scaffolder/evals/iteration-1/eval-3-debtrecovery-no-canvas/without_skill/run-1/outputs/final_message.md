I've scaffolded the DebtRecovery context in `velocity/debtrecovery`, but it doesn't block or unblock accounts the way you described. `./mvnw test` passes: 14 tests, including 9 new ones and the Spring context load.

**Why it doesn't block accounts.** In the reference map (`velocity-context-map.md` and the CML), blocking belongs to Accounts and is driven by Settlement:
- Settlement's *Rental Fee Unpaid* blocks the account.
- Settlement's *Outstanding Debt Cleared* unblocks it.

DebtRecovery's job there is to chase the money: reminders, payment plans, pausing and resuming. If DebtRecovery also blocked accounts, two contexts would decide whether a rider can use the service, which the discovery session ruled out. So DebtRecovery listens to the same two Settlement events and only uses them to start and end its recovery process. Accounts isn't in the code yet, so nothing blocks accounts right now.

**What's in the context:**
- **`package-info.java`:** declares the context as `@BoundedContext(name = "DebtRecovery")`, matching the CML.
- **The `RecoveryProcess` aggregate** (in `domain/recoveryprocess`): tracks one rider's recovery, which is active, paused, succeeded or exhausted. A rider has at most one open process. Its commands are `start`, `attemptCollection`, `pause().because(reason)`, `resume`, `debtCleared` and `exhaust`. Pause reasons are dispute under review, payment plan negotiation and regulatory hold.
- **Rules:** a paused process doesn't try to collect, a process can't be paused twice or resumed unless it's paused, and a finished process rejects every command. Each rule has its own exception, and the events are a sealed `RecoveryProcessEvent` hierarchy.
- **The amount owed isn't stored here.** Settlement owns the debt and the rider's balance, so the process only knows the rider.
- **Use cases:** `StartRecoveryUseCase` (does nothing if the rider already has an open process), `AttemptCollectionUseCase`, `PauseRecoveryUseCase`, `ResumeRecoveryUseCase` and `CloseRecoveryOnDebtClearedUseCase`.
- **Settlement link** (`infra/adapters/secondary/settlementctxacl`, marked `@AntiCorruptionLayer`):
  - Settlement doesn't exist yet, so its two events are placeholder records in `SettlementPublishedLanguage`. Replace them once Settlement is built.
  - `SettlementEventsTranslator` turns those events into the start and close use cases.
  - `SettlementDebtCollector`, which asks Settlement to collect, is a stub that throws `UnsupportedOperationException`.
- **Other adapters:** an in-memory repository, a REST endpoint for pause and resume (`/velocity/recovery-processes/{id}/pause|resume`), and the Spring wiring in `DebtRecoveryContextConfig`.
- **Tests:** `RecoveryProcessTest` has 9 given/when/then examples in the same style as `BikeStationTest`.
- **Docs:** I updated the VeloCity `CLAUDE.md`, which said only `rentals` existed, and added a short DebtRecovery section. The CML needed no changes.

**Not built yet:** reminders, payment plans, and the rules for the recovery schedule and for when recovery is exhausted. The vision says these rules exist but doesn't spell them out.

**Questions:**
1. Do you want DebtRecovery itself to block and unblock accounts? The map's "Accepted Alternatives" allows *Debt Recovery → Accounts* for unblocking. Even then, Accounts would still apply the block; DebtRecovery would only report the fact. If you want that, I'll add the event and update the CML and `velocity-context-map.md` to match.
2. Is one recovery process per rider right? If the rider runs up new unpaid fees, I add them to the open process instead of starting a new one.
3. Should a paused recovery end successfully if the debt is cleared while it's paused? I assumed yes.
4. Should the rider stay blocked after recovery is exhausted? That decision belongs to Accounts and nothing models it yet.
