package logisticspipes.network.packets.gui;

import java.util.BitSet;

import net.minecraft.world.entity.player.Player;

import lombok.Getter;
import lombok.Setter;

import logisticspipes.interfaces.IFuzzySlot;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolve;

@StaticResolve
public class FuzzySlotSettingsPacket extends ModernPacket {

	@Getter
	@Setter
	private int slotNumber;

	@Getter
	@Setter
	private BitSet flags;

	public FuzzySlotSettingsPacket(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		slotNumber = input.readInt();
		flags = input.readBitSet();
	}

	@Override
	public void processPacket(Player player) {
		if (player.containerMenu != null && player.containerMenu.getSlot(slotNumber) instanceof IFuzzySlot) {
			((IFuzzySlot) player.containerMenu.getSlot(slotNumber)).getFuzzyFlags().replaceWith(flags);
		}
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeInt(slotNumber);
		output.writeBitSet(flags);
	}

	@Override
	public ModernPacket template() {
		return new FuzzySlotSettingsPacket(getId());
	}

}
