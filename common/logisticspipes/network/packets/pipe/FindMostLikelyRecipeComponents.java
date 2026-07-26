package logisticspipes.network.packets.pipe;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;
import logisticspipes.gui.popup.GuiRecipeImport;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.StaticResolve;
import logisticspipes.utils.item.ItemIdentifier;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;
import network.rs485.logisticspipes.world.CoordinateUtils;

@StaticResolve
public class FindMostLikelyRecipeComponents extends CoordinatesPacket {

	@Getter
	@Setter
	private List<GuiRecipeImport.Candidates> content;

	public FindMostLikelyRecipeComponents(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		BlockEntity tile = this.getTileAs(player.level(), BlockEntity.class);
		CoreRoutedPipe pipe = null;
		if (tile instanceof LogisticsCraftingTableBlockEntity) {
			for (Direction dir : Direction.values()) {
				BlockEntity conn = CoordinateUtils.add(((LogisticsCraftingTableBlockEntity) tile).getLPPosition(), dir).getTileEntity(player.level());
				if (conn instanceof LogisticsTileGenericPipe) {
					if (((LogisticsTileGenericPipe) conn).pipe instanceof PipeItemsCraftingLogistics) {
						pipe = (CoreRoutedPipe) ((LogisticsTileGenericPipe) conn).pipe;
						break;
					}
				}
			}
		} else if (tile instanceof LogisticsTileGenericPipe) {
			if (((LogisticsTileGenericPipe) tile).pipe instanceof PipeBlockRequestTable) {
				pipe = (CoreRoutedPipe) ((LogisticsTileGenericPipe) tile).pipe;
			}
		}
		List<Integer> list = new ArrayList<Integer>(content.size());
		while (list.size() < content.size()) {
			list.add(-1);
		}
		if (pipe == null) return;
		LinkedList<ItemIdentifier> craftable = null;
		for (int j = 0; j < content.size(); j++) {
			GuiRecipeImport.Candidates candidates = content.get(j);
			int maxItemPos = -1;
			int max = 0;
			for (int i = 0; i < candidates.order.size(); i++) {
				ItemIdentifier ident = candidates.order.get(i).getItem();
				int newAmount = SimpleServiceLocator.logisticsManager.getAmountFor(ident, pipe.getRouter().getIRoutersByCost());
				if (newAmount > max) {
					max = newAmount;
					maxItemPos = i;
				}
			}
			if (max < 64) {
				if (craftable == null) {
					craftable = SimpleServiceLocator.logisticsManager.getCraftableItems(pipe.getRouter().getIRoutersByCost());
				}
				for (ItemIdentifier craft : craftable) {
					for (int i = 0; i < candidates.order.size(); i++) {
						ItemIdentifier ident = candidates.order.get(i).getItem();
						if (craft == ident) {
							maxItemPos = i;
							break;
						}
					}
				}
			}
			list.set(j, maxItemPos);
		}
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(MostLikelyRecipeComponentsResponse.class).setResponse(list), player);
	}

	@Override
	public void readData(LPDataInput input) {
		super.readData(input);
		content = input.readArrayList(input1 -> {
			GuiRecipeImport.Candidates can = new GuiRecipeImport.Candidates(new TreeSet<>());
			can.order = input1.readArrayList(LPDataInput::readItemIdentifierStack);
			return can;
		});
	}

	@Override
	public void writeData(LPDataOutput output) {
		super.writeData(output);
		output.writeCollection(content, (data, object) -> data.writeCollection(object.order,
				LPDataOutput::writeItemIdentifierStack));
	}

	@Override
	public ModernPacket template() {
		return new FindMostLikelyRecipeComponents(getId());
	}
}
