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

package bisq.desktop.primary.onboarding.selectUserType;

import bisq.application.DefaultApplicationService;
import bisq.common.data.ByteArray;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.robohash.RoboHash;
import bisq.i18n.Res;
import bisq.social.user.ChatUserIdentity;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SelectUserTypeControllerOld implements Controller {
    private final SelectUserTypeModelOld model;
    @Getter
    private final SelectUserTypeViewOld view;

    public SelectUserTypeControllerOld(DefaultApplicationService applicationService) {
        ChatUserIdentity chatUserIdentity = applicationService.getChatUserService().getSelectedUserProfile().get();
        String profileId = chatUserIdentity.getProfileId();
        model = new SelectUserTypeModelOld(profileId, RoboHash.getImage(new ByteArray(chatUserIdentity.getPubKeyHash())));
        model.getUserTypes().addAll(SelectUserTypeModelOld.TypeOld.NEWBIE, SelectUserTypeModelOld.TypeOld.PRO_TRADER);
        view = new SelectUserTypeViewOld(model, this);
    }

    @Override
    public void onActivate() {
        onSelect(SelectUserTypeModelOld.TypeOld.NEWBIE);
    }

    @Override
    public void onDeactivate() {
    }

    public void onSelect(SelectUserTypeModelOld.TypeOld selectedType) {
        model.setSelectedType(selectedType);
        if (selectedType != null) {
            switch (selectedType) {
                case NEWBIE -> {
                    model.getInfo().set(Res.get("satoshisquareapp.selectTraderType.newbie.info"));
                    model.getButtonText().set(Res.get("satoshisquareapp.selectTraderType.newbie.button"));
                }
                case PRO_TRADER -> {
                    model.getInfo().set(Res.get("satoshisquareapp.selectTraderType.proTrader.info"));
                    model.getButtonText().set(Res.get("satoshisquareapp.selectTraderType.proTrader.button"));
                }
            }
        }
    }

    public void onAction() {
        switch (model.getSelectedType()) {
            case NEWBIE -> {
                Navigation.navigateTo(NavigationTarget.ONBOARD_NEWBIE);
            }
            case PRO_TRADER -> {
                Navigation.navigateTo(NavigationTarget.ONBOARD_PRO_TRADER);
            }
        }
    }

    void onGoBack() {
        Navigation.navigateTo(NavigationTarget.INIT_USER_PROFILE);
    }
}