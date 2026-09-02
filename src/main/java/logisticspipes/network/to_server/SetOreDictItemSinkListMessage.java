package logisticspipes.network.to_server;

import java.util.List;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import net.neoforged.neoforge.network.handling.IPayloadContext;

import logisticspipes.LPConstants;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.modules.ModuleOreDictItemSink;
import logisticspipes.network.ModuleTarget;

/**
 * The player edited the ore dictionary list in the module's GUI.
 *
 * <p>The names travel as themselves. The obvious alternative -- shipping the module's serialized
 * form -- would send a whole {@code CompoundTag} to carry a list of strings, with no
 * schema for the receiver to check and, on this direction, arbitrary NBT handed to
 * {@code deserialize}.
 */
public record SetOreDictItemSinkListMessage(ModuleTarget target, List<String> oreNames)
        implements CustomPacketPayload {

    public static final Type<SetOreDictItemSinkListMessage> TYPE =
            new Type<>(LPConstants.rl("set_ore_dict_item_sink_list"));

    public static final StreamCodec<RegistryFriendlyByteBuf, SetOreDictItemSinkListMessage> STREAM_CODEC =
            StreamCodec.composite(
                    ModuleTarget.STREAM_CODEC, SetOreDictItemSinkListMessage::target,
                    ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list()),
                    SetOreDictItemSinkListMessage::oreNames,
                    SetOreDictItemSinkListMessage::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(SetOreDictItemSinkListMessage message, IPayloadContext context) {
        final ModuleOreDictItemSink module =
                message.target.resolve(context.player(), ModuleOreDictItemSink.class);
        if (module == null) {
            return;
        }
        module.setOreList(message.oreNames);
        // A module in the world has watchers to tell; one held in hand has nobody.
        if (message.target.slot().filter(ModulePositionType::isInWorld).isPresent()) {
            module.oreListChanged(context.player().level());
        }
    }
}
