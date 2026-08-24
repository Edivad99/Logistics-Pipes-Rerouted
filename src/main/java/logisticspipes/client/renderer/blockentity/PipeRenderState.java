package logisticspipes.client.renderer.blockentity;

import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.basic.LogisticsTileGenericPipe;

/**
 * Render state for {@link LogisticsRenderPipe}.
 *
 * <p><strong>This is not a snapshot, and that is a deliberate shortcut.</strong> 1.21.9 split
 * {@code BlockEntityRenderer#render} into an extract step that may read the world and a submit
 * step that is supposed to read only the state object. Honouring that fully here would mean
 * copying, every frame and for every visible pipe, the traveling items with their interpolated
 * positions, the pipe signs and whatever each sign implementation draws, the fluid levels and the
 * connection matrix -- a rewrite of LP's whole client draw path rather than a port of it.</p>
 *
 * <p>Instead the state carries the block entity itself and the partial tick, and submission reads
 * through to it exactly as the old {@code render} did. What this costs: the two steps are no longer
 * independent, so LP would break if Mojang ever moved extraction onto a worker thread or started
 * reusing a frame's states across frames. Both run on the render thread today, one straight after
 * the other, so the behaviour is identical to 1.21.8.</p>
 */
public class PipeRenderState extends BlockEntityRenderState {

    @Nullable
    public LogisticsTileGenericPipe blockEntity;

    public float partialTicks;
}
