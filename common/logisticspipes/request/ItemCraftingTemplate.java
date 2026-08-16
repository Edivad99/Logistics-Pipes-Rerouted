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

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.ICraftItems;
import logisticspipes.request.resources.IResource;
import logisticspipes.request.resources.ItemResource;
import logisticspipes.routing.LogisticsExtraPromise;
import logisticspipes.routing.LogisticsPromise;
import logisticspipes.routing.order.IOrderInfoProvider.ResourceType;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;

public class ItemCraftingTemplate implements IReqCraftingTemplate {

	protected ItemIdentifierStack result;
	protected ICraftItems crafter;

	protected ArrayList<Pair<IResource, IAdditionalTargetInformation>> required = new ArrayList<>(9);

	protected ArrayList<ItemIdentifierStack> byproduct = new ArrayList<>(9);

	private final int priority;

	public ItemCraftingTemplate(ItemIdentifierStack result, ICraftItems crafter, int priority) {
		this.result = result;
		this.crafter = crafter;
		this.priority = priority;
	}

	public void addRequirement(IResource requirement, IAdditionalTargetInformation info) {
		required.add(new Pair<>(requirement, info));
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
	public LogisticsPromise generatePromise(int nResultSets) {
		return new LogisticsPromise(result.getItem(), result.getStackSize() * nResultSets, crafter, ResourceType.CRAFTING);
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
				.map(stack -> new LogisticsExtraPromise(stack.getItem(), stack.getStackSize() * workSets, getCrafter(), false))
				.collect(Collectors.toList());
	}

	@Override
	public List<Pair<IResource, IAdditionalTargetInformation>> getComponents(int nCraftingSetsNeeded) {
		List<Pair<IResource, IAdditionalTargetInformation>> stacks = new ArrayList<>(required.size());

		// for each thing needed to satisfy this promise
		for (Pair<IResource, IAdditionalTargetInformation> stack : required) {
			Pair<IResource, IAdditionalTargetInformation> pair = new Pair<>(stack.getValue1()
					.clone(nCraftingSetsNeeded), stack.getValue2());
			stacks.add(pair);
		}
		return stacks;
	}
}
