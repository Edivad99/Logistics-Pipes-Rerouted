package logisticspipes.client.model.pipe;

import net.minecraft.resources.Identifier;

import net.neoforged.neoforge.model.data.ModelProperty;

/**
 * Model data keys the pipe block entity passes to {@link PipeBakedModel}.
 *
 * <p>Not marked client-only: {@code LogisticsTileGenericPipe.getModelData()} is a common
 * method, so this class has to link on a dedicated server. {@link ModelProperty} itself is a
 * plain marker object with no client dependencies.</p>
 */
public final class PipeModelProperties {

    private PipeModelProperties() {
    }

    /**
     * Snapshot of everything that decides the pipe's geometry.
     */
    public static final ModelProperty<PipeGeometryKey> GEOMETRY = new ModelProperty<>();

    /**
     * Block-atlas sprite name for break and hit particles, for pipes whose visible surface is
     * not the pipe frame — the request table draws a solid block body, so the frame sprite the
     * model would otherwise hand out has nothing to do with what the player is breaking.
     * Absent means "use the pipe frame sprite".
     */
    public static final ModelProperty<Identifier> PARTICLE_SPRITE = new ModelProperty<>();
}
