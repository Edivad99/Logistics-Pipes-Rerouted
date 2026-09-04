/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.client.gui.screen;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.to_server.module.OpenChassisModuleGuiMessage;
import logisticspipes.network.to_server.pipe.OpenUpgradeConfigMessage;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.string.StringUtils;
import logisticspipes.world.inventory.ChassisMenu;
import logisticspipes.world.item.ItemModule;

public class ChassisPipeScreen extends LogisticsBaseGuiScreen<ChassisMenu> {

	private final PipeLogisticsChassis chassisPipe;
	private final Container moduleInventory;
	private final List<SmallGuiButton> moduleConfigButtons = new LinkedList<>();

	private final List<Slot> upgradeSlots;
	private final AbstractButton[] upgradeConfig;

	private final boolean hasUpgradeModuleUpgrade;

	public ChassisPipeScreen(ChassisMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, 162 + 26, 76 + 14 + 20 * menu.getPipe().getChassisSize(), 0, 0);
		chassisPipe = menu.getPipe();
		moduleInventory = menu.getModuleInventory();
		hasUpgradeModuleUpgrade = menu.hasUpgradeModuleUpgrade();
		upgradeSlots = menu.getUpgradeSlots();
		upgradeConfig = new AbstractButton[chassisPipe.getChassisSize() * 2];
	}

	@Override
	public void init() {
		super.init();

		int left = width / 2 - panelWidth / 2;
		int top = height / 2 - panelHeight / 2;

		moduleConfigButtons.clear();
		for (int i = 0; i < chassisPipe.getChassisSize(); i++) {
			final int slot = i;
			SmallGuiButton cfgBtn = new SmallGuiButton(i, left + 5, top + 12 + 20 * i, 10, 10, "!");
			cfgBtn.setPressListener(b -> {
				if (chassisPipe.getSubModule(slot) != null) {
					ClientPacketDistributor.sendToServer(
							new OpenChassisModuleGuiMessage(chassisPipe.getPos(), slot));
				}
			});
			moduleConfigButtons.add(addRenderableWidget(cfgBtn));
			if (moduleInventory == null) {
				continue;
			}
			updateModuleConfigButtonVisibility(i);

			if (hasUpgradeModuleUpgrade) {
				final int idxA = i * 2;
				SmallGuiButton upA = new SmallGuiButton(100 + i, leftPos + 134, topPos + 12 + i * 20, 10, 10, "!");
				upA.setPressListener(b -> ClientPacketDistributor.sendToServer(new OpenUpgradeConfigMessage(upgradeSlots.get(idxA).index)));
				upgradeConfig[idxA] = addRenderableWidget(upA);
				upgradeConfig[idxA].visible = chassisPipe.getModuleUpgradeManager(i).hasGuiUpgrade(0);
				final int idxB = i * 2 + 1;
				SmallGuiButton upB = new SmallGuiButton(120 + i, leftPos + 182, topPos + 12 + i * 20, 10, 10, "!");
				upB.setPressListener(b -> ClientPacketDistributor.sendToServer(new OpenUpgradeConfigMessage(upgradeSlots.get(idxB).index)));
				upgradeConfig[idxB] = addRenderableWidget(upB);
				upgradeConfig[idxB].visible = chassisPipe.getModuleUpgradeManager(i).hasGuiUpgrade(1);
			}
		}
	}

	private void updateModuleConfigButtonVisibility(int slot) {
		ItemStack module = moduleInventory.getItem(slot);
		LogisticsModule subModule = chassisPipe.getSubModule(slot);
		if (module.isEmpty() || subModule == null) {
			moduleConfigButtons.get(slot).visible = false;
		} else {
			moduleConfigButtons.get(slot).visible = subModule.hasGui();
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		super.extractLabels(guiGraphics, mouseX, mouseY);
		for (int i = 0; i < chassisPipe.getChassisSize(); i++) {
			updateModuleConfigButtonVisibility(i);
		}
		if (hasUpgradeModuleUpgrade) {
			for (int i = 0; i < upgradeConfig.length; i++) {
				upgradeConfig[i].visible = chassisPipe.getModuleUpgradeManager(i / 2).hasGuiUpgrade(i % 2);
			}
		}
		for (int i = 0; i < chassisPipe.getChassisSize(); i++)
			guiGraphics.text(minecraft.font, getModuleName(i), 40, 14 + 20 * i, 0xFF404040, false);
	}

	private String getModuleName(int slot) {
		if (moduleInventory == null) {
			return "";
		}
		if (moduleInventory.getItem(slot).isEmpty()) {
			return "";
		}
		if (!(moduleInventory.getItem(slot).getItem() instanceof ItemModule)) {
			return "";
		}
		String name = moduleInventory.getItem(slot).getHoverName().getString();
		if (!hasUpgradeModuleUpgrade) {
			return name;
		}
		return StringUtils.getWithMaxWidth(name, 100, font);
	}

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int x, int y, float f) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		for (int i = 0; i < chassisPipe.getChassisSize(); i++)
			LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 17, topPos + 8 + 20 * i);

		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 18, topPos + 9 + 20 * chassisPipe.getChassisSize());

		if (hasUpgradeModuleUpgrade) {
			for (int i = 0; i < chassisPipe.getChassisSize(); i++) {
				LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 144, topPos + 8 + i * 20);
				LPGuiGraphics.drawSlotBackground(guiGraphics, leftPos + 164, topPos + 8 + i * 20);
			}
		}
	}
}
