# Accounts bounded context: implementation plan

- **System / package**: `velocity.accounts`
- **Mode**: scaffold
- **Source canvas**: `docs/bounded-contexts/velocity/accounts/canvas.md` (saved on 2026-11-01 from the user's inline request)
- **Last updated**: 2026-11-01
- **Status**: draft - awaiting review

<!-- Reviewers: edit this plan directly or answer in the conversation, then set Status to "approved" (or say so).
     Invoking the skill again resumes from this file. -->

## Review notes
- 2026-11-01: draft written, awaiting review. The inline canvas was saved to `canvas.md`. The shared-kernel request
  (D1) was challenged once in this draft; see D1 and the questions at the end.

## Canvas summary
Accounts keeps rider accounts and account blocks so that only riders in good standing can rent bikes. In this first
slice the back office blocks an account, and Accounts tells Rentals with an Account Blocked event. The canvas gives no
strategic classification and no domain roles, so `package-info.java` leaves those sections out.

## Decisions
| # | Decision | Choice | Reason |
|---|---|---|---|
| - | System | `velocity` | Accounts is one of the VeloCity contexts in the reference map. |
| - | Package | `pl.wojtyna.dddtrainingv3.velocity.accounts` | Derived from the name "Accounts". |
| - | Context name | `@BoundedContext(name = "Accounts")` | Matches the canvas and `BoundedContext Accounts` in `VeloCity-*.cml`. |
| - | Modules | 1 module = 1 context | Default. |
| - | ACLs | none needed | The canvas has no outbound query or command. The only outbound message is an event, published as an integration record in `api/events`. |
| - | Inbound adapter | use case only, no REST controller | The back office is a person or app, but the canvas doesn't describe an endpoint. Listed as a question rather than inventing one. |
| **D1** | **Boundary exception: shared kernel with Rentals' `BorrowerId`** (user insisted, 2026-11-01) | Accounts' domain uses `pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId` directly as the `Account` identity: not wrapped, not through Rentals' api. Allowed scope: **only that one class** in package `pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation` (no subpackages, no other class of that package). | **Risk:** Accounts couples to Rentals' internal domain package. Any refactoring of `BorrowerId` or of the `bikestation` package (rename, move, extra field, validation) becomes a breaking change in Accounts, and the boundary no longer protects either language. It also creates a **code-level cycle**: Accounts depends on Rentals (`BorrowerId`), and Rentals is meant to depend on Accounts (it consumes Account Blocked), which contradicts the reference map's "No cycles" decision. Enforcement: one commented exception clause in `AccountsArchitectureTest` (outgoing rule) naming exactly that class; a `DECISION D1` comment in `domain/account/package-info.java` and in the `Account` class javadoc. **Challenge given (see below).** |
| - | ArchUnit | add `com.tngtech.archunit:archunit-junit5:1.5.1` (test scope) to `pom.xml` | Not on the classpath yet; older versions can't read JDK 27 class files. |

### D1 challenge (given once, as the skill requires)
You told us up front that you know this breaks the boundary rule, so I'm planning it as you asked. Before you approve,
here are the cheaper ways to get the same effect, in order of preference:
1. **Move `BorrowerId` into `velocity.common`** (for example `velocity/common/domain/BorrowerId`). That package exists
   exactly for code shared between VeloCity's contexts. Both Rentals and Accounts would use it, the architecture
   test needs no exception, and the cycle disappears. It's a one-class move in Rentals plus import changes.
2. **Rentals publishes `BorrowerId` in its `api`** (for example `rentals/api/BorrowerId`). This makes it published
   language, and Accounts depends on it legally. The cycle (Accounts -> Rentals api, Rentals -> Accounts api) remains.
3. **Accounts owns its own identifier** (a `BorrowerId` record in Accounts, or `RiderId`) and integration events carry a
   plain string, as they must anyway.

If you still want D1 as written, approve the plan as is. If you pick 1, 2 or 3, D1 is dropped and the architecture test
stays the two plain rules.

## Mapping: canvas -> code
Base path: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/`.

### Inbound
| Collaborator | Type | Message | Code |
|---|---|---|---|
| Back office | Command | Block Account | `api/usecases/BlockAccount` (record: `String borrowerId`) + `api/usecases/BlockAccountUseCase` (`@PrimaryPort`): load `Account` by `BorrowerId`, `account.block()`, save, `DomainEventPublisher.publish(events)`. Unknown account -> `domain/account/AccountNotFound`. |

### Outbound
| Message | Type | Collaborator | Port | Adapter (ACL package) |
|---|---|---|---|---|
| Account Blocked | Event | Rentals | n/a (event). Domain event `domain/account/AccountEvent.AccountBlocked(BorrowerId borrowerId)` | `infra/adapters/secondary/IntegrationEventsPublisher` (`@SecondaryAdapter`, `@EventListener`) translates it into `api/events/AccountBlocked(String borrowerId)` (implements `IntegrationEvent`, primitives only) and publishes it via `ApplicationEventPublisher`. Rentals' listener is not part of this module (follow-up). |

### Ubiquitous language
| Term | Code type | Kind |
|---|---|---|
| Account | `domain/account/Account` | aggregate root (class; only accessor is `@Identity BorrowerId id()`) |
| borrower id | `rentals.domain.bikestation.BorrowerId` (**D1**, used as is) | value object owned by Rentals |
| Account Block | private `blocked` flag on `Account`; javadoc term | state of the aggregate, not a separate type: the canvas defines it as a plain flag with no attributes (no reason, no date) |
| Block Account | `api/usecases/BlockAccount`, `BlockAccountUseCase`, `Account#block()` | command record + use case + aggregate behaviour |
| Account Blocked | `domain/account/AccountEvent.AccountBlocked` (domain) and `api/events/AccountBlocked` (integration) | domain event in sealed `AccountEvent extends DomainEvent`; integration event record |
| "active" / "blocked" (used by the rules) | `Account.active(BorrowerId)` factory; `blocked` flag | state names. "Active" is read as "not blocked" (see questions). |

### Business rules -> aggregates
Reasoning: both rules have the same Given (the block state of one account) and the same When (block). The smallest
consistency boundary that checks both is a single `Account`, so `Account` is the aggregate.

| Rule (GWT) | Invariant | Aggregate | Test |
|---|---|---|---|
| **Block an active account**: given an active account, when the back office blocks it, then the account is blocked and Account Blocked is published | blocking an unblocked account sets the block and records `AccountBlocked` | `Account#block()` returns `DomainEvents` with `AccountEvent.AccountBlocked` | `src/test/.../velocity/accounts/domain/account/AccountTest#blockActiveAccount` (asserts the event; a second `block()` failing proves the state changed) |
| **Block an already blocked account**: given a blocked account, when the back office blocks it again, then the block is rejected | an account carries at most one block | `Account#block()` throws `domain/account/AccountAlreadyBlocked` | `AccountTest#blockAlreadyBlockedAccount` |

Consistency check: both examples agree with the definitions (Account Block = a flag; a flag is either set or not, so a
second block has nothing to add). No contradictions found. "Published" in rule 1 is checked at the aggregate level as the
returned domain event; the use case hands it to `DomainEventPublisher`, and `IntegrationEventsPublisher` turns it into the
published `AccountBlocked`.

### Rules awaiting GWT
- none

## Differences from the reference context map
| Canvas says | Reference map says | Covered by "Accepted Alternatives"? |
|---|---|---|
| Accounts pushes an **Account Blocked event** to Rentals | `Accounts [U]->[D] Rentals : AccountStatus`, Rentals asks "is account active?" (a query) | No. Direction (Accounts upstream) is the same; the integration style (event instead of query) differs. |
| Blocks come from the **back office** (Block Account command) | "Account blocks have exactly two reasons, each from one source: invalid card (Payments) and outstanding debt (Settlement)" (`Payments -> Accounts : PaymentMethodVerification`, `Settlement -> Accounts : DebtBlocking`) | No. A manual back-office block is a third source not in the map. |
| **Shared kernel** on `BorrowerId` with Rentals (D1) | No shared kernel; Accounts and Rentals are plain upstream/downstream. CML would need `Accounts [SK]<->[SK] Rentals`. | No. Also conflicts with the "No cycles" key decision, see D1. |
| Scope: blocking only | Accounts also owns onboarding, activation and unblocking | Not a conflict, the canvas is a first slice. The vision's "block notification to the rider" is also not in the canvas. |

## Files
```
docs/bounded-contexts/velocity/accounts/
  canvas.md                                   (saved from the inline request)
  plan.md                                     (this file)
pom.xml                                       (+ archunit-junit5 1.5.1, test scope)
src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/
  package-info.java                           @BoundedContext(name = "Accounts") + @Module, canvas javadoc, D1 note
  api/usecases/BlockAccount.java
  api/usecases/BlockAccountUseCase.java
  api/events/AccountBlocked.java
  domain/account/package-info.java            DECISION D1 comment
  domain/account/Account.java                 DECISION D1 comment in class javadoc
  domain/account/AccountEvent.java            sealed, with AccountBlocked
  domain/account/AccountAlreadyBlocked.java
  domain/account/AccountNotFound.java
  domain/account/AccountRepository.java       @Repository + @SecondaryPort
  infra/adapters/secondary/InMemoryAccountRepository.java
  infra/adapters/secondary/IntegrationEventsPublisher.java
  infra/config/AccountsContextConfig.java     beans: accountsAccountRepository, accountsBlockAccountUseCase, accountsIntegrationEventsPublisher
src/test/java/pl/wojtyna/dddtrainingv3/velocity/accounts/
  AccountsArchitectureTest.java               two rules + one commented D1 exception clause (outgoing rule, BorrowerId only)
  domain/account/AccountTest.java
```
No Rentals file is changed.

## Stubs and follow-ups
- No stubs: there are no outbound queries or commands, and no REST endpoint is generated.
- **Rentals doesn't consume Account Blocked yet.** A Rentals listener (`rentals/infra/adapters/primary/accountsctxacl`) that translates `accounts.api.events.AccountBlocked` is a follow-up in Rentals, outside this module.
- **If Rentals later gets a `RentalsArchitectureTest`**, its incoming rule will flag Accounts' use of `BorrowerId`; it will need the mirrored D1 exception.
- No way to create accounts yet (onboarding is out of the canvas). Accounts exist only through `Account.active(...)` in tests and the in-memory repository.
- CML (`VeloCity-Context-Map.cml`, `VeloCity-Bounded-Contexts.cml`) and `velocity/velocity-context-map.md` differ from this canvas (see above). Not changed unless you ask.

## Questions for the reviewer
1. **D1:** keep the shared kernel on Rentals' internal `BorrowerId` as asked, or take one of the alternatives (moving it to `velocity.common` is the cheapest)?
2. Does "active account" just mean "not blocked", or is there an activation state (the vision has incomplete / activated / blocked)? The plan reads it as "not blocked".
3. Should a block carry a reason (invalid card, outstanding debt, back office)? The canvas defines it as a plain flag, so the plan models it as one.
4. Do you want a REST endpoint for the back office (for example `POST /velocity/accounts/{borrowerId}/block`), or is the use case enough for now?
5. Should the context map (CML and `velocity-context-map.md`) be updated to the event-based, back-office-driven, shared-kernel version?

## Changelog
### 2026-11-01: plan drafted (no code yet)
- Added: plan for scaffolding `velocity.accounts` from the canvas.
- Triggered by: new canvas (inline request, saved as `canvas.md`).
