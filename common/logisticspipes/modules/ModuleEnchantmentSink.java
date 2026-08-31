package logisticspipes.modules;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.PipeLogisticsChassis.ChassiTargetInformation;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.SinkReply.FixedPriority;
import logisticspipes.utils.item.ItemIdentifier;
import network.rs485.logisticspipes.property.Property;

public class ModuleEnchantmentSink extends LogisticsModule {

	/** Built in {@link #registerPosition}, which runs when the module is installed. */
	private @Nullable SinkReply sinkReply;

	public static String getName() {
		return "enchantment_sink";
	}

	@Override
	public String getLPName() {
		return getName();
	}

	@Override
	public List<Property<?>> getProperties() {
		return Collections.emptyList();
	}

	@Override
	public void registerPosition(ModulePositionType slot, int positionInt) {
		super.registerPosition(slot, positionInt);
		sinkReply = new SinkReply(FixedPriority.EnchantmentItemSink,
				0,
				true,
				false,
				1,
				0,
				new ChassiTargetInformation(getPositionInt()));
	}

	@Override
	public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
			boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		final SinkReply reply = Objects.requireNonNull(sinkReply, "module has not been registered");
		// check to see if a better route is already found
		// Note: Higher MKs are higher priority
		if (bestPriority > reply.fixedPriority.ordinal() || (bestPriority == reply.fixedPriority.ordinal()
				&& bestCustomPriority >= reply.customPriority)) {
			return null;
		}

		//check to see if item is enchanted
		if (stack.isEnchanted()) {
			return reply;
		}
		return null;
	}

	@Override
	public void tick() {}

	@Override
	/*
	 * We will check every item return true
	 * @see logisticspipes.modules.LogisticsModule#hasGenericInterests()
	 */
	public boolean hasGenericInterests() {
		return true;
	}

	@Override
	public boolean interestedInAttachedInventory() {
		return false;
	}

	@Override
	public boolean interestedInUndamagedID() {
		return true;
	}

	@Override
	public boolean receivePassive() {
		return true;
	}

	@Override
	public boolean hasEffect() {
		return true;
	}

}
