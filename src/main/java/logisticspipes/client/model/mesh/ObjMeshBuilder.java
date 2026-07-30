package logisticspipes.client.model.mesh;

import java.util.Arrays;

/**
 * Growable accumulator for {@link ObjMesh}, used while parsing and while assembling
 * derived geometry. Not thread-safe; build one per parse and discard it.
 */
public final class ObjMeshBuilder {

    private static final int INITIAL_QUADS = 16;

    private float[] pos;
    private float[] uv;
    private float[] normal;
    private int quadCount;

    public ObjMeshBuilder() {
        this(INITIAL_QUADS);
    }

    public ObjMeshBuilder(int expectedQuads) {
        int quads = Math.max(1, expectedQuads);
        pos = new float[quads * 12];
        uv = new float[quads * 8];
        normal = new float[quads * 12];
    }

    public int quadCount() {
        return quadCount;
    }

    public boolean isEmpty() {
        return quadCount == 0;
    }

    /**
     * Appends one quad. Each array is indexed by vertex: {@code positions} holds 12 floats
     * (xyz per vertex), {@code uvs} 8, {@code normals} 12.
     */
    public ObjMeshBuilder addQuad(float[] positions, float[] uvs, float[] normals) {
        ensureCapacity(quadCount + 1);
        System.arraycopy(positions, 0, pos, quadCount * 12, 12);
        System.arraycopy(uvs, 0, uv, quadCount * 8, 8);
        System.arraycopy(normals, 0, normal, quadCount * 12, 12);
        quadCount++;
        return this;
    }

    private void ensureCapacity(int quads) {
        if (quads * 12 <= pos.length) {
            return;
        }
        int newQuads = Math.max(quads, (pos.length / 12) * 2);
        pos = Arrays.copyOf(pos, newQuads * 12);
        uv = Arrays.copyOf(uv, newQuads * 8);
        normal = Arrays.copyOf(normal, newQuads * 12);
    }

    /**
     * Produces the mesh and leaves this builder unusable.
     */
    public ObjMesh build() {
        if (quadCount == 0) {
            return ObjMesh.empty();
        }
        ObjMesh mesh = new ObjMesh(
            Arrays.copyOf(pos, quadCount * 12),
            Arrays.copyOf(uv, quadCount * 8),
            Arrays.copyOf(normal, quadCount * 12),
            quadCount);
        pos = null;
        uv = null;
        normal = null;
        return mesh;
    }
}
