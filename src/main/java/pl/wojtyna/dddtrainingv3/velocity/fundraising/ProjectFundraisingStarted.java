package pl.wojtyna.dddtrainingv3.velocity.fundraising;

import pl.wojtyna.dddtrainingv3.velocity.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.velocity.investment.invest.Investor;

public record ProjectFundraisingStarted(Investor investor, ProjectForm projectForm) implements DomainEvent {

}
