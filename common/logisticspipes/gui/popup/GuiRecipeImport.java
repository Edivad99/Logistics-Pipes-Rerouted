package logisticspipes.gui.popup;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import logisticspipes.network.PacketHandler;
import logisticspipes.network.packets.NEISetCraftingRecipe;
import logisticspipes.network.packets.pipe.FindMostLikelyRecipeComponents;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.gui.LPGuiGraphics;
import logisticspipes.utils.gui.SmallGuiButton;
import logisticspipes.utils.gui.SubGuiScreen;
import logisticspipes.utils.item.ItemIdentifierStack;
import network.rs485.logisticspipes.util.TextUtil;

public class GuiRecipeImport extends SubGuiScreen {

	public static class Candidates {

		public Candidates(Set<ItemIdentifierStack> set) {
			this.set = set;
		}

		Set<ItemIdentifierStack> set;
		public List<ItemIdentifierStack> order;
		int pos = 0;
	}

	private final BlockEntity tile;
	private final Candidates[] grid = new Candidates[9];
	private final List<Candidates> list;

	public GuiRecipeImport(BlockEntity tile, ItemStack[][] stacks) {
		super(150, 200, 0, 0);
		this.tile = tile;
		list = new ArrayList<>();
		for (int i = 0; i < 9; i++) {
			if (stacks[i] == null) {
				continue;
			}
			Set<ItemIdentifierStack> part = new TreeSet<>();
			List<ItemIdentifierStack> order = new ArrayList<>();
			for (ItemStack stack : stacks[i]) {
				ItemIdentifierStack iStack = ItemIdentifierStack.getFromStack(stack);
				part.add(iStack);
				order.add(iStack);
			}
			Candidates candidate = new Candidates(part);
			boolean found = false;
			for (Candidates test : list) {
				if (test.set.equals(part)) {
					candidate = test;
					found = true;
					break;
				}
			}
			if (!found) {
				candidate.order = order;
				if (order.size() > 1) {
					list.add(candidate);
				}
			}
			grid[i] = candidate;
		}
	}

	@Override
	public void init() {
		super.init();
		SmallGuiButton done = new SmallGuiButton(0, guiLeft + 100, guiTop + 180, 40, 10, "Done");
		done.setPressListener(b -> {
			NEISetCraftingRecipe packet = PacketHandler.getPacket(NEISetCraftingRecipe.class);
			NonNullList<ItemStack> stackList = packet.getStackList();
			int i = 0;
			for (Candidates candidate : grid) {
				if (candidate == null) {
					i++;
					continue;
				}
				stackList.set(i++, candidate.order.get(candidate.pos).makeNormalStack());
			}
			MainProxy.sendPacketToServer(packet.setBlockPos(tile.getBlockPos()));
			exitGui();
		});
		addRenderableWidget(done);
		SmallGuiButton ml = new SmallGuiButton(1, guiLeft + 10, guiTop + 180, 60, 10, "Most likely");
		ml.setPressListener(b -> MainProxy.sendPacketToServer(PacketHandler.getPacket(FindMostLikelyRecipeComponents.class).setContent(list).setTilePos(tile)));
		addRenderableWidget(ml);
		int x = 0;
		int y = 0;
		int idx = 0;
		for (Candidates candidate : list) {
			final Candidates cRef = candidate;
			SmallGuiButton upBtn = new SmallGuiButton(10 + x + y * 3, guiLeft + 38 + x * 40, guiTop + 88 + y * 40, 15, 10, "/\\");
			upBtn.setPressListener(b -> {
				cRef.pos++;
				if (cRef.pos >= cRef.order.size()) cRef.pos = 0;
			});
			addRenderableWidget(upBtn);
			SmallGuiButton dnBtn = new SmallGuiButton(20 + x + y * 3, guiLeft + 38 + x * 40, guiTop + 98 + y * 40, 15, 10, "\\/");
			dnBtn.setPressListener(b -> {
				cRef.pos--;
				if (cRef.pos < 0) cRef.pos = cRef.order.size() - 1;
			});
			addRenderableWidget(dnBtn);
			x++;
			if (x > 2) {
				x = 0;
				y++;
			}
			idx++;
		}
	}

	@Override
	protected void renderToolTips(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float par3) {
		// Grid items (3×3)
		for (int i = 0; i < 9; i++) {
			Candidates c = grid[i];
			if (c == null || c.order == null || c.order.isEmpty()) continue;
			int gx = i % 3;
			int gy = i / 3;
			int sx = guiLeft + 44 + gx * 18;
			int sy = guiTop + 19 + gy * 18;
			if (mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
				ItemStack stack = c.order.get(c.pos % c.order.size()).makeNormalStack();
                guiGraphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
				return;
			}
		}
		// Candidate selections
		int x = 0, y = 0;
		for (Candidates candidate : list) {
			int sx = guiLeft + 20 + x * 40;
			int sy = guiTop + 90 + y * 40;
			if (candidate.order != null && !candidate.order.isEmpty()
					&& mouseX >= sx && mouseX < sx + 16 && mouseY >= sy && mouseY < sy + 16) {
				ItemStack stack = candidate.order.get(candidate.pos % candidate.order.size()).makeNormalStack();
                guiGraphics.setTooltipForNextFrame(font, stack, mouseX, mouseY);
				return;
			}
			x++;
			if (x > 2) { x = 0; y++; }
		}
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		guiGraphics.text(font, TextUtil.translate("misc.selectOreDict"), guiLeft + 10, guiTop + 6, 0xFF404040, false);
		// Render items in 3×3 crafting grid preview
		for (int i = 0; i < 9; i++) {
			Candidates c = grid[i];
			if (c == null || c.order == null || c.order.isEmpty()) continue;
			int gx = i % 3;
			int gy = i / 3;
			ItemStack stack = c.order.get(c.pos % c.order.size()).makeNormalStack();
			guiGraphics.item(stack, guiLeft + 45 + gx * 18, guiTop + 20 + gy * 18);
		}
		// Render current selection for each candidate group
		int x = 0, y = 0;
		for (Candidates candidate : list) {
			if (candidate.order != null && !candidate.order.isEmpty()) {
				ItemStack stack = candidate.order.get(candidate.pos % candidate.order.size()).makeNormalStack();
				guiGraphics.item(stack, guiLeft + 20 + x * 40, guiTop + 90 + y * 40);
			}
			x++;
			if (x > 2) { x = 0; y++; }
		}
	}

	@Override
	protected void extractGuiBackground(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY) {
		LPGuiGraphics.drawGuiBackGround(guiGraphics, guiLeft, guiTop, right, bottom, 0.0f, true);
        guiGraphics.text(font, TextUtil.translate("misc.selectOreDict"), guiLeft + 10, guiTop + 6, 0xFF404040, false);
		for (int x = 0; x < 3; x++) {
			for (int y = 0; y < 3; y++) {
				LPGuiGraphics.drawSlotBackground(guiGraphics, guiLeft + 44 + x * 18, guiTop + 19 + y * 18);
			}
		}
		int x = 0;
		int y = 0;
		for (Candidates candidate : list) {
			LPGuiGraphics.drawSlotBackground(guiGraphics, guiLeft + 19 + x * 40, guiTop + 89 + y * 40);
			x++;
			if (x > 2) {
				x = 0;
				y++;
			}
		}
	}

public void handleProposePacket(List<Integer> response) {
		if (list.size() != response.size()) return;
		for (int slot = 0; slot < list.size(); slot++) {
			Candidates candidate = list.get(slot);
			int newPos = response.get(slot);
			if (newPos != -1) {
				candidate.pos = newPos;
			}
		}
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		double mouseX = event.x();
		double mouseY = event.y();
		int button = event.button();
		int x = 0;
		int y = 0;
		for (final Candidates candidate : list) {

			if (guiLeft + 20 + x * 40 < mouseX && mouseX < guiLeft + 20 + x * 40 + 16 && guiTop + 90 + y * 40 < mouseY && mouseY < guiTop + 90 + y * 40 + 16) {
				setSubGui(new SelectItemOutOfList(candidate.order, slot -> candidate.pos = slot));
			}

			x++;
			if (x > 2) {
				x = 0;
				y++;
			}
		}
		return super.mouseClicked(event, doubleClick);
	}
}
