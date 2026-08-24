package logisticspipes.network.packets.debuggui;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.StaticResolve;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class DebugAskForTarget extends ModernPacket {

	public DebugAskForTarget(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {}

	@Override
	public void processPacket(Player player) {
		HitResult box = Minecraft.getInstance().hitResult;
		if (box == null) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(DebugTargetResponse.class).setMode(DebugTargetResponse.TargetMode.None));
		} else if (box.getType() == HitResult.Type.BLOCK) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(DebugTargetResponse.class).setMode(DebugTargetResponse.TargetMode.Block)
					.setAdditions(new int[] { ((BlockHitResult) box).getBlockPos().getX(), ((BlockHitResult) box).getBlockPos().getY(), ((BlockHitResult) box).getBlockPos().getZ() }));
		} else if (box.getType() == HitResult.Type.ENTITY) {
			MainProxy.sendPacketToServer(PacketHandler.getPacket(DebugTargetResponse.class).setMode(DebugTargetResponse.TargetMode.Entity)
					.setAdditions(new int[] { ((EntityHitResult) box).getEntity().getId() }));
		}
	}

	@Override
	public void writeData(LPDataOutput output) {}

	@Override
	public ModernPacket template() {
		return new DebugAskForTarget(getId());
	}

	@Override
	public boolean isCompressable() {
		return true;
	}
}
