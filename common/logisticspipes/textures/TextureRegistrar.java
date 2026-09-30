package logisticspipes.textures;

import java.util.List;

import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.client.event.TextureAtlasStitchedEvent;

import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.client.model.pipe.PipeModelStore;
import logisticspipes.client.model.pipe.PipeSprites;

/**
 * Binds the pipe sprites once the block atlas is stitched: the shared pipe-model sprites, and the body
 * sprite of every {@link Textures} entry at its index.
 */
public class TextureRegistrar {

	// Written on the render thread when the atlas is stitched, read by the chunk builders.
	private static volatile List<@Nullable TextureAtlasSprite> pipeSprites = List.of();

	@SubscribeEvent
	public static void onPost(TextureAtlasStitchedEvent event) {
		TextureAtlas atlas = event.getAtlas();
		if (!atlas.location().equals(TextureAtlas.LOCATION_BLOCKS)) return;

		pipeSprites = Textures.pipeTextures().stream()
			.map(texture -> atlas.getSprite(LPConstants.rl(
				"blocks/pipes/new_texture/" + texture.fileName().substring("pipes/".length()))))
			.toList();

		TextureAtlasSprite base = atlas.getSprite(LPConstants.rl("blocks/pipes/pipemodel"));
		TextureAtlasSprite status = atlas.getSprite(LPConstants.rl("blocks/pipes/pipemodel-status"));
		TextureAtlasSprite inactive = atlas.getSprite(LPConstants.rl("blocks/pipes/pipemodel-inactive"));
		TextureAtlasSprite innerBox = atlas.getSprite(LPConstants.rl("blocks/pipes/innerbox"));
		TextureAtlasSprite glassCenter = atlas.getSprite(LPConstants.rl("blocks/pipes/glass_texture_center"));
		PipeModelStore.setSprites(new PipeSprites(base, inactive, status, glassCenter, innerBox,
			TextureRegistrar::pipeSprite));
		Textures.LOGISTICS_SIDE_SELECTION = atlas.getSprite(LPConstants.rl("blocks/sideselection"));
	}

	/**
	 * The body sprite for a {@code TextureMatrix.getTextureIndex()}, or null before the atlas is stitched.
	 */
	@Nullable
	public static TextureAtlasSprite pipeSprite(int index) {
		List<@Nullable TextureAtlasSprite> sprites = pipeSprites;
		return index >= 0 && index < sprites.size() ? sprites.get(index) : null;
	}
}
