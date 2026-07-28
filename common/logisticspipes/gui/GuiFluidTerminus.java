
package logisticspipes.gui;

import logisticspipes.network.packets.pipe.PipePropertiesUpdate;
import logisticspipes.pipes.PipeFluidTerminus;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.item.ItemIdentifierInventory;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import network.rs485.logisticspipes.property.ItemIdentifierInventoryProperty;
import network.rs485.logisticspipes.property.layer.PropertyLayer;
import network.rs485.logisticspipes.property.layer.PropertyOverlay;

public class GuiFluidTerminus extends LogisticsBaseGuiScreen {

	private final PropertyLayer propertyLayer;
	private final BlockPos pipePosition;
	private final PropertyOverlay<ItemIdentifierInventory, ItemIdentifierInventoryProperty> sinkInventoryOverlay;

	public GuiFluidTerminus(Player player, PipeFluidTerminus pipe) {
		super(buildDummy(player, pipe));

		pipePosition = pipe.getPos();
		propertyLayer = new PropertyLayer(pipe.getProperties());
		sinkInventoryOverlay = propertyLayer.overlay(pipe.getSinkInv());

		imageWidth = 180;
		imageHeight = 130;
	}
	private static DummyContainer buildDummy(Player player, PipeFluidTerminus pipe) {
		// propertyLayer is not yet available during static construction; use pipe.getSinkInv() directly
		DummyContainer dummy = new DummyContainer(player.getInventory(), pipe.getSinkInv());
		// Pipe slots
		for (int pipeSlot = 0; pipeSlot < pipe.getSinkInv().getContainerSize(); ++pipeSlot) {
			dummy.addFluidSlot(pipeSlot, 10 + pipeSlot * 18, 19);
		}
		dummy.addNormalSlotsForPlayerInventory(10, 45);
		return dummy;
	}


	@Override
	public void onClose() {
		super.onClose();
		propertyLayer.unregister();
		if (this.minecraft.player != null && !propertyLayer.getProperties().isEmpty()) {
			// send update to server, when there are changed properties
			MainProxy.sendPacketToServer(
					PipePropertiesUpdate.fromPropertyHolder(propertyLayer, this.minecraft.level.registryAccess()).setBlockPos(pipePosition));
		}
	}

	@Override
	public void init() {
		super.init();
	}

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float var1, int var2, int var3) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, topPos + 45);
		for (int i = 0; i < 9; i++) {
			LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 9 + i * 18, topPos + 18);
		}
	}

	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int par1, int par2) {
		super.renderLabels(guiGraphics, par1, par2);
		guiGraphics.drawString(minecraft.font, "Fluid Terminus", 10, 8, 0x404040, false);
	}
}
