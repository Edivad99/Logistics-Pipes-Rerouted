package logisticspipes.renderer.newpipe;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.proxy.object3d.interfaces.I3DOperation;
import logisticspipes.proxy.object3d.interfaces.IModel3D;
import logisticspipes.proxy.object3d.operation.LPUVTransformationList;
import logisticspipes.proxy.object3d.operation.LPUVTranslation;
import net.minecraft.resources.ResourceLocation;

public interface ISpecialPipeRenderer {

	default void modelsToRenderList(final ArrayList<RenderEntry> objectsToRender, final List<IModel3D> models) {
		objectsToRender.ensureCapacity(objectsToRender.size() + models.size());
		models.stream()
				.map(model -> new RenderEntry(model, new I3DOperation[] { new LPUVTransformationList(new LPUVTranslation(0, 0)) }, getTexture()))
				.forEach(objectsToRender::add);
	}

	default void renderToList(@Nullable CoreUnroutedPipe pipe, ArrayList<RenderEntry> objectsToRender) {
		if (pipe == null) {
			modelsToRenderList(objectsToRender, getModelsWithoutPipe());
		} else {
			modelsToRenderList(objectsToRender, getModelsFromPipe(pipe));
		}
	}

	List<IModel3D> getModelsWithoutPipe();

	List<IModel3D> getModelsFromPipe(CoreUnroutedPipe pipe);

	ResourceLocation getTexture();

}
