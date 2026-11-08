package pl.wojtyna.dddtrainingv3.velocity.accounts.domain.account;

import org.jmolecules.architecture.hexagonal.SecondaryPort;

/// Which email addresses already have an account (DE-98). A rule over all accounts, so one `Account` can't check it.
@SecondaryPort
public interface RegisteredEmails {

    boolean isRegistered(EmailAddress email);
}
