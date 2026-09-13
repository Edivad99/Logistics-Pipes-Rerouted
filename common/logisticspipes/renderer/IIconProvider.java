package logisticspipes.renderer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import org.jspecify.annotations.Nullable;

public interface IIconProvider {

	@Nullable
	TextureAtlasSprite getIcon(int iconIndex);

	void registerIcons(Object textureMap);
}
