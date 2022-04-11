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

package bisq.desktop.primary.splash;

import bisq.application.DefaultApplicationService;
import bisq.common.observable.Pin;
import bisq.desktop.common.observable.FxBindings;
import bisq.desktop.common.view.Controller;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SplashController implements Controller {
    private final SplashModel model;
    @Getter
    private final SplashView view;
    private final DefaultApplicationService applicationService;
    private Pin pin;

    public SplashController(DefaultApplicationService applicationService) {
        this.applicationService = applicationService;
        model = new SplashModel();
        view = new SplashView(model, this);
    }

    @Override
    public void onActivate() {
        pin = FxBindings.bind(model.getState()).to(applicationService.getState());
    }

    @Override
    public void onDeactivate() {
        pin.unbind();
    }
}