/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.interfaces.routing;

import java.util.List;
import java.util.Map;

import org.jspecify.annotations.Nullable;

import logisticspipes.request.order.Order;
import logisticspipes.request.promise.ItemPromise;
import logisticspipes.utils.item.ItemIdentifier;

/**
 * A provider of items.
 */
public interface IProvideItems extends IProvide {

    /**
     * @return the order placed, or null when none could be
     */
    @Nullable Order fullFill(ItemPromise promise, IRequestItems destination,
        @Nullable IAdditionalTargetInformation info);

    /**
     * Adds what this can provide to {@code items}, leaving out what {@code filters} block.
     */
    void getAllItems(Map<ItemIdentifier, Integer> items, List<IFilter> filters);
}
