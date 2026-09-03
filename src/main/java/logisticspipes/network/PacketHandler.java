package logisticspipes.network;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.network.bidirectional.DebugConnectionDataMessage;
import logisticspipes.network.bidirectional.FluidSupplierMinModeMessage;
import logisticspipes.network.bidirectional.FluidSupplierPartialsMessage;
import logisticspipes.network.bidirectional.FuzzySlotFlagsMessage;
import logisticspipes.network.to_client.block.MultiBlockPositionMessage;
import logisticspipes.network.to_client.block.RunningCraftingTasksMessage;
import logisticspipes.network.to_client.block.TrackableItemsMessage;
import logisticspipes.network.to_client.config.PlayerConfigMessage;
import logisticspipes.network.to_client.crafting.LikelyRecipeComponentsMessage;
import logisticspipes.network.to_client.debug.AskForDebugTargetMessage;
import logisticspipes.network.to_client.debug.OpenDebugPanelMessage;
import logisticspipes.network.to_client.debug.RoutingDebugCandidateListMessage;
import logisticspipes.network.to_client.debug.RoutingDebugCandidateMessage;
import logisticspipes.network.to_client.debug.RoutingDebugClearMessage;
import logisticspipes.network.to_client.debug.RoutingDebugClosedSetMessage;
import logisticspipes.network.to_client.debug.RoutingDebugDoneMessage;
import logisticspipes.network.to_client.debug.RoutingDebugFiltersMessage;
import logisticspipes.network.to_client.debug.RoutingDebugInitMessage;
import logisticspipes.network.to_client.debug.RoutingDebugSourceMessage;
import logisticspipes.network.to_client.debug.SendLogLineMessage;
import logisticspipes.network.to_client.debug.SendLogWindowMessage;
import logisticspipes.network.to_client.debug.ToggleClientPipeDebugMessage;
import logisticspipes.network.to_client.debug.UpdateStatusEntriesMessage;
import logisticspipes.network.to_client.gui.OpenChatGuiMessage;
import logisticspipes.network.to_client.module.AdvancedExtractorIncludeMessage;
import logisticspipes.network.to_client.block.BlockRotationMessage;
import logisticspipes.network.to_client.module.QuickSortMarkerMessage;
import logisticspipes.network.to_client.orderer.OrderManagerContentMessage;
import logisticspipes.network.to_client.orderer.OrdererContentMessage;
import logisticspipes.network.to_client.pipe.ChassisModuleContentMessage;
import logisticspipes.network.to_client.pipe.ChassisOrientationMessage;
import logisticspipes.network.to_client.block.CompilerStatusMessage;
import logisticspipes.network.to_client.crafting.CraftingDummyInventoryMessage;
import logisticspipes.network.to_client.crafting.CraftingModuleUpdateMessage;
import logisticspipes.network.to_client.crafting.CraftingTargetMessage;
import logisticspipes.network.to_client.block.DiskContentMessage;
import logisticspipes.network.to_client.pipe.ChestContentMessage;
import logisticspipes.network.to_client.pipe.FirewallFlagsMessage;
import logisticspipes.network.to_client.crafting.FluidCraftingAmountMessage;
import logisticspipes.network.to_client.pipe.FluidSupplierAmountMessage;
import logisticspipes.network.to_client.pipe.InvSysConContentMessage;
import logisticspipes.network.to_client.pipe.InvSysConResistanceMessage;
import logisticspipes.network.to_client.pipe.ItemAmountSignMessage;
import logisticspipes.network.to_client.module.ItemSinkDefaultRouteMessage;
import logisticspipes.network.to_client.module.ItemSinkImportedItemsMessage;
import logisticspipes.network.to_client.module.ModuleInventoryMessage;
import logisticspipes.network.to_client.module.ModulePropertiesMessage;
import logisticspipes.network.to_client.orderer.OrderWatchMessage;
import logisticspipes.network.to_client.orderer.OrdererWatchRemoveMessage;
import logisticspipes.network.to_client.module.OreDictItemSinkListMessage;
import logisticspipes.network.to_client.pipe.PipeFluidUpdateMessage;
import logisticspipes.network.to_client.pipe.PipeItemBufferMessage;
import logisticspipes.network.to_client.pipe.PipeOrdersMessage;
import logisticspipes.network.to_client.pipe.PipePropertiesMessage;
import logisticspipes.network.to_client.pipe.PipeRenderUpdateMessage;
import logisticspipes.network.to_client.pipe.PipeSignTypesMessage;
import logisticspipes.network.to_client.pipe.PipeStateMessage;
import logisticspipes.network.to_client.pipe.PipeStatsMessage;
import logisticspipes.network.to_client.pipe.PowerLaserMessage;
import logisticspipes.network.to_client.pipe.RoutingLasersMessage;
import logisticspipes.network.to_client.pipe.SatelliteNameResultMessage;
import logisticspipes.network.to_client.pipe.SatellitePipeListMessage;
import logisticspipes.network.to_client.pipe.SendQueueContentMessage;
import logisticspipes.network.to_client.pipe.TravellingItemContentMessage;
import logisticspipes.network.to_client.pipe.TravellingItemPositionMessage;
import logisticspipes.network.to_client.pipe.UpgradeConfigPopupMessage;
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
import logisticspipes.network.to_server.block.OpenLogicControllerMessage;
import logisticspipes.network.to_server.block.PowerJunctionCheatMessage;
import logisticspipes.network.to_server.block.RequestRunningCraftingTasksMessage;
import logisticspipes.network.to_server.block.RequestTrackableItemsMessage;
import logisticspipes.network.to_server.channel.DeleteChannelMessage;
import logisticspipes.network.to_server.channel.SaveChannelMessage;
import logisticspipes.network.to_server.config.SetHudSettingMessage;
import logisticspipes.network.to_server.config.SetPlayerConfigMessage;
import logisticspipes.network.to_server.crafting.ChangeFluidCraftingAmountMessage;
import logisticspipes.network.to_server.crafting.FindLikelyRecipeComponentsMessage;
import logisticspipes.network.to_server.debug.DebugTargetMessage;
import logisticspipes.network.to_server.gui.DummySlotClickMessage;
import logisticspipes.network.to_server.gui.SetGhostSlotMessage;
import logisticspipes.network.to_server.module.OpenAttachedCrafterGuiMessage;
import logisticspipes.network.to_client.channel.ChannelInformationMessage;
import logisticspipes.network.to_client.channel.ChannelManagerPopupMessage;
import logisticspipes.network.to_client.channel.ChannelSelectPopupMessage;
import logisticspipes.network.to_server.channel.RequestChannelManagerMessage;
import logisticspipes.network.to_server.channel.RequestChannelSelectMessage;
import logisticspipes.network.to_server.module.OpenChassisGuiMessage;
import logisticspipes.network.to_server.module.OpenChassisModuleGuiMessage;
import logisticspipes.network.to_server.module.OpenSneakyDirectionGuiMessage;
import logisticspipes.network.to_server.module.QuickSortChestWatchMessage;
import logisticspipes.network.to_server.orderer.DropDiskMessage;
import logisticspipes.network.to_server.orderer.RequestDiskContentMessage;
import logisticspipes.network.to_server.orderer.RequestDiskMacroMessage;
import logisticspipes.network.to_server.orderer.SubmitRequestListMessage;
import logisticspipes.network.to_server.pipe.ChangeFluidSupplierAmountMessage;
import logisticspipes.network.to_server.crafting.ClearCraftingGridMessage;
import logisticspipes.network.to_server.crafting.CrafterCleanupImportMessage;
import logisticspipes.network.to_server.crafting.CrafterImportRecipeMessage;
import logisticspipes.network.to_server.crafting.CycleCraftingRecipeMessage;
import logisticspipes.network.to_server.crafting.ImportCraftingRecipeMessage;
import logisticspipes.network.to_server.module.ItemSinkImportRequestMessage;
import logisticspipes.network.to_server.module.ModuleWatchMessage;
import logisticspipes.network.to_server.pipe.RequestInvSysConContentMessage;
import logisticspipes.network.to_server.pipe.RequestPipeSignsMessage;
import logisticspipes.network.to_server.pipe.RequestRoutingLasersMessage;
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
import logisticspipes.network.to_server.security.RequestPlayerListMessage;
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

/**
 * Where every LogisticsPipes packet is registered, by hand and by direction.
 *
 * <p>Registration happens from {@code RegisterPayloadHandlersEvent}. A payload record knows which
 * way it travels, so it is registered one way only and the wrong-way case stops being
 * representable.
 */
public class PacketHandler {

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(RegisterPayloadHandlersEvent.class, event -> {
            var registrar = event.registrar(LPConstants.ID).versioned("1");
            registerClientToServer(registrar);
            registerServerToClient(registrar);
            registerBidirectional(registrar);
        });
    }

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
        registrar.playToServer(RequestRunningCraftingTasksMessage.TYPE,
                RequestRunningCraftingTasksMessage.STREAM_CODEC, RequestRunningCraftingTasksMessage::handle);
        registrar.playToServer(RequestTrackableItemsMessage.TYPE,
                RequestTrackableItemsMessage.STREAM_CODEC, RequestTrackableItemsMessage::handle);
        registrar.playToServer(DummySlotClickMessage.TYPE,
                DummySlotClickMessage.STREAM_CODEC, DummySlotClickMessage::handle);
        registrar.playToServer(SetHudSettingMessage.TYPE,
                SetHudSettingMessage.STREAM_CODEC, SetHudSettingMessage::handle);
        registrar.playToServer(DeleteChannelMessage.TYPE,
                DeleteChannelMessage.STREAM_CODEC, DeleteChannelMessage::handle);
        registrar.playToServer(RequestPlayerListMessage.TYPE,
                RequestPlayerListMessage.STREAM_CODEC, RequestPlayerListMessage::handle);
        registrar.playToServer(SaveChannelMessage.TYPE,
                SaveChannelMessage.STREAM_CODEC, SaveChannelMessage::handle);
        registrar.playToServer(DebugTargetMessage.TYPE,
                DebugTargetMessage.STREAM_CODEC, DebugTargetMessage::handle);
        registrar.playToServer(SetPlayerConfigMessage.TYPE,
                SetPlayerConfigMessage.STREAM_CODEC, SetPlayerConfigMessage::handle);
        registrar.playToServer(QuickSortChestWatchMessage.TYPE,
                QuickSortChestWatchMessage.STREAM_CODEC, QuickSortChestWatchMessage::handle);
        registrar.playToServer(RequestInvSysConContentMessage.TYPE,
                RequestInvSysConContentMessage.STREAM_CODEC, RequestInvSysConContentMessage::handle);
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
        registrar.playToServer(SubmitRequestListMessage.TYPE,
                SubmitRequestListMessage.STREAM_CODEC, SubmitRequestListMessage::handle);
        registrar.playToServer(RequestRoutingLasersMessage.TYPE,
                RequestRoutingLasersMessage.STREAM_CODEC, RequestRoutingLasersMessage::handle);
        registrar.playToServer(FindLikelyRecipeComponentsMessage.TYPE,
                FindLikelyRecipeComponentsMessage.STREAM_CODEC, FindLikelyRecipeComponentsMessage::handle);
        registrar.playToServer(SetGhostSlotMessage.TYPE,
                SetGhostSlotMessage.STREAM_CODEC, SetGhostSlotMessage::handle);
        registrar.playToServer(PowerJunctionCheatMessage.TYPE,
                PowerJunctionCheatMessage.STREAM_CODEC, PowerJunctionCheatMessage::handle);
        registrar.playToServer(OpenLogicControllerMessage.TYPE,
                OpenLogicControllerMessage.STREAM_CODEC, OpenLogicControllerMessage::handle);
        registrar.playToServer(OpenChassisModuleGuiMessage.TYPE,
                OpenChassisModuleGuiMessage.STREAM_CODEC, OpenChassisModuleGuiMessage::handle);
        registrar.playToServer(RequestChannelSelectMessage.TYPE,
                RequestChannelSelectMessage.STREAM_CODEC, RequestChannelSelectMessage::handle);
        registrar.playToServer(RequestChannelManagerMessage.TYPE,
                RequestChannelManagerMessage.STREAM_CODEC, RequestChannelManagerMessage::handle);
        registrar.playToClient(ChannelInformationMessage.TYPE,
                ChannelInformationMessage.STREAM_CODEC, ChannelInformationMessage::handle);
        registrar.playToClient(ChannelSelectPopupMessage.TYPE,
                ChannelSelectPopupMessage.STREAM_CODEC, ChannelSelectPopupMessage::handle);
        registrar.playToClient(ChannelManagerPopupMessage.TYPE,
                ChannelManagerPopupMessage.STREAM_CODEC, ChannelManagerPopupMessage::handle);
        registrar.playToServer(OpenChassisGuiMessage.TYPE,
                OpenChassisGuiMessage.STREAM_CODEC, OpenChassisGuiMessage::handle);
        registrar.playToServer(OpenAttachedCrafterGuiMessage.TYPE,
                OpenAttachedCrafterGuiMessage.STREAM_CODEC, OpenAttachedCrafterGuiMessage::handle);
        registrar.playToServer(OpenSneakyDirectionGuiMessage.TYPE,
                OpenSneakyDirectionGuiMessage.STREAM_CODEC, OpenSneakyDirectionGuiMessage::handle);
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
        registrar.playBidirectional(FuzzySlotFlagsMessage.TYPE,
                FuzzySlotFlagsMessage.STREAM_CODEC,
                FuzzySlotFlagsMessage::handle, FuzzySlotFlagsMessage::handle);
        registrar.playBidirectional(FluidSupplierMinModeMessage.TYPE,
                FluidSupplierMinModeMessage.STREAM_CODEC,
                FluidSupplierMinModeMessage::handle, FluidSupplierMinModeMessage::handle);
        registrar.playBidirectional(FluidSupplierPartialsMessage.TYPE,
                FluidSupplierPartialsMessage.STREAM_CODEC,
                FluidSupplierPartialsMessage::handle, FluidSupplierPartialsMessage::handle);
        registrar.playBidirectional(DebugConnectionDataMessage.TYPE,
                DebugConnectionDataMessage.STREAM_CODEC,
                DebugConnectionDataMessage::handle, DebugConnectionDataMessage::handle);
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
        registrar.playToClient(TravellingItemPositionMessage.TYPE,
                TravellingItemPositionMessage.STREAM_CODEC, TravellingItemPositionMessage::handle);
        registrar.playToClient(PipeFluidUpdateMessage.TYPE,
                PipeFluidUpdateMessage.STREAM_CODEC, PipeFluidUpdateMessage::handle);
        registrar.playToClient(RunningCraftingTasksMessage.TYPE,
                RunningCraftingTasksMessage.STREAM_CODEC, RunningCraftingTasksMessage::handle);
        registrar.playToClient(TrackableItemsMessage.TYPE,
                TrackableItemsMessage.STREAM_CODEC, TrackableItemsMessage::handle);
        registrar.playToClient(ChestContentMessage.TYPE,
                ChestContentMessage.STREAM_CODEC, ChestContentMessage::handle);
        registrar.playToClient(InvSysConContentMessage.TYPE,
                InvSysConContentMessage.STREAM_CODEC, InvSysConContentMessage::handle);
        registrar.playToClient(SendQueueContentMessage.TYPE,
                SendQueueContentMessage.STREAM_CODEC, SendQueueContentMessage::handle);
        registrar.playToClient(RoutingLasersMessage.TYPE,
                RoutingLasersMessage.STREAM_CODEC, RoutingLasersMessage::handle);
        registrar.playToClient(SatelliteNameResultMessage.TYPE,
                SatelliteNameResultMessage.STREAM_CODEC, SatelliteNameResultMessage::handle);
        registrar.playToClient(SatellitePipeListMessage.TYPE,
                SatellitePipeListMessage.STREAM_CODEC, SatellitePipeListMessage::handle);
        registrar.playToClient(TravellingItemContentMessage.TYPE,
                TravellingItemContentMessage.STREAM_CODEC, TravellingItemContentMessage::handle);
        registrar.playToClient(UpgradeConfigPopupMessage.TYPE,
                UpgradeConfigPopupMessage.STREAM_CODEC, UpgradeConfigPopupMessage::handle);
        registrar.playToClient(PipeStateMessage.TYPE,
                PipeStateMessage.STREAM_CODEC, PipeStateMessage::handle);
        registrar.playToClient(MultiBlockPositionMessage.TYPE,
                MultiBlockPositionMessage.STREAM_CODEC, MultiBlockPositionMessage::handle);
        registrar.playToClient(ToggleClientPipeDebugMessage.TYPE,
                ToggleClientPipeDebugMessage.STREAM_CODEC, ToggleClientPipeDebugMessage::handle);
        registrar.playToClient(LikelyRecipeComponentsMessage.TYPE,
                LikelyRecipeComponentsMessage.STREAM_CODEC, LikelyRecipeComponentsMessage::handle);
        registrar.playToClient(OpenDebugPanelMessage.TYPE,
                OpenDebugPanelMessage.STREAM_CODEC, OpenDebugPanelMessage::handle);
        registrar.playToClient(UpdateStatusEntriesMessage.TYPE,
                UpdateStatusEntriesMessage.STREAM_CODEC, UpdateStatusEntriesMessage::handle);
        registrar.playToClient(ChassisModuleContentMessage.TYPE,
                ChassisModuleContentMessage.STREAM_CODEC, ChassisModuleContentMessage::handle);
        registrar.playToClient(OrdererContentMessage.TYPE,
                OrdererContentMessage.STREAM_CODEC, OrdererContentMessage::handle);
        registrar.playToClient(OrderManagerContentMessage.TYPE,
                OrderManagerContentMessage.STREAM_CODEC, OrderManagerContentMessage::handle);
        registrar.playToClient(PlayerConfigMessage.TYPE,
                PlayerConfigMessage.STREAM_CODEC, PlayerConfigMessage::handle);
        registrar.playToClient(AskForDebugTargetMessage.TYPE,
                AskForDebugTargetMessage.STREAM_CODEC, AskForDebugTargetMessage::handle);
        registrar.playToClient(OpenChatGuiMessage.TYPE,
                OpenChatGuiMessage.STREAM_CODEC, OpenChatGuiMessage::handle);
        registrar.playToClient(SendLogLineMessage.TYPE,
                SendLogLineMessage.STREAM_CODEC, SendLogLineMessage::handle);
        registrar.playToClient(SendLogWindowMessage.TYPE,
                SendLogWindowMessage.STREAM_CODEC, SendLogWindowMessage::handle);
        registrar.playToClient(RoutingDebugCandidateListMessage.TYPE,
                RoutingDebugCandidateListMessage.STREAM_CODEC, RoutingDebugCandidateListMessage::handle);
        registrar.playToClient(RoutingDebugCandidateMessage.TYPE,
                RoutingDebugCandidateMessage.STREAM_CODEC, RoutingDebugCandidateMessage::handle);
        registrar.playToClient(RoutingDebugClearMessage.TYPE,
                RoutingDebugClearMessage.STREAM_CODEC, RoutingDebugClearMessage::handle);
        registrar.playToClient(RoutingDebugClosedSetMessage.TYPE,
                RoutingDebugClosedSetMessage.STREAM_CODEC, RoutingDebugClosedSetMessage::handle);
        registrar.playToClient(RoutingDebugDoneMessage.TYPE,
                RoutingDebugDoneMessage.STREAM_CODEC, RoutingDebugDoneMessage::handle);
        registrar.playToClient(RoutingDebugFiltersMessage.TYPE,
                RoutingDebugFiltersMessage.STREAM_CODEC, RoutingDebugFiltersMessage::handle);
        registrar.playToClient(RoutingDebugInitMessage.TYPE,
                RoutingDebugInitMessage.STREAM_CODEC, RoutingDebugInitMessage::handle);
        registrar.playToClient(RoutingDebugSourceMessage.TYPE,
                RoutingDebugSourceMessage.STREAM_CODEC, RoutingDebugSourceMessage::handle);
        registrar.playToClient(QuickSortMarkerMessage.TYPE,
                QuickSortMarkerMessage.STREAM_CODEC, QuickSortMarkerMessage::handle);
        registrar.playToClient(PowerLaserMessage.TYPE,
                PowerLaserMessage.STREAM_CODEC, PowerLaserMessage::handle);
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
}
