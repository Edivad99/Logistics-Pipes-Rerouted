package logisticspipes.world.inventory;

import java.util.BitSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import logisticspipes.LPConstants;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.blocks.stats.LogisticsStatisticsTileEntity;
import logisticspipes.blocks.stats.TrackingTask;
import logisticspipes.blocks.powertile.LogisticsPowerProviderTileEntity;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;

public class LPMenuTypes {

    private static final DeferredRegister<MenuType<?>> deferredRegister =
        DeferredRegister.create(BuiltInRegistries.MENU, LPConstants.ID);

    public static void register(IEventBus modEventBus) {
        deferredRegister.register(modEventBus);
    }

    public static final DeferredHolder<MenuType<?>, MenuType<PowerJunctionMenu>> POWER_JUNCTION =
        deferredRegister.register("power_junction",
            () -> blockEntityMenu(LogisticsPowerJunctionBlockEntity.class, PowerJunctionMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AutoCraftingMenu>> AUTO_CRAFTING =
        deferredRegister.register("auto_crafting", () -> new MenuType<>(
            (IContainerFactory<AutoCraftingMenu>) (containerId, inventory, buffer) -> {
                final BlockPos pos = buffer.readBlockPos();
                final BlockEntity entity = inventory.player.level().getBlockEntity(pos);
                if (!(entity instanceof LogisticsCraftingTableBlockEntity crafter)) {
                    throw new IllegalStateException("No crafting table at [%s]".formatted(pos));
                }
                // The recipe the grid is set to, and which ingredients match loosely, live only on
                // the server; the client needs them before the first frame is drawn.
                crafter.targetType = ByteBufCodecs.optional(ItemIdentifier.STREAM_CODEC)
                        .decode(buffer).orElse(null);
                if (crafter.isFuzzy()) {
                    crafter.fuzzyFlags.replaceWith(BitSet.valueOf(buffer.readLongArray()));
                }
                return new AutoCraftingMenu(containerId, inventory, crafter);
            }, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<PowerProviderMenu>> POWER_PROVIDER =
        deferredRegister.register("power_provider",
            () -> blockEntityMenu(LogisticsPowerProviderTileEntity.class, PowerProviderMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SecurityStationMenu>> SECURITY_STATION =
        deferredRegister.register("security_station",
            () -> blockEntityMenu(LogisticsSecurityTileEntity.class, SecurityStationMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<StatisticsMenu>> STATISTICS =
        deferredRegister.register("statistics", () -> new MenuType<>(
            (IContainerFactory<StatisticsMenu>) (containerId, inventory, buffer) -> {
                final BlockPos pos = buffer.readBlockPos();
                final BlockEntity entity = inventory.player.level().getBlockEntity(pos);
                if (!(entity instanceof LogisticsStatisticsTileEntity table)) {
                    throw new IllegalStateException("No statistics table at [%s]".formatted(pos));
                }
                // The recorded history is kept server side only.
                table.tasks = TrackingTask.STREAM_CODEC.apply(ByteBufCodecs.list()).decode(buffer);
                return new StatisticsMenu(containerId, inventory, table);
            }, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ProgramCompilerMenu>> PROGRAM_COMPILER =
        deferredRegister.register("program_compiler",
            () -> blockEntityMenu(LogisticsProgramCompilerBlockEntity.class, ProgramCompilerMenu::new));

    private static <T extends AbstractContainerMenu, E extends BlockEntity> MenuType<T>
    blockEntityMenu(Class<E> entityType, CustomMenuFactory<T, E> factory) {
        IContainerFactory<T> containerFactory = (id, inventory, packetBuffer) -> {
            BlockPos blockPos = packetBuffer.readBlockPos();
            BlockEntity entity = inventory.player.level().getBlockEntity(blockPos);
            if (entityType.isInstance(entity)) {
                return factory.create(id, inventory, entityType.cast(entity));
            }
            throw new IllegalStateException(
                "Cannot find block entity of type %s at [%s]".formatted(entityType.getName(), blockPos));
        };
        return new MenuType<>(containerFactory, FeatureFlags.DEFAULT_FLAGS);
    }

    private interface CustomMenuFactory<C extends AbstractContainerMenu, T> {

        C create(int id, Inventory inventory, T data);
    }
}
