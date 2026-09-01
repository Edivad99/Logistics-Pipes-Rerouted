package logisticspipes.network.to_client;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.ModuleTarget;

import network.rs485.logisticspipes.property.PropertyHolder;

/**
 * A module's properties, as the server has them.
 *
 * <p>Sent whenever a property changes server-side and again in answer to every
 * {@link logisticspipes.network.to_server.SetModulePropertiesMessage}, so the client never has to
 * assume its own edit took.
 */
public record ModulePropertiesMessage(ModuleTarget target, CompoundTag properties)
        implements CustomPacketPayload {

    public static final Type<ModulePropertiesMessage> TYPE =
            new Type<>(LPConstants.rl("module_properties"));

    public static final StreamCodec<RegistryFriendlyByteBuf, ModulePropertiesMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, ModulePropertiesMessage::target,
                    ByteBufCodecs.COMPOUND_TAG, ModulePropertiesMessage::properties,
                    ModulePropertiesMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /** Everything the holder has, for a full resync. */
    public static ModulePropertiesMessage of(ModuleTarget target, PropertyHolder holder,
            HolderLookup.Provider registries) {
        final TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
        PropertyHolder.serialize(output, holder);
        return new ModulePropertiesMessage(target, output.buildResult());
    }

    public static void handle(ModulePropertiesMessage message, IPayloadContext context) {
        final LogisticsModule module = message.target.resolve(context.player(), LogisticsModule.class);
        if (module == null) {
            return;
        }
        final RegistryAccess registries = context.player().level().registryAccess();
        module.deserialize(TagValueInput.create(ProblemReporter.DISCARDING, registries, message.properties));
        if (message.target.slot().filter(ModulePositionType::isInWorld).isEmpty()
                && context.player().containerMenu instanceof InventoryMenu) {
            ItemModuleInformationManager.saveInformation(
                    context.player().getInventory().getItem(message.target.positionInt()), module, registries);
            context.player().getInventory().setChanged();
        }
    }
}
