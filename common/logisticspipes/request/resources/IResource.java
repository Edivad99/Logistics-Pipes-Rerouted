package logisticspipes.request.resources;

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
