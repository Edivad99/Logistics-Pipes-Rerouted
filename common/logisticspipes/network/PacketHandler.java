package logisticspipes.network;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import net.minecraft.client.Minecraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import com.mojang.serialization.Codec;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.exception.DelayPacketException;
import logisticspipes.network.bidirectional.FluidSupplierMinModeMessage;
import logisticspipes.network.bidirectional.FluidSupplierPartialsMessage;
import logisticspipes.network.to_client.module.AdvancedExtractorIncludeMessage;
import logisticspipes.network.to_client.block.BlockRotationMessage;
import logisticspipes.network.to_client.pipe.ChassisOrientationMessage;
import logisticspipes.network.to_client.block.CompilerStatusMessage;
import logisticspipes.network.to_client.crafting.CraftingDummyInventoryMessage;
import logisticspipes.network.to_client.crafting.CraftingModuleUpdateMessage;
import logisticspipes.network.to_client.crafting.CraftingTargetMessage;
import logisticspipes.network.to_client.block.DiskContentMessage;
import logisticspipes.network.to_client.pipe.FirewallFlagsMessage;
import logisticspipes.network.to_client.crafting.FluidCraftingAmountMessage;
import logisticspipes.network.to_client.pipe.FluidSupplierAmountMessage;
import logisticspipes.network.to_client.pipe.InvSysConResistanceMessage;
import logisticspipes.network.to_client.pipe.ItemAmountSignMessage;
import logisticspipes.network.to_client.module.ItemSinkDefaultRouteMessage;
import logisticspipes.network.to_client.module.ItemSinkImportedItemsMessage;
import logisticspipes.network.to_client.module.ModuleInventoryMessage;
import logisticspipes.network.to_client.module.ModulePropertiesMessage;
import logisticspipes.network.to_client.orderer.OrderWatchMessage;
import logisticspipes.network.to_client.orderer.OrdererWatchRemoveMessage;
import logisticspipes.network.to_client.module.OreDictItemSinkListMessage;
import logisticspipes.network.to_client.pipe.PipeItemBufferMessage;
import logisticspipes.network.to_client.pipe.PipeOrdersMessage;
import logisticspipes.network.to_client.pipe.PipePropertiesMessage;
import logisticspipes.network.to_client.pipe.PipeRenderUpdateMessage;
import logisticspipes.network.to_client.pipe.PipeSignTypesMessage;
import logisticspipes.network.to_client.pipe.PipeStatsMessage;
import logisticspipes.network.to_client.security.PlayerListMessage;
import logisticspipes.network.to_client.block.PowerJunctionLevelMessage;
import logisticspipes.network.to_client.block.PowerProviderLevelMessage;
import logisticspipes.network.to_client.module.QuickSortStateMessage;
import logisticspipes.network.to_client.orderer.RequestAnswerMessage;
import logisticspipes.network.to_client.orderer.RequestComponentsMessage;
import logisticspipes.network.to_client.pipe.SatelliteNameMessage;
import logisticspipes.network.to_client.security.SecurityAuthorizedListMessage;
import logisticspipes.network.to_client.security.SecurityStationCCIdsMessage;
import logisticspipes.network.to_client.security.SecurityStationFlagsMessage;
import logisticspipes.network.to_client.security.SecurityStationIdMessage;
import logisticspipes.network.to_client.security.SecurityStationSettingsMessage;
import logisticspipes.network.to_client.crafting.SlotFinderActivateMessage;
import logisticspipes.network.to_client.module.SneakyDirectionMessage;
import logisticspipes.network.to_client.module.StringBasedItemSinkListMessage;
import logisticspipes.network.to_server.block.BlockHudWatchMessage;
import logisticspipes.network.to_server.crafting.ChangeFluidCraftingAmountMessage;
import logisticspipes.network.to_server.orderer.DropDiskMessage;
import logisticspipes.network.to_server.orderer.RequestDiskContentMessage;
import logisticspipes.network.to_server.orderer.RequestDiskMacroMessage;
import logisticspipes.network.to_server.pipe.ChangeFluidSupplierAmountMessage;
import logisticspipes.network.to_server.crafting.ClearCraftingGridMessage;
import logisticspipes.network.to_server.crafting.CrafterCleanupImportMessage;
import logisticspipes.network.to_server.crafting.CrafterImportRecipeMessage;
import logisticspipes.network.to_server.crafting.CycleCraftingRecipeMessage;
import logisticspipes.network.to_server.crafting.ImportCraftingRecipeMessage;
import logisticspipes.network.to_server.module.ItemSinkImportRequestMessage;
import logisticspipes.network.to_server.module.ModuleWatchMessage;
import logisticspipes.network.to_server.pipe.RequestPipeSignsMessage;
import logisticspipes.network.to_server.security.OpenSecurityPlayerMessage;
import logisticspipes.network.to_server.pipe.OpenUpgradeConfigMessage;
import logisticspipes.network.to_server.pipe.PipeHudWatchMessage;
import logisticspipes.network.to_server.pipe.PipeOrderWatchMessage;
import logisticspipes.network.to_server.block.RequestBlockRotationMessage;
import logisticspipes.network.to_server.pipe.RequestChassisOrientationMessage;
import logisticspipes.network.to_server.orderer.RequestFluidOrdererRefreshMessage;
import logisticspipes.network.to_server.orderer.RequestOrdererRefreshMessage;
import logisticspipes.network.to_server.pipe.RequestPipeContentMessage;
import logisticspipes.network.to_server.pipe.RequestSatellitePipeListMessage;
import logisticspipes.network.to_server.security.RequestSecurityStationCCIdsMessage;
import logisticspipes.network.to_server.block.SaveDiskContentMessage;
import logisticspipes.network.to_server.security.SaveSecuritySettingsMessage;
import logisticspipes.network.to_server.security.SecurityCardActionMessage;
import logisticspipes.network.to_server.crafting.SetCraftingSatelliteMessage;
import logisticspipes.network.to_server.block.SetDiskNameMessage;
import logisticspipes.network.to_server.pipe.SetFirewallFlagsMessage;
import logisticspipes.network.to_server.pipe.SetInvSysConChannelMessage;
import logisticspipes.network.to_server.pipe.SetInvSysConResistanceMessage;
import logisticspipes.network.to_server.module.SetModulePropertiesMessage;
import logisticspipes.network.to_server.module.SetOreDictItemSinkListMessage;
import logisticspipes.network.to_server.pipe.SetPipePropertiesMessage;
import logisticspipes.network.to_server.pipe.SetSatelliteNameMessage;
import logisticspipes.network.to_server.security.SetSecurityStationAuthorizedMessage;
import logisticspipes.network.to_server.security.SetSecurityStationCCIdMessage;
import logisticspipes.network.to_server.module.SetSneakyDirectionMessage;
import logisticspipes.network.to_server.pipe.SetSneakyUpgradeSideMessage;
import logisticspipes.network.to_server.module.SetStringBasedItemSinkListMessage;
import logisticspipes.network.to_server.orderer.SimulateRequestMessage;
import logisticspipes.network.to_server.crafting.SlotFinderOpenGuiMessage;
import logisticspipes.network.to_server.crafting.SlotFinderSlotMessage;
import logisticspipes.network.to_server.orderer.SubmitFluidRequestMessage;
import logisticspipes.network.to_server.orderer.SubmitRequestMessage;
import logisticspipes.network.to_server.pipe.ToggleDisconnectionUpgradeSideMessage;
import logisticspipes.network.to_server.security.ToggleSecurityStationFlagMessage;
import logisticspipes.network.to_server.block.TrackItemMessage;
import logisticspipes.network.to_server.block.TriggerCompilerTaskMessage;
import logisticspipes.network.to_server.pipe.UntraceRoutingMessage;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.util.LPDataIOWrapper;
import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;
import logisticspipes.utils.StaticResolverUtil;

/**
 * Central packet registry and dispatcher for LogisticsPipes.
 *
 * Each LP packet registers as its own named {@link LPPayload} type; see {@link LPPayloadTypes}.
 * Registration happens in {@link LogisticsPipes} via {@code RegisterPayloadHandlersEvent}.
 */
public class PacketHandler {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            // The packet table has to exist before the payloads can be registered, and this event
            // fires well before FMLCommonSetup. Building it here is safe: it only scans the
            // classpath and constructs templates, with no game state involved.
            initialize();
            var registrar = event.registrar(LPConstants.ID).versioned("1");
            int registered = 0;
            for (LPPayloadTypes.Entry entry : LPPayloadTypes.all()) {
                // Both directions get the handler explicitly. The three-argument playBidirectional
                // only registers the *server* one and leaves the clientbound side null, which
                // 1.21.8 now rejects outright: "Some clientbound payloads are missing client-side
                // handlers". Every LP packet dispatches by its own type, so the same handler is
                // correct for both.
                registrar.playBidirectional(
                        entry.type(),
                        entry.codec(),
                        PacketHandler::handlePayload,
                        PacketHandler::handlePayload
                );
                registered++;
            }
            // Worth a line: a client and a server that registered different counts will fail to
            // negotiate, and this is the only place that number is visible.
            LogisticsPipes.LOG.info("Registered {} LogisticsPipes packet payloads", registered);

            registerClientToServer(registrar);
            registerServerToClient(registrar);
            registerBidirectional(registrar);
        });
    }

    /**
     * Messages that have left the {@link ModernPacket} hierarchy behind.
     *
     * <p>Listed by hand, and by direction: a payload record knows which way it travels, so it is
     * registered one way only and the wrong-way case stops being representable. The loop above
     * keeps serving the packets that have not migrated yet, which have to stay bidirectional
     * because a {@code ModernPacket} carries no direction.
     */
    private static void registerClientToServer(PayloadRegistrar registrar) {
        registrar.playToServer(ChangeFluidCraftingAmountMessage.TYPE,
                ChangeFluidCraftingAmountMessage.STREAM_CODEC, ChangeFluidCraftingAmountMessage::handle);
        registrar.playToServer(RequestSatellitePipeListMessage.TYPE,
                RequestSatellitePipeListMessage.STREAM_CODEC, RequestSatellitePipeListMessage::handle);
        registrar.playToServer(SetSneakyDirectionMessage.TYPE,
                SetSneakyDirectionMessage.STREAM_CODEC, SetSneakyDirectionMessage::handle);
        registrar.playToServer(BlockHudWatchMessage.TYPE,
                BlockHudWatchMessage.STREAM_CODEC, BlockHudWatchMessage::handle);
        registrar.playToServer(PipeHudWatchMessage.TYPE,
                PipeHudWatchMessage.STREAM_CODEC, PipeHudWatchMessage::handle);
        registrar.playToServer(CrafterCleanupImportMessage.TYPE,
                CrafterCleanupImportMessage.STREAM_CODEC, CrafterCleanupImportMessage::handle);
        registrar.playToServer(CrafterImportRecipeMessage.TYPE,
                CrafterImportRecipeMessage.STREAM_CODEC, CrafterImportRecipeMessage::handle);
        registrar.playToServer(ItemSinkImportRequestMessage.TYPE,
                ItemSinkImportRequestMessage.STREAM_CODEC, ItemSinkImportRequestMessage::handle);
        registrar.playToServer(SlotFinderOpenGuiMessage.TYPE,
                SlotFinderOpenGuiMessage.STREAM_CODEC, SlotFinderOpenGuiMessage::handle);
        registrar.playToServer(SlotFinderSlotMessage.TYPE,
                SlotFinderSlotMessage.STREAM_CODEC, SlotFinderSlotMessage::handle);
        registrar.playToServer(SetModulePropertiesMessage.TYPE,
                SetModulePropertiesMessage.STREAM_CODEC, SetModulePropertiesMessage::handle);
        registrar.playToServer(SetPipePropertiesMessage.TYPE,
                SetPipePropertiesMessage.STREAM_CODEC, SetPipePropertiesMessage::handle);
        registrar.playToServer(ChangeFluidSupplierAmountMessage.TYPE,
                ChangeFluidSupplierAmountMessage.STREAM_CODEC, ChangeFluidSupplierAmountMessage::handle);
        registrar.playToServer(RequestSecurityStationCCIdsMessage.TYPE,
                RequestSecurityStationCCIdsMessage.STREAM_CODEC, RequestSecurityStationCCIdsMessage::handle);
        registrar.playToServer(SecurityCardActionMessage.TYPE,
                SecurityCardActionMessage.STREAM_CODEC, SecurityCardActionMessage::handle);
        registrar.playToServer(SetSecurityStationAuthorizedMessage.TYPE,
                SetSecurityStationAuthorizedMessage.STREAM_CODEC, SetSecurityStationAuthorizedMessage::handle);
        registrar.playToServer(SetSecurityStationCCIdMessage.TYPE,
                SetSecurityStationCCIdMessage.STREAM_CODEC, SetSecurityStationCCIdMessage::handle);
        registrar.playToServer(ToggleSecurityStationFlagMessage.TYPE,
                ToggleSecurityStationFlagMessage.STREAM_CODEC, ToggleSecurityStationFlagMessage::handle);
        registrar.playToServer(RequestFluidOrdererRefreshMessage.TYPE,
                RequestFluidOrdererRefreshMessage.STREAM_CODEC, RequestFluidOrdererRefreshMessage::handle);
        registrar.playToServer(RequestOrdererRefreshMessage.TYPE,
                RequestOrdererRefreshMessage.STREAM_CODEC, RequestOrdererRefreshMessage::handle);
        registrar.playToServer(RequestBlockRotationMessage.TYPE,
                RequestBlockRotationMessage.STREAM_CODEC, RequestBlockRotationMessage::handle);
        registrar.playToServer(SetInvSysConResistanceMessage.TYPE,
                SetInvSysConResistanceMessage.STREAM_CODEC, SetInvSysConResistanceMessage::handle);
        registrar.playToServer(ClearCraftingGridMessage.TYPE,
                ClearCraftingGridMessage.STREAM_CODEC, ClearCraftingGridMessage::handle);
        registrar.playToServer(CycleCraftingRecipeMessage.TYPE,
                CycleCraftingRecipeMessage.STREAM_CODEC, CycleCraftingRecipeMessage::handle);
        registrar.playToServer(ImportCraftingRecipeMessage.TYPE,
                ImportCraftingRecipeMessage.STREAM_CODEC, ImportCraftingRecipeMessage::handle);
        registrar.playToServer(TrackItemMessage.TYPE,
                TrackItemMessage.STREAM_CODEC, TrackItemMessage::handle);
        registrar.playToServer(TriggerCompilerTaskMessage.TYPE,
                TriggerCompilerTaskMessage.STREAM_CODEC, TriggerCompilerTaskMessage::handle);
        registrar.playToServer(PipeOrderWatchMessage.TYPE,
                PipeOrderWatchMessage.STREAM_CODEC, PipeOrderWatchMessage::handle);
        registrar.playToServer(RequestChassisOrientationMessage.TYPE,
                RequestChassisOrientationMessage.STREAM_CODEC, RequestChassisOrientationMessage::handle);
        registrar.playToServer(RequestPipeSignsMessage.TYPE,
                RequestPipeSignsMessage.STREAM_CODEC, RequestPipeSignsMessage::handle);
        registrar.playToServer(DropDiskMessage.TYPE,
                DropDiskMessage.STREAM_CODEC, DropDiskMessage::handle);
        registrar.playToServer(RequestDiskContentMessage.TYPE,
                RequestDiskContentMessage.STREAM_CODEC, RequestDiskContentMessage::handle);
        registrar.playToServer(RequestDiskMacroMessage.TYPE,
                RequestDiskMacroMessage.STREAM_CODEC, RequestDiskMacroMessage::handle);
        registrar.playToServer(ModuleWatchMessage.TYPE,
                ModuleWatchMessage.STREAM_CODEC, ModuleWatchMessage::handle);
        registrar.playToServer(SetOreDictItemSinkListMessage.TYPE,
                SetOreDictItemSinkListMessage.STREAM_CODEC, SetOreDictItemSinkListMessage::handle);
        registrar.playToServer(SetStringBasedItemSinkListMessage.TYPE,
                SetStringBasedItemSinkListMessage.STREAM_CODEC,
                SetStringBasedItemSinkListMessage::handle);
        registrar.playToServer(SetCraftingSatelliteMessage.TYPE,
                SetCraftingSatelliteMessage.STREAM_CODEC, SetCraftingSatelliteMessage::handle);
        registrar.playToServer(RequestPipeContentMessage.TYPE,
                RequestPipeContentMessage.STREAM_CODEC, RequestPipeContentMessage::handle);
        registrar.playToServer(UntraceRoutingMessage.TYPE,
                UntraceRoutingMessage.STREAM_CODEC, UntraceRoutingMessage::handle);
        registrar.playToServer(SetFirewallFlagsMessage.TYPE,
                SetFirewallFlagsMessage.STREAM_CODEC, SetFirewallFlagsMessage::handle);
        registrar.playToServer(SaveDiskContentMessage.TYPE,
                SaveDiskContentMessage.STREAM_CODEC, SaveDiskContentMessage::handle);
        registrar.playToServer(SaveSecuritySettingsMessage.TYPE,
                SaveSecuritySettingsMessage.STREAM_CODEC, SaveSecuritySettingsMessage::handle);
        registrar.playToServer(SubmitRequestMessage.TYPE,
                SubmitRequestMessage.STREAM_CODEC, SubmitRequestMessage::handle);
        registrar.playToServer(SimulateRequestMessage.TYPE,
                SimulateRequestMessage.STREAM_CODEC, SimulateRequestMessage::handle);
        registrar.playToServer(SubmitFluidRequestMessage.TYPE,
                SubmitFluidRequestMessage.STREAM_CODEC, SubmitFluidRequestMessage::handle);
        registrar.playToServer(SetSneakyUpgradeSideMessage.TYPE,
                SetSneakyUpgradeSideMessage.STREAM_CODEC, SetSneakyUpgradeSideMessage::handle);
        registrar.playToServer(ToggleDisconnectionUpgradeSideMessage.TYPE,
                ToggleDisconnectionUpgradeSideMessage.STREAM_CODEC, ToggleDisconnectionUpgradeSideMessage::handle);
        registrar.playToServer(OpenUpgradeConfigMessage.TYPE,
                OpenUpgradeConfigMessage.STREAM_CODEC, OpenUpgradeConfigMessage::handle);
        registrar.playToServer(SetInvSysConChannelMessage.TYPE,
                SetInvSysConChannelMessage.STREAM_CODEC, SetInvSysConChannelMessage::handle);
        registrar.playToServer(SetSatelliteNameMessage.TYPE,
                SetSatelliteNameMessage.STREAM_CODEC, SetSatelliteNameMessage::handle);
        registrar.playToServer(SetDiskNameMessage.TYPE,
                SetDiskNameMessage.STREAM_CODEC, SetDiskNameMessage::handle);
        registrar.playToServer(OpenSecurityPlayerMessage.TYPE,
                OpenSecurityPlayerMessage.STREAM_CODEC, OpenSecurityPlayerMessage::handle);
    }

    /**
     * Messages that mean the same thing whichever way they travel, so both sides run the same
     * handler. See {@code logisticspipes.network.bidirectional} for what belongs here.
     */
    private static void registerBidirectional(PayloadRegistrar registrar) {
        registrar.playBidirectional(FluidSupplierMinModeMessage.TYPE,
                FluidSupplierMinModeMessage.STREAM_CODEC,
                FluidSupplierMinModeMessage::handle, FluidSupplierMinModeMessage::handle);
        registrar.playBidirectional(FluidSupplierPartialsMessage.TYPE,
                FluidSupplierPartialsMessage.STREAM_CODEC,
                FluidSupplierPartialsMessage::handle, FluidSupplierPartialsMessage::handle);
    }

    private static void registerServerToClient(PayloadRegistrar registrar) {
        registrar.playToClient(FluidCraftingAmountMessage.TYPE,
                FluidCraftingAmountMessage.STREAM_CODEC, FluidCraftingAmountMessage::handle);
        registrar.playToClient(SneakyDirectionMessage.TYPE,
                SneakyDirectionMessage.STREAM_CODEC, SneakyDirectionMessage::handle);
        registrar.playToClient(ItemSinkImportedItemsMessage.TYPE,
                ItemSinkImportedItemsMessage.STREAM_CODEC, ItemSinkImportedItemsMessage::handle);
        registrar.playToClient(SlotFinderActivateMessage.TYPE,
                SlotFinderActivateMessage.STREAM_CODEC, SlotFinderActivateMessage::handle);
        registrar.playToClient(ModulePropertiesMessage.TYPE,
                ModulePropertiesMessage.STREAM_CODEC, ModulePropertiesMessage::handle);
        registrar.playToClient(PipePropertiesMessage.TYPE,
                PipePropertiesMessage.STREAM_CODEC, PipePropertiesMessage::handle);
        registrar.playToClient(FluidSupplierAmountMessage.TYPE,
                FluidSupplierAmountMessage.STREAM_CODEC, FluidSupplierAmountMessage::handle);
        registrar.playToClient(SecurityStationFlagsMessage.TYPE,
                SecurityStationFlagsMessage.STREAM_CODEC, SecurityStationFlagsMessage::handle);
        registrar.playToClient(SecurityStationIdMessage.TYPE,
                SecurityStationIdMessage.STREAM_CODEC, SecurityStationIdMessage::handle);
        registrar.playToClient(OrdererWatchRemoveMessage.TYPE,
                OrdererWatchRemoveMessage.STREAM_CODEC, OrdererWatchRemoveMessage::handle);
        registrar.playToClient(BlockRotationMessage.TYPE,
                BlockRotationMessage.STREAM_CODEC, BlockRotationMessage::handle);
        registrar.playToClient(InvSysConResistanceMessage.TYPE,
                InvSysConResistanceMessage.STREAM_CODEC, InvSysConResistanceMessage::handle);
        registrar.playToClient(PipeRenderUpdateMessage.TYPE,
                PipeRenderUpdateMessage.STREAM_CODEC, PipeRenderUpdateMessage::handle);
        registrar.playToClient(PowerJunctionLevelMessage.TYPE,
                PowerJunctionLevelMessage.STREAM_CODEC, PowerJunctionLevelMessage::handle);
        registrar.playToClient(PowerProviderLevelMessage.TYPE,
                PowerProviderLevelMessage.STREAM_CODEC, PowerProviderLevelMessage::handle);
        registrar.playToClient(CraftingTargetMessage.TYPE,
                CraftingTargetMessage.STREAM_CODEC, CraftingTargetMessage::handle);
        registrar.playToClient(CompilerStatusMessage.TYPE,
                CompilerStatusMessage.STREAM_CODEC, CompilerStatusMessage::handle);
        registrar.playToClient(ChassisOrientationMessage.TYPE,
                ChassisOrientationMessage.STREAM_CODEC, ChassisOrientationMessage::handle);
        registrar.playToClient(PipeStatsMessage.TYPE,
                PipeStatsMessage.STREAM_CODEC, PipeStatsMessage::handle);
        registrar.playToClient(RequestAnswerMessage.TYPE,
                RequestAnswerMessage.STREAM_CODEC, RequestAnswerMessage::handle);
        registrar.playToClient(RequestComponentsMessage.TYPE,
                RequestComponentsMessage.STREAM_CODEC, RequestComponentsMessage::handle);
        registrar.playToClient(OrderWatchMessage.TYPE,
                OrderWatchMessage.STREAM_CODEC, OrderWatchMessage::handle);
        registrar.playToClient(PipeOrdersMessage.TYPE,
                PipeOrdersMessage.STREAM_CODEC, PipeOrdersMessage::handle);
        registrar.playToClient(PipeSignTypesMessage.TYPE,
                PipeSignTypesMessage.STREAM_CODEC, PipeSignTypesMessage::handle);
        registrar.playToClient(PipeItemBufferMessage.TYPE,
                PipeItemBufferMessage.STREAM_CODEC, PipeItemBufferMessage::handle);
        registrar.playToClient(CraftingModuleUpdateMessage.TYPE,
                CraftingModuleUpdateMessage.STREAM_CODEC, CraftingModuleUpdateMessage::handle);
        registrar.playToClient(ModuleInventoryMessage.TYPE,
                ModuleInventoryMessage.STREAM_CODEC, ModuleInventoryMessage::handle);
        registrar.playToClient(OreDictItemSinkListMessage.TYPE,
                OreDictItemSinkListMessage.STREAM_CODEC, OreDictItemSinkListMessage::handle);
        registrar.playToClient(StringBasedItemSinkListMessage.TYPE,
                StringBasedItemSinkListMessage.STREAM_CODEC,
                StringBasedItemSinkListMessage::handle);
        registrar.playToClient(ItemSinkDefaultRouteMessage.TYPE,
                ItemSinkDefaultRouteMessage.STREAM_CODEC, ItemSinkDefaultRouteMessage::handle);
        registrar.playToClient(AdvancedExtractorIncludeMessage.TYPE,
                AdvancedExtractorIncludeMessage.STREAM_CODEC, AdvancedExtractorIncludeMessage::handle);
        registrar.playToClient(QuickSortStateMessage.TYPE,
                QuickSortStateMessage.STREAM_CODEC, QuickSortStateMessage::handle);
        registrar.playToClient(PlayerListMessage.TYPE,
                PlayerListMessage.STREAM_CODEC, PlayerListMessage::handle);
        registrar.playToClient(FirewallFlagsMessage.TYPE,
                FirewallFlagsMessage.STREAM_CODEC, FirewallFlagsMessage::handle);
        registrar.playToClient(DiskContentMessage.TYPE,
                DiskContentMessage.STREAM_CODEC, DiskContentMessage::handle);
        registrar.playToClient(SecurityStationSettingsMessage.TYPE,
                SecurityStationSettingsMessage.STREAM_CODEC, SecurityStationSettingsMessage::handle);
        registrar.playToClient(SecurityStationCCIdsMessage.TYPE,
                SecurityStationCCIdsMessage.STREAM_CODEC, SecurityStationCCIdsMessage::handle);
        registrar.playToClient(SatelliteNameMessage.TYPE,
                SatelliteNameMessage.STREAM_CODEC, SatelliteNameMessage::handle);
        registrar.playToClient(CraftingDummyInventoryMessage.TYPE,
                CraftingDummyInventoryMessage.STREAM_CODEC, CraftingDummyInventoryMessage::handle);
        registrar.playToClient(ItemAmountSignMessage.TYPE,
                ItemAmountSignMessage.STREAM_CODEC, ItemAmountSignMessage::handle);
        registrar.playToClient(SecurityAuthorizedListMessage.TYPE,
                SecurityAuthorizedListMessage.STREAM_CODEC, SecurityAuthorizedListMessage::handle);
    }

    private static void handlePayload(LPPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> onPacketData(payload.packet(), context.player()));
    }

    public static final Map<Integer, StackTraceElement[]> debugMap = new HashMap<>();
    /** Null until {@link #initialize()} has run; null slots are packets absent on this side. */
    public static @Nullable List<@Nullable ModernPacket> packetlist;
    /** Null until {@link #initialize()} has run. */
    public static @Nullable Map<Class<? extends ModernPacket>, ModernPacket> packetmap;
    private static int packetDebugID = 1;

    // ── Registration ─────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    public static <T extends ModernPacket> T getPacket(Class<T> clazz) {
        T packet = (T) PacketHandler.packetmap.get(clazz).template();
        if (LogisticsPipes.isDEBUG() && MainProxy.proxy.getSide().equals("Client")) {
            StackTraceElement[] trace = Thread.currentThread().getStackTrace();
            synchronized (PacketHandler.debugMap) {
                int id = PacketHandler.packetDebugID++;
                PacketHandler.debugMap.put(id, trace);
                packet.setDebugId(id);
            }
        }
        return packet;
    }

    /**
     * Enumerates all ModernPacket subclasses, builds the templates and derives the payload types.
     *
     * <p>Idempotent: it runs from {@code RegisterPayloadHandlersEvent}, which needs the table to
     * register the payloads, and the later call from common setup then finds it already built.
     */
    public static void initialize() {
        if (PacketHandler.packetmap != null && !PacketHandler.packetmap.isEmpty()) {
            return;
        }
        Set<Class<? extends ModernPacket>> classes = StaticResolverUtil.findClassesByType(ModernPacket.class);
        loadPackets(classes);
        if (PacketHandler.packetmap.isEmpty()) {
            throw new RuntimeException("Cannot load Packet Classes");
        }
        LPPayloadTypes.build(PacketHandler.packetlist);
    }

    private static void loadPackets(Set<Class<? extends ModernPacket>> classesIn) {
        List<Class<? extends ModernPacket>> classes = classesIn.stream()
                .sorted(Comparator.comparing(Class::getCanonicalName))
                .collect(Collectors.toList());

        // Packet IDs are the index in the sorted class list, so they are IDENTICAL on the client
        // and the dedicated server even if a packet fails to construct on one side (e.g. a class
        // the RuntimeDistCleaner refuses to link). A success-counter would shift every later ID
        // and desync the protocol. Failed slots stay null; the dispatch path guards against that.
        PacketHandler.packetlist = new ArrayList<>(java.util.Collections.nCopies(classes.size(), (ModernPacket) null));
        PacketHandler.packetmap = new HashMap<>(classes.size());

        for (int id = 0; id < classes.size(); id++) {
            Class<? extends ModernPacket> cls = classes.get(id);
            try {
                final ModernPacket instance = cls.getConstructor(int.class).newInstance(id);
                PacketHandler.packetlist.set(id, instance);
                PacketHandler.packetmap.put(cls, instance);
            } catch (Throwable t) {
                LogisticsPipes.LOG.error("Failed to load packet (id " + id + ") " + cls.getName(), t);
            }
        }
    }

    // ── Binary serialization ─────────────────────────────────────────────────

    /**
     * Writes a self-describing packet: payload name, debug id, body.
     *
     * <p>Used by the buffered/compressed channel, which batches several packets into one blob and
     * so has to carry its own discriminator. It is a name rather than the old numeric id for the
     * same reason the payload types are: the number was an index into a classpath scan.
     */
    public static void writeNamedPacket(LPDataOutput output, ModernPacket packet) {
        output.writeUTF(LPPayloadTypes.entryFor(packet).name().toString());
        output.writeInt(packet.getDebugId());
        packet.writeData(output);
    }

    /**
     * Reads back what {@link #writeNamedPacket} wrote.
     *
     * @return the decoded packet, or null when this side has no packet class under that name --
     *         which no longer disturbs the packets batched after it, since each one re-announces
     *         its own name
     */
    public static @Nullable ModernPacket readNamedPacket(LPDataInput input) {
        final Identifier name = Identifier.parse(Objects.requireNonNull(input.readUTF()));
        final LPPayloadTypes.Entry entry = LPPayloadTypes.entryFor(name);
        if (entry == null) {
            LogisticsPipes.LOG.error("Received unknown LP packet {}", name);
            return null;
        }
        final ModernPacket packet = entry.template().template();
        packet.setDebugId(input.readInt());
        packet.readData(input);
        return packet;
    }

    /** NBT key holding the payload name of an embedded packet. */
    private static final String NBT_PACKET_NAME = "LogisticsPipes:PacketName";
    /** NBT key holding the debug id and body of an embedded packet. */
    private static final String NBT_PACKET_DATA = "LogisticsPipes:PacketData";

    /**
     * Embeds a packet in a block entity's update tag, the one path that carries an LP packet
     * outside the payload channel. The packet is named here too, for the same reason it is named
     * when sent: nothing should depend on a numeric id whose value comes from a classpath scan.
     *
     * <p>This rides {@code getUpdateTag}, which is chunk sync and never reaches disk, so the
     * encoding is free to change with the mod.
     */
    public static void addPacketToNBT(ModernPacket packet, CompoundTag nbt) {
        nbt.putString(NBT_PACKET_NAME, LPPayloadTypes.entryFor(packet).name().toString());
        nbt.putByteArray(NBT_PACKET_DATA, LPDataIOWrapper.collectData(output -> {
            output.writeInt(packet.getDebugId());
            packet.writeData(output);
        }));
    }

    /**
     * Reads back the packet {@link #addPacketToNBT} embedded in an update tag and queues it.
     *
     * <p>Takes a {@link ValueInput} because that is what {@code handleUpdateTag} hands out since
     * 1.21.6. It has no byte-array accessor, so the payload comes back through
     * {@code Codec.BYTE_BUFFER}, which NbtOps maps onto the same ByteArrayTag the writer produces.
     * The keys are no longer removed afterwards -- a ValueInput is read-only, and leaving them
     * costs nothing since the block entity ignores unknown keys.</p>
     */
    public static void queuePacketFromUpdateTag(ValueInput input) {
        byte[] data = input.read(NBT_PACKET_DATA, Codec.BYTE_BUFFER)
            .map(buffer -> {
                byte[] copy = new byte[buffer.remaining()];
                buffer.duplicate().get(copy);
                return copy;
            })
            .orElse(new byte[0]);
        if (data.length == 0) {
            return;
        }
        final Identifier name = Identifier.parse(input.getStringOr(NBT_PACKET_NAME, ""));
        final LPPayloadTypes.Entry entry = LPPayloadTypes.entryFor(name);
        if (entry == null) {
            LogisticsPipes.LOG.error("Update tag carries unknown LP packet {}", name);
            return;
        }
        LPDataIOWrapper.provideData(data, Minecraft.getInstance().getConnection() != null ? Minecraft.getInstance().getConnection().registryAccess() : null, dataInput -> {
            final ModernPacket packet = entry.template().template();
            packet.setDebugId(dataInput.readInt());
            packet.readData(dataInput);
            Player localPlayer = Minecraft.getInstance().player;
            SimpleServiceLocator.clientBufferHandler.queuePacket(packet, Objects.requireNonNull(localPlayer));
        });
    }

    // ── Sending ──────────────────────────────────────────────────────────────

    private static LPPayload buildPayload(ModernPacket msg) {
        return LPPayloadTypes.payloadFor(msg);
    }

    public static LPPayload buildPayloadPublic(ModernPacket msg) {
        return buildPayload(msg);
    }

    /** Sends a packet from the client to the server. Must only be called client-side. */
    public static void sendToServer(ModernPacket msg) {
        ClientPacketDistributor.sendToServer(buildPayload(msg));
    }

    /** Sends a packet from the server to a specific player. Must only be called server-side. */
    public static void sendToPlayer(ModernPacket msg, Player player) {
        if (!(player instanceof ServerPlayer sp)) {
            LogisticsPipes.LOG.warn("sendToPlayer: player is not a ServerPlayer, skipping");
            return;
        }

        PacketDistributor.sendToPlayer(
                sp,
                buildPayload(msg)
        );
    }

    /** Sends a packet to every connected player. Must only be called server-side. */
    public static void sendToAll(ModernPacket msg) {
        PacketDistributor.sendToAllPlayers(
                buildPayload(msg)
        );
    }

    /** Decodes one packet of the buffered/compressed channel and dispatches it. */
    public static void onPacketData(final LPDataInput data, final Player player) {
        final ModernPacket packet = readNamedPacket(data);
        if (packet != null) {
            onPacketData(packet, player);
        }
    }

    /** Processes a fully-decoded ModernPacket on the correct thread. */
    public static void onPacketData(ModernPacket packet, final Player player) {
        try {
            packet.processPacket(player);
            if (LogisticsPipes.isDEBUG()) {
                PacketHandler.debugMap.remove(packet.getDebugId());
            }
        } catch (DelayPacketException e) {
            if (packet.retry() && MainProxy.isClient(player.level())) {
                SimpleServiceLocator.clientBufferHandler.queuePacket(packet, player);
            } else if (LogisticsPipes.isDEBUG()) {
                LogisticsPipes.LOG.error(packet.getClass().getName());
                LogisticsPipes.LOG.error(packet.toString());
                LogisticsPipes.LOG.error("Packet handling error", e);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
