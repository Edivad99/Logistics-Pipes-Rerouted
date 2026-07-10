package logisticspipes.network.packets.gui;

import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.network.NewGuiHandler;
import logisticspipes.network.abstractpackets.CoordinatesPacket;
import logisticspipes.network.abstractpackets.ModernPacket;
import logisticspipes.network.guis.pipe.ChassisGuiProvider;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.utils.StaticResolve;
import net.minecraft.world.entity.player.Player;

@StaticResolve
public class GuiOpenChassis extends CoordinatesPacket {

	public GuiOpenChassis(int id) {
		super(id);
	}

	@Override
	public void processPacket(Player player) {
		LogisticsTileGenericPipe pipe = this.getPipe(player.level(), LTGPCompletionCheck.PIPE);
		if (pipe.pipe instanceof CoreRoutedPipe) {
			NewGuiHandler.getGui(ChassisGuiProvider.class).setFlag(pipe.pipe.getUpgradeManager().hasUpgradeModuleUpgrade()).setSlot(ModulePositionType.IN_PIPE).setPositionInt(0).setPosX(getPosX()).setPosY(getPosY()).setPosZ(getPosZ()).open(player);
		}
	}

	@Override
	public ModernPacket template() {
		return new GuiOpenChassis(getId());
	}
}
