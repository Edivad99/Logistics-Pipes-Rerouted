package logisticspipes.network;

import java.util.Objects;
import java.util.Optional;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.item.ItemStack;

import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;

import org.jspecify.annotations.Nullable;

import logisticspipes.LogisticsPipes;
import logisticspipes.gui.GuiCraftingPipe;
import logisticspipes.gui.modules.ModuleBaseGui;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.pipes.PipeLogisticsChassis;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.gui.DummyModuleContainer;
import logisticspipes.world.inventory.ModuleMenu;
import logisticspipes.world.item.ItemModule;

import network.rs485.logisticspipes.inventory.container.LPBaseContainer;

public record ModuleTarget(
        BlockPos pos,
        Optional<ModulePositionType> slot,
        int positionInt
) {

    public static final StreamCodec<RegistryFriendlyByteBuf, ModuleTarget> STREAM_CODEC =
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ModuleTarget::pos,
                    ByteBufCodecs.optional(NeoForgeStreamCodecs.enumCodec(ModulePositionType.class)),
                    ModuleTarget::slot,
                    ByteBufCodecs.VAR_INT, ModuleTarget::positionInt,
                    ModuleTarget::new);

    /**
     * The target a module currently occupies, for the sending side.
     *
     * <p>A module held in hand has no position, and asking one for its block pos throws in debug
     * builds, so the slot decides whether the position is meaningful at all.
     */
    public static ModuleTarget of(LogisticsModule module) {
        final ModulePositionType slot = module.getSlot();
        final boolean inWorld = slot != null && slot.isInWorld();
        return new ModuleTarget(
                inWorld ? Objects.requireNonNullElse(module.getBlockPos(), BlockPos.ZERO) : BlockPos.ZERO,
                Optional.ofNullable(slot),
                module.getPositionInt());
    }

    /**
     * The stack a module held in hand lives in, so its own slot can be locked while its screen is
     * open; empty for a module that sits in the world.
     */
    public ItemStack heldStack(Inventory inventory) {
        return slot.orElse(null) == ModulePositionType.IN_HAND
                ? inventory.getItem(positionInt)
                : ItemStack.EMPTY;
    }

    /**
     * The type may be a module class or an interface several modules implement, such as
     * {@code SneakyDirection}; anything the module is not an instance of resolves to null.
     */
    public <T> @Nullable T resolve(Player player, Class<T> clazz) {
        final LogisticsModule module = find(player);
        if (module == null || !clazz.isInstance(module)) {
            LogisticsPipes.LOG.debug("Dropping a message for {} at {}: no such module", clazz.getSimpleName(), pos);
            return null;
        }
        return clazz.cast(module);
    }

    private @Nullable LogisticsModule find(Player player) {
        final ModulePositionType type = slot.orElse(null);
        if (type == ModulePositionType.IN_HAND) {
            return inHand(player);
        }
        final LogisticsTileGenericPipe pipe = TargetLookup.blockEntityAt(player, pos, LogisticsTileGenericPipe.class);
        if (pipe == null || !(pipe.pipe instanceof CoreRoutedPipe routed)) {
            return null;
        }
        if (type == ModulePositionType.IN_PIPE) {
            return routed.getLogisticsModule();
        }
        return pipe.isInitialized() && routed instanceof PipeLogisticsChassis chassis
                ? chassis.getSubModule(positionInt)
                : null;
    }

    private @Nullable LogisticsModule inHand(Player player) {
        if (player instanceof ServerPlayer) {
            // A module in hand has no position to look it up by, so the module being configured is
            // the one its open menu holds -- rebuilding it from the stack would drop the edits made
            // since the screen opened.
            if (player.containerMenu instanceof DummyModuleContainer dummy) {
                return dummy.getModule();
            }
            if (player.containerMenu instanceof ModuleMenu menu) {
                return menu.getModule();
            }
            if (player.containerMenu instanceof LPBaseContainer<?> menu) {
                return menu.getModule();
            }
            if (player.containerMenu instanceof InventoryMenu) {
                return ItemModule.getLogisticsModule(player, positionInt);
            }
            return null;
        }
        final LogisticsModule fromScreen = fromOpenScreen();
        return fromScreen != null ? fromScreen : ItemModule.getLogisticsModule(player, positionInt);
    }

    /**
     * A module held in hand has no position to look up: the only thing that knows which one it is
     * is the screen the player has open.
     *
     * <p>Reached directly rather than through the proxy, which is on its way out; the branch above
     * is what keeps this off the server, the same arrangement {@code PacketHandler} uses.
     */
    private static @Nullable LogisticsModule fromOpenScreen() {
        final var screen = Minecraft.getInstance().screen;
        if (screen instanceof AbstractContainerScreen<?> containerScreen) {
            if (containerScreen.getMenu() instanceof ModuleMenu menu) {
                return menu.getModule();
            }
            if (containerScreen.getMenu() instanceof LPBaseContainer<?> menu) {
                return menu.getModule();
            }
        }
        if (screen instanceof ModuleBaseGui gui) {
            return gui.getModule();
        }
        if (screen instanceof GuiCraftingPipe gui) {
            return gui.getCraftingModule();
        }
        return null;
    }
}
