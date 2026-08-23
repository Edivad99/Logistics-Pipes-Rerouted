package logisticspipes.pipes;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import java.util.BitSet;
import javax.annotation.Nullable;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.FireWallFlag;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.resources.IResource;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class PipeItemsFirewall extends CoreRoutedPipe {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(6 * 6, "Filter Inv", 1);
	private boolean blockProvider = false;
	private boolean blockCrafter = false;
	private boolean blockSorting = false;
	private boolean blockPower = true;
	private boolean isBlocking = true;
	private IFilter filter = null;

	public PipeItemsFirewall(Item item) {
		super(item);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void onWrenchClicked(Player entityplayer) {
		MainProxy.sendPacketToPlayer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()), entityplayer);
		logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.FirewallGui.class)
				.setPosX(getX()).setPosY(getY()).setPosZ(getZ())
				.open(entityplayer);
	}

	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);
		inv.serialize(output);
		output.putBoolean("blockProvider", blockProvider);
		output.putBoolean("blockCrafer", blockCrafter);
		output.putBoolean("blockSorting", blockSorting);
		output.putBoolean("blockPower", blockPower);
		output.putBoolean("isBlocking", isBlocking);
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);
		inv.deserialize(input);
		blockProvider = input.getBooleanOr("blockProvider", false);
		blockCrafter = input.getBooleanOr("blockCrafer", false);
		blockSorting = input.getBooleanOr("blockSorting", false);
		blockPower = input.getBooleanOr("blockPower", blockPower);
		isBlocking = input.getBooleanOr("isBlocking", false);
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_FIREWALL_TEXTURE;
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
		return null;
	}

	public IFilter getFilter() {
		if (filter == null) {
			filter = new IFilter() {

				@Override
				public boolean isBlocked() {
					return isBlocking;
				}

				@Override
				public boolean isFilteredItem(ItemIdentifier item) {
					return inv.containsUndamagedExcludeNBTItem(item.getIgnoringNBT().getUndamaged());
				}

				@Override
				public boolean blockProvider() {
					return blockProvider;
				}

				@Override
				public boolean blockCrafting() {
					return blockCrafter;
				}

				@Override
				public boolean blockRouting() {
					return blockSorting;
				}

				@Override
				public boolean blockPower() {
					return blockPower;
				}

				@Override
				public int hashCode() {
					return PipeItemsFirewall.this.hashCode();
				}

				@Override
				public String toString() {
					return super.toString() + " (" + PipeItemsFirewall.this.getX() + ", " + PipeItemsFirewall.this.getY() + ", " + PipeItemsFirewall.this.getZ() + ")";
				}

				@Override
				public DoubleCoordinates getLPPosition() {
					return PipeItemsFirewall.this.getLPPosition();
				}

				@Override
				public boolean isFilteredItem(IResource resultItem) {
					for (Pair<ItemIdentifierStack, Integer> pair : inv.contents()) {
						ItemIdentifierStack stack = pair.getValue1();
						if (stack != null && resultItem.matches(stack.getItem(), IResource.MatchSettings.NORMAL)) {
							return true;
						}
					}
					return false;
				}
			};
		}
		return filter;
	}

	public boolean isBlockProvider() {
		return blockProvider;
	}

	public void setBlockProvider(boolean blockProvider) {
		this.blockProvider = blockProvider;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	public boolean isBlockCrafter() {
		return blockCrafter;
	}

	public void setBlockCrafter(boolean blockCrafter) {
		this.blockCrafter = blockCrafter;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	public boolean isBlockSorting() {
		return blockSorting;
	}

	public void setBlockSorting(boolean blockSorting) {
		this.blockSorting = blockSorting;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	public boolean isBlockPower() {
		return blockPower;
	}

	public void setBlockPower(boolean blockPower) {
		this.blockPower = blockPower;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	public boolean isBlocking() {
		return isBlocking;
	}

	public void setBlocking(boolean isBlocking) {
		this.isBlocking = isBlocking;
		MainProxy.sendPacketToServer(PacketHandler.getPacket(FireWallFlag.class).setFlags(getFlags()).setPosX(getX()).setPosY(getY()).setPosZ(getZ()));
	}

	private BitSet getFlags() {
		BitSet flags = new BitSet();
		flags.set(0, blockProvider);
		flags.set(1, blockCrafter);
		flags.set(2, blockSorting);
		flags.set(3, blockPower);
		flags.set(4, isBlocking);
		return flags;
	}

	public void setFlags(BitSet flags) {
		blockProvider = flags.get(0);
		blockCrafter = flags.get(1);
		blockSorting = flags.get(2);
		blockPower = flags.get(3);
		isBlocking = flags.get(4);
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}
}
