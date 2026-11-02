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
import logisticspipes.interfaces.routing.IProvideFluids;
import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.request.order.IOrderInfoProvider;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.resources.FluidResource;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifier;

public class FluidPromise implements IPromise {

    public final FluidIdentifier liquid;
    public int amount;
    public final IProvideFluids sender;
    public final @Nullable ResourceType type;

    public FluidPromise(FluidIdentifier liquid, int amount, IProvideFluids sender, @Nullable ResourceType type) {
        this.liquid = liquid;
        this.amount = amount;
        this.sender = sender;
        this.type = type;
    }

    @Override
    public FluidPromise copy() {
        return new FluidPromise(liquid, amount, sender, type);
    }

    @Override
    public boolean matches(IResource requestType) {
        return requestType instanceof FluidResource fluid && fluid.getFluid().equals(liquid);
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
        IRequestFluid destination = ((FluidResource) requestType).getTarget();
        return sender.fullFill(this, Objects.requireNonNull(destination, "destination"), type, info);
    }
}
