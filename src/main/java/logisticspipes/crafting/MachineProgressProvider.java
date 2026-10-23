package logisticspipes.crafting;

import java.util.List;

import net.minecraft.world.level.block.entity.BlockEntity;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.event.RegisterProgressProvidersEvent;
import logisticspipes.api.provider.IGenericProgressProvider;
import logisticspipes.api.provider.IProgressProvider;

public final class MachineProgressProvider {

    private static @Nullable MachineProgressProvider instance;

    private final List<IGenericProgressProvider> providers;

    private MachineProgressProvider(List<IGenericProgressProvider> providers) {
        this.providers = providers;
    }

    /**
     * Collects everything registered on {@code event}, which is closed from here on: the readers are
     * collected during startup and read from the server tick, with no lock between the two.
     */
    public static void load(RegisterProgressProvidersEvent event) {
        instance = new MachineProgressProvider(event.registered());
    }

    /**
     * The progress providers collected at startup.
     *
     * @throws IllegalStateException before startup collected them
     */
    public static MachineProgressProvider get() {
        MachineProgressProvider current = instance;
        if (current == null) {
            throw new IllegalStateException("MachineProgressProvider is not loaded yet");
        }
        return current;
    }

    public byte getProgressForBlockEntity(BlockEntity blockEntity) {
        if (blockEntity instanceof IProgressProvider provider) {
            return provider.getMachineProgressForLP();
        }
        for (IGenericProgressProvider provider : providers) {
            if (provider.isType(blockEntity)) {
                return provider.getProgress(blockEntity);
            }
        }
        return 0;
    }
}
