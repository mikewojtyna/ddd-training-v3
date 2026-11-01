package pl.wojtyna.dddtrainingv3.crowdsorcery.investment.invest;

import java.util.Optional;

public interface Strategies {

    Optional<Strategy> activeStrategyOf(InvestorId investorId);

    void set(Strategy strategy);
}
