package logisticspipes.integrations.probe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.Translations.Probe;
import logisticspipes.Translations.Screen;
import logisticspipes.integrations.probe.PipeTooltip.Line;
import logisticspipes.modules.AsyncAdvancedExtractor;
import logisticspipes.modules.AsyncExtractorModule;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.modules.ModuleCreativeTabBasedItemSink;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.modules.ModuleModBasedItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.modules.ModulePassiveSupplier;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.modules.ModuleTerminus;
import logisticspipes.pipes.PipeFluidSatellite;
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.pipes.unrouted.PipeItemsBasicTransport;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.item.LPItems;

/**
 * Describes a pipe for {@link PipeTooltip}. Runs on the server.
 *
 * <p>A pipe always says what it does itself. What its upgrades are, and what each module of a
 * chassis does, is only added when the player asks for details.
 */
public final class PipeTooltipBuilder {

    /** How many names a list shows before it is cut short. */
    private static final int LIST_LIMIT = 3;

    private final List<Line> lines = new ArrayList<>();
    /** Set while the modules of a chassis are described, to set their lines apart. */
    private boolean inChassis;

    private PipeTooltipBuilder() {
    }

    public static PipeTooltip build(@Nullable BlockEntity blockEntity, boolean details) {
        PipeTooltipBuilder builder = new PipeTooltipBuilder();
        if (blockEntity instanceof LogisticsTileGenericPipe tile && tile.pipe != null) {
            builder.addPipe(tile, tile.pipe, details);
        }
        return new PipeTooltip(builder.lines);
    }

    private void addPipe(LogisticsTileGenericPipe tile, CoreUnroutedPipe pipe, boolean details) {
        switch (pipe) {
            case PipeItemsFirewall firewall -> addFirewall(firewall);
            case PipeLogisticsChassis chassis -> addChassis(chassis, details);
            case PipeItemsSatelliteLogistics satellite -> addSatellite(satellite.getSatellitePipeName());
            case PipeFluidSatellite satellite -> addSatellite(satellite.getSatellitePipeName());
            case PipeItemsBasicTransport _ -> addUnrouted(tile);
            case CoreRoutedPipe routed -> {
                LogisticsModule module = routed.getLogisticsModule();
                if (module != null) {
                    addModule(module);
                }
            }
            default -> {
            }
        }
        if (details && pipe instanceof CoreRoutedPipe routed) {
            addUpgrades(routed);
        }
    }

    private void addFirewall(PipeItemsFirewall pipe) {
        if (!pipe.inv.isEmpty()) {
            long filtered = pipe.inv.getItemsAndCount().values().stream().filter(count -> count > 0).count();
            add(Component.translatable(Probe.FIREWALL_FILTERING, value(String.valueOf(filtered)),
                blocked(pipe.isBlocking())));
        }
        add(Component.translatable(Probe.FIREWALL_PROVIDING, blocked(pipe.isBlockProvider())));
        add(Component.translatable(Probe.FIREWALL_CRAFTING, blocked(pipe.isBlockCrafter())));
        add(Component.translatable(Probe.FIREWALL_SORTING, blocked(pipe.isBlockSorting())));
        add(Component.translatable(Probe.FIREWALL_POWER, blocked(pipe.isBlockPower())));
    }

    private static Component blocked(boolean blocked) {
        return blocked
            ? Component.translatable(Probe.FIREWALL_BLOCKED).withStyle(ChatFormatting.RED)
            : Component.translatable(Probe.FIREWALL_ALLOWED).withStyle(ChatFormatting.GREEN);
    }

    private void addSatellite(String name) {
        if (name.isBlank()) {
            add(missing(Probe.SATELLITE_NO_NAME));
        } else {
            add(Component.translatable(Probe.SATELLITE_NAME, value(name)));
        }
    }

    private void addUnrouted(LogisticsTileGenericPipe tile) {
        int connections = 0;
        for (boolean connected : tile.pipeConnectionsBuffer) {
            if (connected) {
                connections++;
            }
        }
        if (connections > 2) {
            add(missing(Probe.UNROUTED_TOO_MANY_CONNECTIONS));
        }
    }

    private void addChassis(PipeLogisticsChassis pipe, boolean details) {
        List<ItemStack> icons = new ArrayList<>();
        List<LogisticsModule> modules = new ArrayList<>();
        for (int slot = 0; slot < pipe.getChassisSize(); slot++) {
            LogisticsModule module = pipe.getSubModule(slot);
            if (module != null) {
                modules.add(module);
                icons.add(moduleItem(module));
            }
        }
        if (modules.isEmpty()) {
            add(missing(Probe.CHASSIS_NO_MODULES));
        } else if (!details) {
            lines.add(new Line(icons, Component.empty()));
        } else {
            for (int i = 0; i < modules.size(); i++) {
                ItemStack icon = icons.get(i);
                lines.add(new Line(List.of(icon), icon.getHoverName()));
                inChassis = true;
                addModule(modules.get(i));
                inChassis = false;
            }
        }
    }

    private static ItemStack moduleItem(LogisticsModule module) {
        Identifier id = LPItems.modules.get(module.getLPName());
        return id == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(id));
    }

    private void addModule(LogisticsModule module) {
        switch (module) {
            case ModuleItemSink sink -> {
                if (sink.isDefaultRoute()) {
                    add(Component.translatable(Probe.IS_DEFAULT_ROUTE));
                } else if (inChassis) {
                    add(Component.translatable(Probe.IS_NOT_DEFAULT_ROUTE));
                }
            }
            case ModuleProvider provider -> {
                addList(provider.isExclusionFilter.getValue() ? Probe.PROVIDER_BUT : Probe.PROVIDER_ONLY,
                    Probe.PROVIDER_ALL, itemNames(provider.filterInventory.getItemsAndCount()));
                add(Component.translatable(Probe.PROVIDER_MODE, value(Component.translatable(
                    Screen.enumValue("provider_mode", provider.providerMode.getValue())))));
            }
            case ModuleCrafter crafter -> addCrafter(crafter);
            case ModuleActiveSupplier supplier -> {
                if (supplier.inventory.isEmpty()) {
                    add(missing(Probe.ACTIVE_SUPPLIER_NO_FILTER));
                } else {
                    add(Component.translatable(Probe.ACTIVE_SUPPLIER_MODE, value(Component.translatable(
                        Screen.enumValue("supplier_pipe.mode", supplier.requestMode.getValue())))));
                    addList(Probe.ACTIVE_SUPPLIER_FILTER, null, itemNames(supplier.inventory.getItemsAndCount()));
                }
            }
            case AsyncExtractorModule extractor -> addSneakyDirection(extractor.getSneakyDirection());
            case AsyncAdvancedExtractor extractor -> {
                boolean included = extractor.getItemsIncluded().getValue();
                addList(included ? Probe.ADVANCED_EXTRACTOR_ONLY : Probe.ADVANCED_EXTRACTOR_BUT,
                    included ? Probe.ADVANCED_EXTRACTOR_NONE : Probe.ADVANCED_EXTRACTOR_ALL,
                    itemNames(extractor.getFilterInventory().getItemsAndCount()));
                addSneakyDirection(extractor.getSneakyDirection());
            }
            case ModulePassiveSupplier supplier -> addList(Probe.PASSIVE_SUPPLIER_FILTER,
                Probe.PASSIVE_SUPPLIER_NO_FILTER, itemNames(supplier.filterInventory.getItemsAndCount()));
            case ModuleTerminus terminus -> addList(Probe.TERMINUS_FILTER, Probe.TERMINUS_NO_FILTER,
                itemNames(terminus.filterInventory.getItemsAndCount()));
            case ModuleCreativeTabBasedItemSink sink -> addList(Probe.CREATIVE_TAB_SINK_FILTER,
                Probe.CREATIVE_TAB_SINK_NO_FILTER, literals(sink.stringListProperty()));
            case ModuleModBasedItemSink sink -> addList(Probe.MOD_SINK_FILTER, Probe.MOD_SINK_NO_FILTER,
                literals(sink.stringListProperty()));
            case ModuleOreDictItemSink sink -> addList(Probe.ORE_SINK_FILTER, Probe.ORE_SINK_NO_FILTER,
                literals(sink.getOreList()));
            default -> {
            }
        }
    }

    private void addCrafter(ModuleCrafter crafter) {
        ItemIdentifierStack result = crafter.getCraftedItem();
        if (result == null) {
            add(missing(Probe.CRAFTING_NO_RESULT));
            return;
        }
        ItemIdentifierStack byproduct = crafter.hasByproductUpgrade() ? crafter.getByproductItem() : null;
        MutableComponent text = byproduct == null
            ? Component.translatable(Probe.CRAFTING_RESULT, value(result.makeNormalStack().getHoverName()))
            : Component.translatable(Probe.CRAFTING_RESULT_WITH_BYPRODUCT,
                value(result.makeNormalStack().getHoverName()), value(byproduct.makeNormalStack().getHoverName()));
        if (crafter.hasFuzzyUpgrade()) {
            text.append(" ").append(Component.translatable(Probe.FUZZY).withStyle(ChatFormatting.GOLD));
        }
        add(text);
    }

    private void addSneakyDirection(@Nullable Direction direction) {
        if (direction != null) {
            add(Component.translatable(Probe.EXTRACTOR_SIDE, value(direction.getName())));
        }
    }

    private void addUpgrades(CoreRoutedPipe pipe) {
        Container upgrades = pipe.getOriginalUpgradeManager().getInv();
        List<Component> names = new ArrayList<>();
        for (int slot = 0; slot < upgrades.getContainerSize(); slot++) {
            ItemStack stack = upgrades.getItem(slot);
            if (!stack.isEmpty()) {
                names.add(stack.getHoverName());
            }
        }
        if (names.isEmpty()) {
            add(missing(Probe.NO_UPGRADES));
        } else {
            add(Component.translatable(Probe.UPGRADES, joined(names)));
        }
    }

    /** {@code filledKey} with the names, or {@code emptyKey} when there are none and it is given. */
    private void addList(String filledKey, @Nullable String emptyKey, List<Component> names) {
        if (!names.isEmpty()) {
            add(Component.translatable(filledKey, joined(names)));
        } else if (emptyKey != null) {
            add(Component.translatable(emptyKey));
        }
    }

    private static List<Component> itemNames(Map<ItemIdentifier, Integer> items) {
        List<Component> names = new ArrayList<>();
        items.forEach((item, count) -> {
            if (count > 0) {
                names.add(item.makeNormalStack(1).getHoverName());
            }
        });
        return names;
    }

    private static List<Component> literals(Collection<String> strings) {
        List<Component> names = new ArrayList<>();
        for (String string : strings) {
            names.add(Component.literal(string));
        }
        return names;
    }

    private static Component joined(List<Component> names) {
        MutableComponent joined = Component.empty();
        for (int i = 0; i < Math.min(names.size(), LIST_LIMIT); i++) {
            if (i > 0) {
                joined.append(", ");
            }
            joined.append(value(names.get(i)));
        }
        if (names.size() > LIST_LIMIT) {
            joined.append(", …");
        }
        return joined;
    }

    private static Component value(String text) {
        return value(Component.literal(text));
    }

    private static Component value(Component text) {
        return text.copy().withStyle(ChatFormatting.AQUA);
    }

    /** A line saying that something is not set up. */
    private static Component missing(String key) {
        return Component.translatable(key).withStyle(ChatFormatting.RED);
    }

    private void add(Component text) {
        if (inChassis) {
            text = Component.literal("- ").append(text).withStyle(ChatFormatting.ITALIC);
        }
        lines.add(new Line(List.of(), text));
    }
}
