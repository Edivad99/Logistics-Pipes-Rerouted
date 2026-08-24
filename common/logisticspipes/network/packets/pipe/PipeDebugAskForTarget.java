package logisticspipes.network.packets.pipe;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.world.DoubleCoordinates;

@StaticResolve
public class PipeDebugAskForTarget extends ModernPacket {

	@Setter
	@Getter
	private boolean isServer;

	public PipeDebugAskForTarget(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		isServer = input.readBoolean();
	}

	@Override
	public void processPacket(Player player) {
		HitResult box = Minecraft.getInstance().hitResult;
		if (box != null && box.getType() == HitResult.Type.BLOCK) {
			if (!isServer) {
				BlockEntity tile = new DoubleCoordinates(((BlockHitResult) box).getBlockPos()).getTileEntity(player.level());
				if (tile instanceof LogisticsTileGenericPipe) {
					((LogisticsTileGenericPipe) tile).pipe.debug.debugThisPipe = !((LogisticsTileGenericPipe) tile).pipe.debug.debugThisPipe;
					if (((LogisticsTileGenericPipe) tile).pipe.debug.debugThisPipe) {
						player.sendSystemMessage(Component.literal("Debug enabled On Client"));
					} else {
						player.sendSystemMessage(Component.literal("Debug disabled On Client"));
					}
				}
			} else {
				MainProxy.sendPacketToServer(PacketHandler.getPacket(PipeDebugResponse.class).setBlockPos(((BlockHitResult) box).getBlockPos()));
			}
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeBoolean(isServer);
	}

	@Override
	public ModernPacket template() {
		return new PipeDebugAskForTarget(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
