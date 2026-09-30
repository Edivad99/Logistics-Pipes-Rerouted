package logisticspipes.client.renderer.blockentity;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.core.Direction;

import net.neoforged.neoforge.fluids.FluidStack;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jspecify.annotations.Nullable;

import logisticspipes.transport.PipeFluidTransportLogistics;

/**
 * The fluid held in a fluid pipe's tanks: a level box in the centre and an arm towards each side
 * whose tank holds something, each as high as its tank is full. Port of LP1's
 * {@code LogisticsRenderPipe.renderFluids}, a copy of BuildCraft's fluid pipe renderer.
 */
public final class PipeFluidRenderer {

    /**
     * Keeps the fluid just inside the pipe walls, so it does not z-fight with them.
     */
    private static final float INSET = 0.01F;
    /**
     * The fluid fills the core of a BuildCraft pipe, which is what LP1 drew it in.
     */
    private static final float LOW = 0.25F + INSET;
    private static final float HIGH = 0.75F - INSET;
    private static final float HALF_WIDTH = (HIGH - LOW) / 2;

    private PipeFluidRenderer() {
    }

    private record Box(FluidBoxRenderer.Look look, float x0, float y0, float z0, float x1, float y1, float z1) {
    }

    public static void render(PipeFluidTransportLogistics transport, PoseStack poseStack,
        SubmitNodeCollector collector, int packedLight, int packedOverlay) {
        @Nullable FluidStack[] cache = transport.renderCache;
        List<Box> boxes = new ArrayList<>();

        boolean above = false;
        boolean sides = false;
        for (Direction direction : Direction.values()) {
            FluidStack stack = cache[direction.ordinal()];
            FluidBoxRenderer.Look look = look(stack);
            if (look == null) {
                continue;
            }
            float ratio = ratio(stack, transport.getSideCapacity());
            switch (direction) {
                case UP -> {
                    above = true;
                    boxes.add(verticalArm(look, ratio, HIGH, 1));
                }
                case DOWN -> boxes.add(verticalArm(look, ratio, 0, LOW));
                default -> {
                    sides = true;
                    boxes.add(horizontalArm(look, ratio, direction));
                }
            }
        }

        FluidStack center = cache[PipeFluidTransportLogistics.CENTER];
        FluidBoxRenderer.Look look = look(center);
        if (look != null) {
            float ratio = ratio(center, transport.getInnerCapacity());
            // Fluid rising into the pipe above stands as a column; fluid flowing sideways lies level.
            if (above) {
                float half = (HALF_WIDTH - INSET) * ratio;
                boxes.add(new Box(look, 0.5F - half, LOW, 0.5F - half, 0.5F + half, HIGH, 0.5F + half));
            }
            if (!above || sides) {
                boxes.add(new Box(look, LOW, LOW, LOW, HIGH, LOW + (HIGH - LOW) * ratio, HIGH));
            }
        }

        if (boxes.isEmpty()) {
            return;
        }
        collector.submitCustomGeometry(poseStack, Sheets.translucentBlockSheet(), (pose, buffer) -> {
            for (Box box : boxes) {
                FluidBoxRenderer.emitBox(buffer, pose, box.look(), box.x0(), box.y0(), box.z0(), box.x1(), box.y1(),
                    box.z1(), packedLight, packedOverlay);
            }
        });
    }

    private static FluidBoxRenderer.@Nullable Look look(@Nullable FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return FluidBoxRenderer.look(stack.getFluid());
    }

    private static float ratio(FluidStack stack, int capacity) {
        return Math.min((float) stack.getAmount() / capacity, 1.0F);
    }

    /**
     * The arm towards the pipe above or below: a column that grows wider as its tank fills.
     */
    private static Box verticalArm(FluidBoxRenderer.Look look, float ratio, float y0, float y1) {
        float half = HALF_WIDTH * ratio;
        return new Box(look, 0.5F - half, y0, 0.5F - half, 0.5F + half, y1, 0.5F + half);
    }

    /**
     * The arm towards a horizontal neighbour: a level layer that rises as its tank fills.
     */
    private static Box horizontalArm(FluidBoxRenderer.Look look, float ratio, Direction direction) {
        boolean negative = direction.getAxisDirection() == Direction.AxisDirection.NEGATIVE;
        float along0 = negative ? 0 : HIGH;
        float along1 = negative ? LOW : 1;
        float top = LOW + (HIGH - LOW) * ratio;
        if (direction.getAxis() == Direction.Axis.X) {
            return new Box(look, along0, LOW, LOW, along1, top, HIGH);
        }
        return new Box(look, LOW, LOW, along0, HIGH, top, along1);
    }
}
