package logisticspipes.client.model;

import java.util.List;
import java.util.Map;

import net.minecraft.resources.Identifier;

import logisticspipes.LPConstants;
import logisticspipes.client.model.mesh.ObjModel;

/**
 * The set of OBJ files LP renders from, and the parsed result of the most recent resource
 * reload.
 *
 * <p>Every model is authored at 100× and is scaled to block space by
 * {@link ObjModelManager} as it is parsed, matching the {@code new LPScale(1 / 100f)} that
 * each of the old {@code loadModels()} methods passed to {@code parseObjModels}.</p>
 */
public final class LpObjModels {

    private LpObjModels() {
    }

    // Identifier paths only accept [a-z0-9/._-], so these files are named in lower
    // snake case rather than keeping the CamelCase they had on the classpath.
    public static final Identifier PIPE = obj("pipe_model_moved");
    public static final Identifier TRANSPORT_BOX = obj("pipe_model_transport_box");
    public static final Identifier SOLID_BLOCK = obj("block_model_result");
    public static final Identifier TUBE_LINE = obj("hstube_line_result");
    public static final Identifier TUBE_TURN = obj("hstube_turn_result");
    public static final Identifier TUBE_GAIN = obj("hstube_gain_result");
    public static final Identifier TUBE_SPEEDUP = obj("hstube_speedup_result");

    /**
     * Every file the reload listener loads.
     */
    public static final List<Identifier> ALL = List.of(
        PIPE, TRANSPORT_BOX, SOLID_BLOCK, TUBE_LINE, TUBE_TURN, TUBE_GAIN, TUBE_SPEEDUP);

    /**
     * The models authored at 100×, already scaled down. Swapped wholesale on reload.
     */
    private static volatile Map<Identifier, ObjModel> loaded = Map.of();

    private static Identifier obj(String name) {
        return LPConstants.rl("models/obj/" + name + ".obj");
    }

    static void setLoaded(Map<Identifier, ObjModel> models) {
        loaded = Map.copyOf(models);
    }

    /**
     * The parsed model, or an empty one when the file failed to load. Callers assembling
     * parts should check {@link ObjModel#groups()} rather than assume content — a resource
     * pack can remove or break any of these.
     */
    public static ObjModel get(Identifier location) {
        ObjModel model = loaded.get(location);
        return model == null ? ObjModel.empty() : model;
    }

    /**
     * True once a reload has produced at least one usable model.
     */
    public static boolean isLoaded() {
        return !loaded.isEmpty();
    }
}
