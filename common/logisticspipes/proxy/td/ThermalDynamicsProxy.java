package logisticspipes.proxy.td;
// TODO: ThermalDynamics not ported to 1.20.1 — stub

import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.interfaces.ITDProxy;
import logisticspipes.proxy.td.subproxies.ITDPart;
import logisticspipes.renderer.newpipe.RenderEntry;

public class ThermalDynamicsProxy implements ITDProxy {
    @Override public ITDPart getTDPart(LogisticsTileGenericPipe pipe) { return null; }
    @Override public boolean isActive() { return false; }
    @Override public void registerPipeInformationProvider() {}
    @Override public boolean isItemDuct(BlockEntity tile) { return false; }
    @Override @OnlyIn(Dist.CLIENT) public void renderPipeConnections(LogisticsTileGenericPipe pipeTile, List<RenderEntry> renderList) {}
    @Override @OnlyIn(Dist.CLIENT) public void registerTextures(TextureAtlas iconRegister) {}
    @Override public boolean isBlockedSide(BlockEntity with, Direction opposite) { return false; }
}
