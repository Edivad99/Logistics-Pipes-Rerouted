package logisticspipes.client.renderer.blockentity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import net.neoforged.neoforge.client.fluid.FluidTintSource;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.model.mesh.SpriteUv;
import logisticspipes.renderer.FluidContainerRenderer;

/**
 * Axis-aligned boxes of fluid, drawn with the fluid's still sprite and tint: what LP1's
 * {@code CustomBlockRenderer} did for the fluid inside fluid pipes and traveling fluid containers.
 */
final class FluidBoxRenderer {

    /**
     * How a fluid is drawn: its still sprite and the colour that sprite is multiplied by.
     */
    record Look(TextureAtlasSprite sprite, int color) {
    }

    private FluidBoxRenderer() {
    }

    static @Nullable Look look(Fluid fluid) {
        FluidState state = fluid.defaultFluidState();
        FluidModel model;
        try {
            model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(state);
        } catch (Exception e) {
            // Defensive: a broken third-party fluid must not crash pipe rendering.
            return null;
        }
        FluidTintSource tint = model.fluidTintSource();
        int color = tint != null ? 0xFF000000 | FluidContainerRenderer.tintColor(tint, state) : 0xFFFFFFFF;
        return new Look(model.stillMaterial().sprite(), color);
    }

    /**
     * Every face is wound counter-clockwise seen from outside, so back faces are culled, and samples
     * the part of the still sprite its own extent covers, as LP1's block renderer did.
     */
    static void emitBox(VertexConsumer buffer, PoseStack.Pose pose, Look look, float x0, float y0, float z0,
        float x1, float y1, float z1, int light, int overlay) {
        TextureAtlasSprite sprite = look.sprite();
        int color = look.color();
        // down
        vertex(buffer, pose, sprite, color, light, overlay, x0, y0, z0, x0, z0, 0, -1, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y0, z0, x1, z0, 0, -1, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y0, z1, x1, z1, 0, -1, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x0, y0, z1, x0, z1, 0, -1, 0);
        // up
        vertex(buffer, pose, sprite, color, light, overlay, x0, y1, z0, x0, z0, 0, 1, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x0, y1, z1, x0, z1, 0, 1, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y1, z1, x1, z1, 0, 1, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y1, z0, x1, z0, 0, 1, 0);
        // north
        vertex(buffer, pose, sprite, color, light, overlay, x0, y0, z0, x0, 1 - y0, 0, 0, -1);
        vertex(buffer, pose, sprite, color, light, overlay, x0, y1, z0, x0, 1 - y1, 0, 0, -1);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y1, z0, x1, 1 - y1, 0, 0, -1);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y0, z0, x1, 1 - y0, 0, 0, -1);
        // south
        vertex(buffer, pose, sprite, color, light, overlay, x0, y0, z1, x0, 1 - y0, 0, 0, 1);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y0, z1, x1, 1 - y0, 0, 0, 1);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y1, z1, x1, 1 - y1, 0, 0, 1);
        vertex(buffer, pose, sprite, color, light, overlay, x0, y1, z1, x0, 1 - y1, 0, 0, 1);
        // west
        vertex(buffer, pose, sprite, color, light, overlay, x0, y0, z0, z0, 1 - y0, -1, 0, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x0, y0, z1, z1, 1 - y0, -1, 0, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x0, y1, z1, z1, 1 - y1, -1, 0, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x0, y1, z0, z0, 1 - y1, -1, 0, 0);
        // east
        vertex(buffer, pose, sprite, color, light, overlay, x1, y0, z0, z0, 1 - y0, 1, 0, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y1, z0, z0, 1 - y1, 1, 0, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y1, z1, z1, 1 - y1, 1, 0, 0);
        vertex(buffer, pose, sprite, color, light, overlay, x1, y0, z1, z1, 1 - y0, 1, 0, 0);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, int color,
        int light, int overlay, float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        buffer.addVertex(pose, x, y, z)
            .setColor(color)
            .setUv(SpriteUv.u(sprite, u), SpriteUv.v(sprite, v))
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(pose, nx, ny, nz);
    }
}
