package logisticspipes.proxy;

import java.util.ArrayList;
import java.util.List;

import logisticspipes.api.ILPPipeConfigTool;
import logisticspipes.api.ILPPipeTile;
import logisticspipes.proxy.interfaces.ILPPipeConfigToolWrapper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class ConfigToolHandler {

	public List<ILPPipeConfigToolWrapper> wrappers = new ArrayList<>();

	public boolean canWrench(Player player, ItemStack wrench, ILPPipeTile pipe) {
		if (wrench.isEmpty()) return false;
		if (wrench.getItem() instanceof ILPPipeConfigTool) {
			return ((ILPPipeConfigTool) wrench.getItem()).canWrench(player, wrench, pipe);
		}
		for (ILPPipeConfigToolWrapper wrapper : wrappers) {
			ILPPipeConfigTool wrapped = wrapper.getWrappedTool(wrench);
			if (wrapped != null) {
				return wrapped.canWrench(player, wrench, pipe);
			}
		}
		return false;
	}

	public void wrenchUsed(Player player, ItemStack wrench, ILPPipeTile pipe) {
		if (wrench.isEmpty()) return;
		if (wrench.getItem() instanceof ILPPipeConfigTool) {
			((ILPPipeConfigTool) wrench.getItem()).wrenchUsed(player, wrench, pipe);
			return;
		}
		for (ILPPipeConfigToolWrapper wrapper : wrappers) {
			ILPPipeConfigTool wrapped = wrapper.getWrappedTool(wrench);
			if (wrapped != null) {
				wrapped.wrenchUsed(player, wrench, pipe);
				return;
			}
		}
	}
}
