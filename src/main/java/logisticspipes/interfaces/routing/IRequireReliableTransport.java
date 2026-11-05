/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.interfaces.routing;

import org.jspecify.annotations.Nullable;

import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * A pipe that keeps count of the items on their way to it, and so has to hear how that ends.
 */
public interface IRequireReliableTransport {

    void itemLost(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info);

    void itemArrived(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info);
}
