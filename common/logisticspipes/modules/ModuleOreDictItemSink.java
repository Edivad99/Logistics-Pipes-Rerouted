package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.level.storage.ValueInput;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.gui.hud.modules.HUDOreDictItemSink;
import logisticspipes.interfaces.IClientInformationProvider;
import logisticspipes.interfaces.IHUDModuleHandler;
import logisticspipes.interfaces.IHUDModuleRenderer;
import logisticspipes.interfaces.IModuleWatchReciver;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractguis.ModuleCoordinatesGuiProvider;
import logisticspipes.network.abstractguis.ModuleInHandGuiProvider;
import logisticspipes.network.guis.module.inhand.OreDictItemSinkModuleInHand;
import logisticspipes.network.guis.module.inpipe.OreDictItemSinkModuleSlot;
import logisticspipes.network.to_client.module.OreDictItemSinkListMessage;
import logisticspipes.network.to_server.module.SetOreDictItemSinkListMessage;
import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.StringListProperty;

public class ModuleOreDictItemSink extends LogisticsModule
    implements IClientInformationProvider, IHUDModuleHandler, IModuleWatchReciver, Gui {

    @Getter
    private final StringListProperty oreList = new StringListProperty("");
    private final IHUDModuleRenderer HUD = new HUDOreDictItemSink(this);
    private final PlayerCollectionList localModeWatchers = new PlayerCollectionList();
    /**
     * Every item carrying one of the tags in {@link #oreList}; null until built.
     */
    private @Nullable Set<Item> oreItems;
    private @Nullable List<ItemIdentifierStack> oreHudList;
    /** Built in {@link #registerPosition}, which runs when the module is installed. */
    private @Nullable SinkReply sinkReply;

    public static String getName() {
        return "item_sink_oredict";
    }

    @Override
    public String getLPName() {
        return getName();
    }

    @Override
    public List<Property<?>> getProperties() {
        return Collections.singletonList(oreList);
    }

    @Override
    public void registerPosition(ModulePositionType slot, int positionInt) {
        super.registerPosition(slot, positionInt);
        sinkReply = new SinkReply(FixedPriority.OreDictItemSink,
            0,
            true,
            false,
            5,
            0,
            new ChassiTargetInformation(getPositionInt()));
    }

    @Override
    public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
        boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
        final SinkReply reply = Objects.requireNonNull(sinkReply, "module has not been registered");
        if (bestPriority > reply.fixedPriority.ordinal() || (bestPriority == reply.fixedPriority.ordinal()
            && bestCustomPriority >= reply.customPriority)) {
            return null;
        }
        if (oreItems == null) {
            buildOreItems();
        }
        return oreItems.contains(item.item) ? reply : null;
    }

    public List<ItemIdentifierStack> getHudItemList() {
        if (oreItems == null) {
            buildOreItems();
        }
        return Objects.requireNonNull(oreHudList);
    }

    /**
     * Turns the tag ids in {@link #oreList} into the flat set of items they cover, so that
     * {@code sinksItem} -- called for every item offered to the pipe -- is a single lookup.
     *
     * <p>Also picks one item per tag to stand for it in the HUD, falling back to fire for a tag
     * that resolves to nothing.
     */
    private void buildOreItems() {
        oreItems = new HashSet<>();
        oreHudList = new ArrayList<>(oreList.size());
        for (String identifier : oreList) {
            Identifier loc = Identifier.tryParse(identifier);
            ItemStack stackForHud = ItemStack.EMPTY;
            if (loc != null) {
                TagKey<Item> tag = TagKey.create(Registries.ITEM, loc);
                for (Holder<Item> holder : BuiltInRegistries.ITEM.getTagOrEmpty(tag)) {
                    Item item = holder.value();
                    oreItems.add(item);
                    if (stackForHud.isEmpty()) {
                        stackForHud = new ItemStack(item);
                    }
                }
            }
            if (!stackForHud.isEmpty()) {
                oreHudList.add(new ItemIdentifierStack(ItemIdentifier.get(stackForHud), 1));
            } else {
                oreHudList.add(new ItemIdentifierStack(ItemIdentifier.get(Item.BY_BLOCK.get(Blocks.FIRE)), 1));
            }
        }
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        oreItems = null;
    }

    /**
     * Replaces the list of ore dictionary names, dropping the lookup map built from the old one.
     *
     * <p>The same two steps {@link #deserialize} does, for the path that receives the names
     * themselves rather than a serialized module.
     */
    public void setOreList(List<String> names) {
        oreList.replaceContent(names);
        oreItems = null;
    }

    @Override
    public void tick() {
    }

    @Override
    public List<String> getClientInformation() {
        List<String> list = new ArrayList<>();
        list.add("Ores: ");
        list.addAll(oreList);
        return list;
    }



    @Override
    public void startWatching(Player player) {
        localModeWatchers.add(player);
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                new OreDictItemSinkListMessage(ModuleTarget.of(this), List.copyOf(oreList)));
        }
    }

    @Override
    public void stopWatching(Player player) {
        localModeWatchers.remove(player);
    }

    public void oreListChanged(Level level) {
        final List<String> names = List.copyOf(oreList);
        if (level instanceof ServerLevel) {
            localModeWatchers.send(new OreDictItemSinkListMessage(ModuleTarget.of(this), names));
        } else {
            ClientPacketDistributor.sendToServer(new SetOreDictItemSinkListMessage(ModuleTarget.of(this), names));
        }
    }

    @Override
    public IHUDModuleRenderer getHUDRenderer() {
        return HUD;
    }

    @Override
    public boolean hasGenericInterests() {
        return true;
    }

    @Override
    public boolean interestedInAttachedInventory() {
        return false;
    }

    @Override
    public boolean interestedInUndamagedID() {
        return false;
    }

    @Override
    public boolean receivePassive() {
        return true;
    }

    @Override
    public ModuleCoordinatesGuiProvider getPipeGuiProvider() {
        TagValueOutput moduleOutput = TagValueOutput.createWithContext(ProblemReporter.DISCARDING,
            getWorld().registryAccess());
        serialize(moduleOutput);
        CompoundTag nbt = moduleOutput.buildResult();
        return NewGuiHandler.getGui(OreDictItemSinkModuleSlot.class).setNbt(nbt);
    }

    @Override
    public ModuleInHandGuiProvider getInHandGuiProvider() {
        return NewGuiHandler.getGui(OreDictItemSinkModuleInHand.class);
    }

}
