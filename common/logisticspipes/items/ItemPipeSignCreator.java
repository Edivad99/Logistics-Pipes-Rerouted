package logisticspipes.items;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.CraftingPipeSign;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.pipes.signs.ItemAmountPipeSign;
import logisticspipes.proxy.MainProxy;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class ItemPipeSignCreator extends LogisticsItem {

	public static final List<Class<? extends IPipeSign>> signTypes = new ArrayList<>();

	//private TextureAtlasSprite[] itemIcon = new TextureAtlasSprite[2];

	public ItemPipeSignCreator(Properties properties) {
		super(properties.stacksTo(1).durability(250));
	}

	@Override
	public boolean isEnchantable(ItemStack stack) {
		return false;
	}

	@Override
	public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
		return false;
	}

	@Override
	public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level world = context.getLevel();
		BlockPos pos = context.getClickedPos();
		Direction facing = context.getClickedFace();
		if (MainProxy.isClient(world)) {
			return InteractionResult.FAIL;
		}
		ItemStack itemStack = player.getMainHandItem();
		if (itemStack.isEmpty() || itemStack.getDamageValue() > this.getMaxDamage(itemStack)) {
			return InteractionResult.FAIL;
		}
		if (!(world.getBlockEntity(pos) instanceof LogisticsTileGenericPipe genericPipe)) {
			return InteractionResult.FAIL;
		}

        itemStack.update(
            DataComponents.CUSTOM_DATA,
            CustomData.EMPTY,
            customData -> {
                CompoundTag tag = customData.copyTag();
                tag.putInt("PipeClicked", 0);
                return CustomData.of(tag);
            }
        );

		var tag = Objects.requireNonNull(itemStack.get(DataComponents.CUSTOM_DATA)).copyTag();

		int mode = tag.getInt("CreatorMode");

        if (!(genericPipe.pipe instanceof CoreRoutedPipe pipe)) {
			return InteractionResult.FAIL;
		}

        if (!player.isCrouching()) {
			if (pipe.hasPipeSign(facing)) {
				pipe.activatePipeSign(facing, player);
				return InteractionResult.SUCCESS;
			} else if (mode >= 0 && mode < ItemPipeSignCreator.signTypes.size()) {
				Class<? extends IPipeSign> signClass = ItemPipeSignCreator.signTypes.get(mode);
				try {
					IPipeSign sign = signClass.newInstance();
					if (sign.isAllowedFor(pipe)) {
						itemStack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
						sign.addSignTo(pipe, facing, player);
						return InteractionResult.SUCCESS;
					} else {
						return InteractionResult.FAIL;
					}
				} catch (InstantiationException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			} else {
				return InteractionResult.FAIL;
			}
		} else {
			if (pipe.hasPipeSign(facing)) {
				pipe.removePipeSign(facing, player);
				//OLD CODE: itemStack.hurtAndBreak(-1, player, p -> {});
				itemStack.setDamageValue(Math.max(0, itemStack.getDamageValue() - 1));
			}
			return InteractionResult.SUCCESS;
		}
	}

	// getMetadata removed in 1.20.1 — item variants handled differently
	public int getMetadata(ItemStack stack) {
		if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) {
			return 0;
		}
		var tag = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();
		int mode = tag.getInt("CreatorMode");
		return Math.min(mode, ItemPipeSignCreator.signTypes.size() - 1);
	}

	@Override
	public int getModelCount() {
		return signTypes.size();
	}

	@Override
	public InteractionResultHolder<ItemStack> use(final Level world, final Player player, final InteractionHand hand) {
		ItemStack stack = player.getMainHandItem();
		if (MainProxy.isClient(world)) {
			return InteractionResultHolder.pass(stack);
		}
		if (player.isCrouching()) {
            stack.update(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY,
                customData -> {
                    CompoundTag tag = customData.copyTag();
                    if (!tag.contains("PipeClicked")) {
                        int mode = tag.getInt("CreatorMode");
                        mode++;
                        if (mode >= ItemPipeSignCreator.signTypes.size()) {
                            mode = 0;
                        }
                        tag.putInt("CreatorMode", mode);
                    }
                    return CustomData.of(tag);
                }
            );
		}
		if (stack.has(DataComponents.CUSTOM_DATA)) {
			var tag = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();
			tag.remove("PipeClicked");
			stack.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
		}
		return InteractionResultHolder.success(stack);
	}

	public static void registerPipeSignTypes() {
		// Never change this order. It defines the id each signType has.
		ItemPipeSignCreator.signTypes.add(CraftingPipeSign.class);
		ItemPipeSignCreator.signTypes.add(ItemAmountPipeSign.class);
	}
}
