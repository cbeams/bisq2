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

package bisq.wallets.core;

import bisq.common.observable.ObservableSet;
import bisq.wallets.core.model.AddressType;
import bisq.wallets.core.model.Transaction;
import bisq.wallets.core.model.Utxo;

import java.util.List;
import java.util.Optional;

public interface Wallet {
    void initialize(Optional<String> walletPassphrase);

    void shutdown();

    double getBalance();

    String getNewAddress(AddressType addressType, String label);

    ObservableSet<String> getReceiveAddresses();

    List<? extends Transaction> listTransactions();

    List<? extends Utxo> listUnspent();

    String sendToAddress(String address, double amount);

    String signMessage(String address, String message);
}