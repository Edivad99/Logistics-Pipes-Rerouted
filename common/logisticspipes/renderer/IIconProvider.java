package logisticspipes.renderer;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface IIconProvider {

	TextureAtlasSprite getIcon(int iconIndex);

	void registerIcons(Object textureMap);
}
