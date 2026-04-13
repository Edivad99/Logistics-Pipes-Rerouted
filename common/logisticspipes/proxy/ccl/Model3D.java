package logisticspipes.proxy.ccl;
// TODO: CodeChickenLib not ported to 1.20.1 — stub

import java.util.List;

import net.minecraft.client.renderer.block.model.BakedQuad;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import logisticspipes.proxy.object3d.interfaces.I3DOperation;
import logisticspipes.proxy.object3d.interfaces.IBounds;
import logisticspipes.proxy.object3d.interfaces.IModel3D;

public class Model3D implements IModel3D {
    @Override public IModel3D backfacedCopy() { return null; }
    @Override public void render(I3DOperation... ops) {}
    @Override @OnlyIn(Dist.CLIENT) public List<BakedQuad> renderToQuads(VertexFormat format, I3DOperation... ops) { return null; }
    @Override public void computeNormals() {}
    @Override public void computeStandardLighting() {}
    @Override public IBounds bounds() { return null; }
    @Override public IModel3D apply(I3DOperation op) { return null; }
    @Override public IModel3D copy() { return null; }
    @Override public IModel3D twoFacedCopy() { return null; }
    @Override public Object getOriginal() { return null; }
    @Override public IBounds getBoundsInside(AABB bb) { return null; }
}
