package pl.wojtyna.dddtrainingv3.velocity.investment.invest;

import org.joda.money.Money;

public interface Strategy {

    InvestmentDecisions apply(Money of);
}
