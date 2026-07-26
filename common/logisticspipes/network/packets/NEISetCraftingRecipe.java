package logisticspipes.network.packets;

import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.core.NonNullList;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

@StaticResolve
public class NEISetCraftingRecipe extends CoordinatesPacket {

	private NonNullList<ItemStack> stackList = NonNullList.withSize(9, ItemStack.EMPTY);

	public NEISetCraftingRecipe(int id) {
		super(id);
	}

	public NonNullList<ItemStack> getStackList() {
		return this.stackList;
	}

	@Override
	public void processPacket(Player player) {
		BlockEntity tile = getTileAs(player.level(), BlockEntity.class);
		if (tile instanceof LogisticsCraftingTableBlockEntity) {
			((LogisticsCraftingTableBlockEntity) tile).handleNEIRecipePacket(getStackList());
		} else if (tile instanceof LogisticsTileGenericPipe && ((LogisticsTileGenericPipe) tile).pipe instanceof PipeBlockRequestTable) {
			((PipeBlockRequestTable) ((LogisticsTileGenericPipe) tile).pipe).handleNEIRecipePacket(getStackList());
		}
	}

	@Override
	public ModernPacket template() {
		return new NEISetCraftingRecipe(getId());
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCollection(stackList, LPDataOutput::writeItemStack);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		NonNullList<ItemStack> readList = input.readNonNullList(LPDataInput::readItemStack, ItemStack.EMPTY);
		if (readList != null) stackList = readList;
	}
}
