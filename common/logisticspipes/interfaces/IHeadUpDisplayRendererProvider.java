package logisticspipes.interfaces;

import net.minecraft.world.level.Level;

public interface IHeadUpDisplayRendererProvider {

	IHeadUpDisplayRenderer getRenderer();

	int getX();

	int getY();

	int getZ();

	Level getLevelForHUD();

	void startWatching();

	void stopWatching();
}
