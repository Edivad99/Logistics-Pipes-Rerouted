package logisticspipes.recipes;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import logisticspipes.LPConstants;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

/**
 * A crafting recipe that produces a clean (NBT-stripped) copy of a specific item.
 * Matches a crafting grid containing exactly one instance of the target item.
 * Used to reset module/orderer state.
 */
public class ShapelessResetRecipe extends CustomRecipe {

	public static final ResourceLocation ID = LPConstants.rl("reset");

	public static final RecipeSerializer<ShapelessResetRecipe> SERIALIZER =
			new RecipeSerializer<>() {

				@Override
				public MapCodec<ShapelessResetRecipe> codec() {
					return RecordCodecBuilder.mapCodec(instance -> instance.group(
							BuiltInRegistries.ITEM.byNameCodec()
									.fieldOf("item")
									.forGetter(recipe -> recipe.targetItem)
					).apply(instance, item -> new ShapelessResetRecipe(CraftingBookCategory.MISC, item)));
				}

				@Override
				public StreamCodec<RegistryFriendlyByteBuf, ShapelessResetRecipe> streamCodec() {
					return StreamCodec.composite(
							ByteBufCodecs.registry(Registries.ITEM),
							recipe -> recipe.targetItem,
							item -> new ShapelessResetRecipe(CraftingBookCategory.MISC, item));
				}
			};

	private final Item targetItem;

	public ShapelessResetRecipe(CraftingBookCategory category, Item targetItem) {
		super(category);
		this.targetItem = targetItem;
	}

	@Override
	public boolean matches(CraftingInput craftingInput, Level level) {
		boolean found = false;
		for (var stack : craftingInput.items()) {
			if (!stack.isEmpty()) {
				if (stack.getItem() == targetItem) {
					if (found) return false; // only one allowed
					found = true;
				} else {
					return false; // no other items allowed
				}
			}
		}
		return found;
	}

	@Override
	public ItemStack assemble(CraftingInput craftingInput, HolderLookup.Provider provider) {
		return new ItemStack(targetItem);
	}

	@Override
	public boolean canCraftInDimensions(int width, int height) {
		return true;
	}

	@Override
	public RecipeSerializer<?> getSerializer() {
		return SERIALIZER;
	}

}
