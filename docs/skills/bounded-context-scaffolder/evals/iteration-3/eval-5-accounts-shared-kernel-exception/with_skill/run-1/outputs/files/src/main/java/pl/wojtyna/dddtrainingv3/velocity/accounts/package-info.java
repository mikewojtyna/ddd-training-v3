/// # Accounts bounded context
///
/// ## Purpose
/// Keeps rider accounts and account blocks so only riders in good standing can rent bikes.
///
/// ## Ubiquitous language
/// - **Account**: a rider's VeloCity account, identified by the rider's borrower id.
/// - **Account Block**: a flag on an account that prevents renting.
///
/// ## Business decisions
/// - **Block an active account**: given an active account, when the back office blocks it, then the account is
///   blocked and Account Blocked is published. Enforced by `Account`, see `AccountTest#blockActiveAccount`.
/// - **Block an already blocked account**: given a blocked account, when the back office blocks it again, then the
///   block is rejected. Enforced by `Account`, see `AccountTest#blockAlreadyBlockedAccount`.
///
/// ## Boundary exception
/// DECISION D1 (2026-11-01): Accounts uses Rentals' internal
/// `pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId` as a shared kernel (the `Account`
/// identity), instead of a type from Rentals' api. The user asked for it explicitly and kept it after the challenge.
/// Deliberate, and excluded (that one class only) from `AccountsArchitectureTest`. Risk: any refactoring of
/// `BorrowerId` or of Rentals' `bikestation` package breaks Accounts, and it creates a code-level cycle once Rentals
/// consumes Account Blocked. See the plan.
///
/// Source canvas: `docs/bounded-contexts/velocity/accounts/canvas.md`
@BoundedContext(name = "Accounts")
@Module
package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
