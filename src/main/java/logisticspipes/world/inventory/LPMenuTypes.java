package logisticspipes.world.inventory;

import java.util.BitSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
import java.util.Objects;
import java.util.function.Supplier;

import net.minecraft.util.ProblemReporter;
import net.minecraft.world.level.storage.TagValueInput;

import logisticspipes.interfaces.IFreqCardHolder;
import logisticspipes.interfaces.IStringBasedModule;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.modules.ModuleFluidSupplier;
import logisticspipes.modules.ModuleActiveSupplier.PatternMode;
import logisticspipes.modules.ModuleActiveSupplier.SupplyMode;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.network.ModuleTarget;
import logisticspipes.interfaces.SatellitePipe;
import logisticspipes.pipes.PipeFluidSupplierMk2;
import logisticspipes.pipes.PipeFluidTerminus;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.PipeItemsFluidSupplier;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.basic.fluid.FluidSinkPipe;
import logisticspipes.utils.item.ItemIdentifier;

import network.rs485.logisticspipes.module.AsyncAdvancedExtractor;
import logisticspipes.modules.SneakyDirection;
import logisticspipes.modules.SimpleFilter;
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

    public static final DeferredHolder<MenuType<?>, MenuType<FluidSinkMenu>> FLUID_SINK =
        deferredRegister.register("fluid_sink", () -> pipeMenu(FluidSinkPipe.class, FluidSinkMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidTerminusMenu>> FLUID_TERMINUS =
        deferredRegister.register("fluid_terminus",
            () -> pipeMenu(PipeFluidTerminus.class, FluidTerminusMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<SatelliteMenu>> SATELLITE =
        deferredRegister.register("satellite", () -> pipeMenu(SatellitePipe.class, SatelliteMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FirewallMenu>> FIREWALL =
        deferredRegister.register("firewall", () -> pipeMenu(PipeItemsFirewall.class, FirewallMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidSupplierMenu>> FLUID_SUPPLIER =
        deferredRegister.register("fluid_supplier", () -> new MenuType<>(
            (IContainerFactory<FluidSupplierMenu>) (containerId, inventory, buffer) -> {
                final BlockPos pos = buffer.readBlockPos();
                final BlockEntity entity = inventory.player.level().getBlockEntity(pos);
                if (!(entity instanceof LogisticsTileGenericPipe container)
                    || !(container.pipe instanceof PipeItemsFluidSupplier pipe)) {
                    throw new IllegalStateException("No fluid supplier at [%s]".formatted(pos));
                }
                // Which the screen draws a button for; it is not part of the pipe's client state.
                pipe.setRequestingPartials(buffer.readBoolean());
                return new FluidSupplierMenu(containerId, inventory, pipe);
            }, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<FluidSupplierMk2Menu>> FLUID_SUPPLIER_MK2 =
        deferredRegister.register("fluid_supplier_mk2",
            () -> pipeMenu(PipeFluidSupplierMk2.class, FluidSupplierMk2Menu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<FreqCardMenu>> FREQ_CARD =
        deferredRegister.register("freq_card", () -> pipeMenu(IFreqCardHolder.class, FreqCardMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<HudSettingsMenu>> HUD_SETTINGS =
        deferredRegister.register("hud_settings", () -> new MenuType<>(
            (IContainerFactory<HudSettingsMenu>) (containerId, inventory, buffer) ->
                new HudSettingsMenu(containerId, inventory, buffer.readVarInt()),
            FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<LogicControllerMenu>> LOGIC_CONTROLLER =
        deferredRegister.register("logic_controller",
            () -> blockEntityMenu(LogisticsTileGenericPipe.class, LogicControllerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<AdvancedExtractorMenu>> ADVANCED_EXTRACTOR =
        deferredRegister.register("advanced_extractor", () -> new MenuType<>(
            (IContainerFactory<AdvancedExtractorMenu>) (containerId, inventory, buffer) -> {
                final ModuleTarget target = ModuleTarget.STREAM_CODEC.decode(buffer);
                final AsyncAdvancedExtractor module = target.resolve(inventory.player, AsyncAdvancedExtractor.class);
                if (module == null) {
                    throw new IllegalStateException("Cannot find advanced extractor at %s".formatted(target));
                }
                // Which way the filter reads is the server's copy of the module, not the client's.
                module.getItemsIncluded().setValue(buffer.readBoolean());
                return new AdvancedExtractorMenu(containerId, inventory, target, module);
            }, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<CraftingModuleMenu>> CRAFTING_MODULE =
        deferredRegister.register("crafting_module", () -> new MenuType<>(
            (IContainerFactory<CraftingModuleMenu>) (containerId, inventory, buffer) -> {
                final ModuleTarget target = ModuleTarget.STREAM_CODEC.decode(buffer);
                final ModuleCrafter module = target.resolve(inventory.player, ModuleCrafter.class);
                if (module == null) {
                    throw new IllegalStateException("Cannot find crafting module at %s".formatted(target));
                }
                final CraftingModuleMenu.Layout layout = CraftingModuleMenu.Layout.STREAM_CODEC.decode(buffer);
                module.cleanupModeIsExclude.setValue(layout.cleanupExcludes());
                module.liquidAmounts.replaceContent(layout.fluidAmounts());
                return new CraftingModuleMenu(containerId, inventory, target, module, layout);
            }, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<SneakyDirectionMenu>> SNEAKY_DIRECTION =
        deferredRegister.register("sneaky_direction", () -> new MenuType<>(
            (IContainerFactory<SneakyDirectionMenu>) (containerId, inventory, buffer) -> {
                final ModuleTarget target = ModuleTarget.STREAM_CODEC.decode(buffer);
                final SneakyDirection module = target.resolve(inventory.player, SneakyDirection.class);
                if (!(module instanceof LogisticsModule logisticsModule)) {
                    throw new IllegalStateException("Cannot find a sneaky module at %s".formatted(target));
                }
                // Which side it currently extracts from; the client's copy does not know.
                module.setSneakyDirection(
                    ByteBufCodecs.optional(Direction.STREAM_CODEC).decode(buffer).orElse(null));
                return new SneakyDirectionMenu(containerId, inventory, target, logisticsModule);
            }, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ActiveSupplierMenu>> ACTIVE_SUPPLIER =
        deferredRegister.register("active_supplier", () -> new MenuType<>(
            (IContainerFactory<ActiveSupplierMenu>) (containerId, inventory, buffer) -> {
                final ModuleTarget target = ModuleTarget.STREAM_CODEC.decode(buffer);
                final ModuleActiveSupplier module = target.resolve(inventory.player, ModuleActiveSupplier.class);
                if (module == null) {
                    throw new IllegalStateException("Cannot find active supplier at %s".formatted(target));
                }
                // Everything the screen shows besides the filter: which mode it is in, whether it
                // limits, and where each filter slot points.
                final boolean patternUpgrade = buffer.readBoolean();
                module.isLimited.setValue(buffer.readBoolean());
                final int mode = buffer.readVarInt();
                if (patternUpgrade) {
                    module.patternMode.setValue(PatternMode.values()[mode]);
                } else {
                    module.requestMode.setValue(SupplyMode.values()[mode]);
                }
                module.slotAssignmentPattern.replaceContent(buffer.readVarIntArray());
                return new ActiveSupplierMenu(containerId, inventory, target, module, patternUpgrade);
            }, FeatureFlags.DEFAULT_FLAGS));

    public static final DeferredHolder<MenuType<?>, MenuType<ModuleAnalysisMenu>> ORE_DICT_ITEM_SINK =
        deferredRegister.register("ore_dict_item_sink",
            () -> analysisMenu(() -> LPMenuTypes.ORE_DICT_ITEM_SINK.get(), ModuleOreDictItemSink.class));

    public static final DeferredHolder<MenuType<?>, MenuType<ModuleAnalysisMenu>> STRING_BASED_ITEM_SINK =
        deferredRegister.register("string_based_item_sink",
            () -> analysisMenu(() -> LPMenuTypes.STRING_BASED_ITEM_SINK.get(), IStringBasedModule.class));

    public static final DeferredHolder<MenuType<?>, MenuType<SimpleFilterMenu>> SIMPLE_FILTER =
        deferredRegister.register("simple_filter", () -> moduleMenu(SimpleFilter.class,
            (containerId, inventory, target, module) -> new SimpleFilterMenu(
                LPMenuTypes.SIMPLE_FILTER.get(), containerId, inventory, target, module)));

    public static final DeferredHolder<MenuType<?>, MenuType<SimpleFilterMenu>> FLUID_SUPPLIER_MODULE =
        deferredRegister.register("fluid_supplier_module", () -> moduleMenu(ModuleFluidSupplier.class,
            (containerId, inventory, target, module) -> new SimpleFilterMenu(
                LPMenuTypes.FLUID_SUPPLIER_MODULE.get(), containerId, inventory, target, module)));

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

    /**
     * The name-list menu, which also carries the module's settings: the client's copy of a module
     * in a pipe is not the one being configured, and the list is what the screen shows.
     */
    private static <M> MenuType<ModuleAnalysisMenu> analysisMenu(
        Supplier<MenuType<ModuleAnalysisMenu>> self, Class<M> moduleType) {
        IContainerFactory<ModuleAnalysisMenu> containerFactory = (containerId, inventory, buffer) -> {
            final ModuleTarget target = ModuleTarget.STREAM_CODEC.decode(buffer);
            final M module = target.resolve(inventory.player, moduleType);
            if (!(module instanceof LogisticsModule logisticsModule)) {
                throw new IllegalStateException(
                    "Cannot find module of type %s at %s".formatted(moduleType.getName(), target));
            }
            logisticsModule.deserialize(TagValueInput.create(ProblemReporter.DISCARDING,
                inventory.player.level().registryAccess(), Objects.requireNonNull(buffer.readNbt())));
            return new ModuleAnalysisMenu(self.get(), containerId, inventory, target, logisticsModule);
        };
        return new MenuType<>(containerFactory, FeatureFlags.DEFAULT_FLAGS);
    }

    /**
     * A menu belonging to a module, wherever the module happens to be.
     */
    private static <T extends AbstractContainerMenu, M> MenuType<T>
    moduleMenu(Class<M> moduleType, ModuleMenuFactory<T, M> factory) {
        IContainerFactory<T> containerFactory = (containerId, inventory, buffer) -> {
            final ModuleTarget target = ModuleTarget.STREAM_CODEC.decode(buffer);
            final M module = target.resolve(inventory.player, moduleType);
            if (module == null) {
                throw new IllegalStateException(
                    "Cannot find module of type %s at %s".formatted(moduleType.getName(), target));
            }
            return factory.create(containerId, inventory, target, module);
        };
        return new MenuType<>(containerFactory, FeatureFlags.DEFAULT_FLAGS);
    }

    /**
     * A menu belonging to the pipe inside a block entity rather than to the block entity itself.
     */
    private static <T extends AbstractContainerMenu, P> MenuType<T>
    pipeMenu(Class<P> pipeType, CustomMenuFactory<T, P> factory) {
        IContainerFactory<T> containerFactory = (containerId, inventory, buffer) -> {
            final BlockPos pos = buffer.readBlockPos();
            final BlockEntity entity = inventory.player.level().getBlockEntity(pos);
            if (entity instanceof LogisticsTileGenericPipe container && pipeType.isInstance(container.pipe)) {
                return factory.create(containerId, inventory, pipeType.cast(container.pipe));
            }
            throw new IllegalStateException(
                "Cannot find pipe of type %s at [%s]".formatted(pipeType.getName(), pos));
        };
        return new MenuType<>(containerFactory, FeatureFlags.DEFAULT_FLAGS);
    }

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

    private interface ModuleMenuFactory<C extends AbstractContainerMenu, M> {

        C create(int containerId, Inventory inventory, ModuleTarget target, M module);
    }

    private interface CustomMenuFactory<C extends AbstractContainerMenu, T> {

        C create(int id, Inventory inventory, T data);
    }
}
