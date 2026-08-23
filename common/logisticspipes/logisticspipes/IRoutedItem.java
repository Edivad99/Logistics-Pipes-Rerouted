/**
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.logisticspipes;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import java.util.List;
import java.util.UUID;
import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.routing.IRouter;
import logisticspipes.routing.ItemRoutingInformation;
import logisticspipes.routing.order.IDistanceTracker;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;

/**
 * This interface describes the actions that must be available on an item that
 * is considered routed
 */
public interface IRoutedItem {

	enum TransportMode {
		Unknown,
		Default,
		Passive,
		Active
	}

	int getDestination();

	UUID getDestinationUUID();

	void setDestination(int destination);

	void clearDestination();

	void setTransportMode(TransportMode transportMode);

	TransportMode getTransportMode();

	void setAdditionalTargetInformation(IAdditionalTargetInformation info);

	IAdditionalTargetInformation getAdditionalTargetInformation();

	void setDoNotBuffer(boolean doNotBuffer);

	boolean getDoNotBuffer();

	int getBufferCounter();

	void setBufferCounter(int counter);

	void setArrived(boolean flag);

	boolean getArrived();

	void addToJamList(IRouter router);

	List<Integer> getJamList();

	void checkIDFromUUID();

	ItemIdentifierStack getItemIdentifierStack();

	void deserialize(ValueInput input);

	void serialize(ValueOutput output);

	void setDistanceTracker(IDistanceTracker tracker);

	IDistanceTracker getDistanceTracker();

	ItemRoutingInformation getInfo();

	void split(int itemsToTake, Direction orientation);
}
