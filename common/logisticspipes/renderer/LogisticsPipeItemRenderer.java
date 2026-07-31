package logisticspipes.renderer;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.client.model.pipe.PipeGeometryKey;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.pipe.PipeQuadBaker;
import logisticspipes.items.ItemLogisticsPipe;
import logisticspipes.pipes.PipeBlockRequestTable;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.renderer.state.PipeRenderState;

/**
 * BEWLR that draws a pipe item using the same OBJ geometry pipeline as the in-world
 * {@link LogisticsRenderPipe}. Built with a fresh all-disconnected {@link PipeRenderState}
 * and the item's dummyPipe, so inventory icons show the 3D pipe body instead of a sprite.
 */
public class LogisticsPipeItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static LogisticsPipeItemRenderer INSTANCE = new LogisticsPipeItemRenderer();

    private LogisticsPipeItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose, MultiBufferSource buffers,
        int light, int overlay) {
        if (!(stack.getItem() instanceof ItemLogisticsPipe item)) {
            return;
        }
        CoreUnroutedPipe dummyPipe = item.getDummyPipe();
        if (dummyPipe == null) {
            return;
        }
        // The Request Table has no pipe-frame geometry at all: its placed form is a full solid
        // block, so the item has to be drawn the same way instead of falling back to the pipe
        // body below, which is what made it look like an ordinary pipe in the inventory.
        if (dummyPipe instanceof PipeBlockRequestTable) {
            pose.pushPose();
            try {
                LogisticsRenderPipe.renderRequestTableItem(pose, buffers, light, overlay);
            } finally {
                pose.popPose();
            }
            return;
        }
        renderBaked(dummyPipe, pose, buffers, light, overlay);
    }

    /**
     * Draws the item form from the same quads the in-world block model uses.
     *
     * <p>Going through {@code PipeQuadBaker} rather than re-deriving the geometry is the point:
     * the item and the placed pipe cannot drift apart, because there is only one description of
     * what a pipe looks like. The quads are emitted here rather than baked into an item model
     * because {@code getDummyPipe()} state is per item type, not per block state.</p>
     */
    private void renderBaked(CoreUnroutedPipe dummyPipe, PoseStack pose, MultiBufferSource buffers,
        int light, int overlay) {
        if (!PipeModelStore.isReady()) {
            return;
        }

        PipeRenderState renderState = new PipeRenderState();
        // A fresh ConnectionMatrix has every side disconnected, which is the inventory look.
        renderState.textureMatrix.refreshStatesForItem(dummyPipe);

        List<BakedQuad> quads = PipeQuadBaker.bake(PipeModelStore.parts(), PipeModelStore.sprites(),
            PipeGeometryKey.ofItem(dummyPipe, renderState));
        if (quads.isEmpty()) {
            return;
        }

        pose.pushPose();
        try {
            // Vanilla ItemRenderer.render already applied translate(-0.5, -0.5, -0.5), which
            // centres the [0,1] pipe geometry; the display transform handles rotation and scale.
            VertexConsumer buffer = buffers.getBuffer(RenderType.cutoutMipped());
            for (BakedQuad quad : quads) {
                buffer.putBulkData(pose.last(), quad, 1.0f, 1.0f, 1.0f, 1.0f, light, overlay);
            }
        } finally {
            pose.popPose();
        }
    }
}
