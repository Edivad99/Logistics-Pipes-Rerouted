package logisticspipes.network.packets.routingdebug;

import net.minecraft.core.registries.BuiltInRegistries;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.commands.chathelper.LPChatListener;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.packets.gui.OpenChatGui;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.ServerRouter;
import logisticspipes.routing.debug.DebugController;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.string.ChatColor;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class RoutingUpdateTargetResponse extends ModernPacket {

	@Getter
	@Setter
	private TargetMode mode;
	@Getter
	@Setter
	private int[] additions = new int[0];

	public RoutingUpdateTargetResponse(int id) {
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
			player.sendSystemMessage(Component.literal("Found Block with Id: " + BuiltInRegistries.BLOCK.getId(id)));
			final BlockEntity tile = player.level().getBlockEntity(new BlockPos(x, y, z));
			if (tile == null) {
				player.sendSystemMessage(Component.literal(ChatColor.RED + "No BlockEntity found"));
			} else if (!(tile instanceof LogisticsTileGenericPipe)) {
				player.sendSystemMessage(Component.literal(ChatColor.RED + "No LogisticsTileGenericPipe found"));
			} else if (!(((LogisticsTileGenericPipe) tile).pipe instanceof CoreRoutedPipe)) {
				player.sendSystemMessage(Component.literal(ChatColor.RED + "No CoreRoutedPipe found"));
			} else {
				LPChatListener.addTask(() -> {
					player.sendSystemMessage(Component.literal(ChatColor.GREEN + "Starting RoutingTable debug update."));
					DebugController.instance(player).debug(((ServerRouter) ((CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe).getRouter()));
					MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), player);
					return true;
				}, player);
				player.sendSystemMessage(Component.literal(
						ChatColor.AQUA + "Start RoutingTable debug update ? " + ChatColor.RESET + "<" + ChatColor.GREEN + "yes" + ChatColor.RESET + "/"
								+ ChatColor.RED + "no" + ChatColor.RESET + ">"));
				MainProxy.sendPacketToPlayer(PacketHandler.getPacket(OpenChatGui.class), player);
			}
		} else if (mode == TargetMode.Entity) {
			player.sendSystemMessage(Component.literal(ChatColor.RED + "Entity not allowed"));
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeByte(mode.ordinal());
		output.writeIntArray(additions);
	}

	@Override
	public ModernPacket template() {
		return new RoutingUpdateTargetResponse(getId());
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
