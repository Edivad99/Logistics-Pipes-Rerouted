package logisticspipes.pipes.upgrades;

import java.util.List;

import logisticspipes.modules.AsyncAdvancedExtractor;
import logisticspipes.modules.AsyncExtractorModule;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.modules.ModuleCrafter;
import logisticspipes.modules.ModuleCreativeTabBasedItemSink;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.modules.ModuleModBasedItemSink;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.modules.ModulePolymorphicItemSink;
import logisticspipes.modules.ModuleProvider;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsSupplierLogistics;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;

/**
 * What an upgrade can be applied to. The tooltip names the target and the slot check accepts the
 * pipes and modules listed here, so the two cannot disagree.
 */
public enum UpgradeTarget {
    ALL(List.of(CoreRoutedPipe.class), List.of()),
    BASIC(List.of(PipeItemsBasicLogistics.class), List.of()),
    CRAFTING(List.of(PipeItemsCraftingLogistics.class), List.of(ModuleCrafter.class)),
    PROVIDER(List.of(PipeItemsProviderLogistics.class), List.of(ModuleProvider.class)),
    SUPPLIER(List.of(PipeItemsSupplierLogistics.class), List.of(ModuleActiveSupplier.class)),
    CHASSIS(List.of(PipeLogisticsChassis.class), List.of()),
    REQUEST_BLOCK(List.of(PipeBlockRequestTable.class), List.of()),
    ITEM_SINK(List.of(), List.of(ModuleItemSink.class)),
    /**
     * Every module that sinks items, not just the Item Sink.
     */
    SINK(List.of(), List.of(ModuleItemSink.class, ModulePolymorphicItemSink.class, ModuleModBasedItemSink.class,
        ModuleOreDictItemSink.class, ModuleCreativeTabBasedItemSink.class)),
    EXTRACTOR(List.of(), List.of(AsyncExtractorModule.class)),
    ADVANCED_EXTRACTOR(List.of(), List.of(AsyncAdvancedExtractor.class));

    private final List<Class<? extends CoreRoutedPipe>> pipes;
    private final List<Class<? extends LogisticsModule>> modules;

    UpgradeTarget(List<Class<? extends CoreRoutedPipe>> pipes, List<Class<? extends LogisticsModule>> modules) {
        this.pipes = pipes;
        this.modules = modules;
    }

    public boolean accepts(CoreRoutedPipe pipe) {
        return pipes.stream().anyMatch(type -> type.isInstance(pipe));
    }

    public boolean accepts(LogisticsModule module) {
        return modules.stream().anyMatch(type -> type.isInstance(module));
    }
}
