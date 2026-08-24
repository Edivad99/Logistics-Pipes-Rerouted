package logisticspipes.client.renderer.item;

import java.util.function.Consumer;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.client.renderer.blockentity.LogisticsSolidBlockRenderer;
import logisticspipes.world.item.LogisticsSolidBlockItem;

/**
 * Draws an LP solid block item using the same OBJ geometry as
 * {@link LogisticsSolidBlockRenderer}.
 *
 * <p>Was a {@code BlockEntityWithoutLevelRenderer} attached per item through
 * {@code IClientItemExtensions#getCustomRenderer} until 1.21.4 removed both. The replacement is a
 * {@link SpecialModelRenderer}, referenced by id from the item's model definition -- see
 * {@code LPModelProvider}. The vertex work is unchanged: the render state still applies
 * {@code translate(-0.5, -0.5, -0.5)} before handing over, so the shared draw path still lands in
 * the unit cube it expects.</p>
 *
 * <p>The renderer no longer receives the {@link ItemStack}: whatever it needs is pulled out ahead of
 * time by {@link #extractArgument}, which here is the block type driving the geometry.</p>
 */
public class LogisticsSolidBlockItemRenderer implements SpecialModelRenderer<LogisticsSolidBlock.Type> {

    public static final LogisticsSolidBlockItemRenderer INSTANCE = new LogisticsSolidBlockItemRenderer();

    private LogisticsSolidBlockItemRenderer() {
    }

    @Override
    public LogisticsSolidBlock.@Nullable Type extractArgument(ItemStack stack) {
        return stack.getItem() instanceof LogisticsSolidBlockItem item ? item.getType() : null;
    }

    /**
     * Corners of the volume the model occupies, in the item's own space. New in 1.21.6: the GUI
     * item renderer uses it to size the render target when the model is oversized. Both renderers
     * draw within the standard unit block that the render state has already centred on the origin,
     * so the eight corners of that cube are the extents.
     */
    @Override
    public void getExtents(Consumer<Vector3fc> extents) {
        extents.accept(new Vector3f(-0.5f, -0.5f, -0.5f));
        extents.accept(new Vector3f(0.5f, 0.5f, 0.5f));
    }

    @Override
    public void submit(LogisticsSolidBlock.@Nullable Type type, PoseStack pose,
        SubmitNodeCollector collector, int light, int overlay, boolean hasFoil, int outlineColor) {
        if (type == null) {
            return;
        }
        pose.pushPose();
        try {
            LogisticsSolidBlockRenderer.submitSolid(type, pose, collector, light, overlay);
        } finally {
            pose.popPose();
        }
    }

    public record Unbaked() implements SpecialModelRenderer.Unbaked<LogisticsSolidBlock.Type> {

        public static final Unbaked INSTANCE = new Unbaked();
        public static final MapCodec<Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public SpecialModelRenderer<LogisticsSolidBlock.Type> bake(SpecialModelRenderer.BakingContext context) {
            return LogisticsSolidBlockItemRenderer.INSTANCE;
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
