package logisticspipes.gui.hud;
import net.minecraft.client.gui.GuiGraphics;

import java.io.IOException;


import net.minecraft.world.entity.player.Player;

import lombok.SneakyThrows;

import logisticspipes.LPItems;
import logisticspipes.hud.HUDConfig;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.hud.HUDSettingsPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.GuiCheckBox;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import javax.annotation.Nonnull;

public class GuiHUDSettings extends LogisticsBaseGuiScreen {

	private int slot;
	private Player player;

	public GuiHUDSettings(Player player, int slot) {
		super(buildDummy(player, slot), 180, 160, 0, 0);
		this.slot = slot;
		this.player = player;
	}
	private static DummyContainer buildDummy(Player player, int slot) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), null);
		dummy.addRestrictedHotbarForPlayerInventory(10, 134);
		dummy.addRestrictedArmorForPlayerInventory(10, 65);
		return dummy;
	}


	@Override
	@SneakyThrows(IOException.class)
	public void init() {
		super.init();
		if (!player.getInventory().getItem(slot).isEmpty()) {
			IHUDConfig config = new HUDConfig(player.getInventory().getItem(slot));
			addRenderableWidget(wire(new GuiCheckBox(0, leftPos + 30, topPos + 10, 12, 12, config.isChassisHUD())));
			addRenderableWidget(wire(new GuiCheckBox(1, leftPos + 30, topPos + 30, 12, 12, config.isHUDCrafting())));
			addRenderableWidget(wire(new GuiCheckBox(2, leftPos + 30, topPos + 50, 12, 12, config.isHUDInvSysCon())));
			addRenderableWidget(wire(new GuiCheckBox(3, leftPos + 30, topPos + 70, 12, 12, config.isHUDPowerLevel())));
			addRenderableWidget(wire(new GuiCheckBox(4, leftPos + 30, topPos + 90, 12, 12, config.isHUDProvider())));
			addRenderableWidget(wire(new GuiCheckBox(5, leftPos + 30, topPos + 110, 12, 12, config.isHUDSatellite())));
		} else {
			closeGui();
		}
	}

	private GuiCheckBox wire(GuiCheckBox cb) {
		cb.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(HUDSettingsPacket.class).setButtonId(b.id).setState(b.getState()).setSlot(slot)));
		return cb;
	}

	@Override
	protected void renderBg(@Nonnull GuiGraphics guiGraphics, float var1, int var2, int var3) {
		if (player.getInventory().getItem(slot).isEmpty() || player.getInventory().getItem(slot).getItem() != LPItems.hudGlasses.get()) {
			minecraft.player.closeContainer();
		}
		LPGuiGraphics.drawGuiBackGround(minecraft, leftPos, topPos, right, bottom, 0.0f, true);
		guiGraphics.drawString(minecraft.font, "HUD Chassis Pipe", leftPos + 50, topPos + 13, 0x4c4c4c);
		guiGraphics.drawString(minecraft.font, "HUD Crafting Pipe", leftPos + 50, topPos + 33, 0x4c4c4c);
		guiGraphics.drawString(minecraft.font, "HUD InvSysCon Pipe", leftPos + 50, topPos + 53, 0x4c4c4c);
		guiGraphics.drawString(minecraft.font, "HUD Power Junction", leftPos + 50, topPos + 73, 0x4c4c4c);
		guiGraphics.drawString(minecraft.font, "HUD Provider Pipe", leftPos + 50, topPos + 93, 0x4c4c4c);
		guiGraphics.drawString(minecraft.font, "HUD Satellite Pipe", leftPos + 50, topPos + 113, 0x4c4c4c);
		LPGuiGraphics.drawPlayerHotbarBackground(minecraft, leftPos + 10, topPos + 134);
		LPGuiGraphics.drawPlayerArmorBackground(minecraft, leftPos + 10, topPos + 65);
	}
}
