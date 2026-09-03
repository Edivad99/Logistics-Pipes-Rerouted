package logisticspipes.request.resources;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * With Destination and amount
 */
public interface IResource extends ILPCCTypeHolder {

	/**
	 * Tag and body: the tag says which of the three implementations follows.
	 *
	 * <p>The tag is the enum constant, not its ordinal, so the declaration order of
	 * {@link ResourceNetwork} is no longer part of the protocol.
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

	boolean mergeForDisplay(IResource resource, int withAmount); //Amount overrides existing amount inside the resource

	IResource copyForDisplayWith(int amount);

	String getDisplayText(ColorCode missing);

	ItemIdentifierStack getDisplayItem();

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
