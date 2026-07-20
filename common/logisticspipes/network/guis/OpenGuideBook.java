package logisticspipes.network.guis;

import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import network.rs485.logisticspipes.guidebook.ItemGuideBook;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class OpenGuideBook extends ModernPacket {

	private InteractionHand hand;
	private ItemStack stack;

	public OpenGuideBook(int id) {
		super(id);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		hand = input.readEnum(InteractionHand.class);
		stack = input.readItemStack();
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeEnum(hand);
		output.writeItemStack(stack);
	}

	@Override
	public void processPacket(Player player) {
		ItemGuideBook.openGuideBook(hand, stack);
	}

	@Override
	public ModernPacket template() {
		return new OpenGuideBook(getId());
	}

	public OpenGuideBook setHand(InteractionHand hand) {
		this.hand = hand;
		return this;
	}

	public OpenGuideBook setStack(ItemStack stack) {
		this.stack = stack;
		return this;
	}
}
