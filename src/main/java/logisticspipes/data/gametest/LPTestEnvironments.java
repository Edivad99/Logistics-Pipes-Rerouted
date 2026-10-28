package logisticspipes.data.gametest;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.data.worldgen.BootstrapContext;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.ResourceKey;

import logisticspipes.LPConstants;

public class LPTestEnvironments {

    public static final ResourceKey<TestEnvironmentDefinition<?>> DEFAULT =
        ResourceKey.create(Registries.TEST_ENVIRONMENT, LPConstants.rl("default"));

    public static void bootstrap(BootstrapContext<TestEnvironmentDefinition<?>> bootstrap) {
        bootstrap.register(DEFAULT, new TestEnvironmentDefinition.AllOf(List.of()));
    }
}
