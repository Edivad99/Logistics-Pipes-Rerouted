package logisticspipes.gui;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import org.jspecify.annotations.Nullable;

import net.neoforged.neoforge.client.network.ClientPacketDistributor;

import logisticspipes.interfaces.IGUIChannelInformationReceiver;
import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.pipe.InvSysConContentRequest;
import logisticspipes.network.packets.pipe.InvSysConOpenSelectChannelPopupPacket;
import logisticspipes.network.to_server.SetInvSysConResistanceMessage;
import logisticspipes.pipes.PipeItemsInvSysConnector;
import logisticspipes.proxy.MainProxy;
import logisticspipes.routing.channels.ChannelInformation;
import logisticspipes.utils.Color;
import logisticspipes.utils.gui.DummyContainer;
import logisticspipes.utils.gui.InputBar;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.LogisticsBaseGuiScreen;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.utils.item.ItemStackRenderer;
import logisticspipes.utils.item.ItemStackRenderer.DisplayAmount;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiInvSysConnector extends LogisticsBaseGuiScreen implements IGUIChannelInformationReceiver {

	private static final String PREFIX = "gui.invsyscon.";

	private int page = 0;
	private final List<ItemIdentifierStack> allItems = new ArrayList<>();
	private final PipeItemsInvSysConnector pipe;
	private InputBar resistanceCountBar;

    @Nullable
	private ChannelInformation connectedChannel = null;

	public GuiInvSysConnector(Player player, PipeItemsInvSysConnector pipe) {
		super(buildDummy(player, pipe), 180, 220, 0, 0);
		this.pipe = pipe;

	}
	private static DummyContainer buildDummy(Player player, PipeItemsInvSysConnector pipe) {
		DummyContainer dummy = new DummyContainer(player.getInventory(), null);

		dummy.addNormalSlotsForPlayerInventory(11, 136);
		return dummy;
	}


	@Override
	public void init() {
		

		super.init();
		SmallGuiButton b0 = new SmallGuiButton(0, leftPos + 120, topPos + 67, 10, 10, "<");
		b0.setPressListener(b -> pageDown());
		addRenderableWidget(b0);
		SmallGuiButton b1 = new SmallGuiButton(1, leftPos + 160, topPos + 67, 10, 10, ">");
		b1.setPressListener(b -> pageUp());
		addRenderableWidget(b1);
		SmallGuiButton b2 = new SmallGuiButton(2, leftPos + 68, topPos + 67, 46, 10, TextUtil.translate(GuiInvSysConnector.PREFIX + "Refresh"));
		b2.setPressListener(b -> refreshPacket());
		addRenderableWidget(b2);
		SmallGuiButton b3 = new SmallGuiButton(3, leftPos + 80, topPos + 55, 10, 10, "<");
		b3.setPressListener(b -> resistanceCountBar.setInteger(resistanceCountBar.getInteger() - (Minecraft.getInstance().hasControlDown() ? 10 : 1)));
		addRenderableWidget(b3);
		SmallGuiButton b4 = new SmallGuiButton(4, leftPos + 120, topPos + 55, 10, 10, ">");
		b4.setPressListener(b -> resistanceCountBar.setInteger(resistanceCountBar.getInteger() + 1));
		addRenderableWidget(b4);
		SmallGuiButton b5 = new SmallGuiButton(5, leftPos + 140, topPos + 55, 30, 10, TextUtil.translate(GuiInvSysConnector.PREFIX + "Save"));
		b5.setPressListener(b -> {
			pipe.resistance = resistanceCountBar.getInteger();
			ClientPacketDistributor.sendToServer(
					new SetInvSysConResistanceMessage(pipe.getPos(), pipe.resistance));
		});
		addRenderableWidget(b5);
		SmallGuiButton b6 = new SmallGuiButton(6, leftPos + 130, topPos + 20, 40, 10, TextUtil.translate(GuiInvSysConnector.PREFIX + "Change"));
		b6.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(InvSysConOpenSelectChannelPopupPacket.class).setTilePos(pipe.container)));
		addRenderableWidget(b6);

		if (this.resistanceCountBar == null) {
			this.resistanceCountBar = new InputBar(this.font, this, leftPos + 90, topPos + 55, 30, 12, false, true, InputBar.Align.CENTER);
			this.resistanceCountBar.setMinNumber(0);
			this.resistanceCountBar.setInteger(pipe.resistance);
		}
		this.resistanceCountBar.reposition(leftPos + 90, topPos + 55, 30, 12);
        addRenderableWidget(this.resistanceCountBar);

		refreshPacket();
	}

	@Override
	public void closeGui() throws IOException {
		super.closeGui();
		
	}

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int var2, int var3, float var1) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, leftPos, topPos, right, bottom, 0.0f, true);
		LPGuiGraphics.drawPlayerInventoryBackground(guiGraphics, leftPos + 10, topPos + 135);
		guiGraphics.fill(leftPos + 9, topPos + 78, leftPos + 170, topPos + 132, Color.getValue(Color.GREY));
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		super.extractLabels(guiGraphics, mouseX, mouseY);
		guiGraphics.text(minecraft.font, TextUtil.translate(GuiInvSysConnector.PREFIX + "InventorySystemConnector"), 5, 6, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(GuiInvSysConnector.PREFIX + "ConnectionInformation") + ":", 10, 21, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.getTrimmedString(TextUtil.translate(GuiInvSysConnector.PREFIX + "Channel") + ": " + (connectedChannel != null ? connectedChannel.getName() : "UNDEFINED"), 150, this.font, "..."), 15, 38, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(GuiInvSysConnector.PREFIX + "Resistance") + ":", 10, 55, 0xFF404040, false);
		guiGraphics.text(minecraft.font, TextUtil.translate(GuiInvSysConnector.PREFIX + "Waitingfor") + ":", 10, 68, 0xFF404040, false);
		guiGraphics.text(minecraft.font, (page + 1) + "/" + maxPage(), 136, 69, 0xFF404040, false);
		ItemStackRenderer.renderItemIdentifierStackListIntoGui(guiGraphics, allItems, null, page, 9, 79, 9, 27, 18, 18, 100.0F, DisplayAmount.ALWAYS);

		int ppi = 0;
		int column = 0;
		int row = 0;
		for (ItemIdentifierStack itemStack : allItems) {
			ppi++;
			if (ppi <= 27 * page) continue;
			if (ppi > 27 * (page + 1)) continue;
			ItemStack st = itemStack.makeNormalStack();
			int x = 9 + 18 * column + leftPos;
			int y = 79 + 18 * row + topPos;
			if (x < mouseX && mouseX < x + 18 && y < mouseY && mouseY < y + 18) {
				guiGraphics.setTooltipForNextFrame(minecraft.font, st, mouseX, mouseY);
			}
			column++;
			if (column >= 9) {
				row++;
				column = 0;
			}
		}
	}

	private void refreshPacket() {
		MainProxy.sendPacketToServer(PacketHandler.getPacket(InvSysConContentRequest.class).setPosX(pipe.getX()).setPosY(pipe.getY()).setPosZ(pipe.getZ()));
	}

	private void pageDown() {
		if (page <= 0) {
			page = maxPage() - 1;
		} else {
			page--;
		}
	}

	private void pageUp() {
		if (page >= maxPage() - 1) {
			page = 0;
		} else {
			page++;
		}
	}

	private int maxPage() {
		int i = (int) (Math.floor(((float) allItems.size()) / 27) + (((float) allItems.size()) % 27 == 0 ? 0 : 1));
		if (i <= 0) {
			i = 1;
		}
		return i;
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double x = event.x();
		double y = event.y();
		int k = event.button();
		if (!resistanceCountBar.handleClick(x, y, k)) {
			return super.mouseClicked(event, doubleClick);
		}
		return true;
	}

	@Override
	public boolean charTyped(CharacterEvent event) {
		char c = (char) event.codepoint();
		int i = 0 /* CharacterEvent carries no modifiers in 26.1.2 */;
		if (!resistanceCountBar.handleKey(c, i)) {
			return super.charTyped(event);
		}
		return true;
	}

	public void handleContentAnswer(Collection<ItemIdentifierStack> allItems) {
		this.allItems.clear();
		this.allItems.addAll(allItems);
	}

	public void handleResistanceAnswer(int resistance) {
		resistanceCountBar.setInteger(resistance);
	}

	@Override
	public void handleChannelInformation(ChannelInformation channel, boolean flag) {
		if (this.getSubGui() instanceof IGUIChannelInformationReceiver) {
			((IGUIChannelInformationReceiver) this.getSubGui()).handleChannelInformation(channel, flag);
		}
		if (flag) {
			this.connectedChannel = channel;
		} else if (this.connectedChannel != null && this.connectedChannel.getChannelIdentifier().equals(channel.getChannelIdentifier())) {
			this.connectedChannel = channel;
		}
	}
}
