package logisticspipes.request.resources;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public enum ResourceNetwork {
	DictResource(DictResource.class, logisticspipes.request.resources.DictResource.STREAM_CODEC),
	ItemResource(ItemResource.class, logisticspipes.request.resources.ItemResource.STREAM_CODEC),
	FluidResource(FluidResource.class, logisticspipes.request.resources.FluidResource.STREAM_CODEC);

	private final Class<? extends IResource> clazz;
	private final StreamCodec<RegistryFriendlyByteBuf, ? extends IResource> streamCodec;

	ResourceNetwork(Class<? extends IResource> clazz,
			StreamCodec<RegistryFriendlyByteBuf, ? extends IResource> streamCodec) {
		this.clazz = clazz;
		this.streamCodec = streamCodec;
	}

	/**
	 * Which kind of resource this is.
	 *
	 * <p>The three implementations share no useful supertype beyond {@link IResource}, so the
	 * encoded form is a tag and a body, and this enum is the tag.
	 */
	public static ResourceNetwork of(IResource resource) {
		for (ResourceNetwork value : values()) {
			if (value.clazz.isInstance(resource)) {
				return value;
			}
		}
		throw new UnsupportedOperationException(resource.getClass().toString());
	}

	public StreamCodec<RegistryFriendlyByteBuf, ? extends IResource> codec() {
		return streamCodec;
	}

}
