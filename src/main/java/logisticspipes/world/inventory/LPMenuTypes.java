package logisticspipes.world.inventory;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.network.IContainerFactory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import logisticspipes.LPConstants;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;

public class LPMenuTypes {

    private static final DeferredRegister<MenuType<?>> deferredRegister =
        DeferredRegister.create(BuiltInRegistries.MENU, LPConstants.ID);

    public static void register(IEventBus modEventBus) {
        deferredRegister.register(modEventBus);
    }

    public static final DeferredHolder<MenuType<?>, MenuType<ProgramCompilerMenu>> PROGRAM_COMPILER =
        deferredRegister.register("program_compiler",
            () -> blockEntityMenu(LogisticsProgramCompilerBlockEntity.class, ProgramCompilerMenu::new));

    private static <T extends AbstractContainerMenu, E extends BlockEntity> MenuType<T>
    blockEntityMenu(Class<E> entityType, CustomMenuFactory<T, E> factory) {
        IContainerFactory<T> containerFactory = (id, inventory, packetBuffer) -> {
            BlockPos blockPos = packetBuffer.readBlockPos();
            BlockEntity entity = inventory.player.level().getBlockEntity(blockPos);
            if (entityType.isInstance(entity)) {
                return factory.create(id, inventory, entityType.cast(entity));
            }
            throw new IllegalStateException(
                "Cannot find block entity of type %s at [%s]".formatted(entityType.getName(), blockPos));
        };
        return new MenuType<>(containerFactory, FeatureFlags.DEFAULT_FLAGS);
    }

    private interface CustomMenuFactory<C extends AbstractContainerMenu, T> {

        C create(int id, Inventory inventory, T data);
    }
}
