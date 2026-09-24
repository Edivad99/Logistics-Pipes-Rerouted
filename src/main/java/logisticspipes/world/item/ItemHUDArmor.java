package logisticspipes.world.item;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.equipment.ArmorMaterials;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.level.Level;

import logisticspipes.api.IHUDArmor;
import logisticspipes.world.inventory.HudSettingsMenu;

public class ItemHUDArmor extends Item implements IHUDArmor {

    public ItemHUDArmor(Properties properties) {
        super(properties.humanoidArmor(ArmorMaterials.LEATHER, ArmorType.HELMET));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand handIn) {
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        useItem(player, level);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player != null) {
            useItem(player, level);
        }
        if (level.isClientSide()) {
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    private void useItem(Player player, Level level) {
        if (!level.isClientSide()) {
            if (player instanceof ServerPlayer serverPlayer) {
                final int slot = player.getInventory().getSelectedSlot();
                serverPlayer.openMenu(new SimpleMenuProvider(
                                (containerId, inventory, viewer) -> new HudSettingsMenu(containerId, inventory, slot),
                                Component.empty()),
                        buffer -> buffer.writeVarInt(slot));
            }
        }
    }

    @Override
    public boolean isEnabled(ItemStack item) {
        return true;
    }
}
