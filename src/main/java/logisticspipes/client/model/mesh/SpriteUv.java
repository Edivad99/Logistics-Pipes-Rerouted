package logisticspipes.client.model.mesh;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Maps normalized 0..1 UVs onto a sprite's rectangle in the texture atlas.
 *
 * <p>Shared by {@link MeshBaker} and {@code MeshRenderer} so baked and immediate-mode
 * geometry sample identically. Carried over from {@code LPTextureTransformationImpl}, whose
 * half-texel inset exists to stop mipmapped sampling bleeding into neighbouring sprites.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SpriteUv {

    private SpriteUv() {
    }

    /**
     * Roughly half a texel for a 256px sprite.
     */
    private static final float INSET_DIVISOR = 512.0f;

    public static float u(TextureAtlasSprite sprite, float u) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float inset = (u1 - u0) / INSET_DIVISOR;
        return (u0 + inset) + ((u1 - u0) - 2 * inset) * Math.clamp(u, 0f, 1f);
    }

    public static float v(TextureAtlasSprite sprite, float v) {
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float inset = (v1 - v0) / INSET_DIVISOR;
        return (v0 + inset) + ((v1 - v0) - 2 * inset) * Math.clamp(v, 0f, 1f);
    }
}
