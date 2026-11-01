package pl.wojtyna.dddtrainingv3.velocity.pricing.domain.pricelist;

import org.jmolecules.ddd.annotation.ValueObject;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/// All published price lists. Decides which price list applies at a given moment: the latest one whose valid-from
/// moment is not after that moment (valid-from is inclusive).
@ValueObject
public record PriceLists(List<PriceList> priceLists) {

    public PriceLists {
        priceLists = List.copyOf(Objects.requireNonNull(priceLists, "priceLists"));
    }

    public static PriceLists of(PriceList... priceLists) {
        return new PriceLists(List.of(priceLists));
    }

    public PriceList validAt(Instant moment) {
        Objects.requireNonNull(moment, "moment");
        return priceLists.stream()
                         .filter(priceList -> priceList.isValidAt(moment))
                         .max(Comparator.comparing(PriceList::validFrom))
                         .orElseThrow(() -> new NoPriceListValidAt(moment));
    }
}
