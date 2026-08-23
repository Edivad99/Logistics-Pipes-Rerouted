package logisticspipes.utils.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;


public abstract class IItemTextureRenderSlot implements IRenderSlot {

	public abstract TextureAtlasSprite getTextureIcon();

	public abstract boolean drawSlotIcon();

	public abstract boolean customRender(Minecraft mc, float zLevel);

	@Override
	public int getSize() {
		return 18;
	}
}
