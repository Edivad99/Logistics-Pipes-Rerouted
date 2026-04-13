package logisticspipes.network.packets.pipe;

import net.minecraft.world.entity.player.Player;

import logisticspipes.network.abstractpackets.InventoryModuleCoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class ChassisPipeModuleContent extends InventoryModuleCoordinatesPacket {

	public ChassisPipeModuleContent(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new ChassisPipeModuleContent(getId());
	}

	@Override
	public void processPacket(Player player) {
		final LogisticsTileGenericPipe pipe = this.getPipe(player.level(), LTGPCompletionCheck.PIPE);
		if (pipe.pipe instanceof PipeLogisticsChassis) {
			PipeLogisticsChassis chassis = (PipeLogisticsChassis) pipe.pipe;
			chassis.handleModuleItemIdentifierList(getIdentList());
		}
	}
}
