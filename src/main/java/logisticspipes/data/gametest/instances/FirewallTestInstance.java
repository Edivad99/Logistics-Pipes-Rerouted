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
import logisticspipes.pipes.PipeItemsFirewall;
import logisticspipes.pipes.PipeItemsFirewall.FirewallFlags;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.world.item.LPItems;

/**
 * A firewall pipe between a requester and a provider of cobblestone and dirt, its filter listing
 * cobblestone. Which of the two the requester can have depends on the firewall's flags.
 */
public class FirewallTestInstance extends GameTestInstance {

    public static final MapCodec<FirewallTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, FirewallTestInstance::new));

    private static final int STOCK = 16;
    private static final BlockPos PROVIDER = new BlockPos(2, 1, 1);
    private static final BlockPos FIREWALL = new BlockPos(4, 1, 1);
    private static final BlockPos REQUESTER = new BlockPos(6, 1, 1);

    private final Scenario scenario;

    public FirewallTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        if (scenario.powerOnProviderSide) {
            LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        }
        LPTestHelper.placePowerJunction(helper, new BlockPos(8, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(),
            new ItemStack(Items.COBBLESTONE, STOCK), new ItemStack(Items.DIRT, STOCK));
        var requesterBarrel = LPTestHelper.placeBarrel(helper, REQUESTER.south());
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var provider = LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        LPTestHelper.placePipe(helper, new BlockPos(3, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var firewall = LPTestHelper.placePipe(helper, FIREWALL, new PipeItemsFirewall(LPItems.PIPE_FIREWALL.get()));
        firewall.inv.setItem(0, new ItemStack(Items.COBBLESTONE));
        firewall.setFlags(scenario.flags);
        LPTestHelper.placePipe(helper, new BlockPos(5, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        var requester = LPTestHelper.placePipe(helper, REQUESTER, new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));
        LPTestHelper.placePipe(helper, new BlockPos(7, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));

        if (scenario.powerOnProviderSide) {
            helper.startSequence()
                .thenWaitUntil(() -> {
                    LPTestHelper.assertRouted(helper, requester, provider);
                    LPTestHelper.assertPowered(helper, provider);
                    LPTestHelper.assertPowered(helper, requester);
                })
                .thenExecute(() -> {
                    boolean cobblestone = LPTestHelper.request(requester, new ItemStack(Items.COBBLESTONE, STOCK));
                    boolean dirt = LPTestHelper.request(requester, new ItemStack(Items.DIRT, STOCK));
                    helper.assertTrue(cobblestone == scenario.cobblestonePasses && dirt == scenario.dirtPasses,
                        Component.literal("Through the firewall cobblestone should %s and dirt should %s".formatted(
                            scenario.cobblestonePasses ? "pass" : "not pass", scenario.dirtPasses ? "pass" : "not pass")));
                })
                .thenWaitUntil(() -> {
                    LPTestHelper.assertCount(helper, requesterBarrel, Items.COBBLESTONE,
                        scenario.cobblestonePasses ? STOCK : 0, "Requester");
                    LPTestHelper.assertCount(helper, requesterBarrel, Items.DIRT, scenario.dirtPasses ? STOCK : 0, "Requester");
                })
                .thenExecute(() -> {
                    LPTestHelper.assertCount(helper, providerBarrel, Items.COBBLESTONE,
                        scenario.cobblestonePasses ? 0 : STOCK, "Provider");
                    LPTestHelper.assertCount(helper, providerBarrel, Items.DIRT, scenario.dirtPasses ? 0 : STOCK, "Provider");
                })
                .thenSucceed();
        } else {
            // The only power is on the requester's side: whether it reaches the provider is the test
            var sequence = helper.startSequence()
                .thenWaitUntil(() -> {
                    LPTestHelper.assertRouted(helper, requester, provider);
                    LPTestHelper.assertRouted(helper, provider, requester);
                    LPTestHelper.assertPowered(helper, requester);
                });
            if (scenario.flags.blockPower()) {
                sequence
                    .thenIdle(60)
                    .thenExecute(() -> helper.assertFalse(provider.canUseEnergy(1),
                        Component.literal("Power should not cross the firewall")))
                    .thenSucceed();
            } else {
                sequence
                    .thenWaitUntil(() -> LPTestHelper.assertPowered(helper, provider))
                    .thenSucceed();
            }
        }
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Firewall: " + scenario.getSerializedName());
    }

    public enum Scenario implements StringRepresentable {
        /**
         * Blocking: what the filter lists cannot pass, the rest can.
         */
        BLOCKS_LISTED(new FirewallFlags(false, false, false, true, true), true, false, true),
        /**
         * Not blocking: only what the filter lists can pass.
         */
        ALLOWS_ONLY_LISTED(new FirewallFlags(false, false, false, true, false), true, true, false),
        /**
         * Blocking providers: nothing beyond the firewall can be requested, whatever the filter says.
         */
        BLOCKS_PROVIDERS(new FirewallFlags(true, false, false, true, true), true, false, false),
        BLOCKS_POWER(new FirewallFlags(false, false, false, true, true), false, false, false),
        LETS_POWER_THROUGH(new FirewallFlags(false, false, false, false, true), false, false, false);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final FirewallFlags flags;
        final boolean powerOnProviderSide;
        final boolean cobblestonePasses;
        final boolean dirtPasses;

        Scenario(FirewallFlags flags, boolean powerOnProviderSide, boolean cobblestonePasses, boolean dirtPasses) {
            this.flags = flags;
            this.powerOnProviderSide = powerOnProviderSide;
            this.cobblestonePasses = cobblestonePasses;
            this.dirtPasses = dirtPasses;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
