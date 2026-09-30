package logisticspipes.client.renderer.blockentity;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.fluids.SimpleFluidContent;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.pipe.PipeModelStore;
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
        FluidBoxRenderer.Look look = FluidBoxRenderer.look(content.getFluid());
        if (look == null) {
            return false;
        }

        float fill = Math.max(Math.min(content.getAmount(), FULL_AMOUNT) / FULL_AMOUNT, MIN_FILL);
        float top = MIN + (MAX - MIN) * fill;

        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, buffer) ->
            FluidBoxRenderer.emitBox(buffer, pose, look, MIN, MIN, MIN, MAX, top, MAX, packedLight, packedOverlay));
        return true;
    }
}
