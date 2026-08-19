package logisticspipes.world.item;

import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import logisticspipes.api.IHUDArmor;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.guis.item.HUDSettingsGui;
import logisticspipes.proxy.MainProxy;

public class ItemHUDArmor extends ArmorItem implements IHUDArmor {

    public ItemHUDArmor(Properties properties) {
        super(ArmorMaterials.LEATHER, Type.HELMET, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand handIn) {
        ItemStack stack = player.getItemInHand(handIn);
        if (MainProxy.isClient(level)) {
            return InteractionResultHolder.pass(stack);
        }
        useItem(player, level);
        return InteractionResultHolder.success(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        if (player != null) {
            useItem(player, level);
        }
        if (MainProxy.isClient(level)) {
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    private void useItem(Player player, Level level) {
        if (MainProxy.isServer(level)) {
            NewGuiHandler.getGui(HUDSettingsGui.class)
                .setSlot(player.getInventory().selected)
                .open(player);
        }
    }

    @Override
    public boolean isEnabled(ItemStack item) {
        return true;
    }

    @Override
    public Component getName(ItemStack itemstack) {
        return Component.literal(I18n.get(getDescriptionId(itemstack) + ".name").trim());
    }
}
