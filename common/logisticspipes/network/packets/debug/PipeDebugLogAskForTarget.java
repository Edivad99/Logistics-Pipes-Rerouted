package logisticspipes.network.packets.debug;

import static net.minecraft.world.phys.HitResult.Type.BLOCK;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class PipeDebugLogAskForTarget extends ModernPacket {

	public PipeDebugLogAskForTarget(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		HitResult box = Minecraft.getInstance().hitResult;
		if (box != null && box.getType() == BLOCK) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(PipeDebugLogResponse.class).setBlockPos(((BlockHitResult) box).getBlockPos()));
		}
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new PipeDebugLogAskForTarget(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
