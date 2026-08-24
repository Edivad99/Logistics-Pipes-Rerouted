package logisticspipes.renderer.newpipe;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

import logisticspipes.client.model.mesh.MeshRenderer;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.FluidIdentifierStack;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.item.LogisticsFluidContainer;

public class LogisticsNewPipeItemBoxRenderer {

	private static final int RENDER_SIZE = 40;

	private int renderList = -1;
	private static final Identifier BLOCKS = Identifier.withDefaultNamespace("textures/atlas/blocks.png");
	private static final Map<FluidIdentifier, int[]> renderLists = new HashMap<>();

	public void doRenderItem(ItemStack itemstack, float light, double x, double y, double z, double boxScale, double yaw, double pitch, double yawForPitch, PoseStack poseStack, SubmitNodeCollector collector, int packedLight, int packedOverlay) {
		poseStack.pushPose();

		poseStack.translate(x, y, z);
		poseStack.scale((float) boxScale, (float) boxScale, (float) boxScale);
		poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yaw)));
		poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(yawForPitch)));
		poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(pitch)));
		poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-yawForPitch)));
		poseStack.translate(-0.5, -0.5, -0.5);

		// Everything the emitter needs is passed in, so there is no bound render state to go stale.
		collector.submitCustomGeometry(poseStack, Sheets.cutoutBlockSheet(), (pose, buffer) -> MeshRenderer.emit(
			buffer, pose,
			PipeModelStore.parts().innerTransportBox(),
			PipeModelStore.sprites().innerBox(),
			packedLight, packedOverlay));

		if (!itemstack.isEmpty() && itemstack.getItem() instanceof LogisticsFluidContainer) {
			FluidIdentifierStack f = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(ItemIdentifierStack.getFromStack(itemstack), Minecraft.getInstance().level.registryAccess());
			if (f != null) {
				/*
				FluidContainerRenderer.skipNext = true;
				int list = getRenderListFor(f);
				// TODO: glPushAttrib removed
				// TODO: GL11.glEnable(GL11.GL_CULL_FACE) → RenderSystem equivalent
				// GL_LIGHTING removed — use shaders
				// TODO: glBlendFunc → RenderSystem.blendFunc()

				// TODO: glCallList removed — use vertex buffer rendering
				// TODO: glPopAttrib removed
				*/
			}
		}

		poseStack.popPose();
	}
/*
	private int getRenderListFor(FluidStack fluid) {
		FluidIdentifier ident = FluidIdentifier.get(fluid);
		int[] array = LogisticsNewPipeItemBoxRenderer.renderLists.get(fluid);
		if (array == null) {
			array = new int[LogisticsNewPipeItemBoxRenderer.RENDER_SIZE];
			LogisticsNewPipeItemBoxRenderer.renderLists.put(ident, array);
		}
		int pos = Math.min((int) (((Math.min(fluid.amount, 5000) * 1.0F) * LogisticsNewPipeItemBoxRenderer.RENDER_SIZE) / 5000), LogisticsNewPipeItemBoxRenderer.RENDER_SIZE - 1);
		if (array[pos] != 0) {
			return array[pos];
		}
		RenderInfo block = new RenderInfo();

		block.baseBlock = fluid.getFluid().getBlock();
		block.texture = fluid.getFluid().getStillIcon();

		float ratio = pos * 1.0F / (LogisticsNewPipeItemBoxRenderer.RENDER_SIZE - 1);

		// CENTER HORIZONTAL

		array[pos] = 0;
		// TODO: glNewList removed — use vertex buffer rendering

		block.minX = 0.32;
		block.maxX = 0.68;

		block.minY = 0.32;
		block.maxY = 0.32 + (0.68 - 0.32) * ratio;

		block.minZ = 0.32;
		block.maxZ = 0.68;

		CustomBlockRenderer.INSTANCE.renderBlock(block, Minecraft.getInstance().theWorld, 0, 0, 0, false, true);

		// TODO: glEndList removed
		return array[pos];
	}*/
}
