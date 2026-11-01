/**
 * Accounts bounded context. Keeps rider accounts and account blocks, so only riders in good standing can rent bikes.
 * <p>
 * Ubiquitous language:
 * <ul>
 *     <li><b>Account</b>: a rider's VeloCity account, identified by the rider's borrower id.</li>
 *     <li><b>Account Block</b>: a flag on an account that prevents renting.</li>
 * </ul>
 * <p>
 * Shared kernel: Rentals' {@code BorrowerId} ({@code velocity.rentals.domain.bikestation.BorrowerId}) is used directly
 * in this context's domain. This is a deliberate exception to the rule that contexts integrate only through each
 * other's api or an anti-corruption layer. Any change to {@code BorrowerId} must be agreed by both Accounts and Rentals.
 * <p>
 * Currently, it's implemented as a single module.
 */
@BoundedContext(name = "Accounts")
@Module
package pl.wojtyna.dddtrainingv3.velocity.accounts;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
