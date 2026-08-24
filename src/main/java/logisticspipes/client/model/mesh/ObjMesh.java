package logisticspipes.client.model.mesh;

import java.util.Collection;
import java.util.List;

import net.minecraft.world.phys.AABB;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

/**
 * An immutable quad mesh: flat float arrays, four vertices per quad, each carrying a
 * position, a UV and a normal.
 *
 * <p>Replaces the CodeChickenLib-derived {@code LPModel3DImpl} + {@code LPQuadData} pair.
 * The differences that matter:</p>
 * <ul>
 *   <li><b>Immutable.</b> Every operation returns a new mesh, so a mesh can be shared
 *       across threads and cached without defensive copying. The old code mutated
 *       {@code LPQuadData} in place on copies and relied on callers to get that right.</li>
 *   <li><b>Transform-agnostic.</b> Positional operations are plain {@link Matrix4f}
 *       instead of the {@code I3DOperation} tagged union, so translation, scale and
 *       rotation compose by matrix multiplication rather than by sequential passes over
 *       the vertex data.</li>
 *   <li><b>No render coupling.</b> This class knows nothing about {@code VertexConsumer},
 *       sprites or render state, so it stays server-safe and can be baked off-thread.
 *       Emission lives in {@code logisticspipes.client.model.render.MeshRenderer}.</li>
 * </ul>
 *
 * <p>Triangles are stored as degenerate quads (last vertex duplicated), matching what
 * {@link ObjParser} produces; {@link #withComputedNormals()} handles that case.</p>
 */
public final class ObjMesh {

    public static final int VERTICES_PER_QUAD = 4;

    private static final ObjMesh EMPTY = new ObjMesh(new float[0], new float[0], new float[0], 0);

    /**
     * 3 floats per vertex, 12 per quad.
     */
    private final float[] pos;
    /**
     * 2 floats per vertex, 8 per quad.
     */
    private final float[] uv;
    /**
     * 3 floats per vertex, 12 per quad.
     */
    private final float[] normal;
    private final int quadCount;

    /**
     * Lazily computed; benign race, the value is deterministic.
     */
    private AABB cachedBounds;

    /**
     * Takes ownership of the arrays — callers must not retain a reference. Use
     * {@link ObjMeshBuilder} rather than calling this directly.
     */
    ObjMesh(float[] pos, float[] uv, float[] normal, int quadCount) {
        this.pos = pos;
        this.uv = uv;
        this.normal = normal;
        this.quadCount = quadCount;
    }

    public static ObjMesh empty() {
        return EMPTY;
    }

    public int quadCount() {
        return quadCount;
    }

    public boolean isEmpty() {
        return quadCount == 0;
    }

    // ─── Per-vertex accessors (quad in [0, quadCount), vertex in [0, 4)) ─────

    public float x(int quad, int vertex) {
        return pos[(quad * 4 + vertex) * 3];
    }

    public float y(int quad, int vertex) {
        return pos[(quad * 4 + vertex) * 3 + 1];
    }

    public float z(int quad, int vertex) {
        return pos[(quad * 4 + vertex) * 3 + 2];
    }

    public float u(int quad, int vertex) {
        return uv[(quad * 4 + vertex) * 2];
    }

    public float v(int quad, int vertex) {
        return uv[(quad * 4 + vertex) * 2 + 1];
    }

    public float nx(int quad, int vertex) {
        return normal[(quad * 4 + vertex) * 3];
    }

    public float ny(int quad, int vertex) {
        return normal[(quad * 4 + vertex) * 3 + 1];
    }

    public float nz(int quad, int vertex) {
        return normal[(quad * 4 + vertex) * 3 + 2];
    }

    // ─── Transforms ─────────────────────────────────────────────────────────

    /**
     * Applies {@code matrix} to every position and its normal matrix to every normal.
     * Replaces the whole {@code LPScale} / {@code LPTranslation} / {@code LPRotation}
     * family: build the matrix with JOML and pass it here.
     */
    public ObjMesh transform(Matrix4f matrix) {
        if (quadCount == 0) {
            return this;
        }
        float[] newPos = new float[pos.length];
        float[] newNormal = new float[normal.length];
        Matrix3f normalMatrix = matrix.normal(new Matrix3f());

        for (int i = 0; i < pos.length; i += 3) {
            float px = pos[i], py = pos[i + 1], pz = pos[i + 2];
            newPos[i] = matrix.m00() * px + matrix.m10() * py + matrix.m20() * pz + matrix.m30();
            newPos[i + 1] = matrix.m01() * px + matrix.m11() * py + matrix.m21() * pz + matrix.m31();
            newPos[i + 2] = matrix.m02() * px + matrix.m12() * py + matrix.m22() * pz + matrix.m32();

            float nx = normal[i], ny = normal[i + 1], nz = normal[i + 2];
            float tx = normalMatrix.m00() * nx + normalMatrix.m10() * ny + normalMatrix.m20() * nz;
            float ty = normalMatrix.m01() * nx + normalMatrix.m11() * ny + normalMatrix.m21() * nz;
            float tz = normalMatrix.m02() * nx + normalMatrix.m12() * ny + normalMatrix.m22() * nz;
            float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
            if (len > 1e-9f) {
                tx /= len;
                ty /= len;
                tz /= len;
            }
            newNormal[i] = tx;
            newNormal[i + 1] = ty;
            newNormal[i + 2] = tz;
        }
        return new ObjMesh(newPos, uv.clone(), newNormal, quadCount);
    }

    /**
     * Applies a UV-space transform to every vertex, leaving geometry untouched.
     */
    public ObjMesh uvTransform(UvTransform transform) {
        if (quadCount == 0 || transform.isIdentity()) {
            return this;
        }
        float[] newUv = new float[uv.length];
        for (int i = 0; i < uv.length; i += 2) {
            newUv[i] = transform.applyU(uv[i]);
            newUv[i + 1] = transform.applyV(uv[i + 1]);
        }
        return new ObjMesh(pos.clone(), newUv, normal.clone(), quadCount);
    }

    /**
     * Returns a mesh containing every quad plus a winding-reversed, normal-negated copy,
     * so the geometry is visible from both sides. Replaces {@code backfacedCopy()} and
     * {@code twoFacedCopy()}, which were already identical in the previous implementation.
     */
    public ObjMesh doubleSided() {
        if (quadCount == 0) {
            return this;
        }
        float[] newPos = new float[pos.length * 2];
        float[] newUv = new float[uv.length * 2];
        float[] newNormal = new float[normal.length * 2];

        System.arraycopy(pos, 0, newPos, 0, pos.length);
        System.arraycopy(uv, 0, newUv, 0, uv.length);
        System.arraycopy(normal, 0, newNormal, 0, normal.length);

        for (int q = 0; q < quadCount; q++) {
            int dstQuad = quadCount + q;
            for (int vert = 0; vert < 4; vert++) {
                int src = q * 4 + (3 - vert);
                int dst = dstQuad * 4 + vert;
                newPos[dst * 3] = pos[src * 3];
                newPos[dst * 3 + 1] = pos[src * 3 + 1];
                newPos[dst * 3 + 2] = pos[src * 3 + 2];
                newUv[dst * 2] = uv[src * 2];
                newUv[dst * 2 + 1] = uv[src * 2 + 1];
                newNormal[dst * 3] = -normal[src * 3];
                newNormal[dst * 3 + 1] = -normal[src * 3 + 1];
                newNormal[dst * 3 + 2] = -normal[src * 3 + 2];
            }
        }
        return new ObjMesh(newPos, newUv, newNormal, quadCount * 2);
    }

    /**
     * Returns a mesh whose normals are the per-quad face normals, replacing whatever the
     * OBJ file declared. Immutable counterpart of the old {@code computeNormals()}.
     */
    public ObjMesh withComputedNormals() {
        if (quadCount == 0) {
            return this;
        }
        float[] newNormal = new float[normal.length];
        for (int q = 0; q < quadCount; q++) {
            int base = q * 4;
            double ax = x(q, 1) - x(q, 0), ay = y(q, 1) - y(q, 0), az = z(q, 1) - z(q, 0);
            double bx = x(q, 2) - x(q, 0), by = y(q, 2) - y(q, 0), bz = z(q, 2) - z(q, 0);
            double nx = ay * bz - az * by;
            double ny = az * bx - ax * bz;
            double nz = ax * by - ay * bx;
            double mag = Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (mag <= 1e-9) {
                // Degenerate: v[0] == v[1], which is how a triangle looks after doubleSided()
                // reverses the winding of a [v0, v1, v2, v2] quad. Fall back to
                // (v[2] - v[0]) x (v[3] - v[0]) to recover the opposite-facing normal.
                ax = x(q, 2) - x(q, 0);
                ay = y(q, 2) - y(q, 0);
                az = z(q, 2) - z(q, 0);
                bx = x(q, 3) - x(q, 0);
                by = y(q, 3) - y(q, 0);
                bz = z(q, 3) - z(q, 0);
                nx = ay * bz - az * by;
                ny = az * bx - ax * bz;
                nz = ax * by - ay * bx;
                mag = Math.sqrt(nx * nx + ny * ny + nz * nz);
            }
            if (mag > 1e-9) {
                nx /= mag;
                ny /= mag;
                nz /= mag;
            }
            for (int vert = 0; vert < 4; vert++) {
                int idx = (base + vert) * 3;
                newNormal[idx] = (float) nx;
                newNormal[idx + 1] = (float) ny;
                newNormal[idx + 2] = (float) nz;
            }
        }
        return new ObjMesh(pos.clone(), uv.clone(), newNormal, quadCount);
    }

    /**
     * Returns a mesh where every vertex carries the same normal, so that anything shading from
     * the normal shades the whole mesh uniformly.
     *
     * <p>This is the immediate-mode counterpart of baking chunk quads with {@code shade = false}:
     * both opt out of letting the geometry decide the brightness. {@link #withComputedNormals()}
     * derives the normal from each quad's winding order, and the pipe OBJs do not wind their faces
     * consistently, so neighbouring faces of one surface can end up with opposed normals and be lit
     * as if they faced opposite ways.</p>
     */
    public ObjMesh withUniformNormal(float nx, float ny, float nz) {
        if (quadCount == 0) {
            return this;
        }
        float[] newNormal = new float[normal.length];
        for (int vertex = 0; vertex < quadCount * VERTICES_PER_QUAD; vertex++) {
            newNormal[vertex * 3] = nx;
            newNormal[vertex * 3 + 1] = ny;
            newNormal[vertex * 3 + 2] = nz;
        }
        return new ObjMesh(pos.clone(), uv.clone(), newNormal, quadCount);
    }

    // ─── Queries ────────────────────────────────────────────────────────────

    /**
     * Axis-aligned bounds of every vertex; the empty mesh reports a zero-size box at origin.
     */
    public AABB bounds() {
        AABB bounds = cachedBounds;
        if (bounds != null) {
            return bounds;
        }
        if (quadCount == 0) {
            bounds = new AABB(0, 0, 0, 0, 0, 0);
        } else {
            float minX = Float.POSITIVE_INFINITY, minY = Float.POSITIVE_INFINITY, minZ = Float.POSITIVE_INFINITY;
            float maxX = Float.NEGATIVE_INFINITY, maxY = Float.NEGATIVE_INFINITY, maxZ = Float.NEGATIVE_INFINITY;
            for (int i = 0; i < pos.length; i += 3) {
                minX = Math.min(minX, pos[i]);
                maxX = Math.max(maxX, pos[i]);
                minY = Math.min(minY, pos[i + 1]);
                maxY = Math.max(maxY, pos[i + 1]);
                minZ = Math.min(minZ, pos[i + 2]);
                maxZ = Math.max(maxZ, pos[i + 2]);
            }
            bounds = new AABB(minX, minY, minZ, maxX, maxY, maxZ);
        }
        cachedBounds = bounds;
        return bounds;
    }

    /**
     * Bounds of just the vertices lying inside {@code box}, or null when none do.
     *
     * <p>This is how the multi-block tubes derive their collision: they walk the tube in about
     * fifty steps, ask what geometry falls in each step's slice, and turn the answer into a
     * collision box. CodeChickenLib provided it; the intermediate port left it as a stub
     * returning the whole model's bounds, which collapsed every one of those slices onto the
     * same box at the model's centre — so a tube's collision was a single small cube in the
     * middle instead of a surface along its length.</p>
     */
    @Nullable
    public AABB boundsInside(AABB box) {
        double minX = Double.POSITIVE_INFINITY, minY = Double.POSITIVE_INFINITY, minZ = Double.POSITIVE_INFINITY;
        double maxX = Double.NEGATIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
        boolean any = false;

        // Per quad, clipped to the box — not per vertex. The callers slice the tube about every
        // 0.08 blocks along its axis, which is far finer than the geometry: a slice usually cuts
        // through the middle of a quad without containing any of its four corners. Testing
        // vertices alone therefore reported "nothing here" for most slices, and where a corner
        // did happen to fall inside, the bounds covered only that corner and put the collision
        // box off to one side of the tube.
        for (int quad = 0; quad < quadCount; quad++) {
            int base = quad * 4 * 3;
            double qMinX = Double.POSITIVE_INFINITY, qMinY = Double.POSITIVE_INFINITY, qMinZ = Double.POSITIVE_INFINITY;
            double qMaxX = Double.NEGATIVE_INFINITY, qMaxY = Double.NEGATIVE_INFINITY, qMaxZ = Double.NEGATIVE_INFINITY;
            for (int vertex = 0; vertex < 4; vertex++) {
                int i = base + vertex * 3;
                qMinX = Math.min(qMinX, pos[i]);
                qMaxX = Math.max(qMaxX, pos[i]);
                qMinY = Math.min(qMinY, pos[i + 1]);
                qMaxY = Math.max(qMaxY, pos[i + 1]);
                qMinZ = Math.min(qMinZ, pos[i + 2]);
                qMaxZ = Math.max(qMaxZ, pos[i + 2]);
            }

            double clipMinX = Math.max(qMinX, box.minX);
            double clipMaxX = Math.min(qMaxX, box.maxX);
            double clipMinY = Math.max(qMinY, box.minY);
            double clipMaxY = Math.min(qMaxY, box.maxY);
            double clipMinZ = Math.max(qMinZ, box.minZ);
            double clipMaxZ = Math.min(qMaxZ, box.maxZ);
            if (clipMinX > clipMaxX || clipMinY > clipMaxY || clipMinZ > clipMaxZ) {
                continue;
            }

            any = true;
            minX = Math.min(minX, clipMinX);
            maxX = Math.max(maxX, clipMaxX);
            minY = Math.min(minY, clipMinY);
            maxY = Math.max(maxY, clipMaxY);
            minZ = Math.min(minZ, clipMinZ);
            maxZ = Math.max(maxZ, clipMaxZ);
        }
        return any ? new AABB(minX, minY, minZ, maxX, maxY, maxZ) : null;
    }

    // ─── Combination ────────────────────────────────────────────────────────

    /**
     * Concatenates the quads of every mesh into one. Replaces {@code CCLProxy.combine}.
     */
    public static ObjMesh merge(Collection<ObjMesh> meshes) {
        int total = 0;
        for (ObjMesh mesh : meshes) {
            total += mesh.quadCount;
        }
        if (total == 0) {
            return EMPTY;
        }

        float[] pos = new float[total * 12];
        float[] uv = new float[total * 8];
        float[] normal = new float[total * 12];
        int posAt = 0, uvAt = 0;
        for (ObjMesh mesh : meshes) {
            if (mesh.quadCount == 0) {
                continue;
            }
            System.arraycopy(mesh.pos, 0, pos, posAt, mesh.pos.length);
            System.arraycopy(mesh.normal, 0, normal, posAt, mesh.normal.length);
            System.arraycopy(mesh.uv, 0, uv, uvAt, mesh.uv.length);
            posAt += mesh.pos.length;
            uvAt += mesh.uv.length;
        }
        return new ObjMesh(pos, uv, normal, total);
    }

    public static ObjMesh merge(ObjMesh... meshes) {
        return merge(List.of(meshes));
    }
}
