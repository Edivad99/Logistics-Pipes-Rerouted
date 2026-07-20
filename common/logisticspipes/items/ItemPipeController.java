package logisticspipes.items;

import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.guis.LogisticsPlayerSettingsGuiProvider;
import logisticspipes.proxy.MainProxy;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class ItemPipeController extends LogisticsItem {

	public ItemPipeController() {
		super();
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
	public InteractionResult useOn(net.minecraft.world.item.context.UseOnContext _ctx) {
		Player player = _ctx.getPlayer();
		Level world = _ctx.getLevel();
		if (MainProxy.isClient(world)) {
			return InteractionResult.PASS;
		}
		useItem(player, world);
		return InteractionResult.SUCCESS;
	}

	private void useItem(Player player, Level world) {
		NewGuiHandler.getGui(LogisticsPlayerSettingsGuiProvider.class).open(player);
	}
}
