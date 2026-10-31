/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.request.promise;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IProvide;
import logisticspipes.interfaces.routing.IProvideFluids;
import logisticspipes.request.resources.FluidResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.routing.order.IOrderInfoProvider;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifier;

public class FluidPromise implements IPromise {

	public FluidIdentifier liquid;
	public int amount;
	public IProvideFluids sender;
	public ResourceType type;

	@Override
	public FluidPromise copy() {
		FluidPromise result = new FluidPromise();
		result.liquid = liquid;
		result.amount = amount;
		result.sender = sender;
		result.type = type;
		return result;
	}

	@Override
	public boolean matches(IResource requestType) {
		if (requestType instanceof FluidResource) {
			FluidResource fluid = (FluidResource) requestType;
			return fluid.getFluid().equals(liquid);
		}
		return false;
	}

	@Override
	public int getAmount() {
		return amount;
	}

	@Override
	public IExtraPromise split(int more) {
		amount -= more;
		return new ExtraFluidPromise(liquid, more, sender, false);
	}

	@Override
	public IProvide getProvider() {
		return sender;
	}

	@Override
	public ItemIdentifier getItemType() {
		return liquid.getItemIdentifier();
	}

	@Override
	public @Nullable ResourceType getType() {
		return type;
	}

	@Override
	public @Nullable IOrderInfoProvider fullFill(IResource requestType, @Nullable IAdditionalTargetInformation info) {
		return sender.fullFill(this, ((FluidResource) requestType).getTarget(), type, info);
	}
}
