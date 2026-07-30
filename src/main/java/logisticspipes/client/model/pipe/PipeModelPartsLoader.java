package logisticspipes.client.model.pipe;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import org.joml.Matrix4f;

import logisticspipes.client.model.LpObjModels;
import logisticspipes.client.model.mesh.MeshTransforms;
import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.client.model.mesh.ObjModel;

/**
 * Assembles {@link PipeModelParts} from the parsed OBJ groups.
 *
 * <p>Port of {@code LogisticsNewRenderPipe.loadModels()}. The transforms are unchanged —
 * every part is double-sided, shifted by {@code (0, 0, 1)} into the unit cube, and has its
 * normals recomputed — but the part lookups now go through {@link ObjModel}'s token indices
 * instead of scanning all 512 group lines per part.</p>
 *
 * <p>The part-count checks from the original are kept and are the main regression test on
 * the lookups: if the indices ever disagree with the old string matching, a count comes out
 * wrong here. Unlike the original, a mismatch is reported through {@link Problems} rather
 * than thrown, so one bad family does not cost the whole model.</p>
 */
public final class PipeModelPartsLoader {

    private PipeModelPartsLoader() {
    }

    /**
     * Collects part-count mismatches instead of aborting on the first one.
     */
    public interface Problems {

        void report(String message);
    }

    /**
     * Every part is authored offset by one block along Z.
     */
    private static final Matrix4f INTO_UNIT_CUBE = MeshTransforms.translation(0, 0, 1);

    /**
     * Loads from the models published by the resource reload.
     */
    public static PipeModelParts load(Problems problems) {
        return load(LpObjModels.get(LpObjModels.PIPE), LpObjModels.get(LpObjModels.TRANSPORT_BOX), problems);
    }

    /**
     * Assembles the parts from explicitly supplied models. Taking the models as arguments
     * rather than reading the static holder is what lets the part-count checks be exercised
     * as a unit test against the real OBJ file.
     */
    public static PipeModelParts load(ObjModel model, ObjModel transportBoxModel, Problems problems) {
        if (model.groups().isEmpty()) {
            problems.report("pipe OBJ model is empty or failed to load");
            return PipeModelParts.empty();
        }

        Map<Direction, List<ObjMesh>> sideNormal = new EnumMap<>(Direction.class);
        Map<Direction, List<ObjMesh>> sideBC = new EnumMap<>(Direction.class);
        Map<PipeEdge, ObjMesh> edges = new EnumMap<>(PipeEdge.class);
        Map<PipeCorner, List<ObjMesh>> cornersM = new EnumMap<>(PipeCorner.class);
        Map<PipeCorner, List<ObjMesh>> cornersI3 = new EnumMap<>(PipeCorner.class);
        Map<PipeTurnCorner, ObjMesh> cornersI = new EnumMap<>(PipeTurnCorner.class);
        Map<PipeSupport, ObjMesh> supports = new EnumMap<>(PipeSupport.class);
        Map<PipeTurnCorner, ObjMesh> spacers = new EnumMap<>(PipeTurnCorner.class);
        Map<PipeMount, ObjMesh> mounts = new EnumMap<>(PipeMount.class);
        Map<Direction, List<ObjMesh>> plateInner = new EnumMap<>(Direction.class);
        Map<Direction, List<ObjMesh>> plateOuter = new EnumMap<>(Direction.class);
        Map<Direction, Map<PipeModelParts.SidePlate, List<ObjMesh>>> sidePlates = new EnumMap<>(Direction.class);
        Map<PipeMount, List<ObjMesh>> connectorPlates = new EnumMap<>(PipeMount.class);
        List<ObjMesh> highlightParts = new ArrayList<>();

        for (Direction dir : Direction.values()) {
            String group = "Side_" + PipeDirections.letter(dir);
            sideNormal.put(dir, exact(model, group));
            expect(problems, sideNormal.get(dir).size(), 4, dir.name(), group);

            group = "Side_BC_" + PipeDirections.letter(dir);
            sideBC.put(dir, exact(model, group));
            expect(problems, sideBC.get(dir).size(), 8, dir.name(), group);
        }

        for (PipeEdge edge : PipeEdge.values()) {
            List<ObjMesh> found = exact(model, edge.groupName());
            if (found.isEmpty()) {
                problems.report("Couldn't load " + edge.name() + " (" + edge.groupName() + ")");
                continue;
            }
            edges.put(edge, found.get(0));
            highlightParts.add(found.get(0));
        }

        for (PipeCorner corner : PipeCorner.values()) {
            String group = "Corner_M_" + corner.groupSuffix();
            cornersM.put(corner, exact(model, group));
            expect(problems, cornersM.get(corner).size(), 2, corner.name(), group);
            highlightParts.addAll(cornersM.get(corner));

            group = "Corner_I3_" + corner.groupSuffix();
            cornersI3.put(corner, exact(model, group));
            expect(problems, cornersI3.get(corner).size(), 2, corner.name(), group);
        }

        for (PipeSupport support : PipeSupport.values()) {
            single(model, support.groupName()).ifPresentOrElse(
                mesh -> supports.put(support, mesh),
                () -> problems.report("Couldn't load " + support.name() + " (" + support.groupName() + ")"));
        }

        for (PipeTurnCorner corner : PipeTurnCorner.values()) {
            // The three turns sharing a corner are the numeric variants of one base name:
            // no suffix is UP_DOWN, 1 is EAST_WEST, 2 is NORTH_SOUTH.
            String group = "Corner_I_" + corner.corner.groupSuffix();
            model.byNamePrefix(group).stream()
                .filter(part -> part.variant() == corner.groupVariant())
                .findFirst()
                .ifPresentOrElse(
                    part -> cornersI.put(corner, prepare(part.mesh())),
                    () -> problems.report("Couldn't load " + corner.name() + " (" + group + ")"));

            String spacer = "Spacer" + corner.number;
            single(model, spacer).ifPresentOrElse(
                mesh -> spacers.put(corner, mesh),
                () -> problems.report("Couldn't load " + corner.name() + " (" + spacer + ")"));
        }

        for (PipeMount mount : PipeMount.values()) {
            single(model, mount.groupName()).ifPresentOrElse(
                mesh -> mounts.put(mount, mesh),
                () -> problems.report("Couldn't load " + mount.name() + " (" + mount.groupName() + ")"));

            connectorPlates.put(mount, exact(model, mount.connectorGroupName()));
            expect(problems, connectorPlates.get(mount).size(), 4, mount.name(), mount.connectorGroupName());
        }

        for (Direction dir : Direction.values()) {
            String group = "Inner_Plate_" + PipeDirections.letter(dir);
            plateInner.put(dir, prefix(model, group));
            expect(problems, plateInner.get(dir).size(), 2, dir.name(), group);

            group = "Texture_Plate_" + PipeDirections.letter(dir);
            // The 1.001 scale about the block centre lifts the outer plates off the frame so
            // they do not z-fight with it.
            Matrix4f inflate = new Matrix4f(MeshTransforms.translation(0.5, 0.5, 0.5))
                .mul(MeshTransforms.scale(1.001))
                .mul(MeshTransforms.translation(-0.5, -0.5, -0.5));
            List<ObjMesh> outer = new ArrayList<>();
            for (ObjModel.Part part : model.byNamePrefix(group)) {
                outer.add(prepare(part.mesh()).transform(inflate).withComputedNormals());
            }
            plateOuter.put(dir, outer);
            expect(problems, outer.size(), 2, dir.name(), group);

            sidePlates.put(dir, classifySidePlates(model, dir, problems));
        }

        ObjMesh transportBox = loadTransportBox(transportBoxModel, problems);

        return new PipeModelParts(sideNormal, sideBC, edges, cornersM, cornersI3, cornersI,
            supports, spacers, mounts, plateInner, plateOuter, sidePlates, connectorPlates,
            transportBox, ObjMesh.merge(highlightParts));
    }

    /**
     * Sorts the {@code Texture_Side_<dir>} groups into the four plate kinds.
     *
     * <p>The OBJ carries no distinguishing name for these, so the original classified them
     * geometrically: total bounding-box extent under 0.5 means a small plate, and the squared
     * distance from the min corner to the block centre falls into two bands. Both the
     * thresholds and the bands are reproduced exactly.</p>
     */
    private static Map<PipeModelParts.SidePlate, List<ObjMesh>> classifySidePlates(
        ObjModel model, Direction dir, Problems problems) {
        Map<PipeModelParts.SidePlate, List<ObjMesh>> byPlate = new EnumMap<>(PipeModelParts.SidePlate.class);
        for (PipeModelParts.SidePlate plate : PipeModelParts.SidePlate.values()) {
            byPlate.put(plate, new ArrayList<>());
        }

        String group = "Texture_Side_" + PipeDirections.letter(dir);
        for (ObjModel.Part part : model.byNamePrefix(group)) {
            ObjMesh mesh = prepare(part.mesh());
            AABB bounds = mesh.bounds();
            double extent = (bounds.maxX - bounds.minX) + (bounds.maxY - bounds.minY) + (bounds.maxZ - bounds.minZ);
            double distance =
                Math.pow(bounds.minX - 0.5, 2) + Math.pow(bounds.minY - 0.5, 2) + Math.pow(bounds.minZ - 0.5, 2);

            boolean small = extent < 0.5;
            boolean outer = (distance > 0.22 && distance < 0.24) || (distance > 0.38 && distance < 0.40);
            boolean inner = (distance < 0.2 && distance > 0.18) || (distance < 0.36 && distance > 0.34);
            if (!outer && !inner) {
                problems.report("Unclassifiable side plate in " + group + " at distance " + distance);
                continue;
            }
            PipeModelParts.SidePlate plate;
            if (small) {
                plate = outer ? PipeModelParts.SidePlate.SMALL_OUTER : PipeModelParts.SidePlate.SMALL_INNER;
            } else {
                plate = outer ? PipeModelParts.SidePlate.LARGE_OUTER : PipeModelParts.SidePlate.LARGE_INNER;
            }
            byPlate.get(plate).add(mesh);
        }

        for (PipeModelParts.SidePlate plate : PipeModelParts.SidePlate.values()) {
            expect(problems, byPlate.get(plate).size(), 8, dir.name() + " " + plate, group);
        }
        return byPlate;
    }

    private static ObjMesh loadTransportBox(ObjModel box, Problems problems) {
        ObjMesh mesh = box.group("InnerTransportBox");
        if (mesh == null) {
            problems.report("Couldn't load InnerTransportBox");
            return ObjMesh.empty();
        }
        // Shrunk to 0.99 about the block centre so it sits inside the pipe frame.
        Matrix4f shrink = new Matrix4f(MeshTransforms.translation(0.5, 0.5, 0.5))
            .mul(MeshTransforms.scale(0.99))
            .mul(MeshTransforms.translation(-0.5, -0.5, -0.5));
        return prepare(mesh).transform(shrink).withComputedNormals();
    }

    /**
     * Double-sided, moved into the unit cube, normals recomputed — as every part was.
     */
    private static ObjMesh prepare(ObjMesh mesh) {
        return mesh.doubleSided().transform(INTO_UNIT_CUBE).withComputedNormals();
    }

    private static List<ObjMesh> exact(ObjModel model, String group) {
        List<ObjMesh> out = new ArrayList<>();
        for (ObjModel.Part part : model.byName(group)) {
            out.add(prepare(part.mesh()));
        }
        return out;
    }

    private static List<ObjMesh> prefix(ObjModel model, String group) {
        List<ObjMesh> out = new ArrayList<>();
        for (ObjModel.Part part : model.byNamePrefix(group)) {
            out.add(prepare(part.mesh()));
        }
        return out;
    }

    private static Optional<ObjMesh> single(ObjModel model, String group) {
        List<ObjModel.Part> parts = model.byName(group);
        return parts.isEmpty() ? Optional.empty() : Optional.of(prepare(parts.get(0).mesh()));
    }

    private static void expect(Problems problems, int actual, int expected, String what, String group) {
        if (actual != expected) {
            problems.report("Couldn't load " + what + " (" + group + "). Expected " + expected + ", loaded " + actual);
        }
    }
}
