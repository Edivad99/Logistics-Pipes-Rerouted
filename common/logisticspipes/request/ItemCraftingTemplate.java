/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.request;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.request.order.IOrderInfoProvider.ResourceType;
import logisticspipes.request.promise.ExtraItemPromise;
import logisticspipes.request.promise.IExtraPromise;
import logisticspipes.request.promise.ItemPromise;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ItemResource;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ItemCraftingTemplate implements IReqCraftingTemplate {

	protected ItemIdentifierStack result;
	protected ICraftItems crafter;

	protected ArrayList<Requirement> required = new ArrayList<>(9);

	protected ArrayList<ItemIdentifierStack> byproduct = new ArrayList<>(9);

	private final int priority;

	public ItemCraftingTemplate(ItemIdentifierStack result, ICraftItems crafter, int priority) {
		this.result = result;
		this.crafter = crafter;
		this.priority = priority;
	}

	public void addRequirement(IResource requirement, @Nullable IAdditionalTargetInformation info) {
		required.add(new Requirement(requirement, info));
	}

	public void addByproduct(ItemIdentifierStack stack) {
		for (ItemIdentifierStack i : byproduct) {
			if (i.getItem().equals(stack.getItem())) {
				i.setStackSize(i.getStackSize() + stack.getStackSize());
				return;
			}
		}
		byproduct.add(stack);
	}

	@Override
	public ItemPromise generatePromise(int nResultSets) {
		return new ItemPromise(result.getItem(), result.getStackSize() * nResultSets, crafter, ResourceType.CRAFTING);
	}

	//TODO: refactor so that other classes don't reach through the template to the crafter.
	// needed to get the crafter todo, in order to sort
	@Override
	public ICraftItems getCrafter() {
		return crafter;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	@Override
	public int compareTo(ICraftingTemplate o) {
		int c = o.comparePriority(priority);
		if (c == 0) {
			c = o.compareStack(result);
		}
		if (c == 0) {
			c = o.compareCrafter(crafter);
		}
		return c;
	}

	@Override
	public int comparePriority(int priority) {
		return priority - this.priority;
	}

	@Override
	public int compareStack(ItemIdentifierStack stack) {
		return stack.compareTo(this.result);
	}

	@Override
	public int compareCrafter(ICraftItems crafter) {
		return crafter.compareTo(this.crafter);
	}

	@Override
	public boolean canCraft(IResource type) {
		return type.matches(result.getItem(), IResource.MatchSettings.NORMAL);
	}

	@Override
	public int getResultStackSize() {
		return result.getStackSize();
	}

	@Override
	public IResource getResultItem() {
		return new ItemResource(result, null);
	}

	@Override
	public List<IExtraPromise> getByproducts(int workSets) {
		return byproduct.stream()
				.map(stack -> new ExtraItemPromise(stack.getItem(), stack.getStackSize() * workSets, getCrafter(), false))
				.collect(Collectors.toList());
	}

	@Override
	public List<Requirement> getComponents(int nCraftingSetsNeeded) {
		List<Requirement> stacks = new ArrayList<>(required.size());

		// for each thing needed to satisfy this promise
		for (Requirement requirement : required) {
			stacks.add(new Requirement(requirement.resource().clone(nCraftingSetsNeeded), requirement.info()));
		}
		return stacks;
	}
}
