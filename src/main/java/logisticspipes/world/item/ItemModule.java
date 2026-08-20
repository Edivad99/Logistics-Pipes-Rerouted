package logisticspipes.world.item;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import javax.annotation.Nullable;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.DummyLevelProvider;
import logisticspipes.world.item.tooltip.ModuleInventoryTooltip;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.util.TextUtil;

public class ItemModule extends LogisticsItem {

    private final Module moduleType;

    private ItemModule(Module moduleType, Properties properties) {
        super(properties);
        this.moduleType = moduleType;
    }

    /**
     * Factory for use with DeferredRegister.
     */
    public static ItemModule of(Supplier<? extends LogisticsModule> moduleConstructor, Properties properties) {
        return new ItemModule(new Module(moduleConstructor), properties);
    }

    @Nullable
    public static LogisticsModule getLogisticsModule(Player player, int invSlot) {
        ItemStack item = player.getInventory().items.get(invSlot);
        if (item.isEmpty() || !(item.getItem() instanceof ItemModule itemModule)) {
            return null;
        }
        LogisticsModule module = itemModule.getModuleForItem(item, null, new DummyLevelProvider(player.level()), null);
        if (module == null) {
            return null;
        }
        module.registerPosition(ModulePositionType.IN_HAND, invSlot);
        ItemModuleInformationManager.readInformation(item, module);
        return module;
    }

    private void openConfigGui(ItemStack stack, Player player, Level level) {
        LogisticsModule module = getModuleForItem(stack, null, new DummyLevelProvider(level), null);
        if (module instanceof Gui && !stack.isEmpty()) {
            module.registerPosition(ModulePositionType.IN_HAND, player.getInventory().selected);
            ItemModuleInformationManager.readInformation(stack, module);
            Gui.getInHandGuiProvider((Gui) module).open(player);
        }
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        LogisticsModule module = getModuleForItem(stack, null, null, null);
        if (module != null) {
            if (stack.getCount() > 0) {
                return module.hasEffect();
            }
        }
        return false;
    }

    @Override
    public InteractionResult use(final Level level, final Player player,
        final InteractionHand hand) {
        if (MainProxy.isServer(player.level())) {
            openConfigGui(player.getItemInHand(hand), player, level);
        }
        return super.use(level, player, hand);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        InteractionHand hand = context.getHand();
        if (player != null && MainProxy.isServer(player.level())) {
            BlockEntity tile = level.getBlockEntity(pos);
            if (tile instanceof LogisticsTileGenericPipe) {
                if (player.getDisplayName().getString()
                    .equals("ComputerCraft")) { // Allow turtle to place modules in pipes.
                    CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(level, pos);
                    if (LogisticsBlockGenericPipe.isValid(pipe)) {
                        pipe.blockActivated(player);
                    }
                }
                return InteractionResult.PASS;
            }
            openConfigGui(player.getItemInHand(hand), player, level);
        }
        return InteractionResult.PASS;
    }

    @Nullable
    public LogisticsModule getModule(
        @Nullable LogisticsModule currentModule,
        @Nullable IWorldProvider world,
        @Nullable IPipeServiceProvider service
    ) {
        if (currentModule != null) {
            if (moduleType.getILogisticsModuleClass().equals(currentModule.getClass())) {
                return currentModule;
            }
        }
        LogisticsModule newModule = moduleType.getILogisticsModule();
        newModule.registerHandler(world, service);
        return newModule;
    }

    @Nullable
    public LogisticsModule getModuleForItem(
        ItemStack itemStack,
        @Nullable LogisticsModule currentModule,
        @Nullable IWorldProvider world,
        @Nullable IPipeServiceProvider service
    ) {

        if (itemStack.isEmpty()) {
            return null;
        }
        if (itemStack.getItem() != this) {
            return null;
        }
        return getModule(currentModule, world, service);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents,
        TooltipFlag tooltipFlag) {
        ListTag informationList = getInformationList(stack);
        if (informationList == null) {
            TextUtil.addTooltipInformation(stack, tooltipComponents, Screen.hasShiftDown());
            return;
        }
        if (!Screen.hasShiftDown()) {
            TextUtil.addTooltipInformation(stack, tooltipComponents, false);
            return;
        }
        for (int i = 0; i < informationList.size(); i++) {
            String data = informationList.getString(i);
            if (data.equals("<inventory>") && i + 1 < informationList.size()) {
                // The filter contents are drawn as an item grid, see getTooltipImage.
                i++;
            } else {
                tooltipComponents.add(Component.literal(data));
            }
        }
    }

    @Override
    public Optional<TooltipComponent> getTooltipImage(ItemStack stack) {
        if (!Screen.hasShiftDown()) {
            return Optional.empty();
        }
        return Optional.ofNullable(getInventoryTooltip(stack));
    }

    @Nullable
    private static ListTag getInformationList(ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) {
            return null;
        }
        CompoundTag nbt = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();
        if (!nbt.contains("informationList")) {
            return null;
        }
        return nbt.getList("informationList", Tag.TAG_STRING);
    }

    /**
     * Index of the {@code <inventory>} entry in the module's information list, which is the line the
     * item grid takes the place of. Returns {@code -1} when the module has no inventory to show.
     */
    public static int getInventoryLineIndex(ItemStack stack) {
        ListTag informationList = getInformationList(stack);
        return informationList == null ? -1 : findInventoryEntry(informationList);
    }

    private static int findInventoryEntry(ListTag informationList) {
        for (int i = 0; i + 1 < informationList.size(); i++) {
            if (informationList.getString(i).equals("<inventory>")
                && informationList.getString(i + 1).startsWith("<that>")) {
                return i;
            }
        }
        return -1;
    }

    @Nullable
    private static ModuleInventoryTooltip getInventoryTooltip(ItemStack stack) {
        ListTag informationList = getInformationList(stack);
        if (informationList == null) {
            return null;
        }
        int entry = findInventoryEntry(informationList);
        if (entry < 0) {
            return null;
        }
        String prefix = informationList.getString(entry + 1).substring("<that>".length());
        CompoundTag nbt = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();
        CompoundTag moduleInformation = nbt.getCompound("moduleInformation");
        int size = moduleInformation.contains(prefix + "itemsCount")
            ? moduleInformation.getInt(prefix + "itemsCount")
            : moduleInformation.getList(prefix + "items", Tag.TAG_COMPOUND).size();
        if (size <= 0) {
            return null;
        }
        return new ModuleInventoryTooltip(moduleInformation, prefix, size);
    }

    private static class Module {

        private final Supplier<? extends LogisticsModule> moduleConstructor;
        private final Class<? extends LogisticsModule> moduleClass;

        private Module(Supplier<? extends LogisticsModule> moduleConstructor) {
            this.moduleConstructor = moduleConstructor;
            this.moduleClass = moduleConstructor.get().getClass();
        }

        private LogisticsModule getILogisticsModule() {
            return moduleConstructor.get();
        }

        private Class<? extends LogisticsModule> getILogisticsModuleClass() {
            return moduleClass;
        }
    }
}
