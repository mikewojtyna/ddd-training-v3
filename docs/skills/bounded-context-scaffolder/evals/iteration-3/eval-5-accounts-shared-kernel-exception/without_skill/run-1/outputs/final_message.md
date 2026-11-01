I added the Accounts bounded context to VeloCity under `pl.wojtyna.dddtrainingv3.velocity.accounts`. `./mvnw test` passes: 10 tests, 0 failures. That count includes 5 new tests and the Spring context-load test, which now starts with the new beans. Nothing is committed.

**The shared kernel exception is in place, as you asked.** Accounts' domain imports `pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId` directly. There's no wrapper, no ACL, and it doesn't go through Rentals' api. `Account` is identified by that `BorrowerId`, and the repository, the domain event and the use case all use it too. I wrote the exception down in three places so nobody "fixes" it later:
- the context's `package-info.java`
- a comment on the import in `Account.java`
- a new Accounts section in `velocity/CLAUDE.md`

The CML gets only a comment, not a new relationship (see question 1).

**What it does:**
- **Rule 1:** `Account.active(id).block()` sets the account to `BLOCKED` and returns `DomainEvents` containing `AccountEvent.AccountBlocked(BorrowerId)`.
- **Rule 2:** calling `block()` on an account that is already blocked throws `AccountAlreadyBlockedException`. Nothing changes and no event is published.
- **Inbound (Block Account):** `BlockAccountUseCase` is the `@PrimaryPort`. It loads the account, blocks it, saves it and publishes the events. The back office reaches it through a REST adapter: `POST /velocity/back-office/accounts/{borrowerId}/block` returns 204. It returns 409 if the account is already blocked and 404 if there is no such account.
- **Outbound (Account Blocked):** `api/events/AccountBlocked(String borrowerId)` is an `IntegrationEvent` meant for Rentals. A listener turns the internal domain event into this published one. It carries plain strings, the way CrowdSorcery's `DepositMade` does.
- **Wiring:** the repository is in memory. Spring beans are set up in `AccountsContextConfig`. The bean names start with `velocityAccounts` because CrowdSorcery already has a bean called `accountRepository`.
- **Tests:** `AccountTest` covers both rules as given/when/then examples. `BlockAccountUseCaseTest` covers the use case for an active account, an already-blocked account and a missing account.

**Files** (all under `src/main/java/pl/wojtyna/dddtrainingv3/velocity/accounts/` unless noted):
- `package-info.java`
- `domain/account/`: `Account`, `AccountStatus`, `AccountEvent`, `AccountRepository`, `AccountAlreadyBlockedException`, `AccountNotFoundException`
- `api/usecases/BlockAccountUseCase`, `api/events/AccountBlocked`
- `infra/adapters/primary/BackOfficeAccountBlockRestApi`, `infra/adapters/secondary/repository/InMemoryAccountRepository`, `infra/event/AccountsIntegrationEventsPublisher`, `infra/config/AccountsContextConfig`
- Tests: `src/test/java/.../velocity/accounts/domain/account/AccountTest`, `src/test/java/.../velocity/accounts/api/usecases/BlockAccountUseCaseTest`
- Edited: `src/main/java/pl/wojtyna/dddtrainingv3/velocity/CLAUDE.md`, `src/main/cml/VeloCity-Context-Map.cml` (comment only)

**Questions I would have asked (I couldn't ask, so I made these calls):**
1. **Context map:** should the CML and `velocity/velocity-context-map.md` show a Shared Kernel between Accounts and Rentals? The reference map has `Accounts [U]->[D] Rentals : AccountStatus`. I left that relationship and the reference doc unchanged, because they are the workshop's reference solution, and added only a comment in the CML.
2. **Rentals consuming Account Blocked:** should Rentals start refusing rentals for blocked borrowers now? I only publish the event. Rentals has no listener yet, and `RentBikeUseCase` is still a workshop stub.
3. **Where accounts come from:** onboarding and activation aren't in scope. For now accounts are created only through `Account.active(...)`, so in the running app every block returns 404 until onboarding exists.
4. **Block reason and unblocking:** should a block record a reason, such as unpaid debt from Settlement or a back-office decision, and who blocked it? Should there be an Unblock command? The reference map has Settlement blocking and unblocking accounts. I modelled only the back-office block, with no reason.
5. **REST shape:** the endpoint path and status codes (204, 409, 404) are my own choice. Is there a back-office API convention I should follow?
