package logisticspipes.client.model.pipe;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import javax.annotation.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;


import org.joml.Matrix4f;

import logisticspipes.LPConstants;
import logisticspipes.client.model.mesh.MeshBaker;
import logisticspipes.client.model.mesh.MeshTransforms;
import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.client.model.mesh.UvTransform;

/**
 * Produces the baked quads for one pipe configuration.
 *
 * <p>Port of {@code LogisticsNewRenderPipe.fillObjectsToRenderList}, which built a list of
 * {@code RenderEntry} (mesh + operations + texture) that the block entity renderer then
 * walked every frame. Here the same decisions run once per distinct
 * {@link PipeGeometryKey} and produce {@link BakedQuad}s that the chunk mesh owns.</p>
 *
 * <p>Everything the original read from the mutable render state, the pipe and the
 * surrounding world now comes from the key, so this class is pure and can run on the chunk
 * build threads.</p>
 */
public final class PipeQuadBaker {

    private PipeQuadBaker() {
    }

    /**
     * Status overlay UV offsets, as authored into the status sprite.
     */
    private static final float STATUS_U_STEP = 2.5f / 10;
    private static final float STATUS_V_ROW = 37f / 100;
    private static final float STATUS_BC_V_STEP = 23f / 100;
    /**
     * The per-type body texture occupies the middle 12/16 of its sprite.
     */
    private static final UvTransform OUTER_PLATE_UV = UvTransform.scale(12f / 16, 12f / 16);

    /**
     * Directional shading is off, matching what the immediate-mode renderer did — it emitted a
     * flat white colour and a uniform packed light for every vertex.
     *
     * <p>Turning it on banded the joints. Vanilla picks one of four fixed brightness levels per
     * quad from its {@link Direction}, which {@code Direction.getNearest} derives from the
     * normal; the frame's joints have diagonal normals, so neighbouring quads on one visual
     * surface snapped to different axes and came out at different brightness. The double-sided
     * geometry compounds it: every surface exists twice with opposed normals, so which copy
     * survives backface culling — and therefore which shade it gets — depends on a winding the
     * OBJ does not apply consistently.</p>
     */
    private static final boolean SHADE = false;

    public static List<BakedQuad> bake(PipeModelParts parts, PipeSprites sprites, PipeGeometryKey key) {
        List<BakedQuad> quads = new ArrayList<>();
        if (parts.isEmpty() || !sprites.isComplete()) {
            return quads;
        }

        List<PipeEdge> edgesToRender = new ArrayList<>(Arrays.asList(PipeEdge.values()));
        List<PipeMount> mountCandidates = new ArrayList<>(Arrays.asList(PipeMount.values()));
        Map<PipeCorner, Integer> connectionAtCorner = new EnumMap<>(PipeCorner.class);

        bakeSides(quads, parts, sprites, key, edgesToRender, mountCandidates, connectionAtCorner);
        bakeCorners(quads, parts, sprites, key, connectionAtCorner);
        bakeRemainingEdges(quads, parts, sprites, edgesToRender);
        bakeSupports(quads, parts, sprites, key);
        bakeMounts(quads, parts, sprites, key, mountCandidates);
        bakeOuterPlates(quads, parts, sprites, key);
        bakeFluidPlates(quads, parts, sprites, key);
        return quads;
    }

    // ─── Sides ──────────────────────────────────────────────────────────────

    private static void bakeSides(List<BakedQuad> quads, PipeModelParts parts, PipeSprites sprites,
        PipeGeometryKey key, List<PipeEdge> edgesToRender, List<PipeMount> mountCandidates,
        Map<PipeCorner, Integer> connectionAtCorner) {
        for (Direction dir : Direction.values()) {
            if (!key.isConnected(dir) && !key.hasSpecialPipeEndAt(dir)) {
                continue;
            }

            if (key.isTDConnected(dir) || key.isBCConnected(dir)) {
                Textured textured = bcSideTexture(sprites, key, dir);
                for (ObjMesh mesh : parts.sideBC(dir)) {
                    MeshBaker.bake(quads, mesh, textured.sprite, textured.uv, MeshBaker.WHITE, SHADE);
                }
            } else if (!key.hasSpecialPipeEndAt(dir)) {
                Textured textured = normalSideTexture(sprites, key, dir);
                for (ObjMesh mesh : parts.sideNormal(dir)) {
                    ObjMesh stretched = stretchToNeighbour(mesh, dir, key.neighbourBound(dir));
                    MeshBaker.bake(quads, stretched, textured.sprite, textured.uv, MeshBaker.WHITE, SHADE);
                }
            }

            for (PipeEdge edge : PipeEdge.values()) {
                if (edge.part1 != dir && edge.part2 != dir) {
                    continue;
                }
                edgesToRender.remove(edge);
                mountCandidates.removeIf(mount ->
                    (mount.dir == edge.part1 && mount.side == edge.part2)
                        || (mount.dir == edge.part2 && mount.side == edge.part1));
            }

            for (PipeCorner corner : PipeCorner.values()) {
                if (corner.ew.dir == dir || corner.ns.dir == dir || corner.ud.dir == dir) {
                    connectionAtCorner.merge(corner, 1, Integer::sum);
                }
            }
        }
    }

    /**
     * A sprite together with the UV shift that selects the right patch of it.
     */
    private record Textured(@Nullable TextureAtlasSprite sprite, UvTransform uv) {}

    private static Textured bcSideTexture(PipeSprites sprites, PipeGeometryKey key, Direction dir) {
        if (!key.isRouted()) {
            return new Textured(sprites.basicPipe(), UvTransform.IDENTITY);
        }
        if (!key.isRoutedInDir(dir)) {
            return new Textured(sprites.statusBC(), UvTransform.translate(0, -STATUS_BC_V_STEP));
        }
        if (key.isSubPowerInDir(dir)) {
            return new Textured(sprites.statusBC(), UvTransform.translate(0, STATUS_BC_V_STEP));
        }
        return new Textured(sprites.statusBC(), UvTransform.IDENTITY);
    }

    private static Textured normalSideTexture(PipeSprites sprites, PipeGeometryKey key, Direction dir) {
        if (!key.isRouted()) {
            return new Textured(sprites.basicPipe(), UvTransform.IDENTITY);
        }
        if (key.isRoutedInDir(dir)) {
            return key.isSubPowerInDir(dir)
                ? new Textured(sprites.status(), UvTransform.translate(-STATUS_U_STEP, 0))
                : new Textured(sprites.status(), UvTransform.IDENTITY);
        }
        boolean pointed = key.pointedOrientation() == dir;
        if (key.hasPowerUpgrade()) {
            return pointed
                ? new Textured(sprites.status(), UvTransform.translate(STATUS_U_STEP, 0))
                : new Textured(sprites.status(), UvTransform.translate(-STATUS_U_STEP, STATUS_V_ROW));
        }
        return pointed
            ? new Textured(sprites.status(), UvTransform.translate(STATUS_U_STEP, STATUS_V_ROW))
            : new Textured(sprites.status(), UvTransform.translate(0, STATUS_V_ROW));
    }

    /**
     * Stretches a pipe end along its axis so it meets a neighbouring block whose collision
     * box does not reach the block boundary — a slab, a fence post, a chest lid.
     *
     * <p>The original cached these in a static {@code HashMap} keyed by (mesh, bound). The
     * cache is gone: the whole quad list is cached by {@link PipeGeometryKey} one level up,
     * which subsumes it, and a plain {@code HashMap} could not be read from the chunk build
     * threads anyway.</p>
     */
    private static ObjMesh stretchToNeighbour(ObjMesh mesh, Direction dir, double bound) {
        AABB bounds = mesh.bounds();
        double minX = bounds.minX;
        double minY = bounds.minY;
        double minZ = bounds.minZ;

        double stretch;
        double shift = 0;
        if (dir.ordinal() % 2 == 1) {
            stretch = 1 + (bound / LPConstants.PIPE_MIN_POS);
        } else {
            shift = 1 - bound;
            stretch = 1 + (shift / LPConstants.PIPE_MIN_POS);
        }
        double sx = dir.getStepX() != 0 ? stretch : 1;
        double sy = dir.getStepY() != 0 ? stretch : 1;
        double sz = dir.getStepZ() != 0 ? stretch : 1;

        // Scale about the mesh's own min corner, then push negative faces back out.
        Matrix4f transform = new Matrix4f(MeshTransforms.translation(minX, minY, minZ))
            .mul(MeshTransforms.translation(
                dir.getStepX() * shift,
                dir.getStepY() * shift,
                dir.getStepZ() * shift))
            .mul(MeshTransforms.scale(sx, sy, sz))
            .mul(MeshTransforms.translation(-minX, -minY, -minZ));
        return mesh.transform(transform);
    }

    // ─── Corners ────────────────────────────────────────────────────────────

    private static void bakeCorners(List<BakedQuad> quads, PipeModelParts parts, PipeSprites sprites,
        PipeGeometryKey key, Map<PipeCorner, Integer> connectionAtCorner) {
        int connectionCount = key.connectionCount();
        TextureAtlasSprite cornerSprite = sprites.basicPipe();
        if (!key.hasPower() && key.isRouted()) {
            cornerSprite = sprites.inactive();
        } else if (!key.isRouted() && connectionCount > 2) {
            cornerSprite = sprites.inactive();
        }
        // inactive may be absent if its sprite failed to stitch; fall back to the body.
        final TextureAtlasSprite sprite = cornerSprite == null ? sprites.basicPipe() : cornerSprite;

        for (PipeCorner corner : PipeCorner.values()) {
            int count = connectionAtCorner.getOrDefault(corner, 0);
            switch (count) {
                case 0 -> bakeAll(quads, parts.cornerM(corner), sprite);
                case 1 -> firstTurn(corner, turn ->
                    key.isConnected(turn.getPointer()) || key.hasSpecialPipeEndAt(turn.getPointer()))
                    .ifPresent(turn -> MeshBaker.bake(quads, parts.spacer(turn), sprite,
                        UvTransform.IDENTITY, MeshBaker.WHITE, SHADE));
                case 2 -> firstTurn(corner, turn ->
                    !key.isConnected(turn.getPointer()) || key.hasSpecialPipeEndAt(turn.getPointer()))
                    .ifPresent(turn -> MeshBaker.bake(quads, parts.cornerI(turn), sprite,
                        UvTransform.IDENTITY, MeshBaker.WHITE, SHADE));
                case 3 -> bakeAll(quads, parts.cornerI3(corner), sprite);
                default -> { /* a corner touches at most three faces */ }
            }
        }
    }

    private static Optional<PipeTurnCorner> firstTurn(PipeCorner corner,
        Predicate<PipeTurnCorner> accept) {
        for (PipeTurnCorner turn : PipeTurnCorner.values()) {
            if (turn.corner != corner) {
                continue;
            }
            if (accept.test(turn)) {
                return Optional.of(turn);
            }
        }
        return Optional.empty();
    }

    private static void bakeRemainingEdges(List<BakedQuad> quads, PipeModelParts parts,
        PipeSprites sprites, List<PipeEdge> edgesToRender) {
        for (PipeEdge edge : edgesToRender) {
            MeshBaker.bake(quads, parts.edge(edge), sprites.basicPipe(), UvTransform.IDENTITY, MeshBaker.WHITE, SHADE);
        }
    }

    // ─── Supports ───────────────────────────────────────────────────────────

    /**
     * A pipe that runs straight through with no other connection gets support feet on the
     * four faces perpendicular to its axis.
     */
    private static void bakeSupports(List<BakedQuad> quads, PipeModelParts parts,
        PipeSprites sprites, PipeGeometryKey key) {
        for (int i = 0; i < 6; i += 2) {
            Direction dir = Direction.from3DDataValue(i);
            if (!key.isConnected(dir) || !key.isConnected(dir.getOpposite())) {
                continue;
            }

            boolean otherConnection = false;
            for (Direction other : Direction.values()) {
                if (other == dir || other == dir.getOpposite()) {
                    continue;
                }
                if (key.isConnected(other)) {
                    otherConnection = true;
                    break;
                }
            }
            if (otherConnection) {
                continue;
            }

            List<PipeSupport> supports = switch (dir) {
                case DOWN -> List.of(PipeSupport.EAST_SIDE, PipeSupport.WEST_SIDE,
                    PipeSupport.NORTH_SIDE, PipeSupport.SOUTH_SIDE);
                case NORTH -> List.of(PipeSupport.EAST_UP, PipeSupport.WEST_UP,
                    PipeSupport.UP_SIDE, PipeSupport.DOWN_SIDE);
                case WEST -> List.of(PipeSupport.UP_UP, PipeSupport.DOWN_UP,
                    PipeSupport.NORTH_UP, PipeSupport.SOUTH_UP);
                default -> List.of();
            };
            for (PipeSupport support : supports) {
                MeshBaker.bake(quads, parts.support(support), sprites.basicPipe(),
                    UvTransform.IDENTITY, MeshBaker.WHITE, SHADE);
            }
        }
    }

    // ─── Mounts ─────────────────────────────────────────────────────────────

    private static void bakeMounts(List<BakedQuad> quads, PipeModelParts parts, PipeSprites sprites,
        PipeGeometryKey key, List<PipeMount> candidates) {
        // A mount can only sit against a face that is solid and unconnected. With no world
        // (the item form) no side is solid, so every candidate drops — the original spelled
        // this out as mountCanidates.clear().
        for (Direction dir : Direction.values()) {
            if (!key.isSolidSide(dir)) {
                candidates.removeIf(mount -> mount.dir == dir);
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        if (key.isSolidSide(Direction.DOWN)) {
            keepOpposingPairOn(candidates, Direction.DOWN);
        } else if (key.isSolidSide(Direction.UP)) {
            keepOpposingPairOn(candidates, Direction.UP);
        } else {
            candidates.removeIf(mount -> mount.dir == Direction.DOWN);
            candidates.removeIf(mount -> mount.dir == Direction.UP);
            if (candidates.size() > 2) {
                removeIfHasOpposingSide(candidates);
            }
            if (candidates.size() > 2) {
                removeIfHasConnectedSide(candidates);
            }
            if (candidates.size() > 2) {
                keepOpposingPairOn(candidates, candidates.get(0).dir);
            }
        }

        for (PipeMount mount : candidates) {
            MeshBaker.bake(quads, parts.mount(mount), sprites.basicPipe(),
                UvTransform.IDENTITY, MeshBaker.WHITE, SHADE);
        }
    }

    /**
     * Was {@code findOponentOnSameSide}: keep only mounts on {@code dir}, preferring a pair
     * of opposing sides when more than two survive.
     */
    private static void keepOpposingPairOn(List<PipeMount> candidates, Direction dir) {
        boolean[] sides = new boolean[6];
        Iterator<PipeMount> iter = candidates.iterator();
        while (iter.hasNext()) {
            PipeMount mount = iter.next();
            if (mount.dir != dir) {
                iter.remove();
            } else {
                sides[mount.side.ordinal()] = true;
            }
        }
        if (candidates.size() <= 2) {
            return;
        }

        List<Direction> keep;
        if (sides[Direction.NORTH.ordinal()] && sides[Direction.SOUTH.ordinal()]) {
            keep = List.of(Direction.NORTH, Direction.SOUTH);
        } else if (sides[Direction.WEST.ordinal()] && sides[Direction.EAST.ordinal()]) {
            keep = List.of(Direction.EAST, Direction.WEST);
        } else if (sides[Direction.DOWN.ordinal()] && sides[Direction.UP.ordinal()]) {
            keep = List.of(Direction.UP, Direction.DOWN);
        } else {
            keep = List.of();
        }
        candidates.removeIf(mount -> !keep.contains(mount.side));
    }

    private static void reduceToOnePerSide(List<PipeMount> candidates, Direction dir) {
        boolean found = false;
        Iterator<PipeMount> iter = candidates.iterator();
        while (iter.hasNext()) {
            PipeMount mount = iter.next();
            if (mount.dir != dir) {
                continue;
            }
            if (found) {
                iter.remove();
            } else {
                found = true;
            }
        }
    }

    private static void reduceToOnePerSide(List<PipeMount> candidates, Direction dir, Direction preferred) {
        boolean hasPreferred = candidates.stream()
            .anyMatch(mount -> mount.dir == dir && mount.side == preferred);
        if (!hasPreferred) {
            reduceToOnePerSide(candidates, dir);
        } else {
            candidates.removeIf(mount -> mount.dir == dir && mount.side != preferred);
        }
    }

    private static void removeIfHasOpposingSide(List<PipeMount> candidates) {
        boolean[] sides = new boolean[6];
        for (PipeMount mount : candidates) {
            sides[mount.dir.ordinal()] = true;
        }
        if (sides[Direction.NORTH.ordinal()] && sides[Direction.SOUTH.ordinal()]) {
            candidates.removeIf(mount -> mount.dir == Direction.EAST || mount.dir == Direction.WEST);
            reduceToOnePerSide(candidates, Direction.NORTH);
            reduceToOnePerSide(candidates, Direction.SOUTH);
        } else if (sides[Direction.WEST.ordinal()] && sides[Direction.EAST.ordinal()]) {
            candidates.removeIf(mount -> mount.dir == Direction.NORTH || mount.dir == Direction.SOUTH);
            reduceToOnePerSide(candidates, Direction.EAST);
            reduceToOnePerSide(candidates, Direction.WEST);
        }
    }

    private static void removeIfHasConnectedSide(List<PipeMount> candidates) {
        boolean[] sides = new boolean[6];
        for (PipeMount mount : candidates) {
            sides[mount.dir.ordinal()] = true;
        }
        for (int i = 2; i < 6; i++) {
            Direction dir = Direction.from3DDataValue(i);
            Direction rotated = dir.getClockWise();
            if (sides[dir.ordinal()] && sides[rotated.ordinal()]) {
                reduceToOnePerSide(candidates, dir, dir.getCounterClockWise());
                reduceToOnePerSide(candidates, rotated, rotated.getClockWise());
            }
        }
    }

    // ─── Plates ─────────────────────────────────────────────────────────────

    /**
     * The per-pipe-type face texture, drawn on every unconnected side.
     */
    private static void bakeOuterPlates(List<BakedQuad> quads, PipeModelParts parts,
        PipeSprites sprites, PipeGeometryKey key) {
        TextureAtlasSprite icon = sprites.icon(key.textureIndex());
        if (icon == null) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (key.isConnected(dir)) {
                continue;
            }
            for (ObjMesh mesh : parts.texturePlateOuter(dir)) {
                MeshBaker.bake(quads, mesh, icon, OUTER_PLATE_UV, MeshBaker.WHITE, SHADE);
            }
        }
    }

    private static void bakeFluidPlates(List<BakedQuad> quads, PipeModelParts parts,
        PipeSprites sprites, PipeGeometryKey key) {
        if (!key.isFluid()) {
            return;
        }
        for (Direction dir : Direction.values()) {
            if (!key.isConnected(dir)) {
                bakeAll(quads, parts.texturePlateInner(dir), sprites.glassCenter());
            } else if (!key.isRoutedInDir(dir)) {
                bakeAll(quads, parts.sideTexturePlate(dir, PipeModelParts.SidePlate.LARGE_INNER),
                    sprites.basicPipe());
            }
        }
    }

    private static void bakeAll(List<BakedQuad> quads, List<ObjMesh> meshes, @Nullable TextureAtlasSprite sprite) {
        for (ObjMesh mesh : meshes) {
            MeshBaker.bake(quads, mesh, sprite, UvTransform.IDENTITY, MeshBaker.WHITE, SHADE);
        }
    }
}
