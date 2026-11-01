package pl.wojtyna.dddtrainingv3.crowdsorcery.investment.invest;

import org.joda.money.Money;

public interface Strategy {

    InvestmentDecisions apply(Money of);
}
