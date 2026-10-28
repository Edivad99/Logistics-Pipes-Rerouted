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
import logisticspipes.data.gametest.instances.ChainedCraftingTestInstance;
import logisticspipes.data.gametest.instances.ExtractorTestInstance;
import logisticspipes.data.gametest.instances.FirewallTestInstance;
import logisticspipes.data.gametest.instances.FuzzyCraftingTestInstance;
import logisticspipes.data.gametest.instances.InTransitReloadTestInstance;
import logisticspipes.data.gametest.instances.ItemSinkTestInstance;
import logisticspipes.data.gametest.instances.MissingItemsTestInstance;
import logisticspipes.data.gametest.instances.NetworkSplitTestInstance;
import logisticspipes.data.gametest.instances.PowerJunctionReloadTestInstance;
import logisticspipes.data.gametest.instances.ProviderOptionsTestInstance;
import logisticspipes.data.gametest.instances.ProviderToRequestTestInstance;
import logisticspipes.data.gametest.instances.SinkVariantsTestInstance;
import logisticspipes.data.gametest.instances.SupplierTestInstance;
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
