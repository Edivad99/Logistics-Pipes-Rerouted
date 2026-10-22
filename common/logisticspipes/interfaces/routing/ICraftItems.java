/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.interfaces.routing;

import java.util.List;

import logisticspipes.utils.item.ItemIdentifierStack;

public interface ICraftItems extends ICraft, IProvideItems, IItemSpaceControl, IRequireReliableTransport {

	//void canCraft(LogisticsTransaction transaction);

	/**
	 * Lists the items this crafter can make, for displays such as the request
	 * screen, signs and the HUD. Crafters without a fixed set of outputs (ie,
	 * AE crafting system, fuzzy crafting) may return an incomplete list, so to
	 * check a specific item use {@link ICraft#canCraft} instead.
	 */
	List<ItemIdentifierStack> getCraftedItems();
}
