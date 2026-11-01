/// The `Account` aggregate: a rider's account and its block.
///
/// DECISION D1 (2026-11-01): this package uses Rentals' internal
/// `pl.wojtyna.dddtrainingv3.velocity.rentals.domain.bikestation.BorrowerId` directly as the account identity
/// (shared kernel), not a type from Rentals' api. Deliberate, user insisted after challenge, and excluded from
/// `AccountsArchitectureTest` for that one class only. Risk: Rentals' internal refactorings break Accounts. See the plan.
package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;
