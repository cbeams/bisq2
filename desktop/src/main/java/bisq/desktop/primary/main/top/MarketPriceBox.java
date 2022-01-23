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

package bisq.desktop.primary.main.top;

import bisq.common.currency.BisqCurrency;
import bisq.common.monetary.Market;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Model;
import bisq.desktop.common.view.View;
import bisq.desktop.components.controls.BisqComboBox;
import bisq.oracle.marketprice.MarketPrice;
import bisq.oracle.marketprice.MarketPriceService;
import bisq.presentation.formatters.QuoteFormatter;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * We pack the MVC classes directly into the Component class to have it more compact as scope and complexity is
 * rather limited.
 * <p>
 * Is never removed so no need to handle onViewDetached case
 */
@Slf4j
public class MarketPriceBox {
    public static class MarketPriceController implements Controller, MarketPriceService.Listener {
        private final MarketPriceService marketPriceService;
        private final MarketPriceModel model;
        @Getter
        private final MarketPriceView view;

        public MarketPriceController(MarketPriceService marketPriceService) {
            this.marketPriceService = marketPriceService;
            model = new MarketPriceModel();
            view = new MarketPriceView(model, this);
            marketPriceService.addListener(this);
        }

        @Override
        public void onMarketPriceUpdate(Map<Market, MarketPrice> map) {
            UIThread.run(() -> model.applyMarketPriceMap(map));
        }

        @Override
        public void onMarketPriceSelected(MarketPrice selected) {
            UIThread.run(() -> model.selected.set(new ListItem(selected)));
        }

        private void onSelect(ListItem selectedItem) {
            if (selectedItem != null) {
                marketPriceService.select(selectedItem.marketPrice);
            }
        }
    }

    private static class MarketPriceModel implements Model {
        private final ObservableList<ListItem> items = FXCollections.observableArrayList();
        private final ObjectProperty<ListItem> selected = new SimpleObjectProperty<>();

        public void applyMarketPriceMap(Map<Market, MarketPrice> map) {
            //todo use preferred currencies + edit entry
            List<ListItem> list = map.values().stream().map(ListItem::new).collect(Collectors.toList());
            items.setAll(list);
            if (selected.get() != null) {
                selected.set(new ListItem(map.get(selected.get().marketPrice.getMarket())));
            }
        }
    }

    @Slf4j
    public static class MarketPriceView extends View<VBox, Model, Controller> {
        public MarketPriceView(MarketPriceModel model, MarketPriceController controller) {
            super(new VBox(), model, controller);
            root.setAlignment(Pos.CENTER_LEFT);

            ComboBox<ListItem> comboBox = new BisqComboBox<>();
            comboBox.setVisibleRowCount(12);
            comboBox.setFocusTraversable(false);
            comboBox.setId("price-feed-combo");
            comboBox.setPadding(new Insets(0, -4, -4, 0));
            comboBox.setItems(model.items);
            comboBox.setOnAction(e -> controller.onSelect(comboBox.getSelectionModel().getSelectedItem()));
            comboBox.setConverter(new StringConverter<>() {
                @Override
                public String toString(@Nullable ListItem listItem) {
                    return listItem != null ? listItem.displayStringProperty.get() : "";
                }

                @Override
                public ListItem fromString(String string) {
                    return null;
                }
            });

            Label marketPriceLabel = new Label();
            marketPriceLabel.getStyleClass().add("nav-balance-label");
            marketPriceLabel.setPadding(new Insets(-2, 0, 4, 9));
            //todo add provider info to marketPriceLabel

            root.getChildren().addAll(comboBox, marketPriceLabel);

            model.selected.addListener((o, old, newValue) -> comboBox.getSelectionModel().select(newValue));
        }
    }

    @ToString
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    private static class ListItem {
        public final StringProperty displayStringProperty = new SimpleStringProperty();
        private final MarketPrice marketPrice;
        @EqualsAndHashCode.Include
        private final String code;

        public ListItem(MarketPrice marketPrice) {
            this.marketPrice = marketPrice;
            code = marketPrice.code();
            String pair = BisqCurrency.isFiat(code) ? ("BTC/" + code) : (code + "/BTC");
            displayStringProperty.set(pair + ": " + QuoteFormatter.format(marketPrice.quote(), true));
        }
    }
}