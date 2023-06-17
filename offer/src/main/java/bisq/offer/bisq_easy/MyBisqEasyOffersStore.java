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

package bisq.offer.bisq_easy;

import bisq.common.observable.collection.ObservableSet;
import bisq.common.proto.ProtoResolver;
import bisq.common.proto.UnresolvableProtobufMessageException;
import bisq.persistence.PersistableStore;
import com.google.protobuf.InvalidProtocolBufferException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public final class MyBisqEasyOffersStore implements PersistableStore<MyBisqEasyOffersStore> {
    @Getter
    private final ObservableSet<BisqEasyOffer> offers = new ObservableSet<>();

    public MyBisqEasyOffersStore() {
    }

    private MyBisqEasyOffersStore(Set<BisqEasyOffer> offers) {
        this.offers.addAll(offers);
    }


    @Override
    public MyBisqEasyOffersStore getClone() {
        return new MyBisqEasyOffersStore(offers);
    }

    @Override
    public void applyPersisted(MyBisqEasyOffersStore persisted) {
        offers.clear();
        offers.addAll(persisted.getOffers());
    }

    @Override
    public bisq.offer.protobuf.MyBisqEasyOffersStore toProto() {
        return bisq.offer.protobuf.MyBisqEasyOffersStore.newBuilder()
                .addAllOffers(offers.stream().map(BisqEasyOffer::toProto).collect(Collectors.toList()))
                .build();
    }

    public static MyBisqEasyOffersStore fromProto(bisq.offer.protobuf.MyBisqEasyOffersStore proto) {
        return new MyBisqEasyOffersStore(proto.getOffersList().stream().map(BisqEasyOffer::fromProto).collect(Collectors.toSet()));
    }

    @Override
    public ProtoResolver<PersistableStore<?>> getResolver() {
        return any -> {
            try {
                return fromProto(any.unpack(bisq.offer.protobuf.MyBisqEasyOffersStore.class));
            } catch (InvalidProtocolBufferException e) {
                throw new UnresolvableProtobufMessageException(e);
            }
        };
    }

    public void add(BisqEasyOffer offer) {
        offers.add(offer);
    }

    public void remove(BisqEasyOffer offer) {
        offers.remove(offer);
    }
}