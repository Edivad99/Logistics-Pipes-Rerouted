package logisticspipes.network.packets.pipe;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class AskForOpenTarget extends ModernPacket {

	public AskForOpenTarget(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		HitResult box = Minecraft.getInstance().hitResult;
		if (box != null && box.getType() == HitResult.Type.BLOCK) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(SlotFinderActivatePacket.class)
					.setTargetPosX(((BlockHitResult) box).getBlockPos().getX())
					.setTargetPosY(((BlockHitResult) box).getBlockPos().getY())
					.setTargetPosZ(((BlockHitResult) box).getBlockPos().getZ()));
		}
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new AskForOpenTarget(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
