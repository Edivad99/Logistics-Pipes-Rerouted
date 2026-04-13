package logisticspipes.proxy.interfaces;

import java.util.List;

import net.minecraft.client.renderer.texture.TextureAtlas; // was TextureAtlas
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.Direction;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.td.subproxies.ITDPart;
import logisticspipes.renderer.newpipe.RenderEntry;

public interface ITDProxy {

	ITDPart getTDPart(LogisticsTileGenericPipe pipe);

	boolean isActive();

	void registerPipeInformationProvider();

	boolean isItemDuct(BlockEntity tile);

	@OnlyIn(Dist.CLIENT)
	void renderPipeConnections(LogisticsTileGenericPipe pipeTile, List<RenderEntry> renderList);

	@OnlyIn(Dist.CLIENT)
	void registerTextures(TextureAtlas iconRegister);

	boolean isBlockedSide(BlockEntity with, Direction opposite);
}
