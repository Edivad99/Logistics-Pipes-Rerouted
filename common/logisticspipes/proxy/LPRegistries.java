package logisticspipes.proxy;

import javax.annotation.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.core.RegistryAccess;
import net.minecraft.server.MinecraftServer;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Resolves a {@link RegistryAccess} for code that has no world, player or connection in hand.
 * <p>
 * Data component codecs need registry access to encode values that reference datapack registries
 * (enchantments, for instance), but {@code LPDataIOWrapper} wraps a bare netty buffer. Prefer
 * threading a provider explicitly wherever one is already available; this is the fallback.
 */
public final class LPRegistries {

	private LPRegistries() {}

	/**
	 * Holds the client-side lookup so that {@code net.minecraft.client.Minecraft} is only linked
	 * when this nested class is initialized, which the dist check below prevents on a dedicated
	 * server, where that class does not exist.
	 */
	private static final class ClientHolder {

		@Nullable
		static RegistryAccess access() {
			// The connection's registry access rather than the level's: it is non-null earlier in
			// the join sequence, and it is what vanilla's own RegistryFriendlyByteBuf decorator uses.
			ClientPacketListener connection =
					Minecraft.getInstance().getConnection();
			return connection != null ? connection.registryAccess() : null;
		}
	}

	/**
	 * Server-first, deliberately: on an integrated server both sides live in the same JVM, and the
	 * client's registries are synced <i>from</i> that server, so they agree. Taking the server's is
	 * authoritative and is non-null for as long as a world is loaded.
	 *
	 * @return the registry access, or null when neither side has one yet.
	 */
	@Nullable
	public static RegistryAccess accessOrNull() {
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if (server != null) {
			return server.registryAccess();
		}
		if (!FMLEnvironment.dist.isClient()) {
			return null;
		}
		return ClientHolder.access();
	}

	/**
	 * @throws IllegalStateException when no registry access is available, which means something is
	 * trying to encode or decode components outside of a loaded world.
	 */
	public static RegistryAccess access() {
		RegistryAccess access = LPRegistries.accessOrNull();
		if (access == null) {
			throw new IllegalStateException("No RegistryAccess available: no server running and no client connection");
		}
		return access;
	}
}
