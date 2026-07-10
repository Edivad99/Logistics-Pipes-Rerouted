package logisticspipes.network.packets.debuggui;

import logisticspipes.commands.chathelper.LPChatListener;
import logisticspipes.commands.commands.debug.DebugGuiController;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.gui.OpenChatGui;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.string.ChatColor;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class DebugTargetResponse extends ModernPacket {

	@Getter
	@Setter
	private TargetMode mode;
	@Getter
	@Setter
	private int[] additions = new int[0];

	public DebugTargetResponse(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		mode = TargetMode.values()[input.readByte()];
		additions = input.readIntArray();
	}

	@Override
	public void processPacket(final Player player) {
		if (mode == TargetMode.None) {
			player.sendSystemMessage(Component.literal(ChatColor.RED + "No Target Found"));
		} else if (mode == TargetMode.Block) {
			int x = additions[0];
			int y = additions[1];
			int z = additions[2];
			player.sendSystemMessage(Component.literal("Checking Block at: x:" + x + " y:" + y + " z:" + z));
			Block id = player.level().getBlockState(new BlockPos(x, y, z)).getBlock();
			player.sendSystemMessage(Component.literal("Found Block with Id: " + id.getClass()));
			final BlockEntity tile = player.level().getBlockEntity(new BlockPos(x, y, z));
			if (tile == null) {
				player.sendSystemMessage(Component.literal(ChatColor.RED + "No BlockEntity found"));
			} else {
				LPChatListener.addTask(() -> {
					player.sendSystemMessage(Component.literal(
							ChatColor.GREEN + "Starting debuging of BlockEntity: " + ChatColor.BLUE + ChatColor.UNDERLINE + tile.getClass().getSimpleName()));
					DebugGuiController.instance().startWatchingOf(tile, player);
					MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), player);
					return true;
				}, player);
				player.sendSystemMessage(Component.literal(
						ChatColor.AQUA + "Start debuging of BlockEntity: " + ChatColor.BLUE + ChatColor.UNDERLINE + tile.getClass().getSimpleName()
								+ ChatColor.AQUA + "? " + ChatColor.RESET + "<" + ChatColor.GREEN + "yes" + ChatColor.RESET + "/" + ChatColor.RED + "no"
								+ ChatColor.RESET + ">"));
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), player);
			}
		} else if (mode == TargetMode.Entity) {
			int entityId = additions[0];
			final Entity entity = player.level().getEntity(entityId);
			if (entity == null) {
				player.sendSystemMessage(Component.literal(ChatColor.RED + "No Entity found"));
			} else {
				LPChatListener.addTask(() -> {
					player.sendSystemMessage(Component.literal(
							ChatColor.GREEN + "Starting debuging of Entity: " + ChatColor.BLUE + ChatColor.UNDERLINE + entity.getClass().getSimpleName()));
					DebugGuiController.instance().startWatchingOf(entity, player);
					MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), player);
					return true;
				}, player);
				player.sendSystemMessage(Component.literal(
						ChatColor.AQUA + "Start debuging of Entity: " + ChatColor.BLUE + ChatColor.UNDERLINE + entity.getClass().getSimpleName()
								+ ChatColor.AQUA + "? " + ChatColor.RESET + "<" + ChatColor.GREEN + "yes" + ChatColor.RESET + "/" + ChatColor.RED + "no"
								+ ChatColor.RESET + ">"));
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), player);
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeByte(mode.ordinal());
		output.writeIntArray(additions);
	}

	@Override
	public ModernPacket template() {
		return new DebugTargetResponse(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}

	public enum TargetMode {
		Block,
		Entity,
		None
	}
}
