package logisticspipes.world.level.block.entity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.util.StringUtil;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.network.PacketDistributor;

import com.google.common.collect.Iterables;
import com.mojang.authlib.GameProfile;
import org.jspecify.annotations.Nullable;

import logisticspipes.Translations;
import logisticspipes.api.provider.IRoutedPowerProvider;
import logisticspipes.interfaces.IBlockEntityMenuProvider;
import logisticspipes.interfaces.IScreenOpenController;
import logisticspipes.interfaces.ISecurityProvider;
import logisticspipes.network.to_client.security.SecurityStationCCIdsMessage;
import logisticspipes.network.to_client.security.SecurityStationFlagsMessage;
import logisticspipes.network.to_client.security.SecurityStationIdMessage;
import logisticspipes.network.to_client.security.SecurityStationSettingsMessage;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.security.SecuritySettings;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.world.inventory.SecurityStationMenu;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;

public class LogisticsSecurityBlockEntity extends LogisticsSolidBlockEntity
    implements IScreenOpenController, ISecurityProvider, IBlockEntityMenuProvider {

    public static final SecuritySettings allowAll = new SecuritySettings("");
    /** What a player the station has no entry for may do: nothing. Shared, so never written to. */
    private static final SecuritySettings noPermissions = new SecuritySettings("");
    public static PlayerCollectionList byPassed = new PlayerCollectionList();

    static {
        LogisticsSecurityBlockEntity.allowAll.openGui = true;
        LogisticsSecurityBlockEntity.allowAll.openRequest = true;
        LogisticsSecurityBlockEntity.allowAll.openUpgrades = true;
        LogisticsSecurityBlockEntity.allowAll.openNetworkMonitor = true;
        LogisticsSecurityBlockEntity.allowAll.removePipes = true;
    }

    public ItemIdentifierInventory inv = new ItemIdentifierInventory(1, "ID Slots", 64);
    public List<Integer> excludedCC = new ArrayList<>();
    public boolean allowCC = false;
    public boolean allowAutoDestroy = false;
    private final PlayerCollectionList listener = new PlayerCollectionList();
    @Nullable
    private UUID secId = null;
    /**
     * Settings by the id in the player's game profile, which survives a rename where the name
     * does not.
     */
    private final Map<UUID, SecuritySettings> settingsById = new HashMap<>();
    /**
     * Settings for names no profile has been found for yet, by lower-cased name: entries saved
     * before the station keyed by id, and names Mojang could not resolve. The first player seen
     * under the name claims the entry, and it moves to {@link #settingsById}.
     */
    private final Map<String, SecuritySettings> settingsByName = new HashMap<>();

    public LogisticsSecurityBlockEntity(BlockPos pos, BlockState state) {
        super(LPBlockEntityTypes.SECURITY_STATION.get(), pos, state);
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        if (!level.isClientSide()) {
            SimpleServiceLocator.securityStationManager.remove(this);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!level.isClientSide()) {
            SimpleServiceLocator.securityStationManager.add(this);
        }
    }

    public void deauthorizeStation() {
        SimpleServiceLocator.securityStationManager.deauthorizeUUID(getSecId());
    }

    public void authorizeStation() {
        SimpleServiceLocator.securityStationManager.authorizeUUID(getSecId());
    }

    @Override
    public void screenOpenedByPlayer(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                new SecurityStationFlagsMessage(getBlockPos(), allowCC, allowAutoDestroy));
            PacketDistributor.sendToPlayer(serverPlayer,
                new SecurityStationIdMessage(getBlockPos(), Optional.ofNullable(getSecId())));
        }
        SimpleServiceLocator.securityStationManager.sendClientAuthorizationList();
        listener.add(player);
    }

    @Override
    public void screenClosedByPlayer(Player player) {
        listener.remove(player);
    }

    @Nullable
    public UUID getSecId() {
        if (!level.isClientSide()) {
            if (secId == null) {
                secId = UUID.randomUUID();
            }
        }
        return secId;
    }

    public void setClientUUID(UUID id) {
        if (level.isClientSide()) {
            secId = id;
        }
    }

    public void setClientCC(boolean flag) {
        if (level.isClientSide()) {
            allowCC = flag;
        }
    }

    public void setClientDestroy(boolean flag) {
        if (level.isClientSide()) {
            allowAutoDestroy = flag;
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getString("UUID").ifPresent(raw -> secId = UUID.fromString(raw));
        allowCC = input.getBooleanOr("allowCC", false);
        allowAutoDestroy = input.getBooleanOr("allowAutoDestroy", false);
        inv.deserialize(input);
        settingsById.clear();
        settingsByName.clear();
        for (ValueInput entry : input.childrenListOrEmpty("settings")) {
            String name = entry.getStringOr("name", "");
            SecuritySettings settings = new SecuritySettings(name);
            settings.deserialize(entry.childOrEmpty("content"));
            if (settings.id != null) {
                settingsById.put(settings.id, settings);
            } else if (!name.isEmpty()) {
                settingsByName.put(nameKey(name), settings);
            }
        }
        excludedCC.clear();
        for (int id : input.getIntArray("excludedCC").orElse(new int[0])) {
            excludedCC.add(id);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        UUID secId = getSecId();
        if (secId != null) {
            output.putString("UUID", secId.toString());
        }
        output.putBoolean("allowCC", allowCC);
        output.putBoolean("allowAutoDestroy", allowAutoDestroy);
        inv.serialize(output);
        ValueOutput.ValueOutputList list = output.childrenList("settings");
        for (SecuritySettings settings : Iterables.concat(settingsById.values(), settingsByName.values())) {
            ValueOutput settingsEntry = list.addChild();
            settingsEntry.putString("name", Objects.requireNonNullElse(settings.name, ""));
            settingsEntry.putChild("content", settings);
        }
        output.putIntArray("excludedCC", excludedCC.stream().mapToInt(Integer::intValue).toArray());
    }

    public void handleCardAction(CardAction action, Player player) {
        switch (action) {
            case CLEAR:
                inv.setItem(0, ItemStack.EMPTY);
                break;
            case TAKE_ONE:
                inv.removeItem(0, 1);
                break;
            case GIVE_ONE:
                if (!useEnergy(10)) {
                    player.sendSystemMessage(Component.translatable(Translations.Chat.NO_ENERGY));
                    return;
                }
                if (inv.getIDStackInSlot(0) == null) {
                    ItemStack stack = new ItemStack(LPItems.SECURITY_CARD.get(), 1);
                    stack.set(LPDataComponents.UUID, getSecId());
                    inv.setItem(0, stack);
                } else {
                    ItemStack slot = inv.getItem(0);
                    if (slot.getCount() < 64) {
                        slot.grow(1);
                        slot.set(LPDataComponents.UUID, getSecId());
                        inv.setItem(0, slot);
                    }
                }
                break;
            case GIVE_STACK:
                if (!useEnergy(640)) {
                    player.sendSystemMessage(Component.translatable(Translations.Chat.NO_ENERGY));
                    return;
                }
                ItemStack stack = new ItemStack(LPItems.SECURITY_CARD.get(), 64);
                stack.set(LPDataComponents.UUID, getSecId());
                inv.setItem(0, stack);
                break;
        }
    }

    /**
     * Sends the settings for the player typed into the station's search bar to the one who typed it.
     *
     * <p>The name is resolved to a profile id first. A name the server has not seen sends the
     * lookup to Mojang, over the network, so that part runs off the server thread and the reply
     * goes out once it is back.
     */
    public void handleOpenSecurityPlayer(Player player, String name) {
        if (!(player instanceof ServerPlayer viewer) || !isValidName(name)) {
            return;
        }
        final MinecraftServer server = viewer.level().getServer();
        final ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            sendSettings(viewer, online.getGameProfile().name(), online.getUUID());
            return;
        }
        CompletableFuture.supplyAsync(() -> server.services().nameToIdCache().get(name), Util.nonCriticalIoPool())
            .thenAcceptAsync(resolved -> {
                if (!isRemoved()) {
                    sendSettings(viewer, resolved.map(NameAndId::name).orElse(name),
                        resolved.map(NameAndId::id).orElse(null));
                }
            }, server);
    }

    private void sendSettings(ServerPlayer viewer, String name, @Nullable UUID id) {
        final SecuritySettings setting = id != null ? settingsFor(id, name) : settingsByName.get(nameKey(name));
        PacketDistributor.sendToPlayer(viewer, new SecurityStationSettingsMessage(
            name, Optional.ofNullable(id), SecurityPermissions.of(setting != null ? setting : noPermissions)));
    }

    public void saveSecuritySettings(String name, Optional<UUID> id, SecurityPermissions permissions) {
        if (!isValidName(name)) {
            return;
        }
        SecuritySettings setting = id.isPresent() ? settingsFor(id.get(), name) : settingsByName.get(nameKey(name));
        if (setting == null) {
            setting = new SecuritySettings(name);
            setting.id = id.orElse(null);
            if (setting.id != null) {
                settingsById.put(setting.id, setting);
            } else {
                settingsByName.put(nameKey(name), setting);
            }
        }
        permissions.applyTo(setting);
        setChanged();
    }

    public SecuritySettings getSecuritySettingsForPlayer(Player entityplayer, boolean usePower) {
        if (LogisticsSecurityBlockEntity.byPassed.contains(entityplayer)) {
            return LogisticsSecurityBlockEntity.allowAll;
        }
        if (usePower && !useEnergy(10)) {
            entityplayer.sendSystemMessage(Component.translatable(Translations.Chat.NO_ENERGY));
            return new SecuritySettings("No Energy");
        }
        final GameProfile profile = entityplayer.getGameProfile();
        final SecuritySettings setting = settingsFor(profile.id(), profile.name());
        return setting != null ? setting : noPermissions;
    }

    /**
     * The settings for a resolved profile, or null if the station has none.
     *
     * <p>An entry still waiting under the profile's name is claimed here and moved under the id,
     * and an entry found by id takes the profile's current name, so the GUI shows what the player
     * is called now.
     */
    private @Nullable SecuritySettings settingsFor(UUID id, String name) {
        SecuritySettings setting = settingsById.get(id);
        if (setting == null) {
            setting = settingsByName.remove(nameKey(name));
            if (setting == null) {
                return null;
            }
            setting.id = id;
            settingsById.put(id, setting);
            setChanged();
        }
        if (!name.equals(setting.name)) {
            setting.name = name;
            setChanged();
        }
        return setting;
    }

    private static boolean isValidName(String name) {
        return !name.isEmpty() && StringUtil.isValidPlayerName(name);
    }

    /** Player names are unique regardless of case, as the server's own profile cache treats them. */
    private static String nameKey(String name) {
        return name.toLowerCase(Locale.ROOT);
    }

    public void toggleFlag(SecurityFlag flag) {
        switch (flag) {
            case ALLOW_CC -> allowCC = !allowCC;
            case AUTO_DESTROY -> allowAutoDestroy = !allowAutoDestroy;
        }
        listener.send(new SecurityStationFlagsMessage(getBlockPos(), allowCC, allowAutoDestroy));
    }

    public void addCCToList(Integer id) {
        if (!excludedCC.contains(id)) {
            excludedCC.add(id);
        }
        Collections.sort(excludedCC);
    }

    public void removeCCFromList(Integer id) {
        excludedCC.remove(id);
    }

    public void requestList(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            PacketDistributor.sendToPlayer(serverPlayer,
                new SecurityStationCCIdsMessage(getBlockPos(), List.copyOf(excludedCC)));
        }
    }

    public void setExcludedCC(List<Integer> ids) {
        excludedCC.clear();
        excludedCC.addAll(ids);
    }

    @Override
    public boolean getAllowCC(int id) {
        if (!useEnergy(10)) {
            return false;
        }
        return allowCC != excludedCC.contains(id);
    }

    @Override
    public boolean canAutomatedDestroy() {
        if (!useEnergy(10)) {
            return false;
        }
        return allowAutoDestroy;
    }

    private boolean useEnergy(int amount) {
        for (int i = 0; i < 4; i++) {
            BlockEntity be = level.getBlockEntity(getBlockPos().relative(Direction.values()[i + 2]));
            if (be instanceof IRoutedPowerProvider routedPowerProvider) {
                if (routedPowerProvider.useEnergy(amount)) {
                    return true;
                }
            }
            if (be instanceof LogisticsTileGenericPipe genericPipe) {
                if (genericPipe.pipe instanceof IRoutedPowerProvider routedPowerProvider) {
                    if (routedPowerProvider.useEnergy(amount)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(getBlockState().getBlock().getDescriptionId());
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new SecurityStationMenu(containerId, inventory, this);
    }

    /**
     * What the four buttons under the security station's card slot do.
     */
    public enum CardAction {
        /**
         * Empty the slot.
         */
        CLEAR,
        /**
         * Take one card out.
         */
        TAKE_ONE,
        /**
         * Put one card in.
         */
        GIVE_ONE,
        /**
         * Fill the slot.
         */
        GIVE_STACK,
    }

    /**
     * One of the two checkboxes the station's GUI shows.
     */
    public enum SecurityFlag {
        /**
         * Whether computers may talk to the network at all.
         */
        ALLOW_CC,
        /**
         * Whether the station destroys itself when it loses power.
         */
        AUTO_DESTROY,
    }

    /**
     * What one player is allowed to do at a security station.
     *
     * <p>The same six switches {@link SecuritySettings} holds, as a value that can travel. The
     * settings themselves stay mutable and keep their own NBT shape for the save file; this is only
     * the sent form, so a message never has to hand a raw {@code CompoundTag} to the security store.
     */
    public record SecurityPermissions(
        boolean openGui,
        boolean openRequest,
        boolean openUpgrades,
        boolean openNetworkMonitor,
        boolean removePipes,
        boolean accessRoutingChannels
    ) {

        public static final StreamCodec<RegistryFriendlyByteBuf, SecurityPermissions> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.BOOL, SecurityPermissions::openGui,
                ByteBufCodecs.BOOL, SecurityPermissions::openRequest,
                ByteBufCodecs.BOOL, SecurityPermissions::openUpgrades,
                ByteBufCodecs.BOOL, SecurityPermissions::openNetworkMonitor,
                ByteBufCodecs.BOOL, SecurityPermissions::removePipes,
                ByteBufCodecs.BOOL, SecurityPermissions::accessRoutingChannels,
                SecurityPermissions::new);

        public static SecurityPermissions of(SecuritySettings settings) {
            return new SecurityPermissions(
                settings.openGui,
                settings.openRequest,
                settings.openUpgrades,
                settings.openNetworkMonitor,
                settings.removePipes,
                settings.accessRoutingChannels);
        }

        public void applyTo(SecuritySettings settings) {
            settings.openGui = openGui;
            settings.openRequest = openRequest;
            settings.openUpgrades = openUpgrades;
            settings.openNetworkMonitor = openNetworkMonitor;
            settings.removePipes = removePipes;
            settings.accessRoutingChannels = accessRoutingChannels;
        }
    }
}
