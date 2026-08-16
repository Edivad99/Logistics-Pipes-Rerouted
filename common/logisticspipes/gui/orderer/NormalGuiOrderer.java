package logisticspipes.gui.orderer;


import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.orderer.OrdererRefreshRequestPacket;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifier;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

public class NormalGuiOrderer extends GuiOrderer {

	private enum DisplayOptions {
		Both,
		SupplyOnly,
		CraftOnly,
	}

	private DisplayOptions displayOptions = DisplayOptions.Both;

	public NormalGuiOrderer(int x, int y, int z, ResourceLocation dim, Player entityPlayer) {
		super(x, y, z, dim, entityPlayer);
		refreshItems();
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton refreshBtn = new SmallGuiButton(3, leftPos + 10, bottom - 15, 46, 10, "Refresh");
		refreshBtn.setPressListener(b -> refreshItems());
		addRenderableWidget(refreshBtn);
		addRenderableWidget(new SmallGuiButton(13, leftPos + 10, bottom - 28, 46, 10, "Content"));
		SmallGuiButton modeBtn = new SmallGuiButton(9, leftPos + 10, bottom - 41, 46, 10, "Both");
		modeBtn.setPressListener(b -> {
			String displayString = switch (displayOptions) {
                case Both -> {
                    displayOptions = DisplayOptions.CraftOnly;
                    yield "Craft";
                }
                case CraftOnly -> {
                    displayOptions = DisplayOptions.SupplyOnly;
                    yield "Supply";
                }
                case SupplyOnly -> {
                    displayOptions = DisplayOptions.Both;
                    yield "Both";
                }
            };
            b.setMessage(Component.literal(displayString));
			refreshItems();
		});
		addRenderableWidget(modeBtn);
	}

	@Override
	public void refreshItems() {
		int integer = switch (displayOptions) {
            case Both -> 0;
            case SupplyOnly -> 1;
            case CraftOnly -> 2;
        };
        MainProxy.sendPacketToServer(PacketHandler.getPacket(OrdererRefreshRequestPacket.class).putInt(integer).setPosX(xCoord).setPosY(yCoord).setPosZ(zCoord).setDimension(dimension));
	}

	@Override
	public void specialItemRendering(ItemIdentifier item, int x, int y) {}
}
