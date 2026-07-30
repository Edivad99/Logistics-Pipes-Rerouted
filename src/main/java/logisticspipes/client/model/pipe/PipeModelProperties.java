package logisticspipes.client.model.pipe;

import net.neoforged.neoforge.client.model.data.ModelProperty;

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
}
