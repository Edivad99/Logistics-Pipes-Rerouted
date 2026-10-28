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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import org.jspecify.annotations.Nullable;

import logisticspipes.data.gametest.LPTestHelper;
import logisticspipes.pipes.PipeItemsBasicLogistics;
import logisticspipes.pipes.PipeItemsCraftingLogistics;
import logisticspipes.pipes.PipeItemsProviderLogistics;
import logisticspipes.pipes.PipeItemsRequestLogistics;
import logisticspipes.pipes.PipeItemsSatelliteLogistics;
import logisticspipes.pipes.upgrades.AdvancedSatelliteUpgrade;
import logisticspipes.world.item.LPItems;
import logisticspipes.world.level.block.LPBlocks;
import logisticspipes.world.level.block.entity.LogisticsCraftingTableBlockEntity;

/**
 * A chest requested from a crafting pipe that has a satellite: the planks of the bottom row of the
 * recipe go to the satellite, or with the advanced satellite upgrade those of the slots it is
 * assigned to, and the crafting pipe sends the rest to its own crafting table.
 */
public class SatelliteCraftingTestInstance extends GameTestInstance {

    public static final MapCodec<SatelliteCraftingTestInstance> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
        Scenario.CODEC.fieldOf("scenario").forGetter(t -> t.scenario),
        TestData.CODEC.forGetter(GameTestInstance::info)
    ).apply(instance, SatelliteCraftingTestInstance::new));

    private static final BlockPos PROVIDER = new BlockPos(2, 1, 1);
    private static final BlockPos CRAFTER = new BlockPos(3, 1, 1);
    private static final BlockPos TABLE = CRAFTER.south();
    private static final BlockPos REQUESTER = new BlockPos(6, 1, 1);
    private static final String SATELLITE_NAME = "bottom row";

    private final Scenario scenario;

    public SatelliteCraftingTestInstance(Scenario scenario, TestData<Holder<TestEnvironmentDefinition<?>>> info) {
        super(info);
        this.scenario = scenario;
    }

    @Override
    public void run(GameTestHelper helper) {
        helper.setBlock(TABLE, LPBlocks.CRAFTER.get());
        var table = helper.getBlockEntity(TABLE, LogisticsCraftingTableBlockEntity.class);
        for (int i = 0; i < 9; i++) {
            if (i != 4) {
                table.matrix.setItem(i, new ItemStack(Items.OAK_PLANKS));
            }
        }
        table.cacheRecipe();

        LPTestHelper.placePowerJunction(helper, new BlockPos(0, 1, 1));
        var providerBarrel = LPTestHelper.placeBarrel(helper, PROVIDER.south(), new ItemStack(Items.OAK_PLANKS, 8));
        var requesterBarrel = LPTestHelper.placeBarrel(helper, REQUESTER.south());
        var satelliteBarrel = scenario.spot.barrel == null ? null : LPTestHelper.placeBarrel(helper, scenario.spot.barrel);
        LPTestHelper.placePipe(helper, new BlockPos(1, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        LPTestHelper.placePipe(helper, PROVIDER, new PipeItemsProviderLogistics(LPItems.PIPE_PROVIDER.get()));
        var crafter = LPTestHelper.placePipe(helper, CRAFTER, new PipeItemsCraftingLogistics(LPItems.PIPE_CRAFTING.get()));
        LPTestHelper.placePipe(helper, new BlockPos(4, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        LPTestHelper.placePipe(helper, new BlockPos(5, 1, 1), new PipeItemsBasicLogistics(LPItems.PIPE_BASIC.get()));
        LPTestHelper.placePipe(helper, REQUESTER, new PipeItemsRequestLogistics(LPItems.PIPE_REQUEST.get()));
        var satellite = LPTestHelper.placePipe(helper, scenario.spot.pos,
            new PipeItemsSatelliteLogistics(LPItems.PIPE_SATELLITE.get()));
        if (scenario.advanced) {
            LPTestHelper.addUpgrade(crafter, AdvancedSatelliteUpgrade.getName());
        }

        GameTestSequence sequence = helper.startSequence()
            .thenWaitUntil(() -> assertReady(helper))
            .thenExecute(() -> {
                satellite.setSatellitePipeName(SATELLITE_NAME);
                satellite.ensureAllSatelliteStatus();
                // Set by hand, as a player does for a satellite: importing from the table packs all
                // eight planks into the first slot, leaving the bottom row empty.
                var module = crafter.getLogisticsModule();
                for (int i = 0; i < 9; i++) {
                    if (i != 4) {
                        module.dummyInventory.setItem(i, new ItemStack(Items.OAK_PLANKS));
                    }
                }
                module.dummyInventory.setItem(9, new ItemStack(Items.CHEST));
                if (scenario.advanced) {
                    module.setAdvancedSatelliteUUID(0, satellite.getRouter().getId());
                } else {
                    module.setSatelliteUUID(satellite.getRouter().getId());
                }
            });
        if (scenario.reload) {
            sequence = sequence
                .thenExecute(() -> {
                    LPTestHelper.reload(helper, CRAFTER);
                    LPTestHelper.reload(helper, scenario.spot.pos);
                })
                .thenWaitUntil(() -> assertReady(helper))
                .thenExecute(() -> {
                    var name = LPTestHelper.pipeAt(helper, scenario.spot.pos, PipeItemsSatelliteLogistics.class)
                        .getSatellitePipeName();
                    helper.assertTrue(name.equals(SATELLITE_NAME), Component.literal(
                        "The satellite should still be called '%s', is called '%s'".formatted(SATELLITE_NAME, name)));
                });
        }
        sequence = sequence
            .thenWaitUntil(() -> LPTestHelper.assertInterestedIn(helper,
                LPTestHelper.pipeAt(helper, CRAFTER, PipeItemsCraftingLogistics.class), Items.CHEST))
            .thenExecute(() -> {
                var requester = LPTestHelper.pipeAt(helper, REQUESTER, PipeItemsRequestLogistics.class);
                boolean accepted = LPTestHelper.request(requester, new ItemStack(Items.CHEST));
                boolean expected = scenario.spot != Spot.DISCONNECTED;
                helper.assertTrue(accepted == expected, Component.literal(expected
                    ? "The request for a chest should have been accepted"
                    : "The request for a chest should have been refused: the satellite is unreachable"));
            });

        switch (scenario.spot) {
            case NEXT_TO_TABLE -> sequence = sequence
                .thenWaitUntil(() -> LPTestHelper.assertCount(helper, requesterBarrel, Items.CHEST, 1, "Requester"))
                .thenExecute(() -> {
                    LPTestHelper.assertOnly(helper, requesterBarrel, Items.CHEST, "Requester");
                    LPTestHelper.assertCount(helper, providerBarrel, Items.OAK_PLANKS, 0, "Provider");
                    LPTestHelper.assertCount(helper, table, Items.OAK_PLANKS, 0, "Crafting table");
                });
            case OWN_BARREL -> {
                int toSatellite = scenario.advanced ? 1 : 3;
                sequence = sequence
                    .thenWaitUntil(() -> {
                        LPTestHelper.assertCount(helper, satelliteBarrel, Items.OAK_PLANKS, toSatellite, "Satellite");
                        LPTestHelper.assertCount(helper, table, Items.OAK_PLANKS, 8 - toSatellite, "Crafting table");
                    })
                    .thenIdle(40)
                    .thenExecute(() -> {
                        LPTestHelper.assertCount(helper, satelliteBarrel, Items.OAK_PLANKS, toSatellite, "Satellite");
                        LPTestHelper.assertCount(helper, table, Items.OAK_PLANKS, 8 - toSatellite, "Crafting table");
                        LPTestHelper.assertCount(helper, requesterBarrel, Items.CHEST, 0, "Requester");
                    });
            }
            case DISCONNECTED -> sequence = sequence
                .thenIdle(60)
                .thenExecute(() -> {
                    LPTestHelper.assertCount(helper, providerBarrel, Items.OAK_PLANKS, 8, "Provider");
                    LPTestHelper.assertCount(helper, table, Items.OAK_PLANKS, 0, "Crafting table");
                    LPTestHelper.assertCount(helper, satelliteBarrel, Items.OAK_PLANKS, 0, "Satellite");
                });
        }
        sequence.thenSucceed();
    }

    private void assertReady(GameTestHelper helper) {
        var crafter = LPTestHelper.pipeAt(helper, CRAFTER, PipeItemsCraftingLogistics.class);
        var satellite = LPTestHelper.pipeAt(helper, scenario.spot.pos, PipeItemsSatelliteLogistics.class);
        LPTestHelper.assertRouted(helper, LPTestHelper.pipeAt(helper, REQUESTER, PipeItemsRequestLogistics.class), crafter);
        LPTestHelper.assertRouted(helper, crafter, LPTestHelper.pipeAt(helper, PROVIDER, PipeItemsProviderLogistics.class));
        LPTestHelper.assertPowered(helper, crafter);
        if (scenario.spot == Spot.DISCONNECTED) {
            helper.assertFalse(satellite.initialInit(), Component.literal("The satellite has not ticked yet"));
        } else {
            LPTestHelper.assertRouted(helper, crafter, satellite);
            LPTestHelper.assertPowered(helper, satellite);
        }
    }

    @Override
    public MapCodec<? extends GameTestInstance> codec() {
        return CODEC;
    }

    @Override
    protected MutableComponent typeDescription() {
        return Component.literal("Satellite Crafting: " + scenario.getSerializedName());
    }

    /**
     * Where the satellite goes, and the barrel it fills when it is not next to the crafting table.
     */
    enum Spot {
        /**
         * Beside the crafting table, so the planks it receives complete the recipe.
         */
        NEXT_TO_TABLE(new BlockPos(4, 1, 2), null),
        /**
         * Above the network, filling a barrel of its own: what lands there is what the crafting
         * pipe sent to the satellite.
         */
        OWN_BARREL(new BlockPos(4, 2, 1), new BlockPos(4, 3, 1)),
        /**
         * Off on its own, with no route to the crafting pipe.
         */
        DISCONNECTED(new BlockPos(7, 1, 4), new BlockPos(7, 1, 5));

        final BlockPos pos;
        final @Nullable BlockPos barrel;

        Spot(BlockPos pos, @Nullable BlockPos barrel) {
            this.pos = pos;
            this.barrel = barrel;
        }
    }

    public enum Scenario implements StringRepresentable {
        /**
         * The satellite brings the bottom row to the same table, which then has the whole recipe.
         */
        SHARED_TABLE(Spot.NEXT_TO_TABLE, false, false),
        /**
         * The same, after the crafting pipe and the satellite were saved and loaded again: the
         * crafting pipe still knows its satellite and the satellite its name.
         */
        SHARED_TABLE_AFTER_RELOAD(Spot.NEXT_TO_TABLE, false, true),
        /**
         * The three planks of the bottom row go to the satellite, the other five to the table.
         */
        BOTTOM_ROW_TO_SATELLITE(Spot.OWN_BARREL, false, false),
        /**
         * With the advanced satellite upgrade only the first slot is the satellite's: one plank to
         * the satellite, seven to the table.
         */
        ADVANCED_FIRST_SLOT(Spot.OWN_BARREL, true, false),
        /**
         * A satellite with no route makes the crafting pipe refuse to craft at all.
         */
        UNREACHABLE(Spot.DISCONNECTED, false, false);

        public static final Codec<Scenario> CODEC = StringRepresentable.fromEnum(Scenario::values);

        final Spot spot;
        final boolean advanced;
        final boolean reload;

        Scenario(Spot spot, boolean advanced, boolean reload) {
            this.spot = spot;
            this.advanced = advanced;
            this.reload = reload;
        }

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }
}
