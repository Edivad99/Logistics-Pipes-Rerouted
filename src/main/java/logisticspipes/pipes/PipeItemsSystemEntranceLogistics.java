package logisticspipes.pipes;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IFreqCardHolder;
import logisticspipes.interfaces.IPipeMenuProvider;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.particle.Particles;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.EntrencsTransport;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.world.inventory.FreqCardMenu;
import logisticspipes.world.item.component.LPDataComponents;

public class PipeItemsSystemEntranceLogistics extends CoreRoutedPipe implements IFreqCardHolder, IPipeMenuProvider {

    public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "Freq Slot", 1);

    public PipeItemsSystemEntranceLogistics(Item item) {
        super(new EntrencsTransport(), item);
        ((EntrencsTransport) transport).pipe = this;
    }

    @Nullable
    public UUID getLocalFreqUUID() {
        UUID uuid = inv.getItem(0).get(LPDataComponents.UUID);
        if (uuid == null) {
            return null;
        }

        spawnParticle(Particles.WHITE_SPARKLE, 2);
        return uuid;
    }

    @Override
    public ItemSendMode getItemSendMode() {
        return ItemSendMode.NORMAL;
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
    public void serialize(ValueOutput output) {
        super.serialize(output);
        inv.serialize(output);
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        inv.deserialize(input);
    }

    @Override
    public void onAllowedRemoval() {
        dropFreqCard();
    }

    private void dropFreqCard() {
        ItemStack card = inv.getItem(0);
        if (card.isEmpty()) {
            return;
        }
        BlockPos pos = getPos();
        ItemEntity item = new ItemEntity(getLevel(), pos.getX(), pos.getY(), pos.getZ(), card);
        getLevel().addFreshEntity(item);
        inv.clearInventorySlotContents(0);
    }

    @Override
    public void onWrenchClicked(Player entityplayer) {
        if (entityplayer instanceof ServerPlayer serverPlayer) {
            serverPlayer.openMenu(this);
        }
    }

    @Override
    public ItemIdentifierInventory getFreqCardInventory() {
        return inv;
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new FreqCardMenu(containerId, inventory, this);
    }
}
