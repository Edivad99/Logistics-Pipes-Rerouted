package logisticspipes.modules;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;
import kotlin.Unit;
import logisticspipes.LogisticsPipes;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.proxy.MainProxy;
import logisticspipes.proxy.computers.interfaces.CCCommand;
import logisticspipes.proxy.computers.interfaces.CCType;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.proxy.computers.objects.CCSinkResponder;
import logisticspipes.utils.SinkReply;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.item.LPItems;
import lombok.Getter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import network.rs485.logisticspipes.IStore;
import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.property.Property;
import network.rs485.logisticspipes.property.PropertyHolder;
import network.rs485.logisticspipes.property.UtilKt;
import org.jetbrains.annotations.NotNull;

@CCType(name = "LogisticsModule")
public abstract class LogisticsModule implements IStore, ILPCCTypeHolder, PropertyHolder {

	private final Object[] ccTypeHolder = new Object[1];
	@Nullable
	protected IWorldProvider worldProvider;
	@Nullable
	protected IPipeServiceProvider service;
	@Getter
    @Nullable
    protected ModulePositionType slot;
	@Getter
    protected int positionInt;
	protected boolean initialized;

	/**
	 * Registers the Inventory and ItemSender to the module
	 *
	 * @param world   that the module is in.
	 * @param service Inventory access, power and utility functions provided by the pipe.
	 */
	public void registerHandler(IWorldProvider world, IPipeServiceProvider service) {
		this.worldProvider = world;
		this.service = service;
	}

	/**
	 * Returns the name this module is registered in LP with, as used in
	 * {@link logisticspipes.items.ItemModule#registerModule} and saved in {@link LPItems#modules}.
	 */
	public abstract String getLPName();

	@Override
	public List<Property<?>> getProperties() {
		return Collections.emptyList();
	}

	/**
	 * Registers the slot type the module is in
	 */
	public void registerPosition(ModulePositionType slot, int positionInt) {
		this.slot = slot;
		this.positionInt = positionInt;
	}

    @Nullable
	public BlockPos getBlockPos() {
		final IPipeServiceProvider service = this.service;
		if (service == null) {
			if (LogisticsPipes.isDEBUG()) {
				throw new IllegalStateException("Module has no service, but getBlockPos was called");
			}
			return BlockPos.ZERO;
		} else if (slot.isInWorld()) {
			return service.getPos();
		} else {
			if (LogisticsPipes.isDEBUG()) {
				throw new IllegalStateException("Module is not in world, but getBlockPos was called");
			}
			return BlockPos.ZERO;
		}
	}

	@Nullable
	public Level getWorld() {
		final IWorldProvider worldProvider = this.worldProvider;
		if (worldProvider == null) return null;
		return worldProvider.getWorld();
	}

    @Override
	public void readFromNBT(CompoundTag tag, HolderLookup.@NotNull Provider provider) {
		PropertyHolder.readFromNBT(tag, provider, this);
	}

	@Override
	public void writeToNBT(CompoundTag tag, HolderLookup.@NotNull Provider provider) {
		PropertyHolder.writeToNBT(tag, provider, this);
	}

	/**
     * Gives a sink answer on the given itemstack
     *
     * @param stack              to sink
     * @param item               to sink
     * @param bestPriority       best priority seen so far
     * @param bestCustomPriority best custom sub-priority
     * @param allowDefault       is a default only sink allowed to sink this?
     * @param includeInTransit   include the "in transit" items? -- true for a destination
     *                           search, false for a sink check.
     * @param forcePassive       check for passive routing only, in case this method is redirected to other sinks
     * @return SinkReply whether the module sinks the item or not
     */
	public @Nullable SinkReply sinksItem(ItemStack stack, ItemIdentifier item, int bestPriority, int bestCustomPriority,
			boolean allowDefault, boolean includeInTransit, boolean forcePassive) {
		return null;
	}

	/**
	 * A tick for the Module
	 */
	public abstract void tick();

	/**
	 * Is this module interested in all items, or just some specific ones?
	 *
	 * @return true: this module will be checked against every item request
	 * false: only requests involving items collected by {@link #collectSpecificInterests(Collection)} will be checked
	 */
	public abstract boolean hasGenericInterests();

	/**
	 * Collects the items which this module is capable of providing or supplying
	 * (or is otherwise interested in)
	 *
	 * @param itemIdentifiers the collection to add the interests to
	 */
	public void collectSpecificInterests(Collection<ItemIdentifier> itemIdentifiers) {
	}

	public abstract boolean interestedInAttachedInventory();

	/**
	 * is this module interested in receiving any damage variant of items in the
	 * attached inventory?
	 */
	public abstract boolean interestedInUndamagedID();

	/**
	 * is this module a valid destination for bounced items.
	 */
	public abstract boolean receivePassive();

	/**
	 * Returns whether the module should be displayed the effect when as an
	 * item.
	 *
	 * @return True to show effect False to no effect (default)
	 */
	public boolean hasEffect() {
		return false;
	}

	public List<CCSinkResponder> queueCCSinkEvent(ItemIdentifierStack item) {
		return new ArrayList<>(0);
	}

	@CCCommand(description = "Returns true if the Pipe has a gui")
	public boolean hasGui() {
		return this instanceof Gui;
	}

	public LogisticsModule getModule() {
		return this;
	}

	protected ISlotUpgradeManager getUpgradeManager() {
		return Objects.requireNonNull(service, "service object was null in " + this)
				.getUpgradeManager(slot, positionInt);
	}

	@Override
	public String toString() {
		String at = "{service is null}";
		if (service != null) {
			at = Objects.toString(service.getPos());
		}
		String in = "{world is null}";
		if (worldProvider != null) {
			in = Objects.toString(worldProvider.getWorld());
		}
		return String.format("%s at %s in %s", getClass().getName(), at, in);
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}

	public void finishInit() {
		if (initialized) {
			if (LogisticsPipes.isTesting()) {
				throw new IllegalStateException("finishInit called on initialized " + getClass().getName());
			} else if (LogisticsPipes.isDEBUG()) {
				System.err.println("finishInit called on initialized " + getClass().getName());
				new Exception().printStackTrace();
			}
			return;
		}
		if (service != null) {
			final Level blockAccess = worldProvider == null ? null : worldProvider.getWorld();
			MainProxy.runOnServer(blockAccess, () -> () ->
					UtilKt.addObserver(getProperties(), (prop) -> {
						service.markTileDirty();
						return Unit.INSTANCE;
					})
			);
		}
		initialized = true;
	}

	public enum ModulePositionType {
		SLOT(true), IN_HAND(false), IN_PIPE(true);

		@Getter
		private final boolean inWorld;

		ModulePositionType(boolean inWorld) {
			this.inWorld = inWorld;
		}
	}

}
