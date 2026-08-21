package logisticspipes.data.models;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.ModelProvider;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

import logisticspipes.LPConstants;
import logisticspipes.client.renderer.item.LogisticsPipeItemRenderer;
import logisticspipes.client.renderer.item.LogisticsSolidBlockItemRenderer;
import logisticspipes.client.renderer.item.properties.CreatorMode;
import logisticspipes.client.renderer.item.properties.FluidTint;
import logisticspipes.client.renderer.item.properties.HasFluid;
import logisticspipes.world.item.ItemLogisticsPipe;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.LogisticsSolidBlockItem;

public class LPModelProvider extends ModelProvider {

    public LPModelProvider(PackOutput output) {
        super(output, LPConstants.ID);
    }

    @Override
    protected Stream<? extends Holder<Item>> getKnownItems() {
        return generatedItems().stream().map(Item::builtInRegistryHolder);
    }

    @Override
    protected Stream<? extends Holder<Block>> getKnownBlocks() {
        return Stream.empty();
    }

    @Override
    protected void registerModels(BlockModelGenerators blockModels, ItemModelGenerators itemModels) {
        for (Item item : flatItems()) {
            itemModels.generateFlatItem(item, ModelTemplates.FLAT_ITEM);
        }
        signCreator(itemModels);
        fluidContainer(itemModels);
        handWrittenItems(itemModels);
        specialItems(itemModels);
    }

    private static void specialItems(ItemModelGenerators itemModels) {
        for (Item item : specialRenderedItems(ItemLogisticsPipe.class)) {
            itemModels.itemModelOutput.accept(item, ItemModelUtils.specialModel(
                ModelLocationUtils.getModelLocation(item), LogisticsPipeItemRenderer.Unbaked.INSTANCE));
        }
        for (Item item : specialRenderedItems(LogisticsSolidBlockItem.class)) {
            itemModels.itemModelOutput.accept(item, ItemModelUtils.specialModel(
                ModelLocationUtils.getModelLocation(item), LogisticsSolidBlockItemRenderer.Unbaked.INSTANCE));
        }
    }

    private static List<Item> specialRenderedItems(Class<? extends Item> type) {
        return BuiltInRegistries.ITEM.listElements()
            .filter(holder -> holder.getKey().location().getNamespace().equals(LPConstants.ID))
            .map(Holder::value)
            .filter(type::isInstance)
            .toList();
    }

    private static List<Item> generatedItems() {
        List<Item> items = new ArrayList<>(flatItems());
        items.add(LPItems.SIGN_CREATOR.get());
        items.add(LPItems.FLUID_CONTAINER.get());
        items.addAll(HAND_WRITTEN_MODEL_ITEMS.stream().map(Supplier::get).toList());
        items.addAll(specialRenderedItems(ItemLogisticsPipe.class));
        items.addAll(specialRenderedItems(LogisticsSolidBlockItem.class));
        return items;
    }

    private static List<Item> flatItems() {
        List<Item> items = new ArrayList<>(List.of(
            LPItems.ITEM_CARD.get(),
            LPItems.HUD_GLASSES.get(),
            LPItems.DISK.get(),
            LPItems.PIPE_CONTROLLER.get(),
            LPItems.PIPE_MANAGER.get(),
            LPItems.LOGISTICS_PROGRAMMER.get(),
            LPItems.BROKEN_ITEM.get(),
            LPItems.GUIDE_BOOK.get(),
            LPItems.CHIP_ADVANCED.get(),
            LPItems.CHIP_ADVANCED_RAW.get(),
            LPItems.CHIP_BASIC.get(),
            LPItems.CHIP_BASIC_RAW.get(),
            LPItems.CHIP_FPGA.get(),
            LPItems.CHIP_FPGA_RAW.get(),
            LPItems.MODULE_BLANK.get()));
        LPItems.modules.values().forEach(rl -> items.add(byId(rl)));
        LPItems.upgrades.values().forEach(rl -> items.add(byId(rl)));
        return items;
    }

    private static Item byId(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getValue(id);
    }

    /**
     * The sign creator shows which sign type is selected. Was a base model carrying {@code overrides}
     * that swapped in one model per mode; 1.21.4 expresses that as a {@code minecraft:range_dispatch}
     * over the {@link CreatorMode} property, with the fallback standing in for mode 0.
     */
    private static void signCreator(ItemModelGenerators itemModels) {
        final String[] modes = { "crafting", "item_amount" };
        Item signCreator = LPItems.SIGN_CREATOR.get();

        var entries = new ArrayList<net.minecraft.client.renderer.item.RangeSelectItemModel.Entry>();
        ResourceLocation fallback = null;
        for (int mode = 0; mode < modes.length; mode++) {
            ResourceLocation model =
                itemModels.createFlatItemModel(signCreator, "_" + modes[mode], ModelTemplates.FLAT_ITEM);
            if (mode == 0) {
                fallback = model;
            } else {
                entries.add(ItemModelUtils.override(ItemModelUtils.plainModel(model), mode));
            }
        }

        itemModels.itemModelOutput.accept(signCreator,
            ItemModelUtils.rangeSelect(CreatorMode.INSTANCE, ItemModelUtils.plainModel(fallback), entries));
    }

    /**
     * The fluid container swaps to a filled model when it holds something. Was a numeric predicate
     * plus a model override; a boolean {@code minecraft:condition} says the same thing in 1.21.4.
     * Both models are hand-written -- the filled one is tinted per fluid -- so this only emits the
     * definition that chooses between them.
     */
    private static void fluidContainer(ItemModelGenerators itemModels) {
        Item container = LPItems.FLUID_CONTAINER.get();
        itemModels.itemModelOutput.accept(container, ItemModelUtils.conditional(
            HasFluid.INSTANCE,
            ItemModelUtils.tintedModel(ModelLocationUtils.getModelLocation(container, "_filled"),
                ItemModelUtils.constantTint(-1), FluidTint.INSTANCE),
            ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(container))));
    }

    /**
     * Items whose {@code models/item/*.json} is hand-written and must not be regenerated: their
     * texture does not follow the {@code item/<name>} convention {@code generateFlatItem} assumes.
     *
     * <p>They still need a definition in {@code assets/<ns>/items/}: 1.21.4 resolves every item
     * through one, and an item without it logs "No model loaded for default item ID" and renders as
     * the missing model, however good its {@code models/item} entry is.</p>
     */
    private static final List<Supplier<Item>> HAND_WRITTEN_MODEL_ITEMS = List.of(
        LPItems.REMOTE_ORDERER::get,
        LPItems.PARTS::get);

    private static void handWrittenItems(ItemModelGenerators itemModels) {
        for (Supplier<Item> item : HAND_WRITTEN_MODEL_ITEMS) {
            itemModels.itemModelOutput.accept(item.get(),
                ItemModelUtils.plainModel(ModelLocationUtils.getModelLocation(item.get())));
        }
    }
}
