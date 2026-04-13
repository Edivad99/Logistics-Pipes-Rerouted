package logisticspipes.network.packets.hud;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.LPItems;
import logisticspipes.hud.HUDConfig;
import logisticspipes.interfaces.IHUDConfig;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class HUDSettingsPacket extends ModernPacket {

	@Getter
	@Setter
	private int buttonId;

	@Getter
	@Setter
	private boolean state;

	@Getter
	@Setter
	private int slot;

	public HUDSettingsPacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new HUDSettingsPacket(getId());
	}

	@Override
	public void processPacket(Player player) {
		final ItemStack equipment = player.getInventory().getItem(slot);
		if (equipment.getItem() != LPItems.hudGlasses.get()) return;
		IHUDConfig config = new HUDConfig(equipment);
		switch (buttonId) {
			case 0:
				config.setChassisHUD(state);
				if (config.isChassisHUD()) {
					player.sendSystemMessage(Component.translatable("lp.hud.config.chassie.enabled"));
				} else {
					player.sendSystemMessage(Component.translatable("lp.hud.config.chassie.disabled"));
				}
				break;
			case 1:
				config.setHUDCrafting(state);
				if (config.isHUDCrafting()) {
					player.sendSystemMessage(Component.translatable("lp.hud.config.crafting.enabled"));
				} else {
					player.sendSystemMessage(Component.translatable("lp.hud.config.crafting.disabled"));
				}
				break;
			case 2:
				config.setHUDInvSysCon(state);
				if (config.isHUDInvSysCon()) {
					player.sendSystemMessage(Component.translatable("lp.hud.config.invsyscon.enabled"));
				} else {
					player.sendSystemMessage(Component.translatable("lp.hud.config.invsyscon.disabled"));
				}
				break;
			case 3:
				config.setHUDPowerJunction(state);
				if (config.isHUDPowerLevel()) {
					player.sendSystemMessage(Component.translatable("lp.hud.config.powerjunction.enabled"));
				} else {
					player.sendSystemMessage(Component.translatable("lp.hud.config.powerjunction.disabled"));
				}
				break;
			case 4:
				config.setHUDProvider(state);
				if (config.isHUDProvider()) {
					player.sendSystemMessage(Component.translatable("lp.hud.config.provider.enabled"));
				} else {
					player.sendSystemMessage(Component.translatable("lp.hud.config.provider.disabled"));
				}
				break;
			case 5:
				config.setHUDSatellite(state);
				if (config.isHUDSatellite()) {
					player.sendSystemMessage(Component.translatable("lp.hud.config.satellite.enabled"));
				} else {
					player.sendSystemMessage(Component.translatable("lp.hud.config.satellite.disabled"));
				}
				break;
		}
		// player.inventoryContainer removed in 1.20.1 — use player.inventoryMenu
		if (player.inventoryMenu != null) {
			player.inventoryMenu.broadcastChanges();
		}
	}

	@Override
	public void readData(LPDataInput input) {
		buttonId = input.readInt();
		state = input.readBoolean();
		slot = input.readInt();
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeInt(buttonId);
		output.writeBoolean(state);
		output.writeInt(slot);
	}
}
