package logisticspipes.routing.debug;

import net.minecraft.world.level.Level;

import logisticspipes.interfaces.IHeadUpDisplayRenderer;
import logisticspipes.interfaces.IHeadUpDisplayRendererProvider;
import network.rs485.logisticspipes.world.DoubleCoordinates;

public class HUDRoutingTableDebugProvider implements IHeadUpDisplayRendererProvider {

	private final IHeadUpDisplayRenderer hud;
	private final DoubleCoordinates pos;

	HUDRoutingTableDebugProvider(IHeadUpDisplayRenderer hud, DoubleCoordinates pos) {
		this.hud = hud;
		this.pos = pos;
	}

	@Override
	public IHeadUpDisplayRenderer getRenderer() {
		return hud;
	}

	@Override
	public int getX() {
		return pos.getXInt();
	}

	@Override
	public int getY() {
		return pos.getYInt();
	}

	@Override
	public int getZ() {
		return pos.getZInt();
	}

	@Override
	public Level getLevelForHUD() {
		return null;
	}

	@Override
	public void startWatching() {}

	@Override
	public void stopWatching() {}
}
