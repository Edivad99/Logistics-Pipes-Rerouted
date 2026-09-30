package logisticspipes.client.renderer.blockentity;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.FluidState;

import net.neoforged.neoforge.client.fluid.FluidTintSource;
import net.neoforged.neoforge.fluids.SimpleFluidContent;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Quaternionf;

import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.mesh.SpriteUv;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.renderer.FluidContainerRenderer;
import logisticspipes.world.item.LogisticsFluidContainer;
import logisticspipes.world.item.component.LPDataComponents;

/**
 * The transport box drawn around an item traveling through a pipe, filled with the fluid of a fluid container.
 */
public final class TravelingItemBoxRenderer {

    /**
     * The amount that fills the box to the top: the most a fluid provider sends in one container.
     */
    private static final float FULL_AMOUNT = 5000.0F;
    /**
     * Keeps a nearly empty container visible as a thin layer instead of a flat quad.
     */
    private static final float MIN_FILL = 1.0F / 40.0F;
    private static final float MIN = 0.32F;
    private static final float MAX = 0.68F;

    private TravelingItemBoxRenderer() {
    }

    /**
     * Draws the transport box and, for a filled fluid container, the fluid inside it.
     *
     * @return whether the fluid was drawn, in which case the container item itself must not be:
     * LP1 showed only the fluid for a container traveling through a pipe
     */
    public static boolean render(ItemStack itemstack, double x, double y, double z, double boxScale, double yaw,
        double pitch, double yawForPitch, PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
        int packedOverlay) {
        poseStack.pushPose();

        poseStack.translate(x, y, z);
        poseStack.scale((float) boxScale, (float) boxScale, (float) boxScale);

        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yaw)));
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yawForPitch)));
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(pitch)));
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-yawForPitch)));
        poseStack.translate(-0.5, -0.5, -0.5);

        // Everything the emitter needs is passed in, so there is no bound render state to go stale.
        collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(),
            (pose, buffer) -> MeshRenderer.emit(buffer, pose, PipeModelStore.parts().innerTransportBox(),
                PipeModelStore.sprites().innerBox(), packedLight, packedOverlay));
        poseStack.popPose();

        boolean renderedFluid = false;
        // The buffered items drawn in a stack in the pipe's centre have no box, and keep their own look.
        if (boxScale > 0 && !itemstack.isEmpty() && itemstack.getItem() instanceof LogisticsFluidContainer) {
            SimpleFluidContent content = itemstack.get(LPDataComponents.FLUID);
            if (content != null && !content.isEmpty()) {
                // Like LP1, the fluid does not turn with the box, so it always lies level.
                poseStack.translate(-0.5, -0.5, -0.5);
                renderedFluid = renderFluid(content, poseStack, collector, packedLight, packedOverlay);
            }
        }

        poseStack.popPose();
        return renderedFluid;
    }

    private static boolean renderFluid(SimpleFluidContent content, PoseStack poseStack,
        SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        FluidState state = content.getFluid().defaultFluidState();
        FluidModel model;
        try {
            model = Minecraft.getInstance().getModelManager().getFluidStateModelSet().get(state);
        } catch (Exception e) {
            // Defensive: a broken third-party fluid must not crash pipe rendering.
            return false;
        }
        TextureAtlasSprite sprite = model.stillMaterial().sprite();
        FluidTintSource tint = model.fluidTintSource();
        int color = tint != null ? 0xFF000000 | FluidContainerRenderer.tintColor(tint, state) : 0xFFFFFFFF;

        float fill = Math.max(Math.min(content.getAmount(), FULL_AMOUNT) / FULL_AMOUNT, MIN_FILL);
        float top = MIN + (MAX - MIN) * fill;

        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(),
            (pose, buffer) -> emitBox(buffer, pose, sprite, color, top, packedLight, packedOverlay));
        return true;
    }

    /**
     * Emits the fluid box from {@link #MIN} to {@link #MAX}, cut off at {@code top}. Every face is
     * wound counter-clockwise seen from outside, so back faces are culled, and samples the part of the
     * still sprite its own extent covers, as LP1's block renderer did.
     */
    private static void emitBox(VertexConsumer buffer, PoseStack.Pose pose, TextureAtlasSprite sprite, int color,
        float top, int light, int overlay) {
        float x0 = MIN, x1 = MAX, y0 = MIN, y1 = top, z0 = MIN, z1 = MAX;
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
