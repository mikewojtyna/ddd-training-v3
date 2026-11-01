I've written the plan for the Accounts context and stopped there. No code yet: the plan needs your review first.

**Files**
- Canvas, saved from your inline request: `docs/bounded-contexts/velocity/accounts/canvas.md`
- Plan: `docs/bounded-contexts/velocity/accounts/plan.md` (Status: `draft - awaiting review`)

**What the plan builds** (package `velocity.accounts`, `@BoundedContext(name = "Accounts")`, which matches the CML)
- 1 use case: `BlockAccountUseCase` with the `BlockAccount` command
- 0 queries
- 1 published event: `api/events/AccountBlocked` (string fields only), fed by the domain event `AccountEvent.AccountBlocked`
- 1 port: `AccountRepository`, with an in-memory adapter
- 0 ACLs, because there are no outbound queries or commands
- 1 aggregate: `Account`, identified by `BorrowerId`, with the block flag inside it
- 3 tests: `AccountTest#blockActiveAccount`, `AccountTest#blockAlreadyBlockedAccount`, and `AccountsArchitectureTest`
- ArchUnit 1.5.1 gets added to `pom.xml` (test scope)

Both of your rules are strict Given/When/Then examples and agree with each other, so both get modelled. None are waiting on a GWT example.

**Decisions and pushback**
- **D1, the shared kernel on Rentals' `BorrowerId`.** I've planned it the way you asked. `Account` uses `rentals.domain.bikestation.BorrowerId` directly. The architecture test will carry one commented exception that allows only that class, and there will be `DECISION D1` comments in `domain/account/package-info.java` and on `Account`. My one challenge: this ties Accounts to Rentals' internal domain package, so any refactoring there breaks Accounts. It also makes Accounts and Rentals depend on each other: Accounts uses Rentals' `BorrowerId`, and Rentals will consume Accounts' event. The reference map rules that out ("No cycles"). The cheapest fix is to move `BorrowerId` into `velocity.common`, the package meant for code shared between VeloCity's contexts. Then there's no exception and no cycle. Two other options: Rentals publishes `BorrowerId` in its `api`, or Accounts gets its own id. If you still want D1 as written, just approve.
- No REST endpoint, because the canvas doesn't describe one. Only the use case gets built.

**Questions**
1. D1: keep it as written, or move `BorrowerId` to `velocity.common` (or one of the other options)?
2. Does "active" just mean "not blocked"? The vision also has incomplete and activated states. The plan treats it as "not blocked".
3. Should a block record its reason (invalid card, debt, back office), or stay a plain flag as the canvas says?
4. Do you want a REST endpoint for the back office?

**Differences from the reference context map**
None of these is covered by its "Accepted Alternatives":
- In the map, Rentals asks Accounts "is this account active?". Your canvas has Accounts send an Account Blocked event instead.
- In the map, blocks come only from Payments (invalid card) and Settlement (debt). The back office would be a third source.
- The map has no shared kernel between Accounts and Rentals.

I haven't touched the CML or `velocity-context-map.md`. Say if you want them updated to match.

**Follow-ups that stay outside this module**
- Rentals doesn't listen for Account Blocked yet.
- If Rentals ever gets its own architecture test, it will need the matching D1 exception.

To continue, reply here or edit `plan.md` directly (you can set `Status: approved`). Running the skill again picks up from the saved plan.
