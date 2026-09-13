package logisticspipes.client.model.pipe;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;

import org.junit.jupiter.api.Test;

import logisticspipes.client.model.mesh.MeshTransforms;
import logisticspipes.client.model.mesh.ObjMesh;
import logisticspipes.client.model.mesh.ObjModel;
import logisticspipes.client.model.mesh.ObjParser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runs the part assembly against the real {@code pipe_model_moved.obj}.
 *
 * <p>The old {@code LogisticsNewRenderPipe.loadModels()} threw a {@code RuntimeException} whenever a
 * part family came up with the wrong count, and that was the only check anywhere that the group
 * lookups were correct. Those counts are asserted here instead, so a regression in the token
 * indices fails the build rather than the game.
 */
class PipeModelPartsLoaderTest {

    private final Collector collector = new Collector();

    private ObjModel load(String name) throws IOException {
        try (InputStream stream = Objects.requireNonNull(
            getClass().getResourceAsStream("/assets/logisticspipes/models/obj/" + name + ".obj"),
            "missing OBJ resource " + name)) {
            // Same 1/100 down-scale the reload listener applies while parsing.
            return ObjParser.parse(stream).mapMeshes(mesh -> mesh.transform(MeshTransforms.scale(1 / 100.0)));
        }
    }

    private PipeModelParts loadParts() throws IOException {
        return PipeModelPartsLoader.load(load("pipe_model_moved"), load("pipe_model_transport_box"), collector);
    }

    private static final class Collector implements PipeModelPartsLoader.Problems {

        private final List<String> problems = new ArrayList<>();

        @Override
        public void report(String message) {
            problems.add(message);
        }
    }

    @Test
    void everyPartFamilyResolvesWithTheExpectedCount() throws IOException {
        loadParts();

        assertEquals(List.of(), collector.problems, "part lookups disagree with the expected counts");
    }

    @Test
    void sidesAndEdgesArePresentForEveryDirection() throws IOException {
        PipeModelParts parts = loadParts();

        for (Direction dir : Direction.values()) {
            assertEquals(4, parts.sideNormal(dir).size(), "sideNormal " + dir);
            assertEquals(8, parts.sideBC(dir).size(), "sideBC " + dir);
            assertEquals(2, parts.texturePlateInner(dir).size(), "inner plate " + dir);
            assertEquals(2, parts.texturePlateOuter(dir).size(), "outer plate " + dir);
            for (PipeModelParts.SidePlate plate : PipeModelParts.SidePlate.values()) {
                assertEquals(8, parts.sideTexturePlate(dir, plate).size(), "side plate " + dir + " " + plate);
            }
        }
        for (PipeEdge edge : PipeEdge.values()) {
            assertTrue(!parts.edge(edge).isEmpty(), "edge " + edge);
        }
    }

    @Test
    void cornersSupportsSpacersAndMountsAllResolve() throws IOException {
        PipeModelParts parts = loadParts();

        for (PipeCorner corner : PipeCorner.values()) {
            assertEquals(2, parts.cornerM(corner).size(), "corner_M " + corner);
            assertEquals(2, parts.cornerI3(corner).size(), "corner_I3 " + corner);
        }
        for (PipeTurnCorner turnCorner : PipeTurnCorner.values()) {
            assertTrue(!parts.cornerI(turnCorner).isEmpty(), "corner_I " + turnCorner);
            assertTrue(!parts.spacer(turnCorner).isEmpty(), "spacer " + turnCorner);
        }
        for (PipeSupport support : PipeSupport.values()) {
            assertTrue(!parts.support(support).isEmpty(), "support " + support);
        }
        for (PipeMount mount : PipeMount.values()) {
            assertTrue(!parts.mount(mount).isEmpty(), "mount " + mount);
            assertEquals(4, parts.textureConnectorPlate(mount).size(), "connector plate " + mount);
        }
    }

    @Test
    void theThreeTurnsOfACornerMapToDistinctMeshes() throws IOException {
        // Corner_I_<corner>, _1 and _2 are separate geometry; picking the variant by turn is
        // what the original did by reading the character after the group name.
        PipeModelParts parts = loadParts();

        Map<PipeTurn, ObjMesh> byTurn = new HashMap<>();
        for (PipeTurnCorner turnCorner : PipeTurnCorner.values()) {
            if (turnCorner.corner == PipeCorner.UP_NORTH_WEST) {
                byTurn.put(turnCorner.turn, parts.cornerI(turnCorner));
            }
        }
        assertEquals(3, byTurn.size());

        Set<List<Float>> distinctGeometry = new HashSet<>();
        for (ObjMesh mesh : byTurn.values()) {
            List<Float> firstCorners = new ArrayList<>();
            for (int quad = 0; quad < mesh.quadCount(); ++quad) {
                firstCorners.add(mesh.x(quad, 0));
            }
            distinctGeometry.add(firstCorners);
        }
        assertEquals(3, distinctGeometry.size());
    }

    @Test
    void assembledGeometrySitsInsideTheUnitCube() throws IOException {
        // Every part is authored one block along Z and translated back; if that transform
        // were dropped the pipe would render in the neighbouring block.
        PipeModelParts parts = loadParts();

        for (ObjMesh mesh : parts.allMeshes()) {
            if (mesh.isEmpty()) {
                continue;
            }
            AABB bounds = mesh.bounds();
            assertTrue(bounds.minX >= -0.01 && bounds.maxX <= 1.01, "x out of cube: " + bounds);
            assertTrue(bounds.minY >= -0.01 && bounds.maxY <= 1.01, "y out of cube: " + bounds);
            assertTrue(bounds.minZ >= -0.01 && bounds.maxZ <= 1.01, "z out of cube: " + bounds);
        }
    }

    @Test
    void highlightAndTransportBoxAreBuilt() throws IOException {
        PipeModelParts parts = loadParts();

        assertNotNull(parts.highlight());
        assertTrue(!parts.highlight().isEmpty(), "highlight merges the edges and middle corners");
        assertTrue(!parts.innerTransportBox().isEmpty(), "inner transport box");
    }
}
