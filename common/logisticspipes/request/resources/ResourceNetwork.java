package logisticspipes.request.resources;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import logisticspipes.util.LPDataInput;
import logisticspipes.util.LPDataOutput;

public enum ResourceNetwork {
	DictResource(DictResource.class, logisticspipes.request.resources.DictResource.STREAM_CODEC) {
		@Override
		protected IResource readData(LPDataInput input) {
			return new DictResource(input);
		}
	},
	ItemResource(ItemResource.class, logisticspipes.request.resources.ItemResource.STREAM_CODEC) {
		@Override
		protected IResource readData(LPDataInput input) {
			return new ItemResource(input);
		}
	},
	FluidResource(FluidResource.class, logisticspipes.request.resources.FluidResource.STREAM_CODEC) {
		@Override
		protected IResource readData(LPDataInput input) {
			return new FluidResource(input);
		}
	};

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

	public static void writeResource(LPDataOutput output, IResource resource) {
		if (resource == null) {
			output.writeInt(-1);
			return;
		}
		ResourceNetwork[] values = ResourceNetwork.values();
		for (ResourceNetwork value : values) {
			if (value.clazz.isAssignableFrom(resource.getClass())) {
				output.writeInt(value.ordinal());
				resource.writeData(output);
				return;
			}
		}
		throw new UnsupportedOperationException(resource.getClass().toString());
	}

	public static IResource readResource(LPDataInput input) {
		int id = input.readInt();
		if (id == -1) {
			return null;
		}
		return ResourceNetwork.values()[id].readData(input);
	}

	protected abstract IResource readData(LPDataInput input);
}
