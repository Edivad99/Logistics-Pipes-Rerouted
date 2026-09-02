package logisticspipes.network;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

/**
 * What the player was pointing at when a debug tool asked.
 *
 * <p>The three cases used to travel as a mode plus an {@code int[]} whose length depended on it:
 * three for a block, one for an entity, none otherwise. Reading the array meant knowing the mode
 * first, and nothing stopped the two from disagreeing.
 */
public sealed interface DebugTarget {

    /**
     * What the client's crosshair is on right now.
     *
     * <p>Client side only: the server has no ray trace for a player's view, which is why every
     * debug tool has to ask.
     */
    static DebugTarget lookedAt() {
        final HitResult hit = Minecraft.getInstance().hitResult;
        if (hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK) {
            return new Block(block.getBlockPos());
        }
        if (hit instanceof EntityHitResult entity && hit.getType() == HitResult.Type.ENTITY) {
            return new Entity(entity.getEntity().getId());
        }
        return new Nothing();
    }

    /** The crosshair was on nothing. */
    record Nothing() implements DebugTarget {
    }

    record Block(BlockPos pos) implements DebugTarget {
    }

    record Entity(int entityId) implements DebugTarget {
    }

    /** Which of the three shapes follows. */
    enum Kind {
        NOTHING, BLOCK, ENTITY;

        static Kind of(DebugTarget target) {
            if (target instanceof Block) {
                return BLOCK;
            }
            return target instanceof Entity ? ENTITY : NOTHING;
        }

        StreamCodec<RegistryFriendlyByteBuf, ? extends DebugTarget> codec() {
            return switch (this) {
                case NOTHING -> StreamCodec.unit(new Nothing());
                case BLOCK -> StreamCodec.composite(
                        BlockPos.STREAM_CODEC, Block::pos,
                        Block::new);
                case ENTITY -> StreamCodec.composite(
                        ByteBufCodecs.VAR_INT, Entity::entityId,
                        Entity::new);
            };
        }
    }

    StreamCodec<RegistryFriendlyByteBuf, DebugTarget> STREAM_CODEC =
            NeoForgeStreamCodecs.<RegistryFriendlyByteBuf, Kind>enumCodec(Kind.class)
                    .dispatch(Kind::of, Kind::codec);
}
