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

package network.misq.desktop.main.content.settings;

import network.misq.application.DefaultServiceProvider;
import network.misq.desktop.common.view.Model;

// Handled jfx only concerns, others which can be re-used by other frontends are in OfferbookEntity
public class SettingsModel implements Model {

    private final DefaultServiceProvider serviceProvider;

    public SettingsModel(DefaultServiceProvider serviceProvider) {
        this.serviceProvider = serviceProvider;
    }

    public void initialize() {
    }

    public void activate() {
    }

    public void deactivate() {
    }


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // API
    ///////////////////////////////////////////////////////////////////////////////////////////////////


    ///////////////////////////////////////////////////////////////////////////////////////////////////
    // Package private
    ///////////////////////////////////////////////////////////////////////////////////////////////////
}