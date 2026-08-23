package logisticspipes.client.model.mesh;

import java.util.List;

import javax.annotation.Nullable;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;


/**
 * Turns an {@link ObjMesh} into {@link BakedQuad}s so pipe geometry can live in the chunk
 * mesh instead of being re-emitted by a block entity renderer every frame.
 *
 * <p>This is the piece the old pipeline never had: {@code LPModel3DImpl.renderToQuads}
 * returned an empty list with a TODO, so everything had to go through the immediate-mode
 * {@code render()} path.</p>
 */
public final class MeshBaker {

    private MeshBaker() {
    }

    /**
     * {@code DefaultVertexFormat.BLOCK} is 8 ints per vertex.
     */
    private static final int INTS_PER_VERTEX = 8;
    private static final int INTS_PER_QUAD = INTS_PER_VERTEX * 4;

    public static final int WHITE = 0xFFFFFFFF;

    /**
     * Picks the face direction a quad reports, so that it is lit from the pipe's own position.
     *
     * <p>For quads in the {@code side == null} bucket — which is all of ours, since pipe
     * geometry is not flush with the block faces and cannot be culled per face — the direction
     * is used for exactly one thing: {@code ModelBlockRenderer.renderModelFaceFlat} recomputes
     * each quad's light from either the neighbouring block or this one, deciding between them
     * with {@code ModelBlockRenderer.calculateShape}. Along direction {@code d} that test is</p>
     *
     * <pre>flat on d's axis &amp;&amp; (touches the block face on d || state.isCollisionShapeFullBlock(...))</pre>
     *
     * <p>and its second half is always true for us: {@code isCollisionShapeFullBlock} is cached
     * per {@code BlockState} in {@code BlockStateBase.initCache}, which evaluates it with an
     * {@code EmptyBlockGetter} where {@code LogisticsBlockGenericPipe.getShape} finds no block
     * entity and falls back to a full cube. (The same caching is why that class has to override
     * {@code propagatesSkylightDown} and {@code getLightBlock} by hand.)</p>
     *
     * <p>So a fixed direction lights every quad flat on its axis from the neighbour. With the
     * previous constant {@code UP} that meant every horizontal quad of the frame sampled the
     * block above, and a pipe underneath one went dark — the double-sided geometry doubling the
     * count, since each surface exists in both windings.</p>
     *
     * <p>Only the first half of the test is ours to control, and it is enough: a planar quad is
     * degenerate on at most one axis, so an axis with a non-zero extent always exists, and
     * naming it makes the quad "not flat" and vanilla light it from {@code pos}. Every quad then
     * takes one light level sampled at the pipe's own block — precisely what the immediate-mode
     * renderer did by passing a single {@code packedLight} for the whole pipe. The largest
     * extent is chosen so the comparison is nowhere near float noise. Nothing else reads the
     * direction: face culling only applies to the per-side buckets, and
     * {@code ClientLevel.getShade} ignores it while {@code shade} is false.</p>
     */
    private static Direction lightSampleFace(ObjMesh mesh, int quad) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        for (int vertex = 0; vertex < 4; vertex++) {
            float x = mesh.x(quad, vertex);
            float y = mesh.y(quad, vertex);
            float z = mesh.z(quad, vertex);
            minX = Math.min(minX, x);
            maxX = Math.max(maxX, x);
            minY = Math.min(minY, y);
            maxY = Math.max(maxY, y);
            minZ = Math.min(minZ, z);
            maxZ = Math.max(maxZ, z);
        }
        float dx = maxX - minX, dy = maxY - minY, dz = maxZ - minZ;
        // A degenerate quad draws nothing, so any direction does; UP keeps the old behaviour.
        if (dx <= 0 && dy <= 0 && dz <= 0) {
            return Direction.UP;
        }
        if (dy >= dx && dy >= dz) {
            return Direction.UP;
        }
        return dx >= dz ? Direction.EAST : Direction.SOUTH;
    }

    /**
     * Bakes every quad of {@code mesh} onto {@code sprite}.
     *
     * @param uv    applied to the mesh's 0..1 UVs before mapping them into the sprite's
     *              sub-rectangle; use {@link UvTransform#IDENTITY} for none
     * @param argb  colour multiplier in 0xAARRGGBB, {@link #WHITE} for untinted
     * @param shade whether the quad takes directional shading from the chunk lighting
     */
    public static void bake(List<BakedQuad> out, ObjMesh mesh, @Nullable TextureAtlasSprite sprite,
        UvTransform uv, int argb, boolean shade) {
        if (mesh.isEmpty() || sprite == null) {
            return;
        }

        int packedColour = packColour(argb);
        for (int quad = 0; quad < mesh.quadCount(); quad++) {
            int[] data = new int[INTS_PER_QUAD];
            for (int vertex = 0; vertex < 4; vertex++) {
                int offset = vertex * INTS_PER_VERTEX;
                data[offset] = Float.floatToRawIntBits(mesh.x(quad, vertex));
                data[offset + 1] = Float.floatToRawIntBits(mesh.y(quad, vertex));
                data[offset + 2] = Float.floatToRawIntBits(mesh.z(quad, vertex));
                data[offset + 3] = packedColour;
                data[offset + 4] = Float.floatToRawIntBits(SpriteUv.u(sprite, uv.applyU(mesh.u(quad, vertex))));
                data[offset + 5] = Float.floatToRawIntBits(SpriteUv.v(sprite, uv.applyV(mesh.v(quad, vertex))));
                data[offset + 6] = 0; // lightmap: filled in by the chunk builder
                data[offset + 7] = packNormal(mesh.nx(quad, vertex), mesh.ny(quad, vertex), mesh.nz(quad, vertex));
            }
            // tintIndex -1: the color is baked into the vertices above, so no block color
            // handler should touch it.
            // 1.21.3 added a lightEmission argument; 0 keeps the pre-1.21.3 behaviour of letting
            // the chunk builder's lightmap decide the brightness.
            out.add(new BakedQuad(data, -1, lightSampleFace(mesh, quad), sprite, shade, 0));
        }
    }

    /**
     * Vertex colours in the BLOCK format are little-endian RGBA, i.e. 0xAABBGGRR.
     */
    private static int packColour(int argb) {
        int a = (argb >>> 24) & 0xFF;
        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;
        return (a << 24) | (b << 16) | (g << 8) | r;
    }

    private static int packNormal(float nx, float ny, float nz) {
        int x = ((int) (nx * 127.0f)) & 0xFF;
        int y = ((int) (ny * 127.0f)) & 0xFF;
        int z = ((int) (nz * 127.0f)) & 0xFF;
        return x | (y << 8) | (z << 16);
    }
}
