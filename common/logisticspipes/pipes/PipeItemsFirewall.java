package logisticspipes.pipes;

import java.util.Objects;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IPipeMenuProvider;
import logisticspipes.interfaces.routing.IFilter;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.network.to_client.pipe.FirewallFlagsMessage;
import logisticspipes.network.to_server.pipe.SetFirewallFlagsMessage;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.request.resources.IResource;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.util.DoubleCoordinates;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.tuples.Pair;
import logisticspipes.world.inventory.FirewallMenu;

public class PipeItemsFirewall extends CoreRoutedPipe implements IPipeMenuProvider {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(6 * 6, "Filter Inv", 1);
	@Getter
    private boolean blockProvider = false;
	@Getter
    private boolean blockCrafter = false;
	@Getter
    private boolean blockSorting = false;
	@Getter
    private boolean blockPower = true;
	@Getter
    private boolean isBlocking = true;
	private @Nullable IFilter filter = null;

	public PipeItemsFirewall(Item item) {
		super(item);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void onWrenchClicked(Player player) {
		if (player instanceof ServerPlayer serverPlayer) {
			PacketDistributor.sendToPlayer(serverPlayer, new FirewallFlagsMessage(getPos(), getFlags()));
			serverPlayer.openMenu(this);
		}
	}

	@Override
	public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
		return new FirewallMenu(containerId, inventory, this);
	}

	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);
		inv.serialize(output);
		output.putBoolean("blockProvider", blockProvider);
		output.putBoolean("blockCrafter", blockCrafter);
		output.putBoolean("blockSorting", blockSorting);
		output.putBoolean("blockPower", blockPower);
		output.putBoolean("isBlocking", isBlocking);
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);
		inv.deserialize(input);
		blockProvider = input.getBooleanOr("blockProvider", false);
		blockCrafter = input.getBooleanOr("blockCrafter", false);
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

    public void setBlockProvider(boolean blockProvider) {
		this.blockProvider = blockProvider;
		ClientPacketDistributor.sendToServer(new SetFirewallFlagsMessage(Objects.requireNonNull(getPos()), getFlags()));
	}

    public void setBlockCrafter(boolean blockCrafter) {
		this.blockCrafter = blockCrafter;
        ClientPacketDistributor.sendToServer(new SetFirewallFlagsMessage(Objects.requireNonNull(getPos()), getFlags()));
	}

    public void setBlockSorting(boolean blockSorting) {
		this.blockSorting = blockSorting;
        ClientPacketDistributor.sendToServer(new SetFirewallFlagsMessage(Objects.requireNonNull(getPos()), getFlags()));
	}

    public void setBlockPower(boolean blockPower) {
		this.blockPower = blockPower;
        ClientPacketDistributor.sendToServer(new SetFirewallFlagsMessage(Objects.requireNonNull(getPos()), getFlags()));
	}

    public void setBlocking(boolean isBlocking) {
		this.isBlocking = isBlocking;
        ClientPacketDistributor.sendToServer(new SetFirewallFlagsMessage(Objects.requireNonNull(getPos()), getFlags()));
	}

	private FirewallFlags getFlags() {
		return new FirewallFlags(blockProvider, blockCrafter, blockSorting, blockPower, isBlocking);
	}

	public void setFlags(FirewallFlags flags) {
		blockProvider = flags.blockProvider();
		blockCrafter = flags.blockCrafter();
		blockSorting = flags.blockSorting();
		blockPower = flags.blockPower();
		isBlocking = flags.blocking();
	}

	@Override
	public boolean hasGenericInterests() {
		return true;
	}

    public record FirewallFlags(
            boolean blockProvider,
            boolean blockCrafter,
            boolean blockSorting,
            boolean blockPower,
            boolean blocking
    ) {

        public static final StreamCodec<RegistryFriendlyByteBuf, FirewallFlags> STREAM_CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.BOOL, FirewallFlags::blockProvider,
                        ByteBufCodecs.BOOL, FirewallFlags::blockCrafter,
                        ByteBufCodecs.BOOL, FirewallFlags::blockSorting,
                        ByteBufCodecs.BOOL, FirewallFlags::blockPower,
                        ByteBufCodecs.BOOL, FirewallFlags::blocking,
                        FirewallFlags::new);
    }
}
