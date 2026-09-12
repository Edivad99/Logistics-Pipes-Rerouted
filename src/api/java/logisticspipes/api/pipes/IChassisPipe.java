package logisticspipes.api.pipes;

import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.Container;
import org.jspecify.annotations.Nullable;

public interface IChassisPipe {

    void nextOrientation();

    void setPointedOrientation(@Nullable Direction dir);

    @Nullable Direction getPointedOrientation();

    Container getModuleInventory(HolderLookup.Provider provider);

    int getChassisSize();
}
