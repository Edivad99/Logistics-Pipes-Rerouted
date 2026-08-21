package logisticspipes.world.level.block.entity;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.wrapper.InvWrapper;

import logisticspipes.LPConfigs;
import logisticspipes.api.IRoutedPowerProvider;
import logisticspipes.interfaces.IGuiOpenControler;
import logisticspipes.interfaces.IGuiTileEntity;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.abstractguis.CoordinatesGuiProvider;
import logisticspipes.network.guis.block.AutoCraftingGui;
import logisticspipes.network.packets.block.CraftingSetType;
import logisticspipes.proxy.MainProxy;
import logisticspipes.request.resources.IResource;
import logisticspipes.utils.CraftingUtil;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.PlayerIdentifier;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.level.block.LPBlocks;
import network.rs485.logisticspipes.property.BitSetProperty;
import network.rs485.logisticspipes.property.IBitSet;
import network.rs485.logisticspipes.util.FuzzyUtil;
import network.rs485.logisticspipes.util.items.ItemStackLoader;

public class LogisticsCraftingTableBlockEntity extends LogisticsSolidBlockEntity
    implements Container, IGuiTileEntity, ISimpleInventoryEventHandler, IGuiOpenControler {

    public final BitSetProperty fuzzyFlags = new BitSetProperty(new BitSet(4 * (9 + 1)), "fuzzyBitSet");
    private final InvWrapper invWrapper = new InvWrapper(this);
    private final PlayerCollectionList guiWatcher = new PlayerCollectionList();
    public ItemIdentifierInventory inv = new ItemIdentifierInventory(18, "Crafting Resources", 64);
    public ItemIdentifierInventory matrix = new ItemIdentifierInventory(9, "Crafting Matrix", 1);
    public ItemIdentifierInventory resultInv = new ItemIdentifierInventory(1, "Crafting Result", 1);
    @Nullable
    public ItemIdentifier targetType = null;
    @Nullable
    private RecipeHolder<CraftingRecipe> cache;
    @Nullable
    private ServerPlayer fake;
    @Nullable
    private PlayerIdentifier placedBy = null;

    public LogisticsCraftingTableBlockEntity(BlockPos pos, BlockState state) {
        super(LPBlockEntityTypes.CRAFTING_TABLE.get(), pos, state);
        matrix.addListener(this);
    }

    @Nullable
    private HolderLookup.Provider getProvider() {
        if (getWorld() != null) {
            return getWorld().registryAccess();
        }
        var level = Minecraft.getInstance().level;
        if (level != null) {
            return level.registryAccess();
        }
        return null;
    }

    public void cacheRecipe() {
        ItemIdentifier oldTargetType = targetType;
        cache = null;
        resultInv.setItem(0, ItemStack.EMPTY);
        AutoCraftingContainer craftInv = new AutoCraftingContainer(placedBy);
        for (int i = 0; i < 9; i++) {
            craftInv.setItem(i, matrix.getItem(i));
        }
        List<RecipeHolder<CraftingRecipe>> list = new ArrayList<>();
        CraftingInput craftingInput = CraftingInput.of(3, 3, craftInv.getItems());
        for (RecipeHolder<CraftingRecipe> holder : CraftingUtil.getRecipeList()) {
            CraftingRecipe recipe = holder.value();

            if (recipe.matches(craftingInput, getWorld())) {
                list.add(holder);
            }
        }
        if (list.size() == 1) {
            cache = list.getFirst();
            resultInv.setItem(0, cache.value().assemble(craftingInput, getProvider()));
            targetType = null;
        } else if (list.size() > 1) {
            if (targetType != null) {
                for (RecipeHolder<CraftingRecipe> recipe : list) {
                    craftInv = new AutoCraftingContainer(placedBy);
                    for (int i = 0; i < 9; i++) {
                        craftInv.setItem(i, matrix.getItem(i));
                    }
                    craftingInput = CraftingInput.of(3, 3, craftInv.getItems());
                    ItemStack result = recipe.value().assemble(craftingInput, getProvider());
                    if (!result.isEmpty() && targetType.equals(ItemIdentifier.get(result))) {
                        resultInv.setItem(0, result);
                        cache = recipe;
                        break;
                    }
                }
            }
            if (cache == null) {
                for (RecipeHolder<CraftingRecipe> r : list) {
                    ItemStack result = r.value().assemble(craftingInput, getProvider());
                    if (!result.isEmpty()) {
                        cache = r;
                        resultInv.setItem(0, result);
                        targetType = ItemIdentifier.get(result);
                        break;
                    }
                }
            }
        } else {
            targetType = null;
        }
        if (((targetType == null && oldTargetType != null) || (targetType != null && !targetType.equals(oldTargetType)))
            && !guiWatcher.isEmpty() && MainProxy.isServer(getWorld())) {
            MainProxy.sendToPlayerList(
                PacketHandler.getPacket(CraftingSetType.class).setTargetType(targetType).setTilePos(this),
                guiWatcher);
        }
    }

    public void cycleRecipe(boolean down) {
        cacheRecipe();
        if (targetType == null) {
            return;
        }

        cache = null;
        AutoCraftingContainer craftInv = new AutoCraftingContainer(placedBy);

        for (int i = 0; i < 9; i++) {
            craftInv.setItem(i, matrix.getItem(i));
        }
        CraftingInput craftingInput = CraftingInput.of(3, 3, craftInv.getItems());
        List<RecipeHolder<CraftingRecipe>> list = new ArrayList<>();
        for (RecipeHolder<CraftingRecipe> r : CraftingUtil.getRecipeList()) {
            if (r.value().matches(craftingInput, getWorld())) {
                list.add(r);
            }
        }

        if (list.size() > 1) {
            boolean found = false;
            RecipeHolder<CraftingRecipe> prev = null;
            for (RecipeHolder<CraftingRecipe> recipe : list) {
                if (found) {
                    cache = recipe;
                    break;
                }
                craftInv = new AutoCraftingContainer(placedBy);
                for (int i = 0; i < 9; i++) {
                    craftInv.setItem(i, matrix.getItem(i));
                }
                craftingInput = CraftingInput.of(3, 3, craftInv.getItems());
                if (targetType != null && targetType.equals(
                    ItemIdentifier.get(recipe.value().assemble(craftingInput, getProvider())))) {
                    if (down) {
                        found = true;
                    } else {
                        if (prev == null) {
                            cache = list.getLast();
                        } else {
                            cache = prev;
                        }
                        break;
                    }
                }
                prev = recipe;
            }

            if (cache == null) {
                cache = list.getFirst();
            }

            craftInv = new AutoCraftingContainer(placedBy);
            for (int i = 0; i < 9; i++) {
                craftInv.setItem(i, matrix.getItem(i));
            }
            craftingInput = CraftingInput.of(3, 3, craftInv.getItems());

            if (cache != null) {
                targetType = ItemIdentifier.get(cache.value().assemble(craftingInput, getProvider()));
            }
        }

        if (!guiWatcher.isEmpty() && MainProxy.isServer(getWorld())) {
            MainProxy.sendToPlayerList(
                PacketHandler.getPacket(CraftingSetType.class).setTargetType(targetType).setTilePos(this),
                guiWatcher);
        }

        cacheRecipe();
    }

    public IBitSet outputFuzzy() {
        final int startIdx = 4 * 9; // after the 9th slot
        return fuzzyFlags.get(startIdx, startIdx + 3);
    }

    public IBitSet inputFuzzy(int slot) {
        final int startIdx = 4 * slot;
        return fuzzyFlags.get(startIdx, startIdx + 3);
    }

    public ItemStack getOutput(IResource wanted, IRoutedPowerProvider power) {
        boolean isFuzzy = isFuzzy();
        if (cache == null) {
            cacheRecipe();
            if (cache == null) {
                return ItemStack.EMPTY;
            }
        }
        int[] toUse = new int[9];
        int[] used = new int[inv.getContainerSize()];
        outer:
        for (int i = 0; i < 9; i++) {
            ItemIdentifierStack item = matrix.getIDStackInSlot(i);
            if (item == null) {
                toUse[i] = -1;
                continue;
            }
            ItemIdentifier ident = item.getItem();
            for (int j = 0; j < inv.getContainerSize(); j++) {
                item = inv.getIDStackInSlot(j);
                if (item == null) {
                    continue;
                }

                final boolean doItemsEqual = isFuzzy ?
                    (FuzzyUtil.INSTANCE
                        .fuzzyMatches(FuzzyUtil.INSTANCE.getter(inputFuzzy(i)), ident, item.getItem())) :
                    ident.equalsForCrafting(item.getItem());

                if (doItemsEqual && item.getStackSize() > used[j]) {
                    used[j]++;
                    toUse[i] = j;
                    continue outer;
                }
            }
            //Not enough material
            return ItemStack.EMPTY;
        }
        AutoCraftingContainer crafter = new AutoCraftingContainer(placedBy);
        for (int i = 0; i < 9; i++) {
            int j = toUse[i];
            if (j != -1) {
                crafter.setItem(i, inv.getItem(j));
            }
        }
        CraftingInput craftingInput = CraftingInput.of(3, 3, crafter.getItems());
        RecipeHolder<CraftingRecipe> recipe = cache;
        final ItemIdentifierStack outStack = Objects.requireNonNull(resultInv.getIDStackInSlot(0));
        if (!recipe.value().matches(craftingInput, getWorld())) {
            if (isFuzzy && outputFuzzy().nextSetBit(0) != -1) {
                recipe = null;
                for (RecipeHolder<CraftingRecipe> r : CraftingUtil.getRecipeList()) {

                    if (r.value().matches(craftingInput, getWorld()) && FuzzyUtil.INSTANCE
                        .fuzzyMatches(FuzzyUtil.INSTANCE.getter(outputFuzzy()), outStack.getItem(),
                            ItemIdentifier.get(r.value().assemble(craftingInput, getProvider())))) {
                        recipe = r;
                        break;
                    }
                }
                if (recipe == null) {
                    return ItemStack.EMPTY;
                }
            } else {
                return ItemStack.EMPTY; //Fix MystCraft
            }
        }
        ItemStack result = recipe.value().assemble(craftingInput, getProvider());
        if (result.isEmpty()) {
            return ItemStack.EMPTY;
        }
        if (isFuzzy && outputFuzzy().nextSetBit(0) != -1) {
            if (!FuzzyUtil.INSTANCE.fuzzyMatches(FuzzyUtil.INSTANCE.getter(outputFuzzy()), outStack.getItem(),
                ItemIdentifier.get(result))) {
                return ItemStack.EMPTY;
            }
            if (!FuzzyUtil.INSTANCE.fuzzyMatches(FuzzyUtil.INSTANCE.getter(outputFuzzy()), wanted.getAsItem(),
                ItemIdentifier.get(result))) {
                return ItemStack.EMPTY;
            }
        } else {
            if (!outStack.getItem().equalsWithoutNBT(ItemIdentifier.get(result))) {
                return ItemStack.EMPTY;
            }
            if (!wanted.matches(outStack.getItem(), IResource.MatchSettings.WITHOUT_NBT)) {
                return ItemStack.EMPTY;
            }
        }
        if (!power.useEnergy(LPConfigs.COMMON.LOGISTICS_CRAFTING_TABLE_POWER_USAGE.getAsInt())) {
            return ItemStack.EMPTY;
        }
        crafter = new AutoCraftingContainer(placedBy);
        for (int i = 0; i < 9; i++) {
            int j = toUse[i];
            if (j != -1) {
                crafter.setItem(i, inv.removeItem(j, 1));
            }
        }
        craftingInput = CraftingInput.of(3, 3, crafter.getItems());
        result = recipe.value().assemble(craftingInput, getWorld().registryAccess());
        if (fake == null) {
            fake = MainProxy.getFakePlayer(getWorld());
        }
        result = result.copy();
        result.onCraftedBy(fake, result.getCount());
        NonNullList<ItemStack> remaining = recipe.value().getRemainingItems(craftingInput);
        for (int i = 0; i < remaining.size(); i++) {
            ItemStack left = remaining.get(i);
            crafter.setItem(i, ItemStack.EMPTY);
            if (!left.isEmpty()) {
                left.setCount(inv.addCompressed(left, false));
                if (left.getCount() > 0) {
                    ItemIdentifierInventory.dropItems(level, left, getBlockPos());
                }
            }
        }
        for (int i = 0; i < fake.getInventory().getContainerSize(); i++) {
            ItemStack left = fake.getInventory().getItem(i);
            fake.getInventory().setItem(i, ItemStack.EMPTY);
            if (!left.isEmpty()) {
                left.setCount(inv.addCompressed(left, false));
                if (left.getCount() > 0) {
                    ItemIdentifierInventory.dropItems(level, left, getBlockPos());
                }
            }
        }
        return result;
    }

    @Override
    public void onBlockBreak() {
        inv.dropContents(level, getBlockPos());
    }

    @Override
    public void InventoryChanged(Container inventory) {
        if (inventory == matrix) {
            cacheRecipe();
            setChanged();
        }
    }

    public void handleNEIRecipePacket(NonNullList<ItemStack> content) {
        if (matrix.getContainerSize() != content.size()) {
            throw new IllegalStateException("Different sizes of matrix and inventory from packet");
        }
        for (int i = 0; i < content.size(); i++) {
            matrix.setItem(i, content.get(i));
        }
        cacheRecipe();
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inv.readFromNBT(tag, registries, "inv_");
        matrix.readFromNBT(tag, registries, "matrix_");
        if (tag.contains("placedBy")) {
            String name = tag.getStringOr("placedBy", "");
            placedBy = PlayerIdentifier.convertFromUsername(name);
        } else {
            placedBy = PlayerIdentifier.readFromNBT(tag, "placedBy");
        }
        fuzzyFlags.readFromNBT(tag, registries);
        if (tag.contains("targetType")) {
            targetType = ItemIdentifier
                .get(ItemStackLoader.loadAndFixItemStackFromNBT(tag.getCompoundOrEmpty("targetType"), registries));
        }
        cacheRecipe();
    }

    @Override
    public void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        inv.writeToNBT(tag, registries, "inv_");
        matrix.writeToNBT(tag, registries, "matrix_");
        if (placedBy != null) {
            placedBy.writeToNBT(tag, "placedBy");
        }
        fuzzyFlags.writeToNBT(tag, registries);
        if (targetType != null) {
            CompoundTag type = new CompoundTag();
            tag.put("targetType", targetType.makeNormalStack(1).save(registries, type));
        } else {
            tag.remove("targetType");
        }
    }

    @Nullable
    public IItemHandler getItemCap(@Nullable Direction side) {
        return invWrapper;
    }

    @Override
    public int getContainerSize() {
        return inv.getContainerSize();
    }

    @Override
    public boolean isEmpty() {
        return inv.isEmpty();
    }

    @Override
    public ItemStack getItem(int i) {
        return inv.getItem(i);
    }

    @Override
    public ItemStack removeItem(int i, int j) {
        return inv.removeItem(i, j);
    }

    @Override
    public ItemStack removeItemNoUpdate(int i) {
        return inv.removeItemNoUpdate(i);
    }

    @Override
    public void setItem(int i, ItemStack itemstack) {
        inv.setItem(i, itemstack);
    }

    @Override
    public int getMaxStackSize() {
        return inv.getMaxStackSize();
    }

    @Override
    public boolean stillValid(Player entityplayer) {
        return true;
    }

    @Override
    public void startOpen(Player player) {
    }

    @Override
    public void stopOpen(Player player) {
    }

    @Override
    public boolean canPlaceItem(int i, ItemStack itemstack) {
        if (i < 9 && i >= 0) {
            ItemIdentifierStack stack = matrix.getIDStackInSlot(i);
            if (stack != null && !itemstack.isEmpty()) {
                if (isFuzzy() && inputFuzzy(i).nextSetBit(0) != -1) {
                    return FuzzyUtil.INSTANCE.fuzzyMatches(FuzzyUtil.INSTANCE.getter(inputFuzzy(i)),
                        stack.getItem(),
                        ItemIdentifier.get(itemstack));
                }
                return stack.getItem().equalsWithoutNBT(ItemIdentifier.get(itemstack));
            }
        }
        return true;
    }

    @Override
    public void clearContent() {
    }

    public void placedBy(@Nullable LivingEntity par5EntityLivingBase) {
        if (par5EntityLivingBase instanceof Player) {
            placedBy = PlayerIdentifier.get((Player) par5EntityLivingBase);
        }
    }

    public boolean isFuzzy() {
        return level.getBlockState(worldPosition).is(LPBlocks.CRAFTER_FUZZY);
    }

    @Override
    public CoordinatesGuiProvider getGuiProvider() {
        return NewGuiHandler.getGui(AutoCraftingGui.class).setCraftingTable(this);
    }

    @Override
    public void guiOpenedByPlayer(Player player) {
        guiWatcher.add(player);
    }

    @Override
    public void guiClosedByPlayer(Player player) {
        guiWatcher.remove(player);
    }

    public String getName() {
        return "LogisticsCraftingTable";
    }

    @Override
    public void setRemoved() {
        super.setRemoved();
        fake = null;
    }
}
