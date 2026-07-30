package logisticspipes.client.model.pipe;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Direction;

import logisticspipes.client.model.mesh.ObjMesh;

/**
 * The assembled, atlas-independent geometry of the pipe frame: every side, edge, corner,
 * support, mount and texture plate, already transformed into block space.
 *
 * <p>Replaces the fifteen mutable static maps on {@code LogisticsNewRenderPipe}
 * ({@code sideNormal}, {@code sideBC}, {@code edges}, {@code corners_M}, ...). Being
 * immutable and published as a single reference, it can be swapped wholesale on a resource
 * reload and read from the chunk-baking threads without synchronization — which the old
 * static {@code HashMap}s could not support.</p>
 *
 * @see PipeModelPartsLoader for how these are built from the OBJ groups
 */
public final class PipeModelParts {

    /**
     * The four kinds of side texture plate, distinguished in the OBJ only by their size and
     * distance from the block centre. The old code addressed them as
     * {@code Quartet.getValue1()} .. {@code getValue4()}; the names here say what the
     * classification in {@link PipeModelPartsLoader} actually tests for.
     */
    public enum SidePlate {
        /**
         * Large plate, near the centre. Was {@code Quartet.value1}.
         */
        LARGE_INNER,
        /**
         * Small plate, near the centre. Was {@code Quartet.value2}.
         */
        SMALL_INNER,
        /**
         * Large plate, further out. Was {@code Quartet.value3}.
         */
        LARGE_OUTER,
        /**
         * Small plate, further out. Was {@code Quartet.value4}.
         */
        SMALL_OUTER
    }

    private final Map<Direction, List<ObjMesh>> sideNormal;
    private final Map<Direction, List<ObjMesh>> sideBC;
    private final Map<PipeEdge, ObjMesh> edges;
    private final Map<PipeCorner, List<ObjMesh>> cornersM;
    private final Map<PipeCorner, List<ObjMesh>> cornersI3;
    private final Map<PipeTurnCorner, ObjMesh> cornersI;
    private final Map<PipeSupport, ObjMesh> supports;
    private final Map<PipeTurnCorner, ObjMesh> spacers;
    private final Map<PipeMount, ObjMesh> mounts;
    private final Map<Direction, List<ObjMesh>> texturePlateInner;
    private final Map<Direction, List<ObjMesh>> texturePlateOuter;
    private final Map<Direction, Map<SidePlate, List<ObjMesh>>> sideTexturePlate;
    private final Map<PipeMount, List<ObjMesh>> textureConnectorPlate;
    private final ObjMesh innerTransportBox;
    private final ObjMesh highlight;

    PipeModelParts(
        Map<Direction, List<ObjMesh>> sideNormal,
        Map<Direction, List<ObjMesh>> sideBC,
        Map<PipeEdge, ObjMesh> edges,
        Map<PipeCorner, List<ObjMesh>> cornersM,
        Map<PipeCorner, List<ObjMesh>> cornersI3,
        Map<PipeTurnCorner, ObjMesh> cornersI,
        Map<PipeSupport, ObjMesh> supports,
        Map<PipeTurnCorner, ObjMesh> spacers,
        Map<PipeMount, ObjMesh> mounts,
        Map<Direction, List<ObjMesh>> texturePlateInner,
        Map<Direction, List<ObjMesh>> texturePlateOuter,
        Map<Direction, Map<SidePlate, List<ObjMesh>>> sideTexturePlate,
        Map<PipeMount, List<ObjMesh>> textureConnectorPlate,
        ObjMesh innerTransportBox,
        ObjMesh highlight) {
        this.sideNormal = sideNormal;
        this.sideBC = sideBC;
        this.edges = edges;
        this.cornersM = cornersM;
        this.cornersI3 = cornersI3;
        this.cornersI = cornersI;
        this.supports = supports;
        this.spacers = spacers;
        this.mounts = mounts;
        this.texturePlateInner = texturePlateInner;
        this.texturePlateOuter = texturePlateOuter;
        this.sideTexturePlate = sideTexturePlate;
        this.textureConnectorPlate = textureConnectorPlate;
        this.innerTransportBox = innerTransportBox;
        this.highlight = highlight;
    }

    /**
     * An empty set of parts, published when the OBJ failed to load.
     */
    public static PipeModelParts empty() {
        return new PipeModelParts(
            new EnumMap<>(Direction.class), new EnumMap<>(Direction.class),
            new EnumMap<>(PipeEdge.class), new EnumMap<>(PipeCorner.class),
            new EnumMap<>(PipeCorner.class), new EnumMap<>(PipeTurnCorner.class),
            new EnumMap<>(PipeSupport.class), new EnumMap<>(PipeTurnCorner.class),
            new EnumMap<>(PipeMount.class), new EnumMap<>(Direction.class),
            new EnumMap<>(Direction.class), new EnumMap<>(Direction.class),
            new EnumMap<>(PipeMount.class), ObjMesh.empty(), ObjMesh.empty());
    }

    public boolean isEmpty() {
        return sideNormal.isEmpty();
    }

    public List<ObjMesh> sideNormal(Direction dir) {
        return sideNormal.getOrDefault(dir, List.of());
    }

    public List<ObjMesh> sideBC(Direction dir) {
        return sideBC.getOrDefault(dir, List.of());
    }

    public ObjMesh edge(PipeEdge edge) {
        return edges.getOrDefault(edge, ObjMesh.empty());
    }

    public List<ObjMesh> cornerM(PipeCorner corner) {
        return cornersM.getOrDefault(corner, List.of());
    }

    public List<ObjMesh> cornerI3(PipeCorner corner) {
        return cornersI3.getOrDefault(corner, List.of());
    }

    public ObjMesh cornerI(PipeTurnCorner corner) {
        return cornersI.getOrDefault(corner, ObjMesh.empty());
    }

    public ObjMesh support(PipeSupport support) {
        return supports.getOrDefault(support, ObjMesh.empty());
    }

    public ObjMesh spacer(PipeTurnCorner corner) {
        return spacers.getOrDefault(corner, ObjMesh.empty());
    }

    public ObjMesh mount(PipeMount mount) {
        return mounts.getOrDefault(mount, ObjMesh.empty());
    }

    public List<ObjMesh> texturePlateInner(Direction dir) {
        return texturePlateInner.getOrDefault(dir, List.of());
    }

    public List<ObjMesh> texturePlateOuter(Direction dir) {
        return texturePlateOuter.getOrDefault(dir, List.of());
    }

    public List<ObjMesh> sideTexturePlate(Direction dir, SidePlate plate) {
        return sideTexturePlate.getOrDefault(dir, Map.of()).getOrDefault(plate, List.of());
    }

    public List<ObjMesh> textureConnectorPlate(PipeMount mount) {
        return textureConnectorPlate.getOrDefault(mount, List.of());
    }

    /**
     * The transport box drawn inside a pipe carrying items.
     */
    public ObjMesh innerTransportBox() {
        return innerTransportBox;
    }

    /**
     * All edges and middle corners merged, used for the block placement outline.
     */
    public ObjMesh highlight() {
        return highlight;
    }

    /**
     * Every mesh in this set, for callers that need a flat view (e.g. bounds checks).
     */
    public List<ObjMesh> allMeshes() {
        List<ObjMesh> all = new ArrayList<>();
        sideNormal.values().forEach(all::addAll);
        sideBC.values().forEach(all::addAll);
        all.addAll(edges.values());
        cornersM.values().forEach(all::addAll);
        cornersI3.values().forEach(all::addAll);
        all.addAll(cornersI.values());
        all.addAll(supports.values());
        all.addAll(spacers.values());
        all.addAll(mounts.values());
        texturePlateInner.values().forEach(all::addAll);
        texturePlateOuter.values().forEach(all::addAll);
        sideTexturePlate.values().forEach(byPlate -> byPlate.values().forEach(all::addAll));
        textureConnectorPlate.values().forEach(all::addAll);
        return all;
    }
}
