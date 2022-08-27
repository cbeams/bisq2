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

package bisq.desktop.primary.main.content.settings.reputation.accountAge.tab3;

import bisq.desktop.common.view.View;
import bisq.desktop.components.containers.Spacer;
import bisq.desktop.components.controls.MaterialTextField;
import bisq.i18n.Res;
import de.jensd.fx.fontawesome.AwesomeIcon;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AccountAgeTab3View extends View<VBox, AccountAgeTab3Model, AccountAgeTab3Controller> {
    private final MaterialTextField pubKeyHash;
    private final Button closeButton, backButton, requestCertificateButton;
    private final Hyperlink learnMore;

    public AccountAgeTab3View(AccountAgeTab3Model model,
                              AccountAgeTab3Controller controller,
                              Pane userProfileSelection) {
        super(new VBox(), model, controller);

        root.setSpacing(20);
        root.setAlignment(Pos.TOP_LEFT);

        Label headLine = new Label(Res.get("reputation.accountAge.howToHeadline"));
        headLine.getStyleClass().add("bisq-text-headline-2");

        Label info = new Label(Res.get("reputation.accountAge.howTo"));
        info.getStyleClass().addAll("bisq-text-13", "wrap-text", "bisq-line-spacing-01");

        Label userProfileSelectLabel = new Label(Res.get("settings.userProfile.select").toUpperCase());
        userProfileSelectLabel.getStyleClass().add("bisq-text-4");
        userProfileSelectLabel.setAlignment(Pos.TOP_LEFT);

        pubKeyHash = new MaterialTextField(Res.get("reputation.pubKeyHash"), "");
        pubKeyHash.setEditable(false);
        pubKeyHash.setIcon(AwesomeIcon.COPY);
        pubKeyHash.setIconTooltip(Res.get("copyToClipboard"));

        requestCertificateButton = new Button(Res.get("reputation.request"));
        requestCertificateButton.getStyleClass().add("outlined-button");

        backButton = new Button(Res.get("back"));

        closeButton = new Button(Res.get("close"));
        closeButton.setDefaultButton(true);

        learnMore = new Hyperlink(Res.get("reputation.learnMore"));

        HBox buttons = new HBox(20, backButton, closeButton, Spacer.fillHBox(), learnMore);
        buttons.setAlignment(Pos.BOTTOM_RIGHT);

        VBox.setVgrow(info, Priority.ALWAYS);
        VBox.setMargin(headLine, new Insets(10, 0, 0, 0));
        VBox.setMargin(userProfileSelectLabel, new Insets(10, 0, -20, 0));
        VBox.setMargin(userProfileSelection, new Insets(0, 0, -30, 0));
        VBox.setMargin(buttons, new Insets(10, 0, 0, 0));
        root.getChildren().addAll(headLine, info, userProfileSelectLabel, userProfileSelection, pubKeyHash, requestCertificateButton, buttons);
    }

    @Override
    protected void onViewAttached() {
        pubKeyHash.textProperty().bind(model.getPubKeyHash());
        pubKeyHash.getIconButton().setOnAction(e -> controller.onCopyToClipboard(pubKeyHash.getText()));
        closeButton.setOnAction(e -> controller.onClose());
        backButton.setOnAction(e -> controller.onBack());
        learnMore.setOnAction(e -> controller.onLearnMore());
        requestCertificateButton.setOnAction(e -> controller.onRequestAuthorization());
    }

    @Override
    protected void onViewDetached() {
        pubKeyHash.textProperty().unbind();

        pubKeyHash.getIconButton().setOnAction(null);
        closeButton.setOnAction(null);
        backButton.setOnAction(null);
        learnMore.setOnAction(null);
        requestCertificateButton.setOnAction(null);
    }
}