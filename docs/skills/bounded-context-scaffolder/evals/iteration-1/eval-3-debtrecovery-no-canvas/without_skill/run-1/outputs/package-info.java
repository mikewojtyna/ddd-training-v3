/**
 * DebtRecovery bounded context. Actively pursues a rider's outstanding debt (reminders, payment plans, pausing and
 * resuming recovery) until the debt is cleared or recovery is exhausted. It does not own the debt itself (Settlement
 * does), and it does not block or unblock accounts (Accounts does, reacting to Settlement's events). Currently, it's
 * implemented as a single module.
 */
@BoundedContext(name = "DebtRecovery")
@Module
package pl.wojtyna.dddtrainingv3.velocity.debtrecovery;

import org.jmolecules.ddd.annotation.BoundedContext;
import org.jmolecules.ddd.annotation.Module;
