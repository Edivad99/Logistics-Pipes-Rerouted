package logisticspipes.textures;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import logisticspipes.LPConstants;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

/**
 * Collects (index, fileName) pairs registered via {@link Textures#registerBlockIcons(Object)}
 * and binds real {@link TextureAtlasSprite}s into the LP icon providers during the block
 * atlas stitch events. Powered / unpowered / un-overlayed variants resolve to LP1's
 * pre-generated composites under {@code blocks/pipes/overlay_gen/} (stitched via the
 * atlas config in {@code assets/minecraft/atlases/blocks.json}).
 */
public class TextureRegistrar {

	private static final List<Entry> ENTRIES = new ArrayList<>();
	private static final List<Entry> NEW_ENTRIES = new ArrayList<>();
	private static final java.util.Map<Integer, TextureAtlasSprite> NEW_PIPE_SPRITES =
		new java.util.concurrent.ConcurrentHashMap<>();
	private static boolean collected = false;

	public static void record(int index, String fileName) {
		if (fileName == null || fileName.isEmpty()) return;
		String path = resolvePath(fileName);
		if (path == null) return;
		ENTRIES.add(new Entry(index, LPConstants.rl(path)));
	}

	public static void recordNew(int index, String fileName) {
		if (fileName == null || fileName.isEmpty()) return;
		String path = resolvePath(fileName);
		if (path == null) return;
		NEW_ENTRIES.add(new Entry(index, LPConstants.rl(path)));
	}

	/**
	 * Base+overlay composite, pre-generated on disk under overlay_gen/ (LP1 layout):
	 * e.g. ("pipes/basic", "pipes/status_overlay/powered-pipe")
	 * → blocks/pipes/overlay_gen/basic/powered-pipe.
	 */
	public static void recordOverlay(int index, String fileName, String overlayName) {
		if (fileName == null || fileName.isEmpty() || overlayName == null || overlayName.isEmpty()) return;
		String path = "blocks/" + fileName.replace("pipes/", "pipes/overlay_gen/")
				+ "/" + overlayName.replace("pipes/status_overlay/", "");
		ENTRIES.add(new Entry(index, LPConstants.rl(path)));
	}

	// Maps the legacy Textures.java fileName (e.g. "pipes/basic") to the actual
	// on-disk texture path under assets/logisticspipes/textures/. The 1.12.2
	// code assumed a flat "blocks/<fileName>" layout, but the real files live
	// under several subfolders in resources/assets/logisticspipes/textures/blocks/pipes/.
	private static String resolvePath(String fileName) {
		// Flat file directly under blocks/pipes/ (no new_texture/ prefix)
		if (fileName.equals("pipes/liquid_connector")) {
			return "blocks/" + fileName;
		}
		// Status overlays live under blocks/pipes/status_overlay/
		if (fileName.startsWith("pipes/status_overlay/")) {
			return "blocks/" + fileName;
		}
		// Chassi status overlays exist only as overlay_gen composites in 1.12.2 —
		// no flat sprite file to bind; skip (getSprite would return the missing sprite).
		if (fileName.startsWith("pipes/chassi/status_overlay/")) {
			return null;
		}
		// Everything else (basic pipes, chassi mk1–5, transport) lives under
		// blocks/pipes/new_texture/ — strip the leading "pipes/" and re-prefix.
		if (fileName.startsWith("pipes/")) {
			return "blocks/pipes/new_texture/" + fileName.substring("pipes/".length());
		}
		return null;
	}

	private static void collectOnce() {
		if (collected) return;
		collected = true;
		// Runs registerBlockIcons with a null register; ClientProxy.addLogisticsPipesOverride
		// now forwards each call into record() above.
		new Textures().registerBlockIcons(null);
	}

	@SubscribeEvent
	public static void onPost(TextureAtlasStitchedEvent event) {
		if (!event.getAtlas().location().equals(TextureAtlas.LOCATION_BLOCKS)) return;
		collectOnce();

		// The pipe-model sprites are looked up by Identifier rather than by
		// Textures.java index, and handed to the baked-model pipeline below.
		TextureAtlasSprite base = event.getAtlas().getSprite(LPConstants.rl("blocks/pipes/pipemodel"));
		TextureAtlasSprite status = event.getAtlas().getSprite(LPConstants.rl("blocks/pipes/pipemodel-status"));
		TextureAtlasSprite statusBC = event.getAtlas().getSprite(LPConstants.rl("blocks/pipes/pipemodel-status-bc"));
		TextureAtlasSprite inactive = event.getAtlas().getSprite(LPConstants.rl("blocks/pipes/pipemodel-inactive"));
		TextureAtlasSprite innerBox = event.getAtlas().getSprite(LPConstants.rl("blocks/pipes/innerbox"));
		TextureAtlasSprite glassCenter = event.getAtlas().getSprite(LPConstants.rl("blocks/pipes/glass_texture_center"));
		logisticspipes.client.model.pipe.PipeModelStore.setSprites(
			new logisticspipes.client.model.pipe.PipeSprites(
				base, inactive, status, statusBC, glassCenter, innerBox,
				TextureRegistrar::newPipeIcon));
		for (Entry e : ENTRIES) {
			TextureAtlasSprite sprite = event.getAtlas().getSprite(e.rl);
			if (sprite == null) continue;
			if (Textures.LPpipeIconProvider != null) {
				Textures.LPpipeIconProvider.setIcon(e.index, sprite);
			}
		}
		NEW_PIPE_SPRITES.clear();
		for (Entry e : NEW_ENTRIES) {
			TextureAtlasSprite sprite = event.getAtlas().getSprite(e.rl);
			if (sprite == null) continue;
			NEW_PIPE_SPRITES.put(e.index, sprite);
		}
	}

	/**
	 * The per-pipe-type body sprite for a {@code TextureMatrix.getTextureIndex()},
	 * recorded during the stitch so the baked pipeline can resolve it without a lookup.
	 */
	@Nullable
	public static TextureAtlasSprite newPipeIcon(int index) {
		return NEW_PIPE_SPRITES.get(index);
	}

	private record Entry(int index, Identifier rl) {}
}
