package logisticspipes.proxy.side;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.client.gui.popup.SelectItemOutOfList;
import logisticspipes.network.to_server.gui.DummySlotClickMessage;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.interfaces.IProxy;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.client.gui.screen.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifierStack;

public class ClientProxy implements IProxy {

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
	public boolean checkSinglePlayerOwner(String commandSenderName) {
		var server = Minecraft.getInstance().getSingleplayerServer();
		return server != null && !server.isPublished();
	}

	@Override
	public void openFluidSelectGui(final int slotId) {
		if (Minecraft.getInstance().screen instanceof LogisticsBaseGuiScreen<?> gui) {
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
