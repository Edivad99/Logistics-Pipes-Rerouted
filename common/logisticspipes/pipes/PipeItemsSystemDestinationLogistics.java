package logisticspipes.pipes;

import java.util.UUID;
import javax.annotation.Nullable;

import logisticspipes.LogisticsPipesDataComponents;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class PipeItemsSystemDestinationLogistics extends CoreRoutedPipe {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "Freq Slot", 1);

	public PipeItemsSystemDestinationLogistics(Item item) {
		super(item);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_DESTINATION_TEXTURE;
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
		return null;
	}

	public UUID getTargetUUID() {
		final ItemIdentifierStack itemIdent = inv.getIDStackInSlot(0);
		if (itemIdent == null) {
			return null;
		}
		final ItemStack stack = itemIdent.makeNormalStack();
		if (!stack.has(LogisticsPipesDataComponents.UUID)) {
			return null;
		}
		spawnParticle(Particles.WhiteParticle, 2);
		return stack.get(LogisticsPipesDataComponents.UUID);
	}

	@Override
	public void onAllowedRemoval() {
		dropFreqCard();
	}

	@Override
	public void writeToNBT(CompoundTag nbttagcompound, HolderLookup.Provider provider) {
		super.writeToNBT(nbttagcompound, provider);
		inv.writeToNBT(nbttagcompound, provider);
	}

	@Override
	public void readFromNBT(CompoundTag nbttagcompound, HolderLookup.Provider provider) {
		super.readFromNBT(nbttagcompound, provider);
		inv.readFromNBT(nbttagcompound, provider);
	}

	private void dropFreqCard() {
		final ItemIdentifierStack itemident = inv.getIDStackInSlot(0);
		if (itemident == null) {
			return;
		}
		ItemEntity item = new ItemEntity(getWorld(), getX(), getY(), getZ(), itemident.makeNormalStack());
		getWorld().addFreshEntity(item);
		inv.clearInventorySlotContents(0);
	}

	@Override
	public void onWrenchClicked(Player entityplayer) {
		logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.pipe.FreqCardGui.class)
				.setPosX(getX()).setPosY(getY()).setPosZ(getZ())
				.open(entityplayer);
	}
}
