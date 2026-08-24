package logisticspipes.client.renderer.blockentity;

import java.util.EnumSet;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.jspecify.annotations.Nullable;

import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.client.model.solid.SolidBlockModelParts;

public class SolidBlockRenderState extends BlockEntityRenderState {

    /** Null when the block is not an LP solid block, or the models are not loaded yet. */
    public LogisticsSolidBlock.@Nullable Type type;

    @Nullable
    public TextureAtlasSprite icon;

    /** 0..3; the body and the plates are authored once per quarter turn. */
    public int rotation;

    /** The cover plates to draw: those whose neighbour is not a pipe connecting into this block. */
    public final EnumSet<SolidBlockModelParts.CoverSide> plates =
        EnumSet.noneOf(SolidBlockModelParts.CoverSide.class);
}
