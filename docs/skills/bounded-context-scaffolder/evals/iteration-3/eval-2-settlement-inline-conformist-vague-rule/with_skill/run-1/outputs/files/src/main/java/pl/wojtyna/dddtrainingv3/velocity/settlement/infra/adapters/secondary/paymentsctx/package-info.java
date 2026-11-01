/// Conformist adapter towards Payments, **without an anti-corruption layer**.
///
/// DECISION D1 (2026-11-01): the user asked for a conformist relationship to Payments without an ACL ("we're conformist
/// to Payments, skip the ACL and use their API directly"). The skill pushed back once and recommended a thin ACL; the
/// user approved the plan with the no-ACL variant. The reference map agrees: `Payments [U,OHS,PL] -> [D,CF] Settlement`.
///
/// Risk, accepted consciously: every change to Payments' published language (renamed fields, new failure codes, a
/// changed charge result) ripples straight into Settlement's domain model and tests, and Payments' words mix into
/// Settlement's ubiquitous language. Only Payments' `api` types may be used here, never its internals.
package pl.wojtyna.dddtrainingv3.velocity.settlement.infra.adapters.secondary.paymentsctx;
