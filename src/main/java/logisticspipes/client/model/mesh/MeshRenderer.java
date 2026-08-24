package logisticspipes.client.model.mesh;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jspecify.annotations.Nullable;

/**
 * Emits an {@link ObjMesh} straight into a {@link VertexConsumer}, for geometry that cannot
 * live in the chunk mesh — anything drawn at an arbitrary position rather than at a block.
 *
 * <p>Replaces {@code LPModel3DImpl.render(I3DOperation...)}, whose buffer, matrices, light
 * and colour came from the static {@code LPRenderStateImpl} singleton that callers had to
 * bind beforehand. Everything is a parameter here, so there is no cross-caller state to
 * corrupt and nothing to rebind when a buffer is drained mid-frame.</p>
 */
public final class MeshRenderer {

    private MeshRenderer() {
    }

    public static void emit(@Nullable VertexConsumer buffer, PoseStack.Pose pose, ObjMesh mesh,
        @Nullable TextureAtlasSprite sprite, int packedLight, int packedOverlay) {
        emit(buffer, pose, mesh, sprite, UvTransform.IDENTITY, 0xFFFFFFFF, packedLight, packedOverlay);
    }

    /**
     * Emits with the mesh's own 0..1 UVs, for geometry drawn against a standalone texture
     * rather than a sprite in an atlas — the high-speed tubes, whose PNGs are bound directly
     * by their {@code RenderType}.
     *
     */
    public static void emitRaw(@Nullable VertexConsumer buffer, PoseStack.Pose pose, ObjMesh mesh,
        int argb, int packedLight, int packedOverlay) {
        if (mesh.isEmpty() || buffer == null) {
            return;
        }

        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;

        for (int quad = 0; quad < mesh.quadCount(); quad++) {
            for (int vertex = 0; vertex < 4; vertex++) {
                buffer.addVertex(pose, mesh.x(quad, vertex), mesh.y(quad, vertex), mesh.z(quad, vertex))
                    .setColor(r, g, b, a)
                    .setUv(mesh.u(quad, vertex), mesh.v(quad, vertex))
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(pose, mesh.nx(quad, vertex), mesh.ny(quad, vertex), mesh.nz(quad, vertex));
            }
        }
    }

    /**
     * @param uv   applied to the mesh's 0..1 UVs before they are mapped into the sprite
     * @param argb colour multiplier in 0xAARRGGBB
     */
    public static void emit(@Nullable VertexConsumer buffer, PoseStack.Pose pose, ObjMesh mesh,
        @Nullable TextureAtlasSprite sprite, UvTransform uv, int argb, int packedLight, int packedOverlay) {
        if (mesh.isEmpty() || buffer == null || sprite == null) {
            return;
        }

        float a = ((argb >>> 24) & 0xFF) / 255.0f;
        float r = ((argb >>> 16) & 0xFF) / 255.0f;
        float g = ((argb >>> 8) & 0xFF) / 255.0f;
        float b = (argb & 0xFF) / 255.0f;

        for (int quad = 0; quad < mesh.quadCount(); quad++) {
            for (int vertex = 0; vertex < 4; vertex++) {
                buffer.addVertex(pose, mesh.x(quad, vertex), mesh.y(quad, vertex), mesh.z(quad, vertex))
                    .setColor(r, g, b, a)
                    .setUv(SpriteUv.u(sprite, uv.applyU(mesh.u(quad, vertex))),
                        SpriteUv.v(sprite, uv.applyV(mesh.v(quad, vertex))))
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(pose, mesh.nx(quad, vertex), mesh.ny(quad, vertex), mesh.nz(quad, vertex));
            }
        }
    }
}
