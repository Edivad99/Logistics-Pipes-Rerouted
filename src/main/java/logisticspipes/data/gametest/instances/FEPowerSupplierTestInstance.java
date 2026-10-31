package logisticspipes.data.gametest.instances;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.GameTestSequence;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;

import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.transfer.transaction.Transaction;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestBlocks;
import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.upgrades.FEPowerSupplierUpgrade;
import logisticspipes.pipes.upgrades.PowerTransportationUpgrade;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.level.block.LPBlocks;

/**
 * An FE power provider on one pipe, and a pipe with an FE power supplier upgrade beside a machine.
 * The FE reaches the machine only through pipes that let power pass, and only a machine the pipe
 * connects to, one with an inventory: a block that only stores energy gets nothing.
 */
public class FEPowerSupplierTestInstance extends GameTestInstance {

    public static final MapCodec<FEPowerSupplierTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, FEPowerSupplierTestInstance::new));

    private static final BlockPos FIRST = new BlockPos(1, 1, 1);
    // Beside the pipe: a pipe does not connect to a power provider above or below it
    private static final BlockPos PROVIDER = FIRST.north();
    private static final BlockPos SECOND = new BlockPos(2, 1, 1);
    private static final int STORED = 100_000;
    /**
     * More than the supplier's buffer holds, so the machine has had a refill from the provider too.
     */
    private static final int DELIVERED = 20_000;

    private final Scenario scenario;

    public FEPowerSupplierTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        helper.setBlock(PROVIDER, LPBlocks.POWER_PROVIDER_FE.get());
        charge(helper);
        boolean onTheProvider = scenario == Scenario.ON_THE_PROVIDER || scenario == Scenario.ENERGY_ONLY;
        var supplierPos = onTheProvider ? FIRST : SECOND;
        helper.setBlock(supplierPos.south(),
            scenario == Scenario.ENERGY_ONLY ? LPTestBlocks.BATTERY.get() : LPTestBlocks.MACHINE.get());
        var receiver = helper.getBlockEntity(supplierPos.south(), LPTestBlocks.BatteryBlockEntity.class);
        var first = LPTestHelper.placePipe(helper, FIRST, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var second = LPTestHelper.placePipe(helper, SECOND, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var supplier = onTheProvider ? first : second;
        LPTestHelper.addUpgrade(supplier, FEPowerSupplierUpgrade.getName());
        if (scenario == Scenario.THROUGH_POWER_PASS) {
            LPTestHelper.addUpgrade(first, PowerTransportationUpgrade.getName());
        }

        GameTestSequence sequence = helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, first, second);
                LPTestHelper.assertPowered(helper, supplier);
            });
        if (scenario.reaches) {
            sequence = sequence.thenWaitUntil(() -> helper.assertTrue(
                receiver.energy.getAmountAsLong() >= DELIVERED,
                Component.literal("The block beside the supplier should have got %d FE, has %d".formatted(DELIVERED,
                    receiver.energy.getAmountAsLong()))));
        } else {
            sequence = sequence
                .thenIdle(100)
                .thenExecute(() -> helper.assertTrue(receiver.energy.getAmountAsLong() == 0,
                    Component.literal("The block beside the supplier should have got no FE, has %d".formatted(
                        receiver.energy.getAmountAsLong()))));
        }
        sequence.thenSucceed();
    }

    private static void charge(GameTestHelper helper) {
        var level = helper.getLevel();
        var storage = level.getCapability(Capabilities.Energy.BLOCK, helper.absolutePos(PROVIDER), null);
        if (storage == null) {
            throw helper.assertionException(PROVIDER, "The FE power provider has no energy storage");
        }
        int charged = 0;
        while (charged < STORED) {
            try (Transaction transaction = Transaction.openRoot()) {
                int inserted = storage.insert(STORED - charged, transaction);
                transaction.commit();
                if (inserted == 0) {
                    throw helper.assertionException(PROVIDER, "The FE power provider took only %d FE", charged);
                }
                charged += inserted;
            }
        }
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("FE Power Supplier: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * The pipe with the upgrade is the one on the provider.
         */
        ON_THE_PROVIDER(true),
        /**
         * The pipe on the provider has a power transportation upgrade, and passes the FE on.
         */
        THROUGH_POWER_PASS(true),
        /**
         * The pipe on the provider has no upgrade, and the FE stops there.
         */
        NO_POWER_PASS(false),
        /**
         * The pipe with the upgrade is on the provider, but beside it is a block with no
         * inventory, which the pipe does not connect to.
         */
        ENERGY_ONLY(false);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final boolean reaches;

        Scenario(boolean reaches) {
            this.reaches = reaches;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
