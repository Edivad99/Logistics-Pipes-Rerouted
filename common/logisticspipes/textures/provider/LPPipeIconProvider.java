package logisticspipes.textures.provider;

import java.util.ArrayList;
import java.util.Objects;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import org.jspecify.annotations.Nullable;

import logisticspipes.renderer.IIconProvider;

public class LPPipeIconProvider implements IIconProvider {

	private @Nullable ArrayList<@Nullable TextureAtlasSprite> icons;

	public LPPipeIconProvider() {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			icons = new ArrayList<>();
		}
	}

	@Override
	@Nullable
	public TextureAtlasSprite getIcon(int iconIndex) {
		return Objects.requireNonNull(icons, "no icons off the client").get(iconIndex);
	}

	public void setIcon(int index, TextureAtlasSprite icon) {
		final ArrayList<@Nullable TextureAtlasSprite> icons = Objects.requireNonNull(this.icons, "no icons off the client");
		while (icons.size() < index + 1) {
			icons.add(null);
		}
		icons.set(index, icon);
	}

	@Override
	public void registerIcons(Object iconRegister) {}
}
