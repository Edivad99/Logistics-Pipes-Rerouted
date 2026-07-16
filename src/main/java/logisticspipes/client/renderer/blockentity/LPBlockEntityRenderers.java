package logisticspipes.client.renderer.blockentity;

import logisticspipes.renderer.LogisticsRenderPipe;
import logisticspipes.renderer.LogisticsSolidBlockRenderer;
import logisticspipes.world.level.block.entity.LPBlockEntityTypes;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

public class LPBlockEntityRenderers {

  public static void register(EntityRenderersEvent.RegisterRenderers event) {
    event.registerBlockEntityRenderer(LPBlockEntityTypes.BE_PIPE.get(),
        LogisticsRenderPipe::new);
    // LP solid blocks: shared BER draws the OBJ body + cover plates with the per-type sprite.
    event.registerBlockEntityRenderer(LPBlockEntityTypes.BE_POWER_JUNCTION.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.BE_POWER_PROVIDER_RF.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.BE_POWER_PROVIDER_EU.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.BE_SECURITY_STATION.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.BE_CRAFTING_TABLE.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.BE_STATISTICS_TABLE.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.BE_PROGRAM_COMPILER.get(),
        LogisticsSolidBlockRenderer::new);
    event.registerBlockEntityRenderer(LPBlockEntityTypes.BE_FRAME.get(),
        LogisticsSolidBlockRenderer::new);
  }
}
