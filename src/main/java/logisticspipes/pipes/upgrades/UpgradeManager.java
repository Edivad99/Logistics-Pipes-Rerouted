package logisticspipes.pipes.upgrades;

import java.util.EnumSet;
import java.util.Objects;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.Container;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import net.neoforged.neoforge.common.util.ValueIOSerializable;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.IPipeUpgradeManager;
import logisticspipes.interfaces.IScreenOpenController;
import logisticspipes.interfaces.ISlotUpgradeManager;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.RouterManager;
import logisticspipes.utils.ISimpleInventoryEventHandler;
import logisticspipes.utils.PlayerCollectionList;
import logisticspipes.utils.item.ItemIdentifier;
import logisticspipes.utils.item.SimpleStackInventory;
import logisticspipes.world.item.ItemUpgrade;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.item.component.LPDataComponents;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

public class UpgradeManager
    implements ISimpleInventoryEventHandler, ISlotUpgradeManager, IPipeUpgradeManager, ValueIOSerializable {

    public final SimpleStackInventory inv = new SimpleStackInventory(9, "UpgradeInventory", 16);
    public final SimpleStackInventory sneakyInv = new SimpleStackInventory(9, "SneakyUpgradeInventory", 1);
    public final SimpleStackInventory secInv = new SimpleStackInventory(1, "SecurityInventory", 16);
    private final EnumSet<Direction> disconnectedSides = EnumSet.noneOf(Direction.class);
    private final @Nullable IPipeUpgrade[] upgrades = new @Nullable IPipeUpgrade[9];
    private final @Nullable IPipeUpgrade[] sneakyUpgrades = new @Nullable IPipeUpgrade[9];
    private final CoreRoutedPipe pipe;
    private int securityDelay = 0;
    /* cached attributes */
    private @Nullable Direction sneakyOrientation = null;
    private final @Nullable Direction[] combinedSneakyOrientation = new @Nullable Direction[9];
    private int speedUpgradeCount = 0;
    private boolean isAdvancedCrafter = false;
    private boolean isFuzzyUpgrade = false;
    private boolean isCombinedSneakyUpgrade = false;
    private int liquidCrafter = 0;
    private boolean hasByproductExtractor = false;
    private @Nullable UUID uuid = null;
    private @Nullable String uuidS = null;
    private boolean hasPatternUpgrade = false;
    private boolean hasPowerPassUpgrade = false;
    private boolean hasFEPowerUpgrade = false;
    private boolean hasCCRemoteControlUpgrade = false;
    private boolean hasCraftingMonitoringUpgrade = false;
    private boolean hasOpaqueUpgrade = false;
    private int craftingCleanup = 0;
    private boolean hasLogicControl = false;
    private boolean hasUpgradeModuleUpgrade = false;
    private int actionSpeedUpgrade = 0;
    private int itemExtractionUpgrade = 0;
    private int itemStackExtractionUpgrade = 0;

    private boolean[] guiUpgrades = new boolean[18];

    private boolean needsContainerPositionUpdate = false;

    public UpgradeManager(CoreRoutedPipe pipe) {
        this.pipe = pipe;
        inv.addListener(this);
        sneakyInv.addListener(this);
        secInv.addListener(this);
    }

    public void deserialize(ValueInput input) {
        inv.deserialize(input, "UpgradeInventory_");
        sneakyInv.deserialize(input, "SneakyUpgradeInventory_");
        secInv.deserialize(input, "SecurityInventory_");

        if (!sneakyInv.getItem(8).isEmpty()) {
            if (sneakyInv.getItem(8).is(LPItems.SECURITY_CARD)) {
                secInv.setItem(0, sneakyInv.getItem(8));
                sneakyInv.setItem(8, ItemStack.EMPTY);
            }
        }

        InventoryChanged(inv);
    }

    public void serialize(ValueOutput output) {
        inv.serialize(output, "UpgradeInventory_");
        sneakyInv.serialize(output, "SneakyUpgradeInventory_");
        secInv.serialize(output, "SecurityInventory_");
        InventoryChanged(inv);
    }

    private boolean updateModule(int slot, @Nullable IPipeUpgrade[] upgrades, Container inv) {
        ItemStack stack = inv.getItem(slot);
        final IPipeUpgrade upgrade = stack.getItem() instanceof ItemUpgrade itemUpgrade
            ? itemUpgrade.getUpgradeForItem(stack, upgrades[slot])
            : null;
        upgrades[slot] = upgrade;
        if (upgrade == null) {
            inv.setItem(slot, ItemStack.EMPTY);
            return false;
        }
        return upgrade.needsUpdate();
    }

    private boolean removeUpgrade(int slot, @Nullable IPipeUpgrade[] upgrades) {
        final IPipeUpgrade upgrade = upgrades[slot];
        upgrades[slot] = null;
        return upgrade != null && upgrade.needsUpdate();
    }

    @Override
    public void InventoryChanged(Container inventory) {
        boolean needUpdate = false;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (!item.isEmpty()) {
                needUpdate |= updateModule(i, upgrades, inv);
            } else {
                needUpdate |= removeUpgrade(i, upgrades);
            }
        }
        //update sneaky direction, speed upgrade count and disconnection
        sneakyOrientation = null;
        speedUpgradeCount = 0;
        isAdvancedCrafter = false;
        isFuzzyUpgrade = false;
        boolean combinedBuffer = isCombinedSneakyUpgrade;
        isCombinedSneakyUpgrade = false;
        liquidCrafter = 0;
        disconnectedSides.clear();
        hasByproductExtractor = false;
        hasPatternUpgrade = false;
        hasPowerPassUpgrade = false;
        hasFEPowerUpgrade = false;
        hasCCRemoteControlUpgrade = false;
        hasCraftingMonitoringUpgrade = false;
        hasOpaqueUpgrade = false;
        craftingCleanup = 0;
        hasLogicControl = false;
        hasUpgradeModuleUpgrade = false;
        actionSpeedUpgrade = 0;
        itemExtractionUpgrade = 0;
        itemStackExtractionUpgrade = 0;

        guiUpgrades = new boolean[18];
        for (int i = 0; i < upgrades.length; i++) {
            IPipeUpgrade upgrade = upgrades[i];
            if (upgrade instanceof SneakyUpgradeConfig && sneakyOrientation == null && !isCombinedSneakyUpgrade) {
                sneakyOrientation = SneakyUpgradeConfig.getSide(getInv().getItem(i));
            } else if (upgrade instanceof SpeedUpgrade) {
                speedUpgradeCount += inv.getItem(i).getCount();
            } else if (upgrade instanceof ConnectionUpgradeConfig) {
                ConnectionUpgradeConfig.getSides(getInv().getItem(i)).forEach(disconnectedSides::add);
            } else if (upgrade instanceof AdvancedSatelliteUpgrade) {
                isAdvancedCrafter = true;
            } else if (upgrade instanceof FuzzyUpgrade) {
                isFuzzyUpgrade = true;
            } else if (upgrade instanceof CombinedSneakyUpgrade && sneakyOrientation == null) {
                isCombinedSneakyUpgrade = true;
            } else if (upgrade instanceof FluidCraftingUpgrade) {
                liquidCrafter += inv.getItem(i).getCount();
            } else if (upgrade instanceof CraftingByproductUpgrade) {
                hasByproductExtractor = true;
            } else if (upgrade instanceof PatternUpgrade) {
                hasPatternUpgrade = true;
            } else if (upgrade instanceof PowerTransportationUpgrade) {
                hasPowerPassUpgrade = true;
            } else if (upgrade instanceof FEPowerSupplierUpgrade) {
                hasFEPowerUpgrade = true;
            } else if (upgrade instanceof CCRemoteControlUpgrade) {
                hasCCRemoteControlUpgrade = true;
            } else if (upgrade instanceof CraftingMonitoringUpgrade) {
                hasCraftingMonitoringUpgrade = true;
            } else if (upgrade instanceof OpaqueUpgrade) {
                hasOpaqueUpgrade = true;
            } else if (upgrade instanceof CraftingCleanupUpgrade) {
                craftingCleanup += inv.getItem(i).getCount();
            } else if (upgrade instanceof LogicControllerUpgrade) {
                hasLogicControl = true;
            } else if (upgrade instanceof UpgradeModuleUpgrade) {
                hasUpgradeModuleUpgrade = true;
            } else if (upgrade instanceof ActionSpeedUpgrade) {
                actionSpeedUpgrade += inv.getItem(i).getCount();
            } else if (upgrade instanceof ItemExtractionUpgrade) {
                itemExtractionUpgrade += inv.getItem(i).getCount();
            } else if (upgrade instanceof ItemStackExtractionUpgrade) {
                itemStackExtractionUpgrade += inv.getItem(i).getCount();
            }
            if (upgrade instanceof IConfigPipeUpgrade) {
                guiUpgrades[i] = true;
            }
        }
        liquidCrafter = Math.min(liquidCrafter, ItemUpgrade.MAX_LIQUID_CRAFTER);
        craftingCleanup = Math.min(craftingCleanup, ItemUpgrade.MAX_CRAFTING_CLEANUP);
        itemExtractionUpgrade = Math.min(itemExtractionUpgrade, ItemUpgrade.MAX_ITEM_EXTRACTION);
        itemStackExtractionUpgrade = Math.min(itemStackExtractionUpgrade, ItemUpgrade.MAX_ITEM_STACK_EXTRACTION);
        if (combinedBuffer != isCombinedSneakyUpgrade) {
            needsContainerPositionUpdate = true;
        }
        for (int i = 0; i < sneakyInv.getContainerSize(); i++) {
            ItemStack item = sneakyInv.getItem(i);
            if (!item.isEmpty()) {
                needUpdate |= updateModule(i, sneakyUpgrades, sneakyInv);
            } else {
                needUpdate |= removeUpgrade(i, sneakyUpgrades);
            }
        }
        for (int i = 0; i < sneakyUpgrades.length; i++) {
            IPipeUpgrade upgrade = sneakyUpgrades[i];
            if (upgrade instanceof SneakyUpgradeConfig) {
                ItemStack stack = sneakyInv.getItem(i);
                combinedSneakyOrientation[i] = SneakyUpgradeConfig.getSide(stack);
            }
            if (upgrade instanceof IConfigPipeUpgrade) {
                guiUpgrades[i + 9] = true;
            }
        }
        if (needUpdate) {
            final Level level = pipe.getLevel();
            if (level != null && !level.isClientSide()) {
                pipe.connectionUpdate();
                final LogisticsGenericPipeBlockEntity container = pipe.getContainer();
                if (container != null) {
                    container.sendUpdateToClient();
                }
            }
        }
        uuid = null;
        uuidS = null;
        ItemStack stack = secInv.getItem(0);
        if (stack.isEmpty()) {
            return;
        }
        if (!stack.is(LPItems.SECURITY_CARD)) {
            return;
        }

        if (!stack.has(LPDataComponents.UUID)) {
            return;
        }

        uuid = Objects.requireNonNull(stack.get(LPDataComponents.UUID));
        uuidS = uuid.toString();
    }

    /* Special implementations */

    @Override
    public boolean hasSneakyUpgrade() {
        return sneakyOrientation != null;
    }

    @Override
    @Nullable
    public Direction getSneakyOrientation() {
        return sneakyOrientation;
    }

    @Override
    public int getSpeedUpgradeCount() {
        return speedUpgradeCount;
    }

    @Override
    public boolean hasCombinedSneakyUpgrade() {
        return isCombinedSneakyUpgrade;
    }

    @Override
    public @Nullable Direction @Nullable [] getCombinedSneakyOrientation() {
        return combinedSneakyOrientation;
    }

    public IScreenOpenController getGuiController() {
        return new IScreenOpenController() {

            final PlayerCollectionList players = new PlayerCollectionList();

            @Override
            public void screenOpenedByPlayer(Player player) {
                players.add(player);
            }

            @Override
            public void screenClosedByPlayer(Player player) {
                players.remove(player);
                final Level level = pipe.getLevel();
                if (players.isEmpty() && !isCombinedSneakyUpgrade && level != null) {
                    sneakyInv.dropContents(level, pipe.getPos());
                }
            }
        };
    }

    public void dropUpgrades() {
        final Level level = pipe.getLevel();
        if (level == null) {
            return;
        }
        inv.dropContents(level, pipe.getPos());
        sneakyInv.dropContents(level, pipe.getPos());
    }

    @Override
    public boolean isSideDisconnected(Direction side) {
        return disconnectedSides.contains(side);
    }

    public boolean tryIserting(Level level, Player entityplayer) {
        ItemStack itemStackInMainHand = entityplayer.getItemBySlot(EquipmentSlot.MAINHAND);
        if (!itemStackInMainHand.isEmpty() && itemStackInMainHand.getItem() instanceof ItemUpgrade itemUpgrade) {
            if (level.isClientSide()) {
                return true;
            }
            IPipeUpgrade upgrade = itemUpgrade.getUpgradeForItem(itemStackInMainHand, null);
            if (upgrade != null && upgrade.isAllowedForPipe(pipe)) {
                if (isCombinedSneakyUpgrade) {
                    if (upgrade instanceof SneakyUpgradeConfig) {
                        if (insertIntInv(entityplayer, sneakyInv)) {
                            return true;
                        }
                    }
                }
                if (insertIntInv(entityplayer, inv)) {
                    return true;
                }
            }
        }
        if (itemStackInMainHand.is(LPItems.SECURITY_CARD)) {
            if (level.isClientSide()) {
                return true;
            }
            if (secInv.getItem(0).isEmpty()) {
                ItemStack newItem = itemStackInMainHand.split(1);
                secInv.setItem(0, newItem);
                InventoryChanged(secInv);
                return true;
            }
        }
        return false;
    }

    private boolean insertIntInv(Player entityplayer, SimpleStackInventory inv) {
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack item = inv.getItem(i);
            if (item.isEmpty()) {
                inv.setItem(i, entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).split(1));
                InventoryChanged(inv);
                return true;
            } else if (ItemIdentifier.get(item)
                .equals(ItemIdentifier.get(entityplayer.getItemBySlot(EquipmentSlot.MAINHAND)))) {
                if (item.getCount() < inv.getMaxStackSize()) {
                    item.grow(1);
                    entityplayer.getItemBySlot(EquipmentSlot.MAINHAND).split(1);
                    inv.setItem(i, item);
                    InventoryChanged(inv);
                    return true;
                }
            }
        }
        return false;
    }

    @Nullable
    public UUID getSecurityID() {
        return uuid;
    }

    public void insetSecurityID(UUID id) {
        ItemStack stack = new ItemStack(LPItems.SECURITY_CARD.get(), 1);
        stack.set(LPDataComponents.UUID, id);
        secInv.setItem(0, stack);
        InventoryChanged(secInv);
    }

    public void securityTick() {
        final String securityId = uuidS;
        if (securityId != null) {
            if (!RouterManager.get().isAuthorized(securityId)) {
                securityDelay++;
            } else {
                securityDelay = 0;
            }
            if (securityDelay > 20) {
                secInv.clearInventorySlotContents(0);
                InventoryChanged(secInv);
            }
        }
    }

    @Override
    public boolean isAdvancedSatelliteCrafter() {
        return isAdvancedCrafter;
    }

    @Override
    public boolean isFuzzyUpgrade() {
        return isFuzzyUpgrade;
    }

    @Override
    public int getFluidCrafter() {
        return liquidCrafter;
    }

    @Override
    public boolean hasByproductExtractor() {
        return hasByproductExtractor;
    }

    @Override
    public boolean hasPatternUpgrade() {
        return hasPatternUpgrade;
    }

    @Override
    public boolean hasPowerPassUpgrade() {
        return hasPowerPassUpgrade || hasFEPowerUpgrade;
    }

    @Override
    public boolean hasFEPowerSupplierUpgrade() {
        return hasFEPowerUpgrade;
    }

    @Override
    public boolean hasCCRemoteControlUpgrade() {
        return hasCCRemoteControlUpgrade;
    }

    @Override
    public boolean hasCraftingMonitoringUpgrade() {
        return hasCraftingMonitoringUpgrade;
    }

    @Override
    public boolean isOpaque() {
        return hasOpaqueUpgrade;
    }

    @Override
    public int getCrafterCleanup() {
        return craftingCleanup;
    }

    @Override
    public boolean hasUpgradeModuleUpgrade() {
        return hasUpgradeModuleUpgrade;
    }

    @Override
    public boolean hasOwnSneakyUpgrade() {
        return false;
    }

    public boolean hasGuiUpgrade(int i) {
        return guiUpgrades[i];
    }

    @Nullable
    public IPipeUpgrade getUpgrade(int i) {
        if (i < upgrades.length) {
            return upgrades[i];
        } else {
            return sneakyUpgrades[i - upgrades.length];
        }
    }

    @Override
    public BlockPos getPipePosition() {
        return pipe.getPos();
    }

    @Override
    public int getActionSpeedUpgrade() {
        return actionSpeedUpgrade;
    }

    @Override
    public int getItemExtractionUpgrade() {
        return itemExtractionUpgrade;
    }

    @Override
    public int getItemStackExtractionUpgrade() {
        return itemStackExtractionUpgrade;
    }

    @Override
    public SimpleStackInventory getInv() {
        return this.inv;
    }

}
