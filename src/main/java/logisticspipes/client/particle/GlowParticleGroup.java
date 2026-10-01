package logisticspipes.client.particle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Camera;
import net.minecraft.client.particle.ParticleEngine;
import net.minecraft.client.particle.ParticleGroup;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.state.level.ParticleGroupRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import logisticspipes.client.renderer.LPRenderTypes;

/**
 * The particle group behind {@link GlowGeometryParticle}.
 *
 * <p>Vanilla's {@code QuadParticleGroup} extracts every particle into one big vertex buffer keyed
 * by {@code SingleQuadParticle.Layer}. LP's laser effects are not quads on the particle atlas, so
 * this group does the analogous thing for their geometry: extraction snapshots the vertices, one
 * list per texture, and submission hands each list to {@code submitCustomGeometry}, which is
 * 1.21.9's replacement for grabbing a buffer source and drawing directly.</p>
 *
 * <p>No frustum test. The beams are up to a chunk long and are anchored at the pipe, so culling on
 * the anchor -- which is what {@code QuadParticleGroup} does with its point-in-frustum check --
 * would make a beam vanish whenever its source pipe left the view while the beam itself did not.</p>
 */
public class GlowParticleGroup extends ParticleGroup<GlowGeometryParticle> implements ParticleGroupRenderState {

    /** Camera-relative position, texture coordinates and colour of one vertex; quads are runs of four. */
    private record Vertex(float x, float y, float z, float u, float v, int argb) {
    }

    private final Map<Identifier, List<Vertex>> vertices = new LinkedHashMap<>();

    public GlowParticleGroup(ParticleEngine engine) {
        super(engine);
    }

    @Override
    public ParticleGroupRenderState extractRenderState(Frustum frustum, Camera camera, float partialTick) {
        vertices.values().forEach(List::clear);
        for (GlowGeometryParticle particle : particles) {
            particle.emit(new Collector(vertices.computeIfAbsent(particle.texture(), ignored -> new ArrayList<>())),
                camera, partialTick);
        }
        return this;
    }

    @Override
    public void submit(SubmitNodeCollector collector, CameraRenderState cameraState) {
        vertices.forEach((texture, list) -> {
            if (list.isEmpty()) {
                return;
            }
            // The vertices are already camera-relative, so the pose stays identity. The lasers glow,
            // so they are lit at full brightness whatever the surrounding light.
            collector.submitCustomGeometry(new PoseStack(), LPRenderTypes.POWER_LASER.apply(texture),
                (pose, consumer) -> {
                    for (Vertex vertex : list) {
                        consumer.addVertex(pose, vertex.x(), vertex.y(), vertex.z())
                            .setUv(vertex.u(), vertex.v())
                            .setColor(vertex.argb())
                            .setLight(LightCoordsUtil.FULL_BRIGHT);
                    }
                });
        });
    }

    @Override
    public void clear() {
        vertices.clear();
    }

    /**
     * The narrow slice of {@link VertexConsumer} the particles actually use -- position, texture
     * coordinates and colour, in that order. Everything else throws rather than silently dropping
     * vertex data, so a particle that starts asking for normals fails loudly instead of rendering wrong.
     */
    private static class Collector implements VertexConsumer {

        private final List<Vertex> out;
        private float x;
        private float y;
        private float z;
        private float u;
        private float v;

        Collector(List<Vertex> out) {
            this.out = out;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            this.x = x;
            this.y = y;
            this.z = z;
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            this.u = u;
            this.v = v;
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            return setColor(ARGB.color(alpha, red, green, blue));
        }

        @Override
        public VertexConsumer setColor(int argb) {
            out.add(new Vertex(x, y, z, u, v, argb));
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            throw new UnsupportedOperationException("LP glow geometry has no overlay");
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            throw new UnsupportedOperationException("LP glow geometry is lit at full brightness");
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
