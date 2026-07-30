package logisticspipes.client.model.mesh

import org.joml.Matrix4f
import kotlin.math.PI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the mesh engine that replaces the CodeChickenLib-derived `LPModel3DImpl` /
 * `LPQuadData` pair. The old code had no tests at all — the only regression check was the
 * part-count assertions in `LogisticsNewRenderPipe.loadModels()`.
 */
internal class ObjMeshTest {

    /** A unit quad in the XY plane at z=0, wound counter-clockwise, with 0..1 UVs. */
    private fun unitQuad(): ObjMesh = ObjMeshBuilder(1).addQuad(
        floatArrayOf(
            0f, 0f, 0f,
            1f, 0f, 0f,
            1f, 1f, 0f,
            0f, 1f, 0f,
        ),
        floatArrayOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f),
        floatArrayOf(
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
            0f, 0f, 1f,
        ),
    ).build()

    private fun parse(obj: String): ObjModel = ObjParser.parse(obj.trimIndent().byteInputStream())

    // ─── ObjParser ──────────────────────────────────────────────────────────

    @Test
    fun `parses a quad face with positions, uvs and normals`() {
        val model = parse(
            """
            v 0 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            vt 0 0
            vt 1 0
            vt 1 1
            vt 0 1
            vn 0 0 1
            g Cube Part_A
            f 1/1/1 2/2/1 3/3/1 4/4/1
            """,
        )
        val mesh = model.group("Cube Part_A")!!
        assertEquals(1, mesh.quadCount())
        assertEquals(0f, mesh.x(0, 0))
        assertEquals(1f, mesh.x(0, 1))
        assertEquals(1f, mesh.y(0, 2))
        assertEquals(1f, mesh.nz(0, 0))
    }

    @Test
    fun `flips the V axis because OBJ is bottom-up and Minecraft is top-down`() {
        val model = parse(
            """
            v 0 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            vt 0.25 0.75
            g G
            f 1/1 2/1 3/1 4/1
            """,
        )
        val mesh = model.group("G")!!
        assertEquals(0.25f, mesh.u(0, 0))
        assertEquals(0.25f, mesh.v(0, 0), "1.0 - 0.75")
    }

    @Test
    fun `expands a triangle into a degenerate quad`() {
        val model = parse(
            """
            v 0 0 0
            v 1 0 0
            v 0 1 0
            g Tri
            f 1 2 3
            """,
        )
        val mesh = model.group("Tri")!!
        assertEquals(1, mesh.quadCount())
        assertEquals(mesh.x(0, 2), mesh.x(0, 3))
        assertEquals(mesh.y(0, 2), mesh.y(0, 3))
    }

    // ─── ObjModel indices ───────────────────────────────────────────────────

    @Test
    fun `indexes every token of a multi-name group line`() {
        val model = parse(
            """
            v 0 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            g Mesh332 Side_Texture_Plate_Side3 Texture_Side_N
            f 1 2 3 4
            """,
        )
        assertEquals(1, model.byName("Texture_Side_N").size, "token in last position")
        assertEquals(1, model.byName("Mesh332").size, "token in first position")
        assertEquals(1, model.byName("Side_Texture_Plate_Side3").size, "token in the middle")
        assertEquals(0, model.byName("Texture_Side").size, "must not match a partial token")
    }

    @Test
    fun `base-name lookup returns numbered variants alongside the unsuffixed group`() {
        val model = parse(
            """
            v 0 0 0
            v 1 0 0
            v 1 1 0
            v 0 1 0
            g A Texture_Side_N
            f 1 2 3 4
            g B Texture_Side_N1
            f 1 2 3 4
            g C Texture_Side_N2
            f 1 2 3 4
            g D Texture_Side_S
            f 1 2 3 4
            """,
        )
        val variants = model.byNamePrefix("Texture_Side_N").map { it.variant }.sorted()
        assertEquals(listOf(0, 1, 2), variants)
        assertEquals(1, model.byName("Texture_Side_N").size, "exact lookup must not pick up variants")
    }

    // ─── Transforms ─────────────────────────────────────────────────────────

    @Test
    fun `translation moves every vertex and leaves normals alone`() {
        val moved = unitQuad().transform(MeshTransforms.translation(2.0, 3.0, 4.0))
        assertEquals(2f, moved.x(0, 0))
        assertEquals(3f, moved.y(0, 0))
        assertEquals(4f, moved.z(0, 0))
        assertEquals(1f, moved.nz(0, 0))
    }

    @Test
    fun `uniform scale multiplies positions`() {
        val scaled = unitQuad().transform(MeshTransforms.scale(1 / 100.0))
        assertEquals(0.01f, scaled.x(0, 1), "the pipe models are authored at 100x")
    }

    @Test
    fun `sideOrientation turns the way computeRotated's translations expect`() {
        val rotated = unitQuad().transform(MeshTransforms.sideOrientation(1))
        // (1, 0, 0) maps to (0, 0, 1). That handedness is what puts a unit-cube model on
        // x in [-1, 0], which computeRotated's translate(1, 0, 0) brings back to [0, 1];
        // the opposite sense would strand it at x in [1, 2].
        assertEquals(0f, rotated.x(0, 1), 1e-6f)
        assertEquals(1f, rotated.z(0, 1), 1e-6f)
    }

    @Test
    fun `sideOrientation and rotation have opposite handedness, as in the original`() {
        // sideOrientation comes from CCL's precomputed quarter-turn table and turns the
        // other way from the axis-angle Rotation constructor. Both senses are load-bearing.
        val bySide = unitQuad().transform(MeshTransforms.sideOrientation(1))
        val byAxis = unitQuad().transform(MeshTransforms.rotation(PI / 2, 0.0, 1.0, 0.0))
        assertEquals(1f, bySide.z(0, 1), 1e-6f)
        assertEquals(-1f, byAxis.z(0, 1), 1e-6f)
    }

    @Test
    fun `rotation takes radians and turns about the origin`() {
        // Guards against the two defects in CCLProxy.getRotation, which ran toRadians over
        // an already-radian argument and rotated about (0.5, 0.5, 0.5).
        val rotated = unitQuad().transform(MeshTransforms.rotation(PI / 2, 0.0, 1.0, 0.0))
        assertEquals(0f, rotated.x(0, 1), 1e-6f)
        assertEquals(-1f, rotated.z(0, 1), 1e-6f)
    }

    @Test
    fun `matrices compose so a chain of operations is a single pass`() {
        val chained = Matrix4f(MeshTransforms.translation(0.5, 0.5, 0.5))
            .mul(MeshTransforms.scale(2.0))
            .mul(MeshTransforms.translation(-0.5, -0.5, -0.5))
        val result = unitQuad().transform(chained)
        // Scaling the unit quad by 2 about its own centre puts corner (0,0) at (-0.5,-0.5).
        assertEquals(-0.5f, result.x(0, 0), 1e-6f)
        assertEquals(1.5f, result.x(0, 1), 1e-6f)
    }

    // ─── UV ─────────────────────────────────────────────────────────────────

    @Test
    fun `uv transforms compose in application order`() {
        val combined = UvTransform.scale(0.5f, 0.5f).andThen(UvTransform.translate(0.25f, 0f))
        assertEquals(0.25f, combined.applyU(0f), 1e-6f)
        assertEquals(0.75f, combined.applyU(1f), 1e-6f)
        assertEquals(0.5f, combined.applyV(1f), 1e-6f)
    }

    @Test
    fun `uvTransform leaves geometry untouched`() {
        val mesh = unitQuad().uvTransform(UvTransform.scale(1f, 0.742f))
        assertEquals(0.742f, mesh.v(0, 2), 1e-6f)
        assertEquals(1f, mesh.x(0, 1), "positions must not move")
    }

    // ─── doubleSided / normals / bounds / merge ─────────────────────────────

    @Test
    fun `doubleSided duplicates every quad with reversed winding and negated normals`() {
        val mesh = unitQuad().doubleSided()
        assertEquals(2, mesh.quadCount())
        // The back face's vertex 0 is the front face's vertex 3.
        assertEquals(unitQuad().x(0, 3), mesh.x(1, 0))
        assertEquals(unitQuad().y(0, 3), mesh.y(1, 0))
        assertEquals(-1f, mesh.nz(1, 0))
        assertEquals(unitQuad().u(0, 3), mesh.u(1, 0), "UVs follow the reordered vertices")
    }

    @Test
    fun `withComputedNormals derives the face normal from the winding`() {
        val flat = ObjMeshBuilder(1).addQuad(
            floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 1f, 1f, 0f, 0f, 1f, 0f),
            FloatArray(8),
            FloatArray(12),
        ).build().withComputedNormals()
        assertEquals(0f, flat.nx(0, 0), 1e-6f)
        assertEquals(0f, flat.ny(0, 0), 1e-6f)
        assertEquals(1f, flat.nz(0, 0), 1e-6f)
    }

    @Test
    fun `withComputedNormals falls back for degenerate triangle quads`() {
        // [v0, v0, v1, v2] is what a triangle looks like after doubleSided reverses it;
        // (v1-v0) x (v2-v0) is zero there, so the fallback must kick in.
        val degenerate = ObjMeshBuilder(1).addQuad(
            floatArrayOf(0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f),
            FloatArray(8),
            FloatArray(12),
        ).build().withComputedNormals()
        assertEquals(1f, degenerate.nz(0, 0), 1e-6f)
    }

    @Test
    fun `bounds spans every vertex`() {
        val bounds = unitQuad().transform(MeshTransforms.translation(1.0, 0.0, 0.0)).bounds()
        assertEquals(1.0, bounds.minX, 1e-6)
        assertEquals(2.0, bounds.maxX, 1e-6)
        assertEquals(0.0, bounds.minZ, 1e-6)
    }

    @Test
    fun `merge concatenates quads`() {
        val merged = ObjMesh.merge(unitQuad(), unitQuad().transform(MeshTransforms.translation(5.0, 0.0, 0.0)))
        assertEquals(2, merged.quadCount())
        assertEquals(5f, merged.x(1, 0))
        assertTrue(ObjMesh.merge(ObjMesh.empty()).isEmpty)
    }
}
