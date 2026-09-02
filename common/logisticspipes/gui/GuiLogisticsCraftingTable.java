package logisticspipes.gui;

import java.util.Arrays;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.network.to_server.crafting.CycleCraftingRecipeMessage;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;
import logisticspipes.world.inventory.AutoCraftingMenu;

public class GuiLogisticsCraftingTable extends LogisticsBaseGuiScreen {

	public LogisticsCraftingTableBlockEntity crafter;

	private int fuzzyPanelSelection = -1;
	private int fuzzyPanelHover = -1;
	private int fuzzyPanelHoverTime = 0;

	private AbstractButton[] cycleButtons = new AbstractButton[2];

	public GuiLogisticsCraftingTable(AutoCraftingMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 176, 218, 0, 0);
		this.crafter = menu.getBlockEntity();
		menu.setScreenForJEI(this);
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton upBtn = new SmallGuiButton(0, leftPos + 144, topPos + 25, 15, 10, "/\\");
		upBtn.setPressListener(b -> ClientPacketDistributor.sendToServer(
				new CycleCraftingRecipeMessage(crafter.getBlockPos(), false)));
		(cycleButtons[0] = addRenderableWidget(upBtn)).visible = false;
		SmallGuiButton dnBtn = new SmallGuiButton(1, leftPos + 144, topPos + 37, 15, 10, "\\/");
		dnBtn.setPressListener(b -> ClientPacketDistributor.sendToServer(
				new CycleCraftingRecipeMessage(crafter.getBlockPos(), true)));
		(cycleButtons[1] = addRenderableWidget(dnBtn)).visible = false;
	}

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int iA, int jA, float fA) {
		for (AbstractButton cycleButton : cycleButtons) {
			cycleButton.visible = crafter.targetType != null;
		}
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 34 + x * 18, topPos + 9 + y * 18);
			}
		}
		LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 124, topPos + 27);
		for (int x = 0; x < 9; x++) {
			for (int y = 0; y < 2; y++) {
				LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 7 + x * 18, topPos + 79 + y * 18);
			}
		}
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 8, topPos + 135);

		ItemIdentifierStack[] items = new ItemIdentifierStack[9];
		for (int i = 0; i < 9; i++) {
			if (crafter.matrix.getIDStackInSlot(i) != null) {
				items[i] = crafter.matrix.getIDStackInSlot(i);
			}
		}

		ItemStackRenderer.renderItemIdentifierStackListIntoGui(guiGraphics, Arrays.asList(items), null, 0, leftPos + 8, topPos + 79, 9, 9, 18, 18, 0.0F, DisplayAmount.NEVER);

		for (int a = 0; a < 9; a++) {
			guiGraphics.fill(leftPos + 8 + (a * 18), topPos + 80, leftPos + 24 + (a * 18), topPos + 96, 0xc08b8b8b);
		}
	}

	private boolean isMouseInFuzzyPanel(int mx, int my) {
		if (fuzzyPanelSelection == -1) {
			return false;
		}
		int posX = -60;
		int posY = 0;
		return mx >= posX && my >= posY && mx <= posX + 60 && my <= posY + 52;
	}
}
