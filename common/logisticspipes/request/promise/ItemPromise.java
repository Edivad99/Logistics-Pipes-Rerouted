/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.request.promise;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IProvide;
import logisticspipes.interfaces.routing.IProvideItems;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.request.order.IOrderInfoProvider;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.resources.DictResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ItemResource;
import logisticspipes.utils.item.ItemIdentifier;

public class ItemPromise implements IPromise {

	public ItemIdentifier item;
	public int numberOfItems;
	public IProvideItems sender;
	public @Nullable ResourceType type;

	public ItemPromise(ItemIdentifier item, int numberOfItems, IProvideItems sender, @Nullable ResourceType type) {
		this.item = item;
		this.numberOfItems = numberOfItems;
		this.sender = sender;
		this.type = type;
	}

	@Override
	public ItemPromise copy() {
		return new ItemPromise(item, numberOfItems, sender, type);
	}

	@Override
	public boolean matches(IResource requestType) {
		return requestType.matches(item, IResource.MatchSettings.NORMAL);
	}

	@Override
	public int getAmount() {
		return numberOfItems;
	}

	@Override
	public IExtraPromise split(int more) {
		numberOfItems -= more;
		return new ExtraItemPromise(getItemType(), more, sender, false);
	}

	@Override
	public IProvide getProvider() {
		return sender;
	}

	@Override
	public ItemIdentifier getItemType() {
		return item;
	}

	@Override
	public @Nullable ResourceType getType() {
		return type;
	}

	@Override
	public @Nullable IOrderInfoProvider fullFill(IResource requestType, @Nullable IAdditionalTargetInformation info) {
		IRequestItems destination;
		if (requestType instanceof ItemResource itemResource) {
			destination = itemResource.getTarget();
		} else if (requestType instanceof DictResource dictResource) {
			destination = dictResource.getTarget();
		} else {
			throw new UnsupportedOperationException();
		}
		return sender.fullFill(this, Objects.requireNonNull(destination, "destination"), info);
	}
}
