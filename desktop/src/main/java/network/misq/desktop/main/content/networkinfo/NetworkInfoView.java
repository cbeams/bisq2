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

package network.misq.desktop.main.content.networkinfo;

import com.jfoenix.controls.JFXTabPane;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import lombok.extern.slf4j.Slf4j;
import network.misq.common.data.Pair;
import network.misq.desktop.common.threading.UIThread;
import network.misq.desktop.common.view.View;
import network.misq.desktop.components.containers.MisqGridPane;
import network.misq.desktop.components.controls.MisqTextArea;
import network.misq.desktop.components.table.MisqTableColumn;
import network.misq.desktop.components.table.MisqTableView;
import network.misq.desktop.main.content.networkinfo.transport.TransportTypeView;
import network.misq.i18n.Res;
import network.misq.network.p2p.node.transport.Transport;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class NetworkInfoView extends View<ScrollPane, NetworkInfoModel, NetworkInfoController> {
    private final Map<Transport.Type, Tab> tabByTransportType = new HashMap<>();
    private final ChangeListener<Optional<TransportTypeView>> transportTypeViewChangeListener;
    private final ChangeListener<Tab> tabChangeListener;
    private final JFXTabPane tabPane;
    private final MisqTextArea receivedMessagesTextArea;
    private final MisqTableView<DataListItem> dataTableView;
    private final TextField messageReceiverTextField, nodeIdTextField;
    private ChangeListener<DataListItem> dataTableSelectedItemListener;

    public NetworkInfoView(NetworkInfoModel model, NetworkInfoController controller) {
        super(new ScrollPane(), model, controller);

        root.setFitToWidth(true);
        root.setFitToHeight(true);
        
        VBox vBox = new VBox();
        root.setContent(vBox);
        tabPane = new JFXTabPane();
        tabPane.setMinHeight(440);
        MisqGridPane misqGridPane = new MisqGridPane();
        vBox.getChildren().addAll(tabPane, misqGridPane);

        Tab clearNetTab = createTab(Transport.Type.CLEAR, Res.network.get("clearNet"));
        Tab torTab = createTab(Transport.Type.TOR, "Tor");
        Tab i2pTab = createTab(Transport.Type.I2P, "I2P");
        tabPane.getTabs().addAll(clearNetTab, torTab, i2pTab);

        tabChangeListener = (observable, oldValue, newValue) -> {
            controller.onTabSelected(Optional.ofNullable(newValue).map(tab -> Transport.Type.valueOf(tab.getId())));
        };

        transportTypeViewChangeListener = (observable, oldValue, transportTypeViewOptional) -> {
            Optional<Tab> tabOptional = model.getSelectedTransportType().flatMap(e -> Optional.ofNullable(tabByTransportType.get(e)));
            tabOptional.ifPresent(tab -> tab.setContent(transportTypeViewOptional.map(View::getRoot).orElse(null)));
            tabPane.getSelectionModel().select(tabOptional.orElse(null));
            tabPane.requestFocus();
        };

        misqGridPane.startSection(Res.network.get("addData.title"));
        TextField dataContentTextField = misqGridPane.addTextField(Res.network.get("addData.content"), "Test data");
        TextField idTextField = misqGridPane.addTextField(Res.network.get("addData.id"), UUID.randomUUID().toString().substring(0, 8));
        Pair<Button, Label> addDataButtonPair = misqGridPane.addButton(Res.network.get("addData.add"));
        Button addDataButton = addDataButtonPair.first();
        Label label = addDataButtonPair.second();
        addDataButton.setOnAction(e -> {
            addDataButton.setDisable(true);
            label.textProperty().unbind();
            label.setText("...");
            addDataButton.setDisable(false);
            StringProperty result = controller.addData(dataContentTextField.getText(), idTextField.getText());
            label.textProperty().bind(result);
        });
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("table.data.title"));
        dataTableView = new MisqTableView<>(model.getSortedDataListItems());
        dataTableView.setMinHeight(200);
        misqGridPane.addTableView(dataTableView);
        configDataTableView();
        misqGridPane.endSection();

        misqGridPane.startSection(Res.network.get("sendMessages.title"));
        messageReceiverTextField = misqGridPane.addTextField(Res.network.get("sendMessages.to"), "localhost:8000");
        messageReceiverTextField.setEditable(false);
        nodeIdTextField = misqGridPane.addTextField(Res.network.get("sendMessages.nodeId"), "");
        nodeIdTextField.setEditable(false);
        TextField msgTextField = misqGridPane.addTextField(Res.network.get("sendMessages.text"), "Test message");
        Pair<Button, Label> sendButtonPair = misqGridPane.addButton(Res.network.get("sendMessages.send"));
        Button sendButton = sendButtonPair.first();
        sendButton.setOnAction(e -> {
            String msg = msgTextField.getText();
            sendButton.setDisable(true);
            sendButtonPair.second().setText("...");
            controller.sendMessage(msg).whenComplete((result, throwable) -> {
                UIThread.run(() -> {
                    if (throwable == null) {
                        sendButtonPair.second().setText(result);
                    } else {
                        sendButtonPair.second().setText(throwable.toString());
                    }
                    sendButton.setDisable(false);
                });
            });
        });
        misqGridPane.addHSpacer();
        receivedMessagesTextArea = misqGridPane.addTextArea(Res.network.get("sendMessages.receivedMessage"), model.getReceivedMessages());
        receivedMessagesTextArea.setMinHeight(100);
        misqGridPane.endSection();

        dataTableSelectedItemListener = (observable, oldValue, newValue) -> {
            controller.onSelectNetworkId(newValue.getNetworkId());
        };
    }

    @Override
    public void activate() {
        model.getTransportTypeView().addListener(transportTypeViewChangeListener);
        tabPane.getSelectionModel().selectedItemProperty().addListener(tabChangeListener);

        Tab clearNetTab = tabByTransportType.get(Transport.Type.CLEAR);
        clearNetTab.disableProperty().bind(model.getClearNetDisabled());
        Tab torTab = tabByTransportType.get(Transport.Type.TOR);
        torTab.disableProperty().bind(model.getTorDisabled());
        Tab i2pTab = tabByTransportType.get(Transport.Type.I2P);
        i2pTab.disableProperty().bind(model.getI2pDisabled());

        if (!model.getClearNetDisabled().get()) {
            tabPane.getSelectionModel().select(clearNetTab);
        } else if (!model.getTorDisabled().get()) {
            tabPane.getSelectionModel().select(torTab);
        } else if (!model.getI2pDisabled().get()) {
            tabPane.getSelectionModel().select(i2pTab);
        }

        nodeIdTextField.textProperty().bind(model.getNodeIdString());
        messageReceiverTextField.textProperty().bind(model.getMessageReceiver());
        dataTableView.getSelectionModel().selectedItemProperty().addListener(dataTableSelectedItemListener);
    }

    @Override
    protected void deactivate() {
        model.getTransportTypeView().removeListener(transportTypeViewChangeListener);
        tabPane.getSelectionModel().selectedItemProperty().removeListener(tabChangeListener);
        tabByTransportType.values().forEach(tab -> tab.disableProperty().unbind());

        nodeIdTextField.textProperty().unbind();
        messageReceiverTextField.textProperty().unbind();
        dataTableView.getSelectionModel().selectedItemProperty().removeListener(dataTableSelectedItemListener);
    }

    private Tab createTab(Transport.Type transportType, String title) {
        Tab tab = new Tab(title.toUpperCase());
        tab.setClosable(false);
        tab.setId(transportType.name());
        tabByTransportType.put(transportType, tab);
        return tab;
    }

    private void configDataTableView() {
        var dateColumn = new MisqTableColumn.Builder<DataListItem>()
                .title(Res.network.get("table.data.header.received"))
                .minWidth(180)
                .maxWidth(180)
                .valueSupplier(DataListItem::getReceived)
                .comparator(DataListItem::compareDate)
                .build();
        dataTableView.getColumns().add(dateColumn);
        dataTableView.getSortOrder().add(dateColumn);

        dataTableView.getColumns().add(new MisqTableColumn.Builder<DataListItem>()
                .title(Res.network.get("table.data.header.content"))
                .minWidth(220)
                .valueSupplier(DataListItem::getContent)
                .build());
        dataTableView.getColumns().add(new MisqTableColumn.Builder<DataListItem>()
                .title(Res.network.get("table.data.header.nodeId"))
                .valueSupplier(DataListItem::getNodeId)
                .build());
    }

}