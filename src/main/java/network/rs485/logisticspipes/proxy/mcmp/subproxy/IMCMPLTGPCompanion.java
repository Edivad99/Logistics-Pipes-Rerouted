/*
 * Copyright (c) 2015  RS485
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public License 1.0.1.
 */

package network.rs485.logisticspipes.proxy.mcmp.subproxy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

// TODO: MCMP not ported to 1.20.1. Capability<T> API removed from NeoForge.
// Remove hasCapability/getCapability methods; re-implement if MCMP is revived.
public interface IMCMPLTGPCompanion {

	CompoundTag getUpdateTag();

	void handleUpdateTag(CompoundTag tag);

	BlockEntity getMCMPBlockEntity();

	void update();
}
