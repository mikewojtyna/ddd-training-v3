package pl.wojtyna.dddtrainingv3.crowdsorcery.fundraising;

import pl.wojtyna.dddtrainingv3.common.domain.DomainEvent;
import pl.wojtyna.dddtrainingv3.crowdsorcery.investment.invest.Investor;

public record ProjectFundraisingStarted(Investor investor, ProjectForm projectForm) implements DomainEvent {

}
