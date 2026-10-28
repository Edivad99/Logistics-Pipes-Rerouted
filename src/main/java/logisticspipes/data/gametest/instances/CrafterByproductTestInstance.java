package logisticspipes.data.gametest.instances;

import java.util.Locale;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.upgrades.CraftingByproductUpgrade;
import logisticspipes.world.item.LPItems;

/**
 * A crafting pipe on a machine that turns a log into four planks and a stick: the test plays the
 * machine, swapping the log for its output once it lands in the barrel. With a byproduct upgrade
 * the crafting pipe takes the stick out too and sends it off. It does not offer the stick to
 * requests: only extras of what it crafts are offered.
 */
public class CrafterByproductTestInstance extends GameTestInstance {

    public static final MapCodec<CrafterByproductTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, CrafterByproductTestInstance::new));

    private static final BlockPos PROVIDER = new BlockPos(1, 1, 1);
    private static final BlockPos CRAFTER = new BlockPos(2, 1, 1);
    private static final BlockPos DEFAULT_ROUTE = new BlockPos(3, 1, 1);
    private static final BlockPos REQUESTER = new BlockPos(4, 1, 1);

    private final Scenario scenario;

    public CrafterByproductTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(), new ItemStack(Items.OAK_LOG));
        var machine = LPTestHelper.placeBarrel(helper, CRAFTER.south());
        var defaultBarrel = LPTestHelper.placeBarrel(helper, DEFAULT_ROUTE.south());
        var requesterBarrel = LPTestHelper.placeBarrel(helper, REQUESTER.south());
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var crafter = LPTestHelper.placePipe(helper, CRAFTER, new PipeItemsCraftingLogistics(LPItems.PIPE_CRAFTING.get()));
        var defaultRoute = LPTestHelper.placePipe(helper, DEFAULT_ROUTE, new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        defaultRoute.getLogisticsModule().setDefaultRoute(true);
        var requester = LPTestHelper.placePipe(helper, REQUESTER, new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));
        var module = crafter.getLogisticsModule();
        module.dummyInventory.setItem(0, new ItemStack(Items.OAK_LOG));
        module.dummyInventory.setItem(9, new ItemStack(Items.OAK_PLANKS, 4));
        module.dummyInventory.setItem(10, new ItemStack(Items.STICK));
        if (scenario.upgrade) {
            LPTestHelper.addUpgrade(crafter, CraftingByproductUpgrade.getName());
        }

        int sticksToDefault = scenario.upgrade ? 1 : 0;
        helper.startSequence()
            .thenWaitUntil(() -> {
                LPTestHelper.assertRouted(helper, requester, crafter);
                LPTestHelper.assertRouted(helper, crafter, provider);
                LPTestHelper.assertRouted(helper, crafter, defaultRoute);
                LPTestHelper.assertPowered(helper, crafter);
                LPTestHelper.assertPowered(helper, provider);
                LPTestHelper.assertPowered(helper, defaultRoute);
                LPTestHelper.assertInterestedIn(helper, crafter, Items.OAK_PLANKS);
            })
            .thenExecute(() -> helper.assertTrue(LPTestHelper.request(requester, new ItemStack(Items.OAK_PLANKS, 4)),
                Component.literal("The request for planks should have been accepted")))
            // The machine at work
            .thenWaitUntil(() -> LPTestHelper.assertCount(helper, machine, Items.OAK_LOG, 1, "Machine"))
            .thenExecute(() -> {
                machine.setItem(0, new ItemStack(Items.OAK_PLANKS, 4));
                machine.setItem(1, new ItemStack(Items.STICK));
            })
            .thenWaitUntil(() -> {
                LPTestHelper.assertCount(helper, requesterBarrel, Items.OAK_PLANKS, 4, "Requester");
                LPTestHelper.assertCount(helper, defaultBarrel, Items.STICK, sticksToDefault, "Default route");
            })
            .thenIdle(40)
            .thenExecute(() -> {
                LPTestHelper.assertCount(helper, providerBarrel, Items.OAK_LOG, 0, "Provider");
                LPTestHelper.assertCount(helper, machine, Items.OAK_PLANKS, 0, "Machine");
                LPTestHelper.assertCount(helper, machine, Items.STICK, scenario.upgrade ? 0 : 1, "Machine");
                LPTestHelper.assertOnly(helper, requesterBarrel, Items.OAK_PLANKS, "Requester");
                LPTestHelper.assertCount(helper, defaultBarrel, Items.STICK, sticksToDefault, "Default route");
                LPTestHelper.assertCount(helper, defaultBarrel, Items.OAK_PLANKS, 0, "Default route");
            })
            .thenSucceed();
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Crafter Byproduct: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Without the upgrade the stick stays in the machine.
         */
        NO_UPGRADE(false),
        /**
         * With the upgrade the stick goes off to the default route.
         */
        TO_NETWORK(true);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final boolean upgrade;

        Scenario(boolean upgrade) {
            this.upgrade = upgrade;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
