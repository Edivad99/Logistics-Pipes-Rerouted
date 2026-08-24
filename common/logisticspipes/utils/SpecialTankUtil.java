package logisticspipes.utils;

import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

import logisticspipes.interfaces.ISpecialTankAccessHandler;
import logisticspipes.interfaces.ISpecialTankUtil;

public class SpecialTankUtil extends TankUtil implements ISpecialTankUtil {

	private BlockEntity tile;
	private ISpecialTankAccessHandler handler;

	public SpecialTankUtil(ResourceHandler<FluidResource> fluid, BlockEntity tile, ISpecialTankAccessHandler handler) {
		super(fluid);
		this.tile = tile;
		this.handler = handler;
	}

	@Override
	public BlockEntity getTileEntity() {
		return tile;
	}

	@Override
	public ISpecialTankAccessHandler getSpecialHandler() {
		return handler;
	}
}
