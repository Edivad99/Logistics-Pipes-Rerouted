package logisticspipes.client.renderer.item;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;

import logisticspipes.client.renderer.blockentity.LogisticsSolidBlockRenderer;
import logisticspipes.world.item.LogisticsSolidBlockItem;

/**
 * BEWLR that draws an LP solid block item using the same OBJ geometry as
 * {@link LogisticsSolidBlockRenderer}. Vanilla ItemRenderer pre-applies the
 * {@code -0.5,-0.5,-0.5} centring translation before invoking this renderer, so
 * the shared draw path can be used unmodified.
 */
public class LogisticsSolidBlockItemRenderer extends BlockEntityWithoutLevelRenderer {

    public static LogisticsSolidBlockItemRenderer INSTANCE = new LogisticsSolidBlockItemRenderer();

    private LogisticsSolidBlockItemRenderer() {
        super(Minecraft.getInstance().getBlockEntityRenderDispatcher(), Minecraft.getInstance().getEntityModels());
    }

    @Override
    public void renderByItem(ItemStack stack, ItemDisplayContext ctx, PoseStack pose,
        MultiBufferSource buffers, int light, int overlay) {
        if (stack.getItem() instanceof LogisticsSolidBlockItem item) {
            pose.pushPose();
            try {
                LogisticsSolidBlockRenderer.renderSolid(item.getType(), pose, buffers, light, overlay);
            } finally {
                pose.popPose();
            }
        }
    }
}
