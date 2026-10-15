package logisticspipes.textures;

import java.util.List;

import net.minecraft.client.renderer.texture.SpriteContents;
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
	private static volatile List<@Nullable TextureAtlasSprite> pipeParticleSprites = List.of();

	@SubscribeEvent
	public static void onPost(TextureAtlasStitchedEvent event) {
		TextureAtlas atlas = event.getAtlas();
		if (!atlas.location().equals(TextureAtlas.LOCATION_BLOCKS)) return;

		pipeSprites = Textures.pipeTextures().stream()
			.map(texture -> atlas.getSprite(LPConstants.rl(
				"blocks/pipes/new_texture/" + texture.fileName().substring("pipes/".length()))))
			.toList();
		pipeParticleSprites = pipeSprites.stream()
			.map(sprite -> sprite == null || isBlank(sprite) ? null : sprite)
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

	/**
	 * The body sprite to break a pipe into, or null when there is none to use. Some body textures
	 * are fully transparent -- the basic pipes show only the frame -- and particles cut from them
	 * would be invisible.
	 */
	@Nullable
	public static TextureAtlasSprite pipeParticleSprite(int index) {
		List<@Nullable TextureAtlasSprite> sprites = pipeParticleSprites;
		return index >= 0 && index < sprites.size() ? sprites.get(index) : null;
	}

	private static boolean isBlank(TextureAtlasSprite sprite) {
		SpriteContents contents = sprite.contents();
		for (int y = 0; y < contents.height(); y++) {
			for (int x = 0; x < contents.width(); x++) {
				if (!contents.isTransparent(0, x, y)) {
					return false;
				}
			}
		}
		return true;
	}
}
