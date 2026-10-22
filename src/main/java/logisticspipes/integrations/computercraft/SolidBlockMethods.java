package logisticspipes.integrations.computercraft;

import dan200.computercraft.api.lua.GenericSource;
import dan200.computercraft.api.lua.LuaFunction;

import logisticspipes.LPConstants;
import logisticspipes.interfaces.IPowerLevelDisplay;
import logisticspipes.world.level.block.entity.LogisticsPowerProviderBlockEntity;
import logisticspipes.world.level.block.entity.LogisticsSolidBlockEntity;

/**
 * What a script can call on Logistics Pipes' solid blocks. These are generic methods rather than a
 * peripheral of their own, so they join what CC: Tweaked already offers on the same block: the
 * crafting table stays an {@code inventory} and the power blocks an {@code energy_storage}.
 */
final class SolidBlockMethods implements GenericSource {

    @Override
    public String id() {
        return LPConstants.rl( "solid_block").toString();
    }

    @LuaFunction(mainThread = true)
    public static int getRotation(LogisticsSolidBlockEntity block) {
        return block.getRotation();
    }

    @LuaFunction(mainThread = true)
    public static int getPowerLevel(IPowerLevelDisplay block) {
        return block.getDisplayPowerLevel();
    }

    @LuaFunction(mainThread = true)
    public static int getMaxStorage(IPowerLevelDisplay block) {
        return block.getMaxStorage();
    }

    /**
     * The color of the laser to the machines the provider powers, as {@code 0xRRGGBB}.
     */
    @LuaFunction(mainThread = true)
    public static int getLaserColor(LogisticsPowerProviderBlockEntity block) {
        return block.getLaserColor();
    }
}
