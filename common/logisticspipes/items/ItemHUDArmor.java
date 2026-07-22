package logisticspipes.items;

import logisticspipes.api.IHUDArmor;
import logisticspipes.interfaces.ILogisticsItem;
import logisticspipes.proxy.MainProxy;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemHUDArmor extends ArmorItem implements IHUDArmor, ILogisticsItem {

	public ItemHUDArmor(Properties properties) {
		super(ArmorMaterials.LEATHER, Type.HELMET, properties);
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand handIn) {
		ItemStack stack = player.getItemInHand(handIn);
		if (MainProxy.isClient(world)) {
			return InteractionResultHolder.pass(stack);
		}
		useItem(player, world);
		return InteractionResultHolder.success(stack);
	}

	@Override
	public InteractionResult useOn(UseOnContext _ctx) {
		Player player = _ctx.getPlayer();
		Level world = _ctx.getLevel();
		useItem(player, world);
		if (MainProxy.isClient(world)) {
			return InteractionResult.PASS;
		}
		return InteractionResult.SUCCESS;
	}

	private void useItem(Player player, Level world) {
		if (MainProxy.isServer(world)) {
			logisticspipes.network.NewGuiHandler.getGui(logisticspipes.network.guis.item.HUDSettingsGui.class)
					.setSlot(player.getInventory().selected)
					.open(player);
		}
	}

	@Override
	public boolean isEnabled(ItemStack item) {
		return true;
	}

	@Override
	public net.minecraft.network.chat.Component getName(ItemStack itemstack) {
		return net.minecraft.network.chat.Component.literal(I18n.get(getDescriptionId(itemstack) + ".name").trim());
	}

}
