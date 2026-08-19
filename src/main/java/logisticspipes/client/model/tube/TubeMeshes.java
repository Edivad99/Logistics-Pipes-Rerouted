package logisticspipes.client.model.tube;

import javax.annotation.Nullable;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.interfaces.ITubeRenderOrientation;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.tubes.HSTubeCurve;
import logisticspipes.pipes.tubes.HSTubeGain;
import logisticspipes.pipes.tubes.HSTubeLine;
import logisticspipes.pipes.tubes.HSTubeSCurve;
import logisticspipes.pipes.tubes.HSTubeSpeedup;

/**
 * Resolves a high-speed tube pipe to the geometry and texture it draws with.
 *
 * <p>The dispatch lives here rather than on {@code ISpecialPipeRenderer} so the interface —
 * which is common code — does not gain a dependency on the client-only mesh classes.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TubeMeshes {

    private static final TubeGeometry NONE = new TubeGeometry(ObjMesh.empty(), null);

    private TubeMeshes() {
    }

    public static TubeGeometry forPipe(@Nullable CoreUnroutedPipe pipe) {
        return forPipe(pipe, null);
    }

    /**
     * @param orientation used instead of the pipe's own. The placement preview needs this: it
     *                    draws the dummy pipe of a held item, whose orientation is null, at an
     *                    orientation derived from where the player is standing.
     */
    public static TubeGeometry forPipe(@Nullable CoreUnroutedPipe pipe, @Nullable ITubeOrientation orientation) {
        TubeModels.Kind kind = kindOf(pipe);
        if (kind == null) {
            return NONE;
        }

        ITubeOrientation effective = orientation != null ? orientation : ownOrientation(pipe);
        if (effective == null) {
            return NONE;
        }
        ITubeRenderOrientation render = effective.getRenderOrientation();
        if (!(render instanceof Enum<?> named)) {
            return NONE;
        }

        ObjMesh mesh = TubeModels.mesh(kind, named);
        return mesh.isEmpty() ? NONE : new TubeGeometry(mesh, kind.texture);
    }

    /**
     * The tube's item form: its geometry at a fixed orientation, since a dummy pipe has none.
     * The mesh is in world units and can span several blocks, so the caller has to fit it into
     * the item's unit cube itself.
     */
    public static TubeGeometry forItem(@Nullable CoreUnroutedPipe pipe) {
        TubeModels.Kind kind = kindOf(pipe);
        if (kind == null) {
            return NONE;
        }
        ObjMesh mesh = TubeModels.mesh(kind, kind.orientationWithoutPipe);
        return mesh.isEmpty() ? NONE : new TubeGeometry(mesh, kind.texture);
    }

    /**
     * The tube type a pipe is, or null when it is not a high-speed tube.
     */
    @Nullable
    public static TubeModels.Kind kindOf(@Nullable CoreUnroutedPipe pipe) {
        if (pipe instanceof HSTubeLine) {
            return TubeModels.Kind.LINE;
        }
        if (pipe instanceof HSTubeCurve) {
            return TubeModels.Kind.CURVE;
        }
        if (pipe instanceof HSTubeGain) {
            return TubeModels.Kind.GAIN;
        }
        if (pipe instanceof HSTubeSpeedup) {
            return TubeModels.Kind.SPEEDUP;
        }
        if (pipe instanceof HSTubeSCurve) {
            return TubeModels.Kind.SCURVE;
        }
        return null;
    }

    @Nullable
    private static ITubeOrientation ownOrientation(CoreUnroutedPipe pipe) {
        return switch (pipe) {
            case HSTubeLine tube -> tube.getOrientation();
            case HSTubeCurve tube -> tube.getOrientation();
            case HSTubeGain tube -> tube.getOrientation();
            case HSTubeSpeedup tube -> tube.getOrientation();
            case HSTubeSCurve tube -> tube.getOrientation();
            default -> null;
        };
    }

    /**
     * Empty mesh when the pipe is not a tube, or has no orientation yet.
     */
    public record TubeGeometry(ObjMesh mesh, @Nullable ResourceLocation texture) {

        public boolean isEmpty() {
            return mesh.isEmpty() || texture == null;
        }
    }
}
