package logisticspipes.pipes;

import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.level.storage.ValueInput;
import java.util.UUID;
import javax.annotation.Nullable;

import logisticspipes.particle.Particles;
import logisticspipes.world.item.component.LPDataComponents;
import logisticspipes.modules.LogisticsModule;
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
		if (!stack.has(LPDataComponents.UUID)) {
			return null;
		}
		spawnParticle(Particles.WHITE_SPARKLE, 2);
		return stack.get(LPDataComponents.UUID);
	}

	@Override
	public void onAllowedRemoval() {
		dropFreqCard();
	}

	@Override
	public void serialize(ValueOutput output) {
		super.serialize(output);
		inv.serialize(output);
	}

	@Override
	public void deserialize(ValueInput input) {
		super.deserialize(input);
		inv.deserialize(input);
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
