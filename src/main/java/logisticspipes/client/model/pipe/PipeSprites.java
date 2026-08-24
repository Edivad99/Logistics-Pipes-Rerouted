package logisticspipes.client.model.pipe;

import java.util.function.IntFunction;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.jspecify.annotations.Nullable;

/**
 * The atlas sprites the pipe frame is textured with, resolved once per atlas stitch.
 *
 * <p>Replaces the six static {@code TextureTransformation} fields on
 * {@code LogisticsNewRenderPipe}. Those wrapped a sprite in an operation object so it could
 * be passed through the {@code I3DOperation} varargs at render time; with baking there is
 * nothing to defer, so the sprite is used directly.</p>
 *
 * @param basicPipe   the plain pipe body
 * @param inactive    body texture for an unpowered routed pipe
 * @param status      routed-pipe status overlay, shifted in UV per state
 * @param statusBC    the same for BuildCraft-style connections
 * @param glassCenter centre plate of a fluid pipe
 * @param innerBox    the transport box drawn inside a pipe carrying items
 * @param indexedIcon per-pipe-type body texture, looked up by
 *                    {@code TextureMatrix.getTextureIndex()}
 */
public record PipeSprites(
    @Nullable TextureAtlasSprite basicPipe,
    @Nullable TextureAtlasSprite inactive,
    @Nullable TextureAtlasSprite status,
    @Nullable TextureAtlasSprite statusBC,
    @Nullable TextureAtlasSprite glassCenter,
    @Nullable TextureAtlasSprite innerBox,
    IntFunction<TextureAtlasSprite> indexedIcon) {

    public static PipeSprites empty() {
        return new PipeSprites(null, null, null, null, null, null, index -> null);
    }

    @Nullable
    public TextureAtlasSprite icon(int index) {
        return indexedIcon.apply(index);
    }

    public boolean isComplete() {
        return basicPipe != null && status != null && statusBC != null;
    }
}
