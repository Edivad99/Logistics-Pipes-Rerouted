package network.rs485.logisticspipes

import net.minecraft.SharedConstants
import net.minecraft.core.component.DataComponentInitializers
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.data.registries.VanillaRegistries
import net.minecraft.server.Bootstrap

/**
 * Brings the game far enough up for tests that touch items, fluids or their components.
 *
 * Shared because binding the components is easy to forget and fails far from its cause: every
 * `ItemStack` constructor throws `Components not bound yet` instead of anything that points here.
 */
object TestBootstrap {

    private var booted = false

    @JvmStatic
    @Synchronized
    fun boot() {
        if (booted) return
        SharedConstants.tryDetectVersion()
        Bootstrap.bootStrap()
        bindDefaultComponents()
        booted = true
    }

    /**
     * 26.1 made an item's default components registry-dependent: they are assembled from
     * [BuiltInRegistries.DATA_COMPONENT_INITIALIZERS] while a datapack loads and only then bound
     * onto the registry holders. There is no server here to run that load, so bind them against the
     * vanilla lookup -- the same thing `ReloadableServerResources` does.
     */
    private fun bindDefaultComponents() {
        // NeoForge asserts in dev that every default component value implements equals/hashCode. It
        // whitelists HolderSet.Named, but a lookup without loaded tags hands the initializers the
        // anonymous subclass from HolderSet.emptyNamed, whose class object is not the whitelisted
        // one, so the assert fires on vanilla's own data. Off for the duration of the build; the
        // components themselves are vanilla's and are not what these tests are checking.
        val runningInIde = SharedConstants.IS_RUNNING_IN_IDE
        SharedConstants.IS_RUNNING_IN_IDE = false
        try {
            BuiltInRegistries.DATA_COMPONENT_INITIALIZERS.build(VanillaRegistries.createLookup())
                .forEach(DataComponentInitializers.PendingComponents<*>::apply)
        } finally {
            SharedConstants.IS_RUNNING_IN_IDE = runningInIde
        }
    }
}
