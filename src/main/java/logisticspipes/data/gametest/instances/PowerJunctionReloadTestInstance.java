package logisticspipes.data.gametest.instances;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

import com.mojang.serialization.MapCodec;

import logisticspipes.data.gametest.LPGameTestInstances;
import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.entity.LogisticsPowerJunctionBlockEntity;

/**
 * A power junction keeps the energy it holds when it is saved and loaded back.
 */
public class PowerJunctionReloadTestInstance extends GameTestInstance {

    public static final MapCodec<PowerJunctionReloadTestInstance> CODEC =
        LPGameTestInstances.defaultCodec(PowerJunctionReloadTestInstance::new);

    private static final BlockPos JUNCTION = new BlockPos(1, 1, 1);

    public PowerJunctionReloadTestInstance(TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
    }

    @Override
    public void run(GameTestHelper helper) {
        helper.setBlock(JUNCTION, LPBlocks.POWER_JUNCTION.get());
        var junction = helper.getBlockEntity(JUNCTION, LogisticsPowerJunctionBlockEntity.class);
        // Neither empty nor full, so neither a lost value nor a clamped one looks right by accident
        junction.addEnergy(junction.getMaxStorage() / 3);
        int before = junction.getPowerLevel();

        LPTestHelper.reload(helper, JUNCTION);

        int after = helper.getBlockEntity(JUNCTION, LogisticsPowerJunctionBlockEntity.class).getPowerLevel();
        helper.assertTrue(after == before,
            Component.literal("The junction held %d before the reload and %d after".formatted(before, after)));
        helper.succeed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Power Junction Reload");
    }
}
