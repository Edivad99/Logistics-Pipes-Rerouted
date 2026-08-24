package logisticspipes.client.particle;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.CameraRenderState;
import net.minecraft.client.renderer.state.ParticleGroupRenderState;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.client.renderer.LPRenderTypes;

/**
 * The particle group behind {@link GlowGeometryParticle}.
 *
 * <p>Vanilla's {@code QuadParticleGroup} extracts every particle into one big vertex buffer keyed
 * by {@code SingleQuadParticle.Layer}. LP's laser effects are not quads on the particle atlas, so
 * this group does the analogous thing for untextured geometry: extraction snapshots the vertices,
 * and submission hands them to {@code submitCustomGeometry}, which is 1.21.9's replacement for
 * grabbing a buffer source and drawing directly.</p>
 *
 * <p>No frustum test. The beams are up to a chunk long and are anchored at the pipe, so culling on
 * the anchor -- which is what {@code QuadParticleGroup} does with its point-in-frustum check --
 * would make a beam vanish whenever its source pipe left the view while the beam itself did not.</p>
 */
public class GlowParticleGroup extends ParticleGroup<GlowGeometryParticle> implements ParticleGroupRenderState {

    /** Camera-relative position and colour of one vertex; quads are consecutive runs of four. */
    private record Vertex(float x, float y, float z, int argb) {
    }

    private final java.util.List<Vertex> vertices = new java.util.ArrayList<>();

    public GlowParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
        vertices.clear();
        Collector collector = new Collector();
        for (GlowGeometryParticle particle : particles) {
            particle.emit(collector, camera, partialTick);
        }
        return this;
    }

    @Override
    public void submit(SubmitNodeCollector collector, CameraRenderState cameraState) {
        if (vertices.isEmpty()) {
            return;
        }
        // The vertices are already camera-relative, so the pose stays identity.
        collector.submitCustomGeometry(new PoseStack(), LPRenderTypes.GLOW, (pose, consumer) -> {
            for (Vertex vertex : vertices) {
                consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z()).setColor(vertex.argb());
            }
        });
    }

    @Override
    public void clear() {
        vertices.clear();
    }

    /**
     * The narrow slice of {@link VertexConsumer} the particles actually use -- position and
     * colour. Everything else throws rather than silently dropping vertex data, so a particle
     * that starts asking for UVs or normals fails loudly instead of rendering wrong.
     */
    private class Collector implements VertexConsumer {

        private float x;
        private float y;
        private float z;

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return setColor(net.minecraft.util.ARGB.color(alpha, red, green, blue));
        }

        @Override
        public VertexConsumer setColor(int argb) {
            vertices.add(new Vertex(x, y, z, argb));
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            throw new UnsupportedOperationException("LP glow geometry is untextured");
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            throw new UnsupportedOperationException("LP glow geometry has no overlay");
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            throw new UnsupportedOperationException("LP glow geometry is not lightmapped");
        }

        @Override
        public VertexConsumer setNormal(float x, float y, float z) {
            throw new UnsupportedOperationException("LP glow geometry has no normals");
        }

        @Override
        public VertexConsumer setLineWidth(float width) {
            throw new UnsupportedOperationException("LP glow geometry is not line geometry");
        }
    }
}
