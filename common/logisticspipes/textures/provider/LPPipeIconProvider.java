package logisticspipes.textures.provider;

import java.util.ArrayList;
import logisticspipes.renderer.IIconProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.loading.FMLEnvironment;

public class LPPipeIconProvider implements IIconProvider {

	@OnlyIn(Dist.CLIENT)
	private ArrayList<TextureAtlasSprite> icons;

	public LPPipeIconProvider() {
		if (FMLEnvironment.dist == Dist.CLIENT) {
			icons = new ArrayList<>();
		}
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public TextureAtlasSprite getIcon(int iconIndex) {
		return icons.get(iconIndex);
	}

	@OnlyIn(Dist.CLIENT)
	public void setIcon(int index, TextureAtlasSprite icon) {
		while (icons.size() < index + 1) {
			icons.add(null);
		}
		icons.set(index, icon);
	}

	@Override
	@OnlyIn(Dist.CLIENT)
	public void registerIcons(Object iconRegister) {}
}
