package logisticspipes.data.gametest;

import java.util.function.Function;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.gametest.framework.GameTestInstance;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Rotation;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import logisticspipes.LPConstants;
import logisticspipes.LogisticsPipes;
import logisticspipes.data.gametest.instances.ChainedCraftingTestInstance;
import logisticspipes.data.gametest.instances.CrafterByproductTestInstance;
import logisticspipes.data.gametest.instances.CrafterCleanupTestInstance;
import logisticspipes.data.gametest.instances.ExtractorTestInstance;
import logisticspipes.data.gametest.instances.FEPowerSupplierTestInstance;
import logisticspipes.data.gametest.instances.FirewallTestInstance;
import logisticspipes.data.gametest.instances.FluidSinkTestInstance;
import logisticspipes.data.gametest.instances.FluidSupplierTestInstance;
import logisticspipes.data.gametest.instances.FluidTestInstance;
import logisticspipes.data.gametest.instances.FuzzyCraftingTestInstance;
import logisticspipes.data.gametest.instances.InTransitReloadTestInstance;
import logisticspipes.data.gametest.instances.InvSysConnectorTestInstance;
import logisticspipes.data.gametest.instances.ItemSinkTestInstance;
import logisticspipes.data.gametest.instances.MissingItemsTestInstance;
import logisticspipes.data.gametest.instances.NetworkSplitTestInstance;
import logisticspipes.data.gametest.instances.PowerJunctionReloadTestInstance;
import logisticspipes.data.gametest.instances.ProviderOptionsTestInstance;
import logisticspipes.data.gametest.instances.ProviderToRequestTestInstance;
import logisticspipes.data.gametest.instances.RemoteOrdererTestInstance;
import logisticspipes.data.gametest.instances.RequestTableTestInstance;
import logisticspipes.data.gametest.instances.SatelliteCraftingTestInstance;
import logisticspipes.data.gametest.instances.SinkPriorityTestInstance;
import logisticspipes.data.gametest.instances.SinkVariantsTestInstance;
import logisticspipes.data.gametest.instances.SneakyUpgradeTestInstance;
import logisticspipes.data.gametest.instances.SupplierTestInstance;
import logisticspipes.data.gametest.instances.SystemEntranceTestInstance;
import logisticspipes.data.gametest.instances.TwoProvidersTestInstance;

/**
 * The in-game tests, run with the {@code gameTestServer} run or the {@code /test} command.
 */
public class LPGameTestInstances {

    /**
     * Every test starts from an empty floor and builds its network in code.
     */
    private static final String PLATFORM = "platform_9x9";

    private static final DeferredRegister<MapCodec<? extends GameTestInstance>> TEST_INSTANCE_TYPES =
        DeferredRegister.create(BuiltInRegistries.TEST_INSTANCE_TYPE, LPConstants.ID);

    public static void register(IEventBus modEventBus) {
        TEST_INSTANCE_TYPES.register(modEventBus);
        if (LogisticsPipes.isTesting()) {
            LPTestBlocks.register(modEventBus);
        }
        TEST_INSTANCE_TYPES.register("provider_to_request", () -> ProviderToRequestTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("missing_items", () -> MissingItemsTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("two_providers", () -> TwoProvidersTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("network_split", () -> NetworkSplitTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("item_sink", () -> ItemSinkTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("fuzzy_crafting", () -> FuzzyCraftingTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("power_junction_reload", () -> PowerJunctionReloadTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("in_transit_reload", () -> InTransitReloadTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("supplier", () -> SupplierTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("chained_crafting", () -> ChainedCraftingTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("sink_variants", () -> SinkVariantsTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("provider_options", () -> ProviderOptionsTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("firewall", () -> FirewallTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("extractor", () -> ExtractorTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("satellite_crafting", () -> SatelliteCraftingTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("fluid", () -> FluidTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("fluid_sink", () -> FluidSinkTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("sink_priority", () -> SinkPriorityTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("crafter_cleanup", () -> CrafterCleanupTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("crafter_byproduct", () -> CrafterByproductTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("system_entrance", () -> SystemEntranceTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("sneaky_upgrade", () -> SneakyUpgradeTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("request_table", () -> RequestTableTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("inv_sys_connector", () -> InvSysConnectorTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("remote_orderer", () -> RemoteOrdererTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("fluid_supplier", () -> FluidSupplierTestInstance.CODEC);
        TEST_INSTANCE_TYPES.register("fe_power_supplier", () -> FEPowerSupplierTestInstance.CODEC);
    }

    public static void bootstrap(BootstrapContext<GameTestInstance> bootstrap) {
        registerTestInstance(bootstrap, "provider_to_request", ProviderToRequestTestInstance::new);
        registerTestInstance(bootstrap, "missing_items", MissingItemsTestInstance::new);
        registerTestInstance(bootstrap, "two_providers", TwoProvidersTestInstance::new);
        registerTestInstance(bootstrap, "network_split", NetworkSplitTestInstance::new);
        registerTestInstance(bootstrap, "item_sink", info -> new ItemSinkTestInstance(false, info));
        registerTestInstance(bootstrap, "item_sink_after_reload", info -> new ItemSinkTestInstance(true, info));
        registerTestInstance(bootstrap, "power_junction_reload", PowerJunctionReloadTestInstance::new);
        registerTestInstance(bootstrap, "in_transit_reload", InTransitReloadTestInstance::new);
        for (var scenario : SupplierTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "supplier/" + scenario.getSerializedName(), 1000,
                info -> new SupplierTestInstance(scenario, info));
        }
        for (var variant : SinkVariantsTestInstance.Variant.values()) {
            registerTestInstance(bootstrap, "sink/" + variant.getSerializedName(),
                info -> new SinkVariantsTestInstance(variant, info));
        }
        for (var scenario : RequestTableTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "request_table/" + scenario.getSerializedName(),
                info -> new RequestTableTestInstance(scenario, info));
        }
        for (var scenario : InvSysConnectorTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "inv_sys_connector/" + scenario.getSerializedName(),
                info -> new InvSysConnectorTestInstance(scenario, info));
        }
        for (var scenario : RemoteOrdererTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "remote_orderer/" + scenario.getSerializedName(),
                info -> new RemoteOrdererTestInstance(scenario, info));
        }
        for (var scenario : FEPowerSupplierTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "fe_power_supplier/" + scenario.getSerializedName(),
                info -> new FEPowerSupplierTestInstance(scenario, info));
        }
        for (var scenario : SneakyUpgradeTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "sneaky_upgrade/" + scenario.getSerializedName(),
                info -> new SneakyUpgradeTestInstance(scenario, info));
        }
        for (var scenario : SystemEntranceTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "system_entrance/" + scenario.getSerializedName(),
                info -> new SystemEntranceTestInstance(scenario, info));
        }
        for (var scenario : SinkPriorityTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "sink_priority/" + scenario.getSerializedName(),
                info -> new SinkPriorityTestInstance(scenario, info));
        }
        for (var scenario : ProviderOptionsTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "provider/" + scenario.getSerializedName(),
                info -> new ProviderOptionsTestInstance(scenario, info));
        }
        for (var scenario : FirewallTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "firewall/" + scenario.getSerializedName(),
                info -> new FirewallTestInstance(scenario, info));
        }
        for (var scenario : ExtractorTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "extractor/" + scenario.getSerializedName(), 1200,
                info -> new ExtractorTestInstance(scenario, info));
        }
        for (var scenario : ChainedCraftingTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "chained_crafting/" + scenario.getSerializedName(), 800,
                info -> new ChainedCraftingTestInstance(scenario, info));
        }
        for (var scenario : CrafterByproductTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "crafter_byproduct/" + scenario.getSerializedName(),
                info -> new CrafterByproductTestInstance(scenario, info));
        }
        for (var scenario : CrafterCleanupTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "crafter_cleanup/" + scenario.getSerializedName(),
                info -> new CrafterCleanupTestInstance(scenario, info));
        }
        for (var scenario : FluidTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "fluid/" + scenario.getSerializedName(),
                info -> new FluidTestInstance(scenario, info));
        }
        for (var scenario : FluidSinkTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "fluid_sink/" + scenario.getSerializedName(),
                info -> new FluidSinkTestInstance(scenario, info));
        }
        for (var scenario : FluidSupplierTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "fluid_supplier/" + scenario.getSerializedName(), 800,
                info -> new FluidSupplierTestInstance(scenario, info));
        }
        for (var scenario : SatelliteCraftingTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "satellite_crafting/" + scenario.getSerializedName(), 800,
                info -> new SatelliteCraftingTestInstance(scenario, info));
        }

        for (var scenario : FuzzyCraftingTestInstance.Scenario.values()) {
            registerTestInstance(bootstrap, "fuzzy_crafting/" + scenario.getSerializedName(), 800,
                info -> new FuzzyCraftingTestInstance(scenario, info));
        }
    }

    private static <T extends GameTestInstance> void registerTestInstance(BootstrapContext<GameTestInstance> bootstrap,
        String name, Function<TestData<Holder<TestEnvironmentDefinition<?>>>, T> factory) {
        registerTestInstance(bootstrap, name, 400, factory);
    }

    private static <T extends GameTestInstance> void registerTestInstance(BootstrapContext<GameTestInstance> bootstrap,
        String name, int maxTicks, Function<TestData<Holder<TestEnvironmentDefinition<?>>>, T> factory) {
        bootstrap.register(
            ResourceKey.create(Registries.TEST_INSTANCE, LPConstants.rl(name)),
            factory.apply(testData(bootstrap, maxTicks)));
    }

    private static TestData<Holder<TestEnvironmentDefinition<?>>> testData(BootstrapContext<GameTestInstance> bootstrap,
        int maxTicks) {
        var environments = bootstrap.lookup(Registries.TEST_ENVIRONMENT);
        return new TestData<>(
            environments.getOrThrow(LPTestEnvironments.DEFAULT),
            LPConstants.rl("test/" + PLATFORM),
            maxTicks,
            0,
            true,
            Rotation.NONE,
            false,
            1,
            1,
            true,
            0);
    }

    public static <T extends GameTestInstance> MapCodec<T> defaultCodec(
        Function<TestData<Holder<TestEnvironmentDefinition<?>>>, T> factory) {
        return RecordCodecBuilder.mapCodec(instance -> instance.group(TestData.CODEC.forGetter(GameTestInstance::info))
            .apply(instance, factory));
    }
}
