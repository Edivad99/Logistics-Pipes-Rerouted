package logisticspipes.pipes;

import java.util.UUID;
import javax.annotation.Nullable;

import logisticspipes.world.item.component.LPDataComponents;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.pipefxhandlers.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.EntrencsTransport;
import logisticspipes.utils.item.ItemIdentifierInventory;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;

public class PipeItemsSystemEntranceLogistics extends CoreRoutedPipe {

	public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "Freq Slot", 1);

	public PipeItemsSystemEntranceLogistics(Item item) {
		super(new EntrencsTransport(), item);
		((EntrencsTransport) transport).pipe = this;
	}

	public UUID getLocalFreqUUID() {
		if (inv.getItem(0) == null) {
			return null;
		}
		if (!inv.getItem(0).has(LPDataComponents.UUID)) {
			return null;
		}

		spawnParticle(Particles.WhiteParticle, 2);
		return inv.getItem(0).get(LPDataComponents.UUID);
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_ENTRANCE_TEXTURE;
	}

	@Override
	public @Nullable LogisticsModule getLogisticsModule() {
		return null;
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

	@Override
	public void onAllowedRemoval() {
		dropFreqCard();
	}

	private void dropFreqCard() {
		if (inv.getItem(0) == null) {
			return;
		}
		ItemEntity item = new ItemEntity(getWorld(), getX(), getY(), getZ(), inv.getItem(0));
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
