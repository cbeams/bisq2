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

package bisq.desktop.main.content.bisq_easy.trade_wizard.review;

import bisq.account.payment_method.FiatPaymentMethod;
import bisq.bonded_roles.market_price.MarketPrice;
import bisq.bonded_roles.market_price.MarketPriceService;
import bisq.chat.ChatChannelDomain;
import bisq.chat.ChatChannelSelectionService;
import bisq.chat.ChatService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannel;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookChannelService;
import bisq.chat.bisqeasy.offerbook.BisqEasyOfferbookMessage;
import bisq.chat.bisqeasy.open_trades.BisqEasyOpenTradeChannelService;
import bisq.common.currency.Market;
import bisq.common.monetary.Monetary;
import bisq.common.monetary.PriceQuote;
import bisq.contract.bisq_easy.BisqEasyContract;
import bisq.desktop.ServiceProvider;
import bisq.desktop.common.threading.UIThread;
import bisq.desktop.common.view.Controller;
import bisq.desktop.common.view.Navigation;
import bisq.desktop.common.view.NavigationTarget;
import bisq.desktop.components.overlay.Popup;
import bisq.desktop.main.content.bisq_easy.components.PriceInput;
import bisq.desktop.overlay.OverlayController;
import bisq.i18n.Res;
import bisq.offer.Direction;
import bisq.offer.amount.OfferAmountFormatter;
import bisq.offer.amount.OfferAmountUtil;
import bisq.offer.amount.spec.AmountSpec;
import bisq.offer.amount.spec.FixedAmountSpec;
import bisq.offer.amount.spec.RangeAmountSpec;
import bisq.offer.bisq_easy.BisqEasyOffer;
import bisq.offer.payment_method.FiatPaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpec;
import bisq.offer.payment_method.PaymentMethodSpecFormatter;
import bisq.offer.price.PriceUtil;
import bisq.offer.price.spec.FixPriceSpec;
import bisq.offer.price.spec.FloatPriceSpec;
import bisq.offer.price.spec.MarketPriceSpec;
import bisq.offer.price.spec.PriceSpec;
import bisq.presentation.formatters.AmountFormatter;
import bisq.presentation.formatters.PercentageFormatter;
import bisq.presentation.formatters.PriceFormatter;
import bisq.settings.SettingsService;
import bisq.trade.TradeException;
import bisq.trade.bisq_easy.BisqEasyTrade;
import bisq.trade.bisq_easy.BisqEasyTradeService;
import bisq.user.banned.BannedUserService;
import bisq.user.identity.UserIdentity;
import bisq.user.identity.UserIdentityService;
import bisq.user.profile.UserProfileService;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.google.common.base.Preconditions.checkNotNull;

@Slf4j
public class TradeWizardReviewController implements Controller {
    private final TradeWizardReviewModel model;
    @Getter
    private final TradeWizardReviewView view;
    private final BisqEasyOpenTradeChannelService bisqEasyOpenTradeChannelService;
    private final ChatService chatService;
    private final Runnable resetHandler;
    private final Consumer<Boolean> mainButtonsVisibleHandler;
    private final PriceInput priceInput;
    private final MarketPriceService marketPriceService;
    private final UserIdentityService userIdentityService;
    private final BisqEasyTradeService bisqEasyTradeService;
    private final BannedUserService bannedUserService;
    private final BisqEasyOfferbookChannelService bisqEasyOfferbookChannelService;
    private final SettingsService settingsService;
    private final UserProfileService userProfileService;

    public TradeWizardReviewController(ServiceProvider serviceProvider,
                                       Consumer<Boolean> mainButtonsVisibleHandler,
                                       Runnable resetHandler) {
        this.mainButtonsVisibleHandler = mainButtonsVisibleHandler;
        this.resetHandler = resetHandler;

        userIdentityService = serviceProvider.getUserService().getUserIdentityService();
        chatService = serviceProvider.getChatService();
        bisqEasyOfferbookChannelService = chatService.getBisqEasyOfferbookChannelService();
        bisqEasyOpenTradeChannelService = chatService.getBisqEasyOpenTradeChannelService();
        marketPriceService = serviceProvider.getBondedRolesService().getMarketPriceService();
        bisqEasyTradeService = serviceProvider.getTradeService().getBisqEasyTradeService();
        bannedUserService = serviceProvider.getUserService().getBannedUserService();
        settingsService = serviceProvider.getSettingsService();
        userProfileService = serviceProvider.getUserService().getUserProfileService();

        priceInput = new PriceInput(serviceProvider.getBondedRolesService().getMarketPriceService());

        model = new TradeWizardReviewModel();
        view = new TradeWizardReviewView(model, this);
    }

    public void setFiatPaymentMethods(List<FiatPaymentMethod> fiatPaymentMethods) {
        if (fiatPaymentMethods != null) {
            model.setSelectedFiatPaymentMethod(null);
        }
    }

    public void setDataForCreateOffer(Direction direction,
                                      Market market,
                                      List<FiatPaymentMethod> fiatPaymentMethods,
                                      AmountSpec amountSpec,
                                      PriceSpec priceSpec) {
        model.setCreateOfferMode(true);
        model.setMarket(market);

        String priceInfo;
        if (direction.isSell()) {
            if (priceSpec instanceof FixPriceSpec) {
                FixPriceSpec fixPriceSpec = (FixPriceSpec) priceSpec;
                String price = PriceFormatter.formatWithCode(fixPriceSpec.getPriceQuote());
                priceInfo = Res.get("bisqEasy.createOffer.review.chatMessage.fixPrice", price);
            } else if (priceSpec instanceof FloatPriceSpec) {
                FloatPriceSpec floatPriceSpec = (FloatPriceSpec) priceSpec;
                String percent = PercentageFormatter.formatToPercentWithSymbol(floatPriceSpec.getPercentage());
                priceInfo = Res.get("bisqEasy.createOffer.review.chatMessage.floatPrice", percent);
            } else {
                priceInfo = Res.get("bisqEasy.createOffer.review.chatMessage.marketPrice");
            }
        } else {
            priceInfo = "";
        }

        String directionString = Res.get("offer." + direction.name().toLowerCase()).toUpperCase();
        boolean hasAmountRange = amountSpec instanceof RangeAmountSpec;
        String quoteAmountAsString = OfferAmountFormatter.formatQuoteAmount(marketPriceService, amountSpec, priceSpec, market, hasAmountRange, true);
        model.setQuoteAmountAsString(quoteAmountAsString);

        String paymentMethodNames = PaymentMethodSpecFormatter.fromPaymentMethods(fiatPaymentMethods, true);
        String chatMessageText = Res.get("bisqEasy.createOffer.review.chatMessage",
                directionString,
                quoteAmountAsString,
                paymentMethodNames,
                priceInfo);

        model.setMyOfferText(chatMessageText);

        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        BisqEasyOffer bisqEasyOffer = new BisqEasyOffer(
                userIdentity.getUserProfile().getNetworkId(),
                direction,
                market,
                amountSpec,
                priceSpec,
                new ArrayList<>(fiatPaymentMethods),
                userIdentity.getUserProfile().getTerms(),
                settingsService.getRequiredTotalReputationScore().get(),
                new ArrayList<>(settingsService.getSupportedLanguageCodes()));
        model.setBisqEasyOffer(bisqEasyOffer);

        Optional<BisqEasyOfferbookChannel> optionalChannel = bisqEasyOfferbookChannelService.findChannel(market);
        if (optionalChannel.isPresent()) {
            BisqEasyOfferbookChannel channel = optionalChannel.get();
            model.setSelectedChannel(channel);

            BisqEasyOfferbookMessage myOfferMessage = new BisqEasyOfferbookMessage(channel.getId(),
                    userIdentity.getUserProfile().getId(),
                    Optional.of(bisqEasyOffer),
                    Optional.of(chatMessageText),
                    Optional.empty(),
                    new Date().getTime(),
                    false);

            model.setMyOfferMessage(myOfferMessage);
        } else {
            log.warn("optionalChannel not present");
        }

        applyData(direction,
                market,
                fiatPaymentMethods,
                amountSpec,
                priceSpec);
    }

    public void setDataForTakeOffer(BisqEasyOffer bisqEasyOffer,
                                    AmountSpec amountSpec,
                                    @Nullable PriceSpec priceSpec,
                                    List<FiatPaymentMethod> fiatPaymentMethods) {
        if (bisqEasyOffer == null) {
            return;
        }

        model.setCreateOfferMode(false);
        model.setBisqEasyOffer(bisqEasyOffer);
        model.setMarket(bisqEasyOffer.getMarket());
        Direction direction = bisqEasyOffer.getTakersDirection();
        Market market = bisqEasyOffer.getMarket();

        List<FiatPaymentMethodSpec> quoteSidePaymentMethodSpecs = bisqEasyOffer.getQuoteSidePaymentMethodSpecs();
        Set<FiatPaymentMethod> takersPaymentMethodSet = new HashSet<>(fiatPaymentMethods);
        List<FiatPaymentMethod> fiatPaymentMethodsToUse = quoteSidePaymentMethodSpecs.stream()
                .filter(e -> takersPaymentMethodSet.contains(e.getPaymentMethod()))
                .map(PaymentMethodSpec::getPaymentMethod)
                .collect(Collectors.toList());

        // If taker is seller (buy offer) we use the offers price spec, otherwise the param
        PriceSpec priceSpecToUse = direction.isBuy() ? bisqEasyOffer.getPriceSpec() : priceSpec;
        if (direction.isBuy()) {
            log.info("At buy offers we do not have a priceSpec as parameter {}", priceSpec);
        }

        AmountSpec amountSpecToUse = bisqEasyOffer.getAmountSpec() instanceof FixedAmountSpec ?
                bisqEasyOffer.getAmountSpec() :
                amountSpec;

        applyData(direction,
                market,
                fiatPaymentMethodsToUse,
                amountSpecToUse,
                priceSpecToUse);
    }

    // direction is from user perspective not offer direction
    private void applyData(Direction direction,
                           Market market,
                           List<FiatPaymentMethod> fiatPaymentMethods,
                           AmountSpec amountSpec,
                           PriceSpec priceSpec) {
        boolean isCreateOfferMode = model.isCreateOfferMode();

        String marketCodes = market.getMarketCodes();

        model.setPriceSpec(priceSpec);
        priceInput.setMarket(market);
        priceInput.setDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.taker", marketCodes));

        Optional<PriceQuote> quote = PriceUtil.findQuote(marketPriceService, priceSpec, market);
        quote.ifPresent(priceInput::setQuote);
        model.setPrice(quote
                .map(PriceFormatter::formatWithCode)
                .orElse(Res.get("data.na")));
        applyPriceDetails(direction, model.getPriceSpec(), market);
        model.setNoTradePriceAvailable(isCreateOfferMode && direction.isBuy());
        String formattedBaseAmount, formattedQuoteAmount;
        if (amountSpec instanceof RangeAmountSpec) {
            Monetary minBaseSideAmount = OfferAmountUtil.findBaseSideMinAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMinBaseSideAmount(minBaseSideAmount);
            Monetary maxBaseSideAmount = OfferAmountUtil.findBaseSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMaxBaseSideAmount(maxBaseSideAmount);
            formattedBaseAmount = AmountFormatter.formatAmount(minBaseSideAmount, false) + " - " +
                    AmountFormatter.formatAmountWithCode(maxBaseSideAmount, false);

            Monetary minQuoteSideAmount = OfferAmountUtil.findQuoteSideMinAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMinQuoteSideAmount(minQuoteSideAmount);
            Monetary maxQuoteSideAmount = OfferAmountUtil.findQuoteSideMaxAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setMaxQuoteSideAmount(maxQuoteSideAmount);
            formattedQuoteAmount = AmountFormatter.formatAmount(minQuoteSideAmount) + " - " +
                    AmountFormatter.formatAmountWithCode(maxQuoteSideAmount);

        } else {
            Monetary fixBaseSideAmount = OfferAmountUtil.findBaseSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setFixBaseSideAmount(fixBaseSideAmount);
            formattedBaseAmount = AmountFormatter.formatAmountWithCode(fixBaseSideAmount, false);

            Monetary fixQuoteSideAmount = OfferAmountUtil.findQuoteSideFixedAmount(marketPriceService, amountSpec, priceSpec, market).orElseThrow();
            model.setFixQuoteSideAmount(fixQuoteSideAmount);
            formattedQuoteAmount = AmountFormatter.formatAmountWithCode(fixQuoteSideAmount);

            /*if (priceSpec != null) {
                Optional<Double> percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, market);
                String sellersPremium;
                if (percentFromMarketPrice.isPresent()) {
                    double percentage = percentFromMarketPrice.get();
                    long quoteSidePremium = MathUtils.roundDoubleToLong(fixBaseSideAmount.getValue() * percentage);
                    Monetary quoteSidePremiumAsMonetary = Fiat.fromValue(quoteSidePremium, fixBaseSideAmount.getCode());
                    long baseSidePremium = MathUtils.roundDoubleToLong(fixQuoteSideAmount.getValue() * percentage);
                    Monetary baseSidePremiumAsMonetary = Coin.fromValue(baseSidePremium, fixQuoteSideAmount.getCode());
                    sellersPremium = AmountFormatter.formatAmountWithCode(quoteSidePremiumAsMonetary) + " / " +
                            AmountFormatter.formatAmountWithCode(baseSidePremiumAsMonetary, false);
                } else {
                    sellersPremium = Res.get("data.na");
                }
                model.setSellersPremium(sellersPremium);
            } else {
                model.setSellersPremium("TODO");
            }*/
        }

        String equalSign = model.isCreateOfferMode() && direction.isBuy() ? " ~ " : " = ";
        model.setAmountsHeadline(formattedQuoteAmount + equalSign + formattedBaseAmount);
        model.setFee(direction.isBuy() ?
                Res.get("bisqEasy.tradeWizard.review.fee.buyer") :
                Res.get("bisqEasy.tradeWizard.review.fee.seller"));

        String directionHeadline;
        if (isCreateOfferMode) {
            model.setHeadline(Res.get("bisqEasy.tradeWizard.review.headline.maker"));
            model.setDetailsHeadline(Res.get("bisqEasy.tradeWizard.review.detailsHeadline.maker").toUpperCase());
            model.setPaymentMethodDescription(
                    fiatPaymentMethods.size() == 1 ?
                            Res.get("bisqEasy.tradeWizard.review.paymentMethodDescription") :
                            Res.get("bisqEasy.tradeWizard.review.paymentMethodDescriptions.maker")
            );
            model.setPaymentMethod(PaymentMethodSpecFormatter.fromPaymentMethods(fiatPaymentMethods));

            if (direction.isSell()) {
                // Maker as seller
                model.setPriceDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.maker.seller"));
                model.setToSendAmount(formattedBaseAmount);
                model.setToReceiveAmount(formattedQuoteAmount);

                model.setToSendAmountDescription(Res.get("bisqEasy.tradeWizard.review.toSend"));
                model.setToReceiveAmountDescription(Res.get("bisqEasy.tradeWizard.review.toReceive"));

                directionHeadline = Res.get("bisqEasy.tradeWizard.review.directionHeadline.maker.seller");
            } else {
                // Maker as buyer
                model.setPriceDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.maker.buyer"));
                model.setToSendAmount(formattedQuoteAmount);
                model.setToReceiveAmount(formattedBaseAmount);

                model.setToSendAmountDescription(Res.get("bisqEasy.tradeWizard.review.toPay"));
                model.setToReceiveAmountDescription(Res.get("bisqEasy.tradeWizard.review.toReceive"));

                directionHeadline = Res.get("bisqEasy.tradeWizard.review.directionHeadline.maker.buyer");
            }
        } else {
            model.setHeadline(Res.get("bisqEasy.tradeWizard.review.headline.taker"));
            model.setDetailsHeadline(Res.get("bisqEasy.tradeWizard.review.detailsHeadline.taker").toUpperCase());
            model.setPriceDescription(Res.get("bisqEasy.tradeWizard.review.priceDescription.taker"));
            model.getTakersFiatPaymentMethods().setAll(fiatPaymentMethods);
            if (model.getSelectedFiatPaymentMethod() == null) {
                model.setSelectedFiatPaymentMethod(fiatPaymentMethods.get(0));
            }
            model.setPaymentMethodDescription(
                    fiatPaymentMethods.size() == 1 ?
                            Res.get("bisqEasy.tradeWizard.review.paymentMethodDescription") :
                            Res.get("bisqEasy.tradeWizard.review.paymentMethodDescriptions.taker")
            );
            model.setPaymentMethod(model.getSelectedFiatPaymentMethod().getDisplayString());

            if (direction.isSell()) {
                // Taker as seller
                model.setToSendAmount(formattedBaseAmount);
                model.setToReceiveAmount(formattedQuoteAmount);

                model.setToSendAmountDescription(Res.get("bisqEasy.tradeWizard.review.toSend"));
                model.setToReceiveAmountDescription(Res.get("bisqEasy.tradeWizard.review.toReceive"));

                directionHeadline = Res.get("bisqEasy.tradeWizard.review.directionHeadline.taker.seller");
            } else {
                // Taker as buyer
                model.setToSendAmount(formattedQuoteAmount);
                model.setToReceiveAmount(formattedBaseAmount);

                model.setToSendAmountDescription(Res.get("bisqEasy.tradeWizard.review.toPay"));
                model.setToReceiveAmountDescription(Res.get("bisqEasy.tradeWizard.review.toReceive"));

                directionHeadline = Res.get("bisqEasy.tradeWizard.review.directionHeadline.taker.buyer");
            }
        }

        String postFix = fiatPaymentMethods.size() == 1 ?
                Res.get("bisqEasy.tradeWizard.review.directionHeadline.payment", fiatPaymentMethods.get(0).getDisplayString()) :
                "";
        model.setDirectionHeadline(directionHeadline + " " + postFix);
    }

    public void reset() {
        model.reset();
    }

    public void publishOffer() {
        UserIdentity userIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
        bisqEasyOfferbookChannelService.publishChatMessage(model.getMyOfferMessage(), userIdentity)
                .thenAccept(result -> UIThread.run(() -> {
                    model.getShowCreateOfferSuccess().set(true);
                    mainButtonsVisibleHandler.accept(false);
                }));
    }

    public void takeOffer() {
        return;
    }

    //todo
    public void takeOffer_() {
        try {
            UserIdentity myUserIdentity = checkNotNull(userIdentityService.getSelectedUserIdentity());
            if (bannedUserService.isNetworkIdBanned(model.getBisqEasyOffer().getMakerNetworkId()) ||
                    bannedUserService.isUserProfileBanned(myUserIdentity.getUserProfile())) {
                return;
            }
            BisqEasyOffer bisqEasyOffer = model.getBisqEasyOffer();
            BisqEasyTrade bisqEasyTrade = bisqEasyTradeService.onTakeOffer(myUserIdentity.getIdentity(),
                    bisqEasyOffer,
                    model.getFixBaseSideAmount(),
                    model.getFixQuoteSideAmount(),
                    bisqEasyOffer.getBaseSidePaymentMethodSpecs().get(0),
                    model.getPaymentMethodSpec());

            model.setBisqEasyTrade(bisqEasyTrade);

            BisqEasyContract contract = bisqEasyTrade.getContract();
            String tradeId = bisqEasyTrade.getId();
            bisqEasyOpenTradeChannelService.sendTakeOfferMessage(tradeId, bisqEasyOffer, contract.getMediator())
                    .thenAccept(result -> UIThread.run(() -> {
                        ChatChannelSelectionService chatChannelSelectionService = chatService.getChatChannelSelectionService(ChatChannelDomain.BISQ_EASY_OFFERBOOK);
                        bisqEasyOpenTradeChannelService.findChannel(tradeId)
                                .ifPresent(chatChannelSelectionService::selectChannel);
                        model.getShowTakeOfferSuccess().set(true);
                        mainButtonsVisibleHandler.accept(false);
                    }));
        } catch (TradeException e) {
            new Popup().error(e).show();
        }
    }

    @Override
    public void onActivate() {
        model.getShowCreateOfferSuccess().set(false);

    }

    @Override
    public void onDeactivate() {
    }

    void onShowOfferbook() {
        close();
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_OFFERBOOK);
    }

    void onShowOpenTrades() {
        close();
        Navigation.navigateTo(NavigationTarget.BISQ_EASY_OPEN_TRADES);
    }

    void onSelectFiatPaymentMethod(FiatPaymentMethod paymentMethod) {
        model.setSelectedFiatPaymentMethod(paymentMethod);
    }

    private void close() {
        resetHandler.run();
        OverlayController.hide();
    }

    private void applyPriceDetails(Direction direction, PriceSpec priceSpec, Market market) {
        if (model.isCreateOfferMode() && direction.isBuy()) {
            model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.buyer"));
        } else {
            Optional<PriceQuote> marketPriceQuote = marketPriceService.findMarketPrice(market)
                    .map(MarketPrice::getPriceQuote);
            String marketPrice = marketPriceQuote
                    .map(PriceFormatter::formatWithCode)
                    .orElse(Res.get("data.na"));
            Optional<Double> percentFromMarketPrice;
            percentFromMarketPrice = PriceUtil.findPercentFromMarketPrice(marketPriceService, priceSpec, market);
            double percent = percentFromMarketPrice.orElse(0d);
            if ((priceSpec instanceof FloatPriceSpec || priceSpec instanceof MarketPriceSpec)
                    && percent == 0) {
                model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.seller", marketPrice));
            } else {
                String aboveOrBelow = percent > 0 ?
                        Res.get("offer.price.above") :
                        Res.get("offer.price.below");
                String percentAsString = percentFromMarketPrice.map(Math::abs).map(PercentageFormatter::formatToPercentWithSymbol)
                        .orElse(Res.get("data.na"));
                if (priceSpec instanceof FloatPriceSpec) {
                    model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.seller.float",
                            percentAsString, aboveOrBelow, marketPrice));
                } else {
                    if (percent == 0) {
                        model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.seller.fix.atMarket", marketPrice));
                    } else {
                        model.setPriceDetails(Res.get("bisqEasy.tradeWizard.review.priceDetails.seller.fix",
                                percentAsString, aboveOrBelow, marketPrice));
                    }
                }
            }
        }
    }
}
