package logisticspipes.pipes.signs;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.common.util.ValueIOSerializable;

import org.jspecify.annotations.Nullable;

/**
 * A sign fixed to one side of a routed pipe, placed with the sign creator.
 *
 * <p>This is the sign's state and server-side behaviour; how it looks is up to
 * {@code PipeSignRenderer}, on the client.</p>
 */
public sealed interface IPipeSign extends ValueIOSerializable permits CraftingPipeSign, ItemAmountPipeSign {

    PipeSignType type();

    /**
     * A player used the sign creator on the sign, or has just placed it.
     */
    default void activate(Player player) {
    }

    /**
     * The message that tells clients what this sign shows, or null when it has nothing to say.
     */
    @Nullable
    CustomPacketPayload getPacket();

    /**
     * Called every tick on the server while the pipe is enabled.
     */
    default void updateServerSide() {
    }

    @Override
    default void serialize(ValueOutput output) {
    }

    @Override
    default void deserialize(ValueInput input) {
    }
}
