package logisticspipes.network.to_server;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.ModuleTarget;
import logisticspipes.network.to_client.ModulePropertiesMessage;

import network.rs485.logisticspipes.property.PropertyHolder;

/**
 * The properties the player changed in a module's GUI.
 *
 * <p>Properties are the one thing on this protocol that really is a bag of NBT: a module's set of
 * them is open-ended and each serializes itself, so there is no schema to compose a codec from.
 *
 * <p>The server answers with the module's <em>whole</em> property state rather than an
 * acknowledgement, which is what lets the GUI send only what changed.
 */
public record SetModulePropertiesMessage(ModuleTarget target, CompoundTag properties)
        implements CustomPacketPayload {

    public static final Type<SetModulePropertiesMessage> TYPE =
            new Type<>(LPConstants.rl("set_module_properties"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetModulePropertiesMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, SetModulePropertiesMessage::target,
                    ByteBufCodecs.COMPOUND_TAG, SetModulePropertiesMessage::properties,
                    SetModulePropertiesMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** What the holder has, for the GUI to send on close: normally only the properties it changed. */
    public static SetModulePropertiesMessage of(ModuleTarget target, PropertyHolder holder,
            HolderLookup.Provider registries) {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        PropertyHolder.serialize(output, holder);
        return new SetModulePropertiesMessage(target, output.buildResult());
    }

    public static void handle(SetModulePropertiesMessage message, IPayloadContext context) {
        final LogisticsModule module = message.target.resolve(context.player(), LogisticsModule.class);
        if (module == null || !(context.player() instanceof ServerPlayer player)) {
            return;
        }
        final RegistryAccess registries = player.level().registryAccess();
        module.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, message.properties));
        if (message.target.slot().filter(ModulePositionType::isInWorld).isEmpty()
                && player.containerMenu instanceof InventoryMenu) {
            // A module held in hand lives in the item stack, so its properties have to go back into it.
            ItemModuleInformationManager.saveInformation(
                    player.getInventory().getItem(message.target.positionInt()), module, registries);
            player.getInventory().setChanged();
        }
        PacketDistributor.sendToPlayer(player, ModulePropertiesMessage.of(message.target, module, registries));
    }
}
