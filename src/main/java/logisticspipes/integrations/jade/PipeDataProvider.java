package logisticspipes.integrations.jade;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.StreamServerDataProvider;

import logisticspipes.LPConstants;
import logisticspipes.integrations.probe.PipeTooltip;
import logisticspipes.integrations.probe.PipeTooltipBuilder;

public enum PipeDataProvider implements StreamServerDataProvider<BlockAccessor, PipeTooltip> {
    INSTANCE;

    /** Shared with {@link PipeComponentProvider}; Jade pairs the two by it. */
    static final Identifier UID = LPConstants.rl("pipe");

    @Override
    public PipeTooltip streamData(BlockAccessor accessor) {
        return PipeTooltipBuilder.build(accessor.getBlockEntity(), accessor.showDetails());
    }

    @Override
    public StreamCodec<RegistryFriendlyByteBuf, PipeTooltip> streamCodec() {
        return PipeTooltip.STREAM_CODEC;
    }

    @Override
    public Identifier getUid() {
        return UID;
    }
}
