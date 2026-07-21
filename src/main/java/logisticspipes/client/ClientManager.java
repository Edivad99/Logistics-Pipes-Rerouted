package logisticspipes.client;

import logisticspipes.LogisticsPipes;
import logisticspipes.client.particle.SparkParticle;
import logisticspipes.client.renderer.blockentity.LPBlockEntityRenderers;
import logisticspipes.particle.LPParticleTypes;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.renderer.FluidContainerRenderer;
import logisticspipes.renderer.newpipe.LogisticsNewRenderPipe;
import logisticspipes.renderer.newpipe.LogisticsNewSolidBlockWorldRenderer;
import logisticspipes.renderer.newpipe.tube.CurveTubeRenderer;
import logisticspipes.renderer.newpipe.tube.GainTubeRenderer;
import logisticspipes.renderer.newpipe.tube.LineTubeRenderer;
import logisticspipes.renderer.newpipe.tube.SCurveTubeRenderer;
import logisticspipes.renderer.newpipe.tube.SpeedupTubeRenderer;
import logisticspipes.textures.TextureRegistrar;
import logisticspipes.ticks.ClientPacketBufferHandlerThread;
import logisticspipes.ticks.RenderTickHandler;
import logisticspipes.world.level.block.LPBlocks;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterParticleProvidersEvent;
import net.neoforged.neoforge.common.NeoForge;
import network.rs485.logisticspipes.gui.WidgetScreenHudSuppressor;
import network.rs485.logisticspipes.gui.font.LPFontRenderer;

public class ClientManager {

  public static void init(IEventBus modEventBus) {
    modEventBus.addListener(ClientManager::handleClientSetup);
    modEventBus.addListener(ClientManager::handleRegisterRenderers);
    modEventBus.addListener(ClientManager::handleParticleRegistration);

    modEventBus.register(TextureRegistrar.class);
    modEventBus.register(FluidContainerRenderer.class);

    //NeoForge.EVENT_BUS.register(ClientManager.class);

    NeoForge.EVENT_BUS.register(new RenderTickHandler());
    NeoForge.EVENT_BUS.register(WidgetScreenHudSuppressor.INSTANCE);
    SimpleServiceLocator.setClientPacketBufferHandlerThread(new ClientPacketBufferHandlerThread());
    LPFontRenderer.Factory.asyncPreload();
  }

  private ClientManager() {
  }

  // Mod events
  private static void handleClientSetup(FMLClientSetupEvent event) {
    // Texture atlas sprites and item/block models are supplied declaratively via
    // JSON in assets/logisticspipes/models/** in 1.20.1 — no code registration.
    // The legacy MainProxy.proxy.registerTextures() / registerModels() and
    // LogisticsPipes.textures.registerBlockIcons(...) paths are deferred to the
    // renderer rewrite; they are intentionally not called here.
    // BlockEntityRenderer for the pipe BE is registered via
    // EntityRenderersEvent.RegisterRenderers (see registerRenderers below).
    event.enqueueWork(() -> {
      ItemBlockRenderTypes.setRenderLayer(
          LPBlocks.PIPE.get(),
          RenderType.cutout());
    });

    // Fluid container "filled" model predicate (client-only class, stays in the guard).
    FluidContainerRenderer.registerItemProperties();

    // Preload all render models so they don't get loaded (and crash) on concurrent
    // render-thread class loading. Each loader is wrapped in its own try/catch so a
    // failure in one OBJ file / group lookup doesn't halt init. These reference
    // client-only renderer classes, so they MUST stay inside this Dist.CLIENT guard —
    // the method-reference bootstraps below would otherwise link client classes on a
    // dedicated server.
    safeLoadModels("LogisticsNewRenderPipe", LogisticsNewRenderPipe::loadModels);
    safeLoadModels("LogisticsNewSolidBlockWorldRenderer",
        LogisticsNewSolidBlockWorldRenderer::loadModels);
    safeLoadModels("CurveTubeRenderer", CurveTubeRenderer::loadModels);
    safeLoadModels("GainTubeRenderer", GainTubeRenderer::loadModels);
    safeLoadModels("LineTubeRenderer", LineTubeRenderer::loadModels);
    safeLoadModels("SpeedupTubeRenderer", SpeedupTubeRenderer::loadModels);
    safeLoadModels("SCurveTubeRenderer", SCurveTubeRenderer::loadModels);
  }

  private static void handleRegisterRenderers(EntityRenderersEvent.RegisterRenderers event) {
    LPBlockEntityRenderers.register(event);
  }

    private static void handleParticleRegistration(RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(LPParticleTypes.SPARKLE.get(), SparkParticle.Provider::new);
    }

  private static void safeLoadModels(String name, Runnable loader) {
    try {
      loader.run();
    } catch (Throwable t) {
      LogisticsPipes.LOG.warn(
          "[CCL-replacement] {} failed to load models — pipe visuals will be incomplete: {}", name,
          t.toString());
    }
  }
}
