package logisticspipes.api.provider;

import java.util.List;

import net.minecraft.core.BlockPos;

import org.jspecify.annotations.Nullable;

/**
 * Something the network can spend power through, drawing it from somewhere else -- typically a pipe
 * passing the request on towards a provider, losing some of it to the distance travelled.
 *
 * <p>Implement {@link ILogisticsPowerProvider} instead to be a source of power.
 */
public interface IRoutedPowerProvider {

    /** Spends {@code amount}, and reports whether it could. */
    boolean useEnergy(int amount);

    /** Whether {@link #useEnergy(int)} would succeed, without spending anything. */
    boolean canUseEnergy(int amount);

    /**
     * Spends {@code amount} while one provider is asking another.
     *
     * @param providersToIgnore providers already visited, so the request cannot loop back. An
     *                          implementation that forwards the request must bail out if it is
     *                          already in the list, and otherwise add itself -- creating the list
     *                          when null, which is how the first caller starts the chain.
     */
    boolean useEnergy(int amount, @Nullable List<Object> providersToIgnore);

    /** Whether {@link #useEnergy(int, List)} would succeed, without spending anything. */
    boolean canUseEnergy(int amount, @Nullable List<Object> providersToIgnore);

    /** Where this sits in the world, used to work out the distance power has to travel. */
    BlockPos getPos();
}
