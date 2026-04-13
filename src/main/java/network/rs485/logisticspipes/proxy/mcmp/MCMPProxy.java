package network.rs485.logisticspipes.proxy.mcmp;

import java.util.List;
import javax.annotation.Nonnull;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import network.rs485.logisticspipes.proxy.mcmp.subproxy.IMCMPBlockAccess;
import network.rs485.logisticspipes.proxy.mcmp.subproxy.IMCMPLTGPCompanion;

// TODO: MCMPProxy — MCMultiPart not ported to 1.20.1; all methods stubbed
public class MCMPProxy implements IMCMPProxy {

    @Override
    public IMCMPLTGPCompanion createMCMPCompanionFor(LogisticsTileGenericPipe pipe) {
        return null;
    }

    @Override
    public IMCMPBlockAccess createMCMPBlockAccess() {
        return null;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void addQuads(@Nonnull List<BakedQuad> list, BlockState state, Direction side, long rand) {
        // TODO: MCMP not ported
    }

    @Override
    public void registerTileEntities() {
        // TODO: MCMP not ported
    }

    @Override
    public boolean checkIntersectionWith(LogisticsTileGenericPipe logisticsTileGenericPipe, AABB aabb) {
        return false;
    }

    @Override
    public boolean hasParts(LogisticsTileGenericPipe pipeTile) {
        return false;
    }

    @Override
    @OnlyIn(Dist.CLIENT)
    public void renderTileEntitySpecialRenderer(LogisticsTileGenericPipe tileentity, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        // TODO: MCMP not ported
    }
}
