/*
 * Copyright (c) 2015  RS485
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public License 1.0.1.
 */

package network.rs485.logisticspipes.proxy.mcmp.subproxy;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;

// TODO: MCMP not ported to 1.20.1. Capability<T> API removed from NeoForge.
// This class was MCMultiPart integration — dead until MCMP 1.20.1 is available.
public class MCMPLTGPCompanion implements IMCMPLTGPCompanion {

	@Override
	public CompoundTag getUpdateTag() { return new CompoundTag(); }

	@Override
	public void handleUpdateTag(CompoundTag tag) {}

	@Override
	public BlockEntity getMCMPBlockEntity() { return null; }

	@Override
	public void update() {}
}
