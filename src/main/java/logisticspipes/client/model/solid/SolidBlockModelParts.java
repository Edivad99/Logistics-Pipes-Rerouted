package logisticspipes.client.model.solid;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.joml.Matrix4f;

import logisticspipes.client.model.LpObjModels;
import logisticspipes.client.model.mesh.MeshTransforms;
import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.client.model.mesh.ObjModel;
import logisticspipes.client.model.mesh.UvTransform;

/**
 * The LP solid block body and its cover plates, in each of the four facings.
 *
 * <p>Port of {@code LogisticsNewSolidBlockWorldRenderer}'s static maps and its
 * {@code computeRotated}. Same geometry, on the immutable mesh engine and without the
 * shared render state.</p>
 */
@OnlyIn(Dist.CLIENT)
public final class SolidBlockModelParts {

    private static final Logger LOGGER = LogManager.getLogger(SolidBlockModelParts.class);

    /**
     * The cover plate positions, named by the letter their OBJ groups use.
     * There is no {@code UP} — the block's top has no plate.
     */
    public enum CoverSide {
        DOWN(Direction.DOWN, "D"),
        NORTH(Direction.NORTH, "N"),
        SOUTH(Direction.SOUTH, "S"),
        WEST(Direction.WEST, "W"),
        EAST(Direction.EAST, "E");

        public final Direction dir;
        public final String letter;

        CoverSide(Direction dir, String letter) {
            this.dir = dir;
            this.letter = letter;
        }

        /**
         * The world-space face this plate ends up on once the block is rotated.
         */
        public Direction facing(int rotation) {
            if (dir == Direction.DOWN) {
                return dir;
            }
            Direction result = dir;
            // Reproduces the original's deliberate switch fall-through: rotation 0 turns three
            // times, 3 turns twice, 1 turns once, 2 not at all.
            int turns = switch (rotation) {
                case 0 -> 3;
                case 3 -> 2;
                case 1 -> 1;
                default -> 0;
            };
            for (int i = 0; i < turns; i++) {
                result = result.getClockWise();
            }
            return result;
        }
    }

    private final Map<Integer, ObjMesh> body;
    private final Map<CoverSide, Map<Integer, ObjMesh>> outerPlates;
    private final Map<CoverSide, Map<Integer, ObjMesh>> innerPlates;

    private SolidBlockModelParts(Map<Integer, ObjMesh> body,
        Map<CoverSide, Map<Integer, ObjMesh>> outerPlates,
        Map<CoverSide, Map<Integer, ObjMesh>> innerPlates) {
        this.body = body;
        this.outerPlates = outerPlates;
        this.innerPlates = innerPlates;
    }

    public static SolidBlockModelParts empty() {
        return new SolidBlockModelParts(Map.of(), Map.of(), Map.of());
    }

    public boolean isEmpty() {
        return body.isEmpty();
    }

    public ObjMesh body(int rotation) {
        return body.getOrDefault(rotation, ObjMesh.empty());
    }

    public ObjMesh outerPlate(CoverSide side, int rotation) {
        return outerPlates.getOrDefault(side, Map.of()).getOrDefault(rotation, ObjMesh.empty());
    }

    public ObjMesh innerPlate(CoverSide side, int rotation) {
        return innerPlates.getOrDefault(side, Map.of()).getOrDefault(rotation, ObjMesh.empty());
    }

    public static SolidBlockModelParts load() {
        ObjModel model = LpObjModels.get(LpObjModels.SOLID_BLOCK);
        if (model.groups().isEmpty()) {
            return empty();
        }

        Map<Integer, ObjMesh> body = Map.of();
        List<ObjModel.Part> blockParts = model.byName("Block");
        if (blockParts.isEmpty()) {
            LOGGER.error("Couldn't load solid block body (Block)");
        } else {
            body = rotations(prepare(blockParts.get(0).mesh()));
        }

        Map<CoverSide, Map<Integer, ObjMesh>> outer = new EnumMap<>(CoverSide.class);
        Map<CoverSide, Map<Integer, ObjMesh>> inner = new EnumMap<>(CoverSide.class);
        for (CoverSide side : CoverSide.values()) {
            putPlate(model, "OutSide_" + side.letter, side, outer);
            putPlate(model, "Inside_" + side.letter, side, inner);
        }
        return new SolidBlockModelParts(body, outer, inner);
    }

    private static void putPlate(ObjModel model, String group, CoverSide side,
        Map<CoverSide, Map<Integer, ObjMesh>> target) {
        List<ObjModel.Part> parts = model.byName(group);
        if (parts.isEmpty()) {
            LOGGER.error("Couldn't load solid block plate {}", group);
            return;
        }
        target.put(side, rotations(prepare(parts.get(0).mesh())));
    }

    private static ObjMesh prepare(ObjMesh mesh) {
        return mesh.doubleSided().transform(MeshTransforms.translation(0, 0, 1));
    }

    /**
     * The four facings, each rotated and then translated back into the unit cube.
     *
     * <p>The V scale is 95/128 and not 96/128. The solid block textures are 128×128 with
     * content in rows 0–95 and transparent padding above; after the parser's V flip an OBJ
     * v=0 vertex maps to v=1.0, and scaling by exactly 0.75 lands on the row 95/96 boundary
     * where bilinear filtering mixes in the transparent row and drops alpha just below the
     * cutout threshold — those faces then render as holes.</p>
     */
    private static Map<Integer, ObjMesh> rotations(ObjMesh mesh) {
        ObjMesh scaled = mesh.uvTransform(UvTransform.scale(1, 0.742f));
        Map<Integer, ObjMesh> byRotation = new java.util.HashMap<>(4);
        for (int rotation = 0; rotation < 4; rotation++) {
            Matrix4f transform = switch (rotation) {
                case 0 -> new Matrix4f(MeshTransforms.translation(0, 0, 1)).mul(MeshTransforms.sideOrientation(3));
                case 1 -> new Matrix4f(MeshTransforms.translation(1, 0, 0)).mul(MeshTransforms.sideOrientation(1));
                case 3 -> new Matrix4f(MeshTransforms.translation(1, 0, 1)).mul(MeshTransforms.sideOrientation(2));
                default -> new Matrix4f();
            };
            byRotation.put(rotation, scaled.transform(transform).withComputedNormals());
        }
        return byRotation;
    }

    /**
     * Every mesh, for callers that need a flat view.
     */
    public List<ObjMesh> allMeshes() {
        List<ObjMesh> all = new ArrayList<>(body.values());
        outerPlates.values().forEach(byRotation -> all.addAll(byRotation.values()));
        innerPlates.values().forEach(byRotation -> all.addAll(byRotation.values()));
        return all;
    }
}
