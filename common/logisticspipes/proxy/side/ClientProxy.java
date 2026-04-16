package logisticspipes.proxy.side;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.utils.item.ItemIdentifier;

@OnlyIn(Dist.CLIENT)
public class ClientProxy implements IProxy {

	int renderIndex = 0;

	@Override
	public String getSide() {
		return "Client";
	}

	@Override
	public Level getWorld() {
		return Minecraft.getInstance().level;
	}

	@Override
	public void registerTileEntities() {
		// BlockEntityRenderer registration is handled via EntityRenderersEvent.RegisterRenderers
		// in LogisticsPipes.registerRenderers() — nothing to do here except initialise the render list.
		SimpleServiceLocator.setRenderListHandler(new logisticspipes.renderer.newpipe.GLRenderListHandler());
	}

	@Override
	public Player getClientPlayer() {
		return Minecraft.getInstance().player;
	}

	@Override
	public void registerParticles() {
		// LP has its own particle pipeline — particles are spawned directly via
		// mc.particleEngine.add(...) from PipeFXRenderHandler and do not go through
		// the vanilla ParticleType registry, so RegisterParticleProvidersEvent is
		// not involved. Wire the color → provider map once at client init.
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.WhiteParticle,
			new logisticspipes.pipefxhandlers.providers.EntityWhiteSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.RedParticle,
			new logisticspipes.pipefxhandlers.providers.EntityRedSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.BlueParticle,
			new logisticspipes.pipefxhandlers.providers.EntityBlueSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.GreenParticle,
			new logisticspipes.pipefxhandlers.providers.EntityGreenSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.GoldParticle,
			new logisticspipes.pipefxhandlers.providers.EntityGoldSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.VioletParticle,
			new logisticspipes.pipefxhandlers.providers.EntityVioletSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.OrangeParticle,
			new logisticspipes.pipefxhandlers.providers.EntityOrangeSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.LightGreenParticle,
			new logisticspipes.pipefxhandlers.providers.EntityLightGreenSparkleFXProvider());
		logisticspipes.pipefxhandlers.PipeFXRenderHandler.registerParticleHandler(
			logisticspipes.pipefxhandlers.Particles.LightRedParticle,
			new logisticspipes.pipefxhandlers.providers.EntityLightRedSparkleFXProvider());
	}

	@Override
	public String getName(ItemIdentifier item) {
		return item.getFriendlyName();
	}

	@Override
	public void updateNames(ItemIdentifier item, String name) {}

	@Override
	public void tick() {}

	@Override
	public void sendNameUpdateRequest(Player player) {}

	@Override
	public LogisticsTileGenericPipe getPipeInDimensionAt(ResourceLocation dimension, int x, int y, int z, Player player) {
		Level level = Minecraft.getInstance().level;
		if (level == null) return null;
		if (!level.dimension().location().equals(dimension)) return null;
		return getPipe(level, x, y, z);
	}

	private static LogisticsTileGenericPipe getPipe(Level level, int x, int y, int z) {
		if (level == null) return null;
		BlockPos pos = new BlockPos(x, y, z);
		if (level.isEmptyBlock(pos)) return null;
		BlockEntity tile = level.getBlockEntity(pos);
		return tile instanceof LogisticsTileGenericPipe ? (LogisticsTileGenericPipe) tile : null;
	}

	@Override
	public void addLogisticsPipesOverride(Object par1IIconRegister, int index, String override1, String override2, boolean flag) {
		// Overlay compositing (override2) from 1.12.2 is deferred; record the base sprite only.
		// override2 == "NewPipeTexture" means this call targets LPnewPipeIconProvider's index
		// space (newTextureIndex, separate from LPpipeIconProvider's normal index).
		if ("NewPipeTexture".equals(override2)) {
			logisticspipes.textures.TextureRegistrar.recordNew(index, override1);
		} else {
			logisticspipes.textures.TextureRegistrar.record(index, override1);
		}
	}

	@Override
	public void sendBroadCast(String message) {
		var player = Minecraft.getInstance().player;
		if (player != null) {
			player.sendSystemMessage(Component.literal("[LP] Client: " + message));
		}
	}

	@Override
	public void tickServer() {}

	@Override
	public void tickClient() {
		MainProxy.addTick();
		SimpleServiceLocator.renderListHandler.tick();
	}

	@Override
	public void setIconProviderFromPipe(ItemLogisticsPipe item, CoreUnroutedPipe dummyPipe) {
		// TODO: IIconProvider → deferred to renderer migration
	}

	@Override
	public LogisticsModule getModuleFromGui() {
		var screen = Minecraft.getInstance().screen;
		if (screen instanceof logisticspipes.gui.modules.ModuleBaseGui g) return g.getModule();
		if (screen instanceof logisticspipes.gui.GuiCraftingPipe g)        return g.getCraftingModule();
		return null;
	}

	@Override
	public boolean checkSinglePlayerOwner(String commandSenderName) {
		var server = Minecraft.getInstance().getSingleplayerServer();
		return server != null && !server.isPublished();
	}

	@Override
	public void openFluidSelectGui(int slotId) {
		// TODO: fluid select GUI migration deferred to GUI system migration
	}

	@Override
	public void registerModels() {
		// Model registration migrated to ModelEvent.RegisterAdditional / baked-model system
	}

	@Override
	public void registerTextures() {
		// TextureAtlas sprite registration migrated to RegisterAtlasSpritesEvent
		renderIndex++;
	}

	@Override
	public void initModelLoader() {
		// TODO: IModelLoader → IUnbakedModel system in 1.20.1; deferred to renderer migration
	}

	@Override
	public int getRenderIndex() {
		return renderIndex;
	}
}
