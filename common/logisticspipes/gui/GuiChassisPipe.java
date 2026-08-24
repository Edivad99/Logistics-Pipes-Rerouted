/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.gui;

import java.util.LinkedList;
import java.util.List;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import logisticspipes.LPConfigs;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.guis.pipe.ChassisGuiProvider;
import logisticspipes.network.packets.chassis.ChassisGUI;
import logisticspipes.network.packets.gui.GuiClosePacket;
import logisticspipes.network.packets.gui.OpenUpgradePacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.upgrades.ModuleUpgradeManager;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.string.StringUtils;
import logisticspipes.world.item.ItemModule;
import network.rs485.logisticspipes.module.Gui;

public class GuiChassisPipe extends LogisticsBaseGuiScreen {

	private final PipeLogisticsChassis chassisPipe;
	private final Container moduleInventory;
	//private final Screen _previousGui;
	private final List<SmallGuiButton> moduleConfigButtons = new LinkedList<>();

	private final Slot[] upgradeSlots = new Slot[2 * LPConfigs.CHASSIS_SLOTS_ARRAY[4]];
	private final AbstractButton[] upgradeConfig;

	private final boolean hasUpgradeModuleUpgrade;

	public GuiChassisPipe(Player player, PipeLogisticsChassis chassis, boolean hasUpgradeModuleUpgrade) { //, Screen previousGui) {
		super(buildDummy(player, chassis, hasUpgradeModuleUpgrade));
		chassisPipe = chassis;
		moduleInventory = chassis.getModuleInventory(player.registryAccess());
		//_previousGui = previousGui;
		this.hasUpgradeModuleUpgrade = hasUpgradeModuleUpgrade;

		int playerInventoryWidth = 162;
		int playerInventoryHeight = 76;

		panelWidth = playerInventoryWidth + 26;
		panelHeight = playerInventoryHeight + 14 + (20 * chassisPipe.getChassisSize());

        this.upgradeConfig = new AbstractButton[chassisPipe.getChassisSize() * 2];
    }

	private static DummyContainer buildDummy(Player player, PipeLogisticsChassis chassis, boolean hasUpgradeModuleUpgrade) {
		Container moduleInventory = chassis.getModuleInventory(player.registryAccess());
		DummyContainer dummy = new DummyContainer(player.getInventory(), moduleInventory);
		dummy.addNormalSlotsForPlayerInventory(19, 10 + 20 * chassis.getChassisSize());
		for (int i = 0; i < chassis.getChassisSize(); i++)
			dummy.addModuleSlot(i, moduleInventory, 18, 9 + 20 * i, chassis);

		if (hasUpgradeModuleUpgrade) {
			for (int i = 0; i < chassis.getChassisSize(); i++) {
				final int fI = i;
				ModuleUpgradeManager upgradeManager = chassis.getModuleUpgradeManager(i);
				dummy.addUpgradeSlot(0, upgradeManager, 0, 145, 9 + i * 20, itemStack -> ChassisGuiProvider.checkStack(itemStack, chassis, fI));
				dummy.addUpgradeSlot(1, upgradeManager, 1, 165, 9 + i * 20, itemStack -> ChassisGuiProvider.checkStack(itemStack, chassis, fI));
			}
		}
		return dummy;
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
				LogisticsModule module = chassisPipe.getSubModule(slot);
				if (module != null) {
					MainProxy.sendPacketToServer(PacketHandler.getPacket(ChassisGUI.class).setButtonID(slot)
							.setPosX(chassisPipe.getX()).setPosY(chassisPipe.getY()).setPosZ(chassisPipe.getZ()));
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
				upA.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(OpenUpgradePacket.class).setSlot(upgradeSlots[idxA])));
				upgradeConfig[idxA] = addRenderableWidget(upA);
				upgradeConfig[idxA].visible = chassisPipe.getModuleUpgradeManager(i).hasGuiUpgrade(0);
				final int idxB = i * 2 + 1;
				SmallGuiButton upB = new SmallGuiButton(120 + i, leftPos + 182, topPos + 12 + i * 20, 10, 10, "!");
				upB.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(OpenUpgradePacket.class).setSlot(upgradeSlots[idxB])));
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
			moduleConfigButtons.get(slot).visible = subModule instanceof Gui;
		}
	}

	@Override
	public void onClose() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(GuiClosePacket.class).setTilePos(chassisPipe.container));
		super.onClose();
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
