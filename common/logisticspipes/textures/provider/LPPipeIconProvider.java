package logisticspipes.textures.provider;

import java.util.ArrayList;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import logisticspipes.renderer.IIconProvider;

public class LPPipeIconProvider implements IIconProvider {

	private ArrayList<TextureAtlasSprite> icons;

	public LPPipeIconProvider() {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			icons = new ArrayList<>();
		}
	}

	@Override
	public TextureAtlasSprite getIcon(int iconIndex) {
		return icons.get(iconIndex);
	}

	public void setIcon(int index, TextureAtlasSprite icon) {
		while (icons.size() < index + 1) {
			icons.add(null);
		}
		icons.set(index, icon);
	}

	@Override
	public void registerIcons(Object iconRegister) {}
}
