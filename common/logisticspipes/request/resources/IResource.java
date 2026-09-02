package logisticspipes.request.resources;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.routing.IRouter;
import logisticspipes.util.LPDataOutput;
import logisticspipes.util.LPFinalSerializable;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * With Destination and amount
 */
public interface IResource extends ILPCCTypeHolder, LPFinalSerializable {

	/**
	 * Tag and body: the tag says which of the three implementations follows.
	 *
	 * <p>Replaces {@link ResourceNetwork#writeResource}, which wrote the tag as the ordinal of an
	 * enum constant -- making the declaration order of that enum part of the protocol.
	 */
	StreamCodec<RegistryFriendlyByteBuf, IResource> STREAM_CODEC =
			NeoForgeStreamCodecs.<RegistryFriendlyByteBuf, ResourceNetwork>enumCodec(ResourceNetwork.class)
					.dispatch(ResourceNetwork::of, ResourceNetwork::codec);


	ItemIdentifier getAsItem();

	int getRequestedAmount();

	IRouter getRouter();

	boolean matches(IResource resource, MatchSettings settings);

	boolean matches(ItemIdentifier itemType, MatchSettings settings);

	IResource clone(int multiplier);

	void writeData(LPDataOutput output);

	boolean mergeForDisplay(IResource resource, int withAmount); //Amount overrides existing amount inside the resource

	IResource copyForDisplayWith(int amount);

	String getDisplayText(ColorCode missing);

	ItemIdentifierStack getDisplayItem();

	@Override
	default void write(LPDataOutput output) {
		ResourceNetwork.writeResource(output, this);
	}

	/**
	 * Settings only apply for the normal Item Implementation.
	 */
	enum MatchSettings {
		NORMAL,
		WITHOUT_NBT
	}

	enum ColorCode {
		NONE,
		MISSING,
		SUCCESS
	}
}
