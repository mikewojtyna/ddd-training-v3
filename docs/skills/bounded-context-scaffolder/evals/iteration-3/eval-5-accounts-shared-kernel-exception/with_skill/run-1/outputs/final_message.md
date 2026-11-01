The Accounts context is scaffolded from the approved plan, and the shared kernel on Rentals' `BorrowerId` is in place as decision D1. `./mvnw compile`, the targeted tests (4) and the full `./mvnw test` (9 tests, Spring context starts) all pass.

- **Plan:** `docs/bounded-contexts/velocity/accounts/plan.md`. Status is now `implemented`, with your approval under "Review notes" and a "scaffolded" entry in the changelog.
- **Module:** `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/`
- **Tests:** `src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/`

**What's in it:** 1 use case (Block Account), 1 published event (`AccountBlocked`, strings only), 1 aggregate (`Account`, whose identity is Rentals' `BorrowerId`), 1 repository port with an in-memory adapter, and no ACLs, because the canvas has no outbound queries or commands. Both GWT rules are tested in `AccountTest`: blocking an active account, and rejecting a second block. Both appear in the `package-info.java` javadoc with their test names. `pom.xml` now has ArchUnit 1.5.1 (test scope).

**How D1 is recorded:**
- `AccountsArchitectureTest` has one commented exception in the outgoing rule. It names only the class `...rentals.domain.bikestation.BorrowerId`.
- I checked how narrow it is by adding a temporary use of Rentals' `BikeId`. The rule still failed, so every other Rentals internal is still blocked. The probe file is removed.
- `DECISION D1` comments are in the root `package-info.java`, in `domain/account/package-info.java`, in the `Account` javadoc, and on `AccountEvent` and `BlockAccountUseCase`, which also import `BorrowerId`.
- No Rentals file was changed.

**Open points:**
- **The D1 risk is still there.** Any change to `BorrowerId` or Rentals' `bikestation` package breaks Accounts. Once Rentals consumes Account Blocked, the two contexts depend on each other at code level. A future `RentalsArchitectureTest` will need the same D1 exception.
- **Rentals doesn't listen to `AccountBlocked` yet.** That's a follow-up in Rentals (an `accountsctxacl` listener).
- **Questions you didn't answer**, so the plan's defaults were used:
  - "active" means not blocked
  - a block is a plain flag with no reason
  - there's no REST endpoint for the back office
- There's no way to create accounts yet. Onboarding isn't in the canvas.

**Differences from the reference context map** (all listed in the plan, none covered by "Accepted Alternatives"):
- Accounts pushes an event to Rentals; the map has Rentals query account status.
- The back office is a third source of blocks; the map has only Payments and Settlement.
- The shared kernel conflicts with the map's "No cycles" decision.

`src/main/cml/VeloCity-*.cml` and `velocity/velocity-context-map.md` no longer match this canvas. I haven't changed them, since the context map may be a workshop exercise. Should I update them, for example to `Accounts [SK]<->[SK] Rentals` plus the event and the back-office source?
