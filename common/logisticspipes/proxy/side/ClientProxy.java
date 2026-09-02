package logisticspipes.proxy.side;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.gui.popup.SelectItemOutOfList;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.to_server.gui.DummySlotClickMessage;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ClientProxy implements IProxy {

    @Override
	public String getSide() {
		return "Client";
	}

    @Override
	public String getName(ItemIdentifier item) {
		return item.getFriendlyName();
	}

	@Override
	public void updateNames(ItemIdentifier item, String name) {}

    @Override
	public void sendNameUpdateRequest(Player player) {}

	@Override
    @Nullable
	public LogisticsTileGenericPipe getPipeInDimensionAt(Identifier dimension, int x, int y, int z, Player player) {
		Level level = Minecraft.getInstance().level;
		if (level == null) {
            return null;
        }
		if (!level.dimension().identifier().equals(dimension)) {
            return null;
        }
		return getPipe(level, x, y, z);
	}

    @Nullable
	private static LogisticsTileGenericPipe getPipe(@Nullable Level level, int x, int y, int z) {
		if (level == null) {
            return null;
        }
		BlockPos pos = new BlockPos(x, y, z);
		// isLoaded before anything that reads the block: getBlockState and getBlockEntity both
		// go through getChunkAt, which loads -- or generates -- an absent chunk, and these
		// coordinates were sent by the server.
		if (!level.isLoaded(pos) || level.isEmptyBlock(pos)) {
            return null;
        }
		return level.getBlockEntity(pos) instanceof LogisticsTileGenericPipe be ? be : null;
	}

	@Override
	public void addLogisticsPipesOverride(Object par1IIconRegister, int index, String override1, String override2, boolean flag) {
		// override2 == "NewPipeTexture" means this call targets the new-pipe index space
		// (newTextureIndex, separate from LPpipeIconProvider's normal index).
		// flag == false selects the pre-generated base+overlay composite from overlay_gen/
		// (powered / unpowered / un-overlayed variants), exactly as LP1 did.
		if ("NewPipeTexture".equals(override2)) {
			logisticspipes.textures.TextureRegistrar.recordNew(index, override1);
		} else if (flag) {
			logisticspipes.textures.TextureRegistrar.record(index, override1);
		} else {
			logisticspipes.textures.TextureRegistrar.recordOverlay(index, override1, override2);
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
	}

    @Override
	public @Nullable LogisticsModule getModuleFromGui() {
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
	public void openFluidSelectGui(final int slotId) {
		if (Minecraft.getInstance().screen instanceof LogisticsBaseGuiScreen) {
			final List<ItemIdentifierStack> list = new ArrayList<>();
			for (FluidIdentifier fluid : FluidIdentifier.all()) {
				if (fluid == null) {
					continue;
				}
				list.add(fluid.getItemIdentifier().makeStack(1));
			}
			SelectItemOutOfList subGui = new SelectItemOutOfList(list, slot -> {
				if (slot == -1) {
					return;
				}
				ClientPacketDistributor.sendToServer(
						new DummySlotClickMessage(slotId, list.get(slot).makeNormalStack(), 0));
			});
			LogisticsBaseGuiScreen gui = (LogisticsBaseGuiScreen) Minecraft.getInstance().screen;
			if (!gui.hasSubGui()) {
				gui.setSubGui(subGui);
			} else {
				SubGuiScreen nextGui = gui.getSubGui();
				while (nextGui.hasSubGui()) {
					nextGui = nextGui.getSubGui();
				}
				nextGui.setSubGui(subGui);
			}
		} else {
			throw new UnsupportedOperationException(String.valueOf(Minecraft.getInstance().screen));
		}
	}

}
