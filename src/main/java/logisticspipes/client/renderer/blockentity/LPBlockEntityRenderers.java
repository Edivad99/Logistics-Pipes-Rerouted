package logisticspipes.client.renderer.blockentity;

import logisticspipes.renderer.LogisticsRenderPipe;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class LPBlockEntityRenderers {

  public static void register(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(LPBlockEntityTypes.PIPE.get(),
        LogisticsRenderPipe::new);
    // LP solid blocks: shared BER draws the OBJ body + cover plates with the per-type sprite.
    event.registerBlockEntityRenderer(LPBlockEntityTypes.POWER_JUNCTION.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.POWER_PROVIDER_RF.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.SECURITY_STATION.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.CRAFTING_TABLE.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.STATISTICS_TABLE.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.PROGRAM_COMPILER.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.FRAME.get(),
        LogisticsSolidBlockRenderer::new);
  }
}
