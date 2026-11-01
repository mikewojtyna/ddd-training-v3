package pl.wojtyna.dddtrainingv3.velocity.investment.api.usecases;

import org.jmolecules.architecture.hexagonal.PrimaryPort;
import org.joda.money.Money;
import pl.wojtyna.dddtrainingv3.velocity.investment.invest.Asset;
import pl.wojtyna.dddtrainingv3.velocity.investment.invest.InvestOperations;
import pl.wojtyna.dddtrainingv3.velocity.investment.invest.Investor;

@PrimaryPort
public class InvestManuallyUseCase {

    private final InvestOperations investOperations;

    public InvestManuallyUseCase(InvestOperations investOperations) {
        this.investOperations = investOperations;
    }

    public void invest(Asset asset, Money amount, Investor investor) {
        investOperations.investInto(asset).some(amount).by(investor);
    }
}
