package logisticspipes.network.packets.pipe;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.abstractpackets.ModuleCoordinatesPacket;
import logisticspipes.renderer.GuiOverlay;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class SlotFinderActivatePacket extends ModuleCoordinatesPacket {

	@Getter
	@Setter
	private int targetPosX;
	@Getter
	@Setter
	private int targetPosY;
	@Getter
	@Setter
	private int targetPosZ;
	@Getter
	@Setter
	private int slot;

	public SlotFinderActivatePacket(int id) {
		super(id);
	}

	@Override
	public ModernPacket template() {
		return new SlotFinderActivatePacket(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeInt(targetPosX);
		output.writeInt(targetPosY);
		output.writeInt(targetPosZ);
		output.writeInt(slot);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		targetPosX = input.readInt();
		targetPosY = input.readInt();
		targetPosZ = input.readInt();
		slot = input.readInt();
	}

	@Override
	public void processPacket(Player player) {
		GuiOverlay overlay = GuiOverlay.getInstance();

		overlay.setPipePosX(getPosX());
		overlay.setPipePosY(getPosY());
		overlay.setPipePosZ(getPosZ());
		overlay.setTargetPosX(getTargetPosX());
		overlay.setTargetPosY(getTargetPosY());
		overlay.setTargetPosZ(getTargetPosZ());
		overlay.setSlot(getSlot());
		overlay.setOverlaySlotActive(true);
		overlay.setPositionInt(getPositionInt());
		overlay.setPositionType(getType());
	}
}
