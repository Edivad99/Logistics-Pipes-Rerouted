package logisticspipes.textures.provider;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLEnvironment;

import logisticspipes.renderer.IIconProvider;

public class LPActionTriggerIconProvider implements IIconProvider {

	public static int actionDisablePipeIconIndex = 0;
	public static int triggerCraftingIconIndex = 1;
	public static int triggerPowerDischargingIconIndex = 2;
	public static int triggerPowerNeededIconIndex = 3;
	public static int triggerSupplierFailedIconIndex = 4;
	public static int triggerHasDestinationIconIndex = 5;
	public static int actionRobotRoutingIconIndex = 6;

	private TextureAtlasSprite[] icons;

	public LPActionTriggerIconProvider() {
		if (FMLEnvironment.getDist() == Dist.CLIENT) {
			icons = new TextureAtlasSprite[7];
		}
	}

	@Override
	public TextureAtlasSprite getIcon(int iconIndex) {
		if (iconIndex > 6) {
			return null;
		}
		return icons[iconIndex];
	}

	@Override
	public void registerIcons(Object icon) {
		// TextureAtlas.registerSprite removed in 1.20.1 — BuildCraft integration not ported
	}
}
