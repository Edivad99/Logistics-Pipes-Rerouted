package logisticspipes.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.signs.CraftingPipeSign;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.pipes.signs.ItemAmountPipeSign;
import logisticspipes.proxy.MainProxy;

public class ItemPipeSignCreator extends LogisticsItem {

    public static final List<Class<? extends IPipeSign>> signTypes = new ArrayList<>();

    public ItemPipeSignCreator(Properties properties) {
        super(properties.stacksTo(1).durability(250));
    }

    /**
     * Index into {@link #signTypes} of the sign this creator currently places. Also what the
     * {@code logisticspipes:creator_mode} model predicate reads, so the item shows which type
     * is selected — LP1 showed that through the stack's metadata.
     */
    public static int getMode(ItemStack stack) {
        if (stack.isEmpty() || !stack.has(DataComponents.CUSTOM_DATA)) {
            return 0;
        }
        var tag = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();
        int mode = tag.getInt("CreatorMode");
        return Math.min(mode, ItemPipeSignCreator.signTypes.size() - 1);
    }

    public static void registerPipeSignTypes() {
        // Never change this order. It defines the id each signType has.
        ItemPipeSignCreator.signTypes.add(CraftingPipeSign.class);
        ItemPipeSignCreator.signTypes.add(ItemAmountPipeSign.class);
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        Direction facing = context.getClickedFace();
        if (level.isClientSide()) {
            return InteractionResult.FAIL;
        }
        ItemStack itemStack = player.getMainHandItem();
        if (itemStack.isEmpty() || itemStack.getDamageValue() > this.getMaxDamage(itemStack)) {
            return InteractionResult.FAIL;
        }
        if (!(level.getBlockEntity(pos) instanceof LogisticsTileGenericPipe genericPipe)) {
            return InteractionResult.FAIL;
        }

        int mode = getMode(itemStack);

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
                        player.displayClientMessage(Component.literal("This sign type can't be placed on this pipe."),
                            true);
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

    @Override
    public InteractionResult use(final Level level, final Player player, final InteractionHand hand) {
        ItemStack stack = player.getMainHandItem();
        if (MainProxy.isClient(level)) {
            return InteractionResult.PASS;
        }
        if (player.isCrouching()) {
            stack.update(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY,
                customData -> {
                    CompoundTag tag = customData.copyTag();
                    int mode = tag.getInt("CreatorMode");
                    mode++;
                    if (mode >= ItemPipeSignCreator.signTypes.size()) {
                        mode = 0;
                    }
                    tag.putInt("CreatorMode", mode);
                    return CustomData.of(tag);
                }
            );
        }
        // SUCCESS_SERVER: the early return above leaves only the server side reaching this.
        return InteractionResult.SUCCESS_SERVER;
    }
}
