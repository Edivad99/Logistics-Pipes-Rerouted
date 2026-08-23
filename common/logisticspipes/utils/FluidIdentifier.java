package logisticspipes.utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import com.mojang.serialization.Codec;

import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.transfer.ResourceHandler;
import net.neoforged.neoforge.transfer.fluid.FluidResource;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.transfer.fluid.FluidUtil;

import logisticspipes.proxy.LPRegistries;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.proxy.computers.interfaces.ILPCCTypeHolder;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * The fluid counterpart of {@link ItemIdentifier}: an immutable, interned, unique handle on a fluid
 * identity. Since 1.21 that identity is {@link Fluid} plus a {@link DataComponentPatch}, mirroring
 * how {@link FluidStack} itself is built.
 */
public class FluidIdentifier implements Comparable<FluidIdentifier>, ILPCCTypeHolder {

	// Cache key. DataComponentPatch is immutable with value-based equals/hashCode, so a record
	// covers all the bookkeeping this needs.
	private record FluidKey(Fluid fluid, DataComponentPatch components) {}

	private static final AtomicLong serialCounter = new AtomicLong();

	// Identifiers whose patch is empty. Bounded by the fluid registry, so these are held strongly.
	private final static ConcurrentHashMap<Fluid, FluidIdentifier> simpleIdentifiers = new ConcurrentHashMap<>(256, 0.5f, 1);

	// Everything else. Held weakly for the same reason as the item cache: nothing bounds how many
	// component variants of a fluid a mod may produce at runtime.
	private final static WeakInternCache<FluidKey, FluidIdentifier> patchedIdentifiers =
			WeakInternCache.create("LogisticsPipes FluidIdentifier Cleanup Thread");

	private final Object[] ccTypeHolder = new Object[1];

	public final Fluid fluid;
	/**
	 * The component patch that distinguishes this identity from the fluid's prototype. Always
	 * canonical (sanitized), see {@link #get(Fluid, DataComponentPatch)}.
	 */
	public final DataComponentPatch components;
	/**
	 * Allocation-order tiebreaker keeping {@link #compareTo} a total order consistent with
	 * {@link #equals}. Replaces the old {@code uniqueID}, which was a random int drawn from a fresh
	 * {@code Random} per attempt and made the ordering of component-carrying fluids differ between
	 * runs.
	 */
	private final long serial;

	@Nullable
	private String sortKey = null;

	private FluidIdentifier(Fluid fluid, DataComponentPatch components) {
		this.fluid = fluid;
		this.components = components;
		this.serial = FluidIdentifier.serialCounter.getAndIncrement();
	}

	/* Factories */

	private static FluidIdentifier getOrCreateSimple(Fluid fluid) {
		// No locking: if two threads race they produce equal identifiers and one wins the map.
		FluidIdentifier ret = FluidIdentifier.simpleIdentifiers.get(fluid);
		if (ret != null) {
			return ret;
		}
		ret = new FluidIdentifier(fluid, DataComponentPatch.EMPTY);
		FluidIdentifier.simpleIdentifiers.put(fluid, ret);
		return ret;
	}

	/**
	 * The bare identity of {@code fluid}, carrying no components.
	 */
	public static FluidIdentifier get(Fluid fluid) {
		return FluidIdentifier.getOrCreateSimple(fluid);
	}

	/**
	 * Interns the identity of {@code fluid} carrying {@code rawPatch}.
	 * <p>
	 * As with items, a patch is not canonical on its own: one that sets a component to the value the
	 * fluid's prototype already has is a distinct patch from {@link DataComponentPatch#EMPTY} yet
	 * yields an identical FluidStack. Unseen patches are therefore normalized by round-tripping
	 * through a FluidStack, whose {@link FluidStack#getComponentsPatch()} is canonical by
	 * construction.
	 */
	public static FluidIdentifier get(Fluid fluid, DataComponentPatch rawPatch) {
		if (rawPatch.isEmpty()) {
			return FluidIdentifier.getOrCreateSimple(fluid);
		}
		FluidIdentifier hit = FluidIdentifier.patchedIdentifiers.getIfPresent(new FluidKey(fluid, rawPatch));
		if (hit != null) {
			return hit;
		}
		return FluidIdentifier.get(new FluidStack(fluid.builtInRegistryHolder(), 1, rawPatch));
	}

	@Nullable
	public static FluidIdentifier get(@Nullable FluidStack stack) {
		if (stack == null || stack.isEmpty()) {
			return null;
		}
		DataComponentPatch patch = stack.getComponentsPatch();
		if (patch.isEmpty()) {
			return FluidIdentifier.getOrCreateSimple(stack.getFluid());
		}
		return FluidIdentifier.patchedIdentifiers.getOrCreate(
				new FluidKey(stack.getFluid(), patch),
				key -> new FluidIdentifier(key.fluid(), key.components()));
	}

	@Nullable
	public static FluidIdentifier get(ItemIdentifier stack) {
		return FluidIdentifier.get(stack.makeStack(1));
	}

	@Nullable
	public static FluidIdentifier get(ItemStack stack) {
		return FluidIdentifier.get(ItemIdentifierStack.getFromStack(stack));
	}

	/**
	 * The fluid held by a container item, looked up through LP's own container registry first and
	 * then through the item's fluid capability.
	 */
	@Nullable
	public static FluidIdentifier get(ItemIdentifierStack stack) {
		FluidIdentifierStack fstack = SimpleServiceLocator.logisticsFluidManager.getFluidFromContainer(stack, LPRegistries.access());
		if (fstack != null) {
			return FluidIdentifier.get(fstack.makeFluidStack());
		}
		// Was two steps -- walk the item's fluid handler by hand, then fall back to the deprecated
		// FluidUtil. The transfer API's own FluidUtil is that same walk, so both collapse into it.
		FluidStack contained = FluidUtil.getFirstStackContained(stack.makeNormalStack());
		return contained.isEmpty() ? null : FluidIdentifier.get(contained);
	}

	/* Accessors */

	public Fluid getFluid() {
		return fluid;
	}

	/**
	 * The registry name of this fluid, e.g. {@code minecraft:water}.
	 */
	public String getFluidID() {
		ResourceLocation key = BuiltInRegistries.FLUID.getKey(fluid);
		return key != null ? key.toString() : "unknown";
	}

	public String getName() {
		return getFluidID();
	}

	public boolean hasCustomData() {
		Optional<? extends CustomData> data = components.get(DataComponents.CUSTOM_DATA);
		return data != null && data.isPresent();
	}

	/**
	 * A mutable copy of this identity's {@link DataComponents#CUSTOM_DATA}, or null when it has
	 * none. The closest equivalent of the old {@code tag} field.
	 */
	@Nullable
	public CompoundTag getCustomDataTag() {
		Optional<? extends CustomData> data = components.get(DataComponents.CUSTOM_DATA);
		return data != null && data.isPresent() ? data.get().copyTag() : null;
	}

	public FluidStack makeFluidStack(int amount) {
		return new FluidStack(fluid.builtInRegistryHolder(), amount, components);
	}

	public FluidIdentifierStack makeFluidIdentifierStack(int amount) {
		return new FluidIdentifierStack(this, amount);
	}

	public ItemIdentifier getItemIdentifier() {
		return SimpleServiceLocator.logisticsFluidManager.getFluidContainer(this.makeFluidIdentifierStack(1), LPRegistries.access()).getItem();
	}

	public int getFreeSpaceInsideTank(ResourceHandler<FluidResource> tank) {
		FluidResource held = tank.getResource(0);
		if (held.isEmpty()) {
			return tank.getCapacityAsInt(0, FluidResource.of(makeFluidStack(1)));
		}
		if (this.equals(FluidIdentifier.get(held.toStack(1)))) {
			return tank.getCapacityAsInt(0, held) - tank.getAmountAsInt(0);
		}
		return 0;
	}

	/* Registry-wide access */

	private static boolean init = false;

	public static void initFromNeoForge(boolean flag) {
		if (FluidIdentifier.init) {
			return;
		}
		BuiltInRegistries.FLUID.forEach(FluidIdentifier::get);
		if (flag) {
			FluidIdentifier.init = true;
		}
	}

	/**
	 * Every component-free fluid identity a tank can actually hold, in a stable order. This drives
	 * the fluid picker GUI.
	 * <p>
	 * Two filters matter here. Every flowing fluid is registered twice, as a source and as a flowing
	 * variant ({@code minecraft:water} and {@code minecraft:flowing_water}), and both map to the
	 * same visible container, so listing both showed every fluid twice in the picker. And
	 * {@code minecraft:empty} is not a fluid anyone can select. Fluid handlers only ever report
	 * source fluids, so the flowing variants are of no use as identities.
	 * <p>
	 * The order is sorted rather than a HashMap's iteration order, which is unspecified and could
	 * differ between client and server.
	 */
	public static Collection<FluidIdentifier> all() {
		List<FluidIdentifier> list = new ArrayList<>();
		for (FluidIdentifier ident : FluidIdentifier.simpleIdentifiers.values()) {
			if (ident.isSelectable()) {
				list.add(ident);
			}
		}
		list.sort(null);
		return Collections.unmodifiableCollection(list);
	}

	/**
	 * Whether this identity denotes a fluid that can actually sit in a tank, i.e. neither the empty
	 * fluid nor the flowing variant of a source fluid.
	 */
	public boolean isSelectable() {
		return fluid != Fluids.EMPTY && fluid.isSource(fluid.defaultFluidState());
	}

	/* Identity */

	@Override
	public boolean equals(Object that) {
		if (this == that) {
			return true;
		}
		if (!(that instanceof FluidIdentifier other)) {
			return false;
		}
		return fluid == other.fluid && components.equals(other.components);
	}

	@Override
	public int hashCode() {
		return fluid.hashCode() * 31 + components.hashCode();
	}

	/**
	 * A deterministic rendering of {@link #components}, giving the component half of the identity a
	 * stable sort position. Same approach as {@code ItemIdentifier#sortKey}.
	 */
	private String sortKey() {
		String key = sortKey;
		if (key != null) {
			return key;
		}
		if (components.isEmpty()) {
			return sortKey = "";
		}
		List<String> parts = new ArrayList<>(components.size());
		for (Map.Entry<DataComponentType<?>, Optional<?>> entry : components.entrySet()) {
			ResourceLocation typeKey = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
			String name = typeKey != null ? typeKey.toString() : entry.getKey().toString();
			parts.add(entry.getValue()
					.map(value -> name + "=" + FluidIdentifier.renderComponent(entry.getKey(), value))
					.orElse("!" + name));
		}
		parts.sort(null);
		return sortKey = String.join(",", parts);
	}

	@SuppressWarnings("unchecked")
	private static String renderComponent(DataComponentType<?> type, Object value) {
		Codec<Object> codec = (Codec<Object>) type.codec();
		if (codec != null) {
			Optional<String> encoded = codec.encodeStart(NbtOps.INSTANCE, value).result().map(Tag::toString);
			if (encoded.isPresent()) {
				return encoded.get();
			}
		}
		return String.valueOf(value);
	}

	/**
	 * A total order consistent with {@link #equals}, deterministic across runs and between sides.
	 */
	@Override
	public int compareTo(FluidIdentifier o) {
		if (this == o) {
			return 0;
		}
		int c = Integer.compare(BuiltInRegistries.FLUID.getId(fluid), BuiltInRegistries.FLUID.getId(o.fluid));
		if (c != 0) {
			return c;
		}
		c = sortKey().compareTo(o.sortKey());
		if (c != 0) {
			return c;
		}
		// Only reached when two distinct identities render identically, i.e. they differ solely in
		// components that could not be encoded.
		return Long.compare(serial, o.serial);
	}

	@Override
	public String toString() {
		return getFluidID() + (components.isEmpty() ? "" : ":" + sortKey());
	}

	@Override
	public Object[] getTypeHolder() {
		return ccTypeHolder;
	}
}
