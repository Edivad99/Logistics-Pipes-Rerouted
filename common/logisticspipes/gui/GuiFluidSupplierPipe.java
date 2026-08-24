/**
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import logisticspipes.LPConstants;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.FluidSupplierMode;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiFluidSupplierPipe extends LogisticsBaseGuiScreen {

	private static final String PREFIX = "gui.fluidsupplier.";

	private PipeItemsFluidSupplier logic;

	public GuiFluidSupplierPipe(Container playerInventory, Container dummyInventory, PipeItemsFluidSupplier logic) {
		super(buildDummy(playerInventory, dummyInventory, logic));

		this.logic = logic;
		imageWidth = 194;
		imageHeight = 186;
	}
	private static DummyContainer buildDummy(Container playerInventory, Container dummyInventory, PipeItemsFluidSupplier logic) {
		DummyContainer dummy = new DummyContainer(playerInventory, dummyInventory);
		dummy.addNormalSlotsForPlayerInventory(18, 97);

		int xOffset = 72;
		int yOffset = 18;

		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 3; column++) {
				dummy.addDummySlot(column + row * 3, xOffset + column * 18, yOffset + row * 18);
			}
		}
		return dummy;
	}


	@Override
	protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "TargetInv"), imageWidth / 2 - minecraft.font.width(TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "TargetInv")) / 2, 6, 0xFF404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "Inventory"), 18, imageHeight - 102, 0xFF404040, false);
		guiGraphics.drawString(minecraft.font, TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "Partialrequests") + ":", imageWidth - 140, imageHeight - 112, 0xFF404040, false);
	}

	protected static final Identifier SUPPLIER = LPConstants.rl("textures/gui/supplier.png");

	@Override
	protected void renderBg(GuiGraphics guiGraphics, float f, int x, int y) {
		// texture: GuiFluidSupplierPipe.SUPPLIER
		int j = leftPos;
		int k = topPos;
		guiGraphics.blit(RenderPipelines.GUI_TEXTURED, GuiFluidSupplierPipe.SUPPLIER, j, k, 0.0f, 0.0f, imageWidth, imageHeight, 256, 256);
	}

	@Override
	public void init() {
		super.init();
		logisticspipes.utils.gui.SmallGuiButton partialsBtn = new logisticspipes.utils.gui.SmallGuiButton(0, width / 2 + 45, height / 2 - 25, 30, 20, logic.isRequestingPartials() ? TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "Yes") : TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "No"));
		partialsBtn.setPressListener(b -> {
			logic.setRequestingPartials(!logic.isRequestingPartials());
			b.setMessage(Component.literal(logic.isRequestingPartials() ? TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "Yes") : TextUtil.translate(GuiFluidSupplierPipe.PREFIX + "No")));
			MainProxy.sendPacketToServer(PacketHandler.getPacket(FluidSupplierMode.class).putInt((logic.isRequestingPartials() ? 1 : 0)).setPosX(logic.getX()).setPosY(logic.getY()).setPosZ(logic.getZ()));
		});
		addRenderableWidget(partialsBtn);
	}

	@Override
	public void onClose() {
		super.onClose();
	}
}
