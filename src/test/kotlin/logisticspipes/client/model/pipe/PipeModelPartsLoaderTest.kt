package logisticspipes.client.model.pipe

import logisticspipes.client.model.mesh.MeshTransforms
import logisticspipes.client.model.mesh.ObjModel
import logisticspipes.client.model.mesh.ObjParser
import net.minecraft.core.Direction
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Runs the part assembly against the real `pipe_model_moved.obj`.
 *
 * The old `LogisticsNewRenderPipe.loadModels()` threw a `RuntimeException` whenever a part
 * family came up with the wrong count, and that was the only check anywhere that the group
 * lookups were correct. Those counts are asserted here instead, so a regression in the token
 * indices fails the build rather than the game.
 */
internal class PipeModelPartsLoaderTest {

    private fun load(name: String): ObjModel {
        val stream = checkNotNull(javaClass.getResourceAsStream("/assets/logisticspipes/models/obj/$name.obj")) {
            "missing OBJ resource $name"
        }
        // Same 1/100 down-scale the reload listener applies while parsing.
        return stream.use { ObjParser.parse(it) }
            .mapMeshes { it.transform(MeshTransforms.scale(1 / 100.0)) }
    }

    private class Collector : PipeModelPartsLoader.Problems {
        val problems = mutableListOf<String>()
        override fun report(message: String) {
            problems += message
        }
    }

    private fun loadParts(): Pair<PipeModelParts, List<String>> {
        val collector = Collector()
        val parts = PipeModelPartsLoader.load(load("pipe_model_moved"), load("pipe_model_transport_box"), collector)
        return parts to collector.problems
    }

    @Test
    fun `every part family resolves with the expected count`() {
        val (_, problems) = loadParts()
        assertEquals(emptyList(), problems, "part lookups disagree with the expected counts")
    }

    @Test
    fun `sides and edges are present for every direction`() {
        val (parts, _) = loadParts()
        for (dir in Direction.values()) {
            assertEquals(4, parts.sideNormal(dir).size, "sideNormal $dir")
            assertEquals(8, parts.sideBC(dir).size, "sideBC $dir")
            assertEquals(2, parts.texturePlateInner(dir).size, "inner plate $dir")
            assertEquals(2, parts.texturePlateOuter(dir).size, "outer plate $dir")
            for (plate in PipeModelParts.SidePlate.values()) {
                assertEquals(8, parts.sideTexturePlate(dir, plate).size, "side plate $dir $plate")
            }
        }
        for (edge in PipeEdge.values()) {
            assertTrue(!parts.edge(edge).isEmpty, "edge $edge")
        }
    }

    @Test
    fun `corners, supports, spacers and mounts all resolve`() {
        val (parts, _) = loadParts()
        for (corner in PipeCorner.values()) {
            assertEquals(2, parts.cornerM(corner).size, "corner_M $corner")
            assertEquals(2, parts.cornerI3(corner).size, "corner_I3 $corner")
        }
        for (turnCorner in PipeTurnCorner.values()) {
            assertTrue(!parts.cornerI(turnCorner).isEmpty, "corner_I $turnCorner")
            assertTrue(!parts.spacer(turnCorner).isEmpty, "spacer $turnCorner")
        }
        for (support in PipeSupport.values()) {
            assertTrue(!parts.support(support).isEmpty, "support $support")
        }
        for (mount in PipeMount.values()) {
            assertTrue(!parts.mount(mount).isEmpty, "mount $mount")
            assertEquals(4, parts.textureConnectorPlate(mount).size, "connector plate $mount")
        }
    }

    @Test
    fun `the three turns of a corner map to distinct meshes`() {
        // Corner_I_<corner>, _1 and _2 are separate geometry; picking the variant by turn is
        // what the original did by reading the character after the group name.
        val (parts, _) = loadParts()
        val byTurn = PipeTurnCorner.values()
            .filter { it.corner == PipeCorner.UP_NORTH_WEST }
            .associate { it.turn to parts.cornerI(it) }
        assertEquals(3, byTurn.size)
        assertEquals(3, byTurn.values.map { mesh -> (0 until mesh.quadCount()).map { mesh.x(it, 0) } }.distinct().size)
    }

    @Test
    fun `assembled geometry sits inside the unit cube`() {
        // Every part is authored one block along Z and translated back; if that transform
        // were dropped the pipe would render in the neighbouring block.
        val (parts, _) = loadParts()
        for (mesh in parts.allMeshes()) {
            if (mesh.isEmpty) continue
            val bounds = mesh.bounds()
            assertTrue(bounds.minX >= -0.01 && bounds.maxX <= 1.01, "x out of cube: $bounds")
            assertTrue(bounds.minY >= -0.01 && bounds.maxY <= 1.01, "y out of cube: $bounds")
            assertTrue(bounds.minZ >= -0.01 && bounds.maxZ <= 1.01, "z out of cube: $bounds")
        }
    }

    @Test
    fun `highlight and transport box are built`() {
        val (parts, _) = loadParts()
        assertNotNull(parts.highlight())
        assertTrue(!parts.highlight().isEmpty, "highlight merges the edges and middle corners")
        assertTrue(!parts.innerTransportBox().isEmpty, "inner transport box")
    }
}
