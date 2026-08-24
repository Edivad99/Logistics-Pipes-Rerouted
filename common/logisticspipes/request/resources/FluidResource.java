package logisticspipes.request.resources;

import java.util.Objects;

import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IRequestFluid;
import logisticspipes.routing.IRouter;
import logisticspipes.utils.FluidIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.string.ChatColor;
import network.rs485.logisticspipes.util.LPDataInput;
import network.rs485.logisticspipes.util.LPDataOutput;

public class FluidResource implements IResource {

	private final Object[] ccTypeHolder = new Object[1];
	private final FluidIdentifier liquid;
    @Getter
    @Nullable
	private final IRequestFluid target;
	private int amount;

	public FluidResource(FluidIdentifier liquid, int amount, @Nullable IRequestFluid target) {
		this.liquid = liquid;
		this.amount = amount;
		this.target = target;
	}

	public FluidResource(LPDataInput input) {
		liquid = Objects.requireNonNull(FluidIdentifier.get(Objects.requireNonNull(input.readItemIdentifier())));
		amount = input.readInt();
		target = null;
	}

	@Override
	public void writeData(LPDataOutput output) {
		output.writeItemIdentifier(liquid.getItemIdentifier());
		output.writeInt(amount);
	}

	@Override
	public ItemIdentifier getAsItem() {
		return liquid.getItemIdentifier();
	}

	@Override
	public int getRequestedAmount() {
		return amount;
	}

	public FluidIdentifier getFluid() {
		return liquid;
	}

    @Override
    public IRouter getRouter() {
		return target.getRouter();
	}

	@Override
	public boolean matches(IResource resource, MatchSettings settings) {
		return resource instanceof FluidResource && matches(resource.getAsItem(), settings);
	}

	@Override
	public boolean matches(ItemIdentifier itemType, MatchSettings settings) {
		if (itemType.isFluidContainer()) {
			FluidIdentifier other = FluidIdentifier.get(itemType);
			return other.equals(liquid);
		}
		return false;
	}

	@Override
	public IResource clone(int multiplier) {
		return new FluidResource(liquid, amount * multiplier, target);
	}

	@Override
	public boolean mergeForDisplay(IResource resource, int withAmount) {
		if (resource instanceof FluidResource) {
			if (((FluidResource) resource).liquid.equals(liquid)) {
				amount += withAmount;
				return true;
			}
		}
		return false;
	}

	@Override
	public IResource copyForDisplayWith(int amount) {
		return new FluidResource(liquid, amount, null);
	}

	@Override
	public String getDisplayText(ColorCode code) {
		StringBuilder builder = new StringBuilder();
		if (code != ColorCode.NONE) {
			builder.append(code == ColorCode.MISSING ? ChatColor.RED : ChatColor.GREEN);
		}
		builder.append(amount);
		builder.append("mB ");
		builder.append(liquid.makeFluidStack(1).getHoverName().getString());
		if (code != ColorCode.NONE) {
			builder.append(ChatColor.WHITE);
		}
		return builder.toString();
	}

	@Override
	public ItemIdentifierStack getDisplayItem() {
		return liquid.getItemIdentifier().makeStack(amount);
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}

}
