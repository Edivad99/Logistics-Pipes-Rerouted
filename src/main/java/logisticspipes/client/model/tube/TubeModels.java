package logisticspipes.client.model.tube;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.resources.ResourceLocation;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import logisticspipes.LPConstants;
import logisticspipes.client.model.LpObjModels;
import logisticspipes.client.model.mesh.MeshTransforms;
import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.client.model.mesh.ObjModel;

/**
 * Geometry for the five high-speed tube types, one merged mesh per orientation.
 *
 * <p>The five renderers in {@code logisticspipes.renderer.newpipe.tube} each had their own
 * copy of the same forty-line {@code loadModels()}: read one OBJ, take the groups tagged
 * {@code Lane} or {@code Side}, and place four copies with a translation and a quarter turn.
 * Only the OBJ, the tag and the four transforms ever differed, so those are the only things
 * declared here.</p>
 *
 * <p><b>The orientations actually turn now.</b> Every caller passed its angle in radians to
 * {@code new LPRotation(...)}, which reached {@code CCLProxy.getRotation(double, int, int,
 * int)} — and that ran {@code Math.toRadians} over it a second time, so a quarter turn came
 * out as about 1.57 degrees, around (0.5, 0.5, 0.5) rather than the origin. Every tube
 * orientation was therefore very nearly the unrotated one. {@link MeshTransforms#rotation}
 * restores CodeChickenLib's contract, which is what the 1.12.2 models were authored
 * against.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class TubeModels {

    private static final Logger LOGGER = LogManager.getLogger(TubeModels.class);

    private TubeModels() {
    }

    /**
     * Which OBJ, which group tag, and which standalone texture a tube type draws with.
     */
    // Texture names are the ones the five renderers already used — note that speedup is
    // "hs-speedup" and not "hs-tube-speedup", and that three of the five share "hs-tube".
    public enum Kind {
        LINE(LpObjModels.TUBE_LINE, "Side", "hs-tube-line"),
        CURVE(LpObjModels.TUBE_TURN, "Lane", "hs-tube"),
        GAIN(LpObjModels.TUBE_GAIN, "Lane", "hs-tube"),
        SPEEDUP(LpObjModels.TUBE_SPEEDUP, "Side", "hs-speedup"),
        // Deliberately the gain model: the S-curve is the gain geometry stood on its side.
        SCURVE(LpObjModels.TUBE_GAIN, "Lane", "hs-tube");

        public final ResourceLocation obj;
        public final String groupToken;
        public final ResourceLocation texture;

        Kind(ResourceLocation obj, String groupToken, String texture) {
            this.obj = obj;
            this.groupToken = groupToken;
            this.texture = LPConstants.rl("textures/blocks/pipes/" + texture + ".png");
        }
    }

    private static final float QUARTER = (float) (Math.PI / 2);

    /**
     * Placement per orientation, keyed by the orientation enum's {@code name()} so the five
     * unrelated orientation enums can share one table.
     *
     * <p>Each matrix is written in application order as the original chained {@code apply()}
     * calls: the rightmost factor runs first.</p>
     */
    private static Map<String, Matrix4f> transforms(Kind kind) {
        Map<String, Matrix4f> byOrientation = new HashMap<>();
        switch (kind) {
            case LINE -> {
                byOrientation.put("EAST_WEST", rotY(-QUARTER));
                byOrientation.put("NORTH_SOUTH", MeshTransforms.translation(0, 0, 1));
            }
            case CURVE -> {
                byOrientation.put("SOUTH_WEST", rotY(-QUARTER));
                byOrientation.put("EAST_SOUTH", MeshTransforms.translation(0, 0, 1));
                byOrientation.put("NORTH_EAST", rotY(QUARTER).mul(MeshTransforms.translation(-1, 0, 1)));
                byOrientation.put("WEST_NORTH", rotY((float) Math.PI).mul(MeshTransforms.translation(-1, 0, 0)));
            }
            case GAIN, SPEEDUP -> {
                byOrientation.put("EAST", rotY(-QUARTER));
                byOrientation.put("NORTH", MeshTransforms.translation(0, 0, 1));
                byOrientation.put("WEST", rotY(QUARTER).mul(MeshTransforms.translation(-1, 0, 1)));
                byOrientation.put("SOUTH", rotY((float) Math.PI).mul(MeshTransforms.translation(-1, 0, 0)));
            }
            case SCURVE -> {
                // Rolled a quarter turn about Z first, which is what makes it an S rather than
                // a plain gain, then placed like the others.
                byOrientation.put("EAST", rotY(-QUARTER)
                    .mul(MeshTransforms.translation(1, 0, 0)).mul(rotZ(QUARTER)));
                byOrientation.put("NORTH", new Matrix4f(MeshTransforms.translation(1, 0, 1)).mul(rotZ(QUARTER)));
                byOrientation.put("EAST_INV", rotY(QUARTER)
                    .mul(MeshTransforms.translation(-2, 1, 4)).mul(rotZ(-QUARTER)));
                byOrientation.put("NORTH_INV", rotY((float) Math.PI)
                    .mul(MeshTransforms.translation(-2, 1, 3)).mul(rotZ(-QUARTER)));
            }
        }
        return byOrientation;
    }

    private static Matrix4f rotY(float radians) {
        return MeshTransforms.rotation(radians, 0, 1, 0);
    }

    private static Matrix4f rotZ(float radians) {
        return MeshTransforms.rotation(radians, 0, 0, 1);
    }

    private static volatile Map<Kind, Map<String, ObjMesh>> loaded = Map.of();

    /**
     * Rebuilt whenever the OBJ models are republished.
     */
    public static void reload() {
        Map<Kind, Map<String, ObjMesh>> all = new HashMap<>();
        for (Kind kind : Kind.values()) {
            ObjModel model = LpObjModels.get(kind.obj);
            List<ObjModel.Part> parts = model.byName(kind.groupToken);
            if (parts.isEmpty()) {
                LOGGER.error("Couldn't load {} tube geometry (group {})", kind, kind.groupToken);
                continue;
            }

            Map<String, ObjMesh> byOrientation = new HashMap<>();
            transforms(kind).forEach((orientation, transform) -> {
                List<ObjMesh> placed = new ArrayList<>(parts.size());
                for (ObjModel.Part part : parts) {
                    // Single-sided on purpose. The originals called twoFacedCopy() here, but the
                    // tubes are drawn with RenderType.entityCutoutNoCull, which already shows
                    // both faces of a quad. Duplicating them put two coplanar copies with
                    // opposed normals in the same place, and since entity render types shade
                    // from the normal, the two copies fought for depth and were lit opposite
                    // ways — one side of a tube came out darker than the other.
                    placed.add(part.mesh().transform(transform).withComputedNormals());
                }
                // The originals kept the four lanes as separate models only because each had to
                // become its own RenderEntry; one mesh per orientation is equivalent and cheaper.
                byOrientation.put(orientation, ObjMesh.merge(placed));
            });
            all.put(kind, byOrientation);
        }
        loaded = all;
    }

    /**
     * The mesh for a tube orientation, or an empty mesh if it is unknown.
     *
     * @param orientation the pipe's render orientation; only its {@code name()} is used, since
     *                    each tube type has its own unrelated orientation enum
     */
    public static ObjMesh mesh(Kind kind, Enum<?> orientation) {
        if (orientation == null) {
            return ObjMesh.empty();
        }
        return loaded.getOrDefault(kind, Map.of()).getOrDefault(orientation.name(), ObjMesh.empty());
    }

    public static boolean isLoaded() {
        return !loaded.isEmpty();
    }
}
