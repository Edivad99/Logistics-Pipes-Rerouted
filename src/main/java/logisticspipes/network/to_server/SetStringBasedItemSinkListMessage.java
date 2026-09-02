package logisticspipes.network.to_server;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IStringBasedModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.ModuleTarget;

/**
 * The player edited the mod or creative tab list in the module's GUI.
 *
 * <p>The names travel as themselves. Shipping the module's serialized form instead would send a
 * whole {@code CompoundTag} to carry a list of strings, and would hand arbitrary NBT
 * to {@code deserialize} on the direction the client controls.
 */
public record SetStringBasedItemSinkListMessage(ModuleTarget target, List<String> names)
        implements CustomPacketPayload {

    public static final Type<SetStringBasedItemSinkListMessage> TYPE =
            new Type<>(LPConstants.rl("set_string_based_item_sink_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetStringBasedItemSinkListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, SetStringBasedItemSinkListMessage::target,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    SetStringBasedItemSinkListMessage::names,
                    SetStringBasedItemSinkListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetStringBasedItemSinkListMessage message, IPayloadContext context) {
        final IStringBasedModule module =
                message.target.resolve(context.player(), IStringBasedModule.class);
        if (module == null) {
            return;
        }
        module.stringListProperty().replaceContent(message.names);
        // A module in the world has watchers to tell; one held in hand has nobody.
        if (message.target.slot().filter(ModulePositionType::isInWorld).isPresent()) {
            module.listChanged();
        }
    }
}
