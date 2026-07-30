package logisticspipes.client.model.tube;

import javax.annotation.Nullable;

import net.minecraft.world.phys.AABB;

import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.interfaces.ITubeRenderOrientation;

/**
 * The tube geometry queries the multi-block tubes need for collision.
 *
 * <p>These used to read the renderers' {@code IModel3D} maps directly
 * ({@code GainTubeRenderer.tubeGain.get(...).bounds()} and friends), which tied the collision
 * shape — server-relevant behavior — to the client-only rendering path. Routing them through
 * {@link TubeModels} instead means the legacy renderers can be removed without taking
 * collision with them.</p>
 */
public final class TubeCollision {

    private TubeCollision() {
    }

    /**
     * Bounds of the whole tube in its orientation, or a unit box if it has not loaded.
     */
    public static AABB completeBox(TubeModels.Kind kind, @Nullable ITubeOrientation orientation) {
        ObjMesh mesh = mesh(kind, orientation);
        return mesh.isEmpty() ? new AABB(0, 0, 0, 1, 1, 1) : mesh.bounds();
    }

    /**
     * Bounds of the tube geometry falling inside {@code slice}, or null when the tube does not
     * reach into it. Callers sample along the tube's path with this to build its collision.
     */
    @Nullable
    public static AABB boundsAt(TubeModels.Kind kind, @Nullable ITubeOrientation orientation, AABB slice) {
        ObjMesh mesh = mesh(kind, orientation);
        return mesh.isEmpty() ? null : mesh.boundsInside(slice);
    }

    private static ObjMesh mesh(TubeModels.Kind kind, @Nullable ITubeOrientation orientation) {
        if (orientation == null) {
            return ObjMesh.empty();
        }
        ITubeRenderOrientation render = orientation.getRenderOrientation();
        if (!(render instanceof Enum<?> named)) {
            return ObjMesh.empty();
        }
        return TubeModels.mesh(kind, named);
    }
}
