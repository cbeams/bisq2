/*
 * This file is part of Bisq.
 *
 * Bisq is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at
 * your option) any later version.
 *
 * Bisq is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Bisq. If not, see <http://www.gnu.org/licenses/>.
 */

package bisq.desktop.primary.main.content.trade.offerbook;

import bisq.common.currency.TradeCurrency;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.Quote;
import bisq.desktop.components.table.TableItem;
import bisq.i18n.Res;
import bisq.offer.FixPrice;
import bisq.offer.FloatPrice;
import bisq.offer.Offer;
import bisq.offer.SettlementSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.QuoteFormatter;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.stream.Collectors;

@Slf4j
@Getter
public class OfferListItem implements TableItem {
    private final String market;
    private final String price;
    private final String baseAmount;
    private final String quoteAmount;
    private final String settlement;
    private final String options;
    private final Offer offer;

    OfferListItem(Offer offer) {
        this.offer = offer;

        market = offer.getMarket().toString();
        String baseCurrencyCode = offer.getMarket().baseCurrencyCode();
        String quoteCurrencyCode = offer.getMarket().quoteCurrencyCode();

        long baseAmountValue = offer.getBaseAmount();
        baseAmount = AmountFormatter.formatAmount(Monetary.from(baseAmountValue, baseCurrencyCode));

        if (offer.getPriceSpec() instanceof FixPrice fixPriceSpec) {
            Monetary base = Monetary.from(baseAmountValue, baseCurrencyCode);
            Quote quote = Quote.fromPrice(fixPriceSpec.value(), offer.getMarket());
            price = QuoteFormatter.format(quote);
            long quoteAmountValue = Quote.toQuoteMonetary(base, quote).getValue();
            quoteAmount = AmountFormatter.formatAmount(Monetary.from(quoteAmountValue, quoteCurrencyCode));
        } else if (offer.getPriceSpec() instanceof FloatPrice floatPrice) {
            quoteAmount = "TODO";
            price = "TODO";
        } else {
            log.warn("PriceSpec not supported");
            quoteAmount = Res.common.get("na");
            price = Res.common.get("na");
        }

        String baseSideSettlement = offer.getBaseSideSettlementSpecs().stream()
                .map(SettlementSpec::settlementMethodName)
                .map(settlementMethodName -> Res.offerbook.get(settlementMethodName))
                .collect(Collectors.joining("\n"));
        String quoteSideSettlement = offer.getQuoteSideSettlementSpecs().stream()
                .map(SettlementSpec::settlementMethodName)
                .map(settlementMethodName -> Res.offerbook.get(settlementMethodName))
                .collect(Collectors.joining("\n"));
        boolean isBaseCurrencyFiat = TradeCurrency.isFiat(baseCurrencyCode);
        boolean isQuoteCurrencyFiat = TradeCurrency.isFiat(quoteCurrencyCode);

        boolean isBaseSideFiatOrMultiple = isBaseCurrencyFiat || offer.getBaseSideSettlementSpecs().size() > 1;
        boolean isQuoteSideFiatOrMultiple = isQuoteCurrencyFiat || offer.getQuoteSideSettlementSpecs().size() > 1;
        if (isBaseSideFiatOrMultiple && !isQuoteSideFiatOrMultiple) {
            settlement = baseSideSettlement;
        } else if (isQuoteSideFiatOrMultiple && !isBaseSideFiatOrMultiple) {
            settlement = quoteSideSettlement;
        } else if (isBaseSideFiatOrMultiple) {
            // both
            settlement = Res.offerbook.get("offerbook.table.settlement.multi",
                    baseCurrencyCode, baseSideSettlement, quoteCurrencyCode, quoteSideSettlement);
        } else {
            // none (both are using non fiat mandatory settlement method 
            settlement = "";
        }

        options = ""; //todo
    }

    @Override
    public void activate() {
    }

    @Override
    public void deactivate() {
    }
}