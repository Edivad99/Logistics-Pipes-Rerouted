package logisticspipes.client.renderer.blockentity;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import org.joml.Quaternionf;

import logisticspipes.pipes.signs.CraftingPipeSign;
import logisticspipes.pipes.signs.IPipeSign;
import logisticspipes.pipes.signs.ItemAmountPipeSign;
import logisticspipes.utils.TextUtil;
import logisticspipes.utils.item.ItemIdentifierStack;

/**
 * Draws what each kind of pipe sign shows onto the board {@link LogisticsRenderPipe} has already
 * put in place.
 *
 * <p>The text sits on four lines: the item's numeric id, two lines the sign fills as it likes, and
 * the item's name.</p>
 */
final class PipeSignRenderer {

    /**
     * Opaque black. These calls used to pass a bare 0: Font#drawInBatch forced a zero alpha byte
     * to opaque, so 0 meant black. 1.21.6 removed that fixup and 0 now means invisible.
     */
    private static final int TEXT_COLOR = 0xFF000000;
    /**
     * Depth left to an item drawn on a sign, as a fraction of its width. See {@link #submitItem}.
     */
    private static final float FLAT_ITEM_DEPTH = 0.02F;
    /**
     * Widest a line may be, in font units, before it is cut short.
     */
    private static final int MAX_TEXT_WIDTH = 90;

    private PipeSignRenderer() {
    }

    static void submit(IPipeSign sign, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
        Font font = Minecraft.getInstance().font;
        switch (sign) {
            case CraftingPipeSign crafting -> submitCrafting(crafting, font, poseStack, collector, packedLight);
            case ItemAmountPipeSign itemAmount -> submitItemAmount(itemAmount, font, poseStack, collector, packedLight);
        }
    }

    private static void submitCrafting(CraftingPipeSign sign, Font font, PoseStack poseStack,
        SubmitNodeCollector collector, int packedLight) {
        List<ItemIdentifierStack> craftables = sign.getPipe().getCraftedItems();
        if (craftables == null || craftables.isEmpty()) {
            submitEmpty(font, poseStack, collector, packedLight);
            return;
        }
        ItemStack stack = craftables.getFirst().makeNormalStack();
        submitItem(stack, poseStack, collector, packedLight);

        beginText(poseStack);
        submitIdLine(stack, font, poseStack, collector, packedLight);
        String satellite = sign.getPipe().getLogisticsModule().clientSideSatelliteNames.satelliteName();
        if (!satellite.isEmpty()) {
            submitLine("Sat: " + satellite, 1, font, poseStack, collector, packedLight);
        }
        submitNameLine(stack.getItem().getName(stack).getString(), font, poseStack, collector, packedLight);
        poseStack.popPose();
    }

    private static void submitItemAmount(ItemAmountPipeSign sign, Font font, PoseStack poseStack,
        SubmitNodeCollector collector, int packedLight) {
        ItemIdentifierStack item = sign.getItem();
        if (item == null) {
            submitEmpty(font, poseStack, collector, packedLight);
            return;
        }
        ItemStack stack = item.makeNormalStack();
        submitItem(stack, poseStack, collector, packedLight);

        beginText(poseStack);
        submitIdLine(stack, font, poseStack, collector, packedLight);
        submitLine("Amount:", 1, font, poseStack, collector, packedLight);
        submitLine(TextUtil.getThreeDigitFormattedNumber(sign.amount, false), 2, font, poseStack, collector,
            packedLight);
        submitNameLine(stack.getItem().getName(stack).getString(), font, poseStack, collector, packedLight);
        poseStack.popPose();
    }

    private static void submitEmpty(Font font, PoseStack poseStack, SubmitNodeCollector collector, int packedLight) {
        beginText(poseStack);
        submitNameLine("Empty", font, poseStack, collector, packedLight);
        poseStack.popPose();
    }

    /**
     * Pushes the pose text is laid out in; the caller pops it.
     */
    private static void beginText(PoseStack poseStack) {
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationX((float) Math.toRadians(-180)));
        poseStack.translate(0.5F, 0.08F, 0.0F);
        poseStack.scale(1.0F / 90.0F, 1.0F / 90.0F, 1.0F / 90.0F);
    }

    private static void submitIdLine(ItemStack stack, Font font, PoseStack poseStack, SubmitNodeCollector collector,
        int packedLight) {
        String id = String.format("ID: %d", BuiltInRegistries.ITEM.getId(stack.getItem()));
        submitLine(id, 0, font, poseStack, collector, packedLight);
    }

    /**
     * The last line, shifted left to clear the item drawn beside it.
     */
    private static void submitNameLine(String name, Font font, PoseStack poseStack, SubmitNodeCollector collector,
        int packedLight) {
        String text = cut(name, font);
        submitText(text, -font.width(text) / 2.0F - 15, 3, poseStack, collector, packedLight);
    }

    private static void submitLine(String text, int line, Font font, PoseStack poseStack,
        SubmitNodeCollector collector, int packedLight) {
        submitText(text, -font.width(text) / 2.0F, line, poseStack, collector, packedLight);
    }

    private static void submitText(String text, float x, int line, PoseStack poseStack, SubmitNodeCollector collector,
        int packedLight) {
        collector.submitText(poseStack, x, line * 10 - 4 * 5, Component.literal(text).getVisualOrderText(), false,
            Font.DisplayMode.NORMAL, packedLight, TEXT_COLOR, 0, 0);
    }

    private static void submitItem(ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector,
        int packedLight) {
        if (stack.isEmpty()) {
            return;
        }
        poseStack.pushPose();
        // Flattened along the sign's normal, which is what makes a block item read as a picture
        // on the plank rather than a cube sticking out of it. The GUI display context has
        // already put the model in its inventory pose by the time this scale applies, so
        // collapsing depth here leaves exactly the inventory icon's silhouette, drawn in the
        // plane of the sign. Not zero: the faces still need distinct depths to sort against
        // each other, they just need a thickness nobody can see.
        poseStack.scale(0.25F, 0.25F, 0.25F * FLAT_ITEM_DEPTH);
        // renderStatic is gone: resolve the stack to a render state and submit it.
        Minecraft minecraft = Minecraft.getInstance();
        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver()
            .updateForTopItem(renderState, stack, ItemDisplayContext.GUI, minecraft.level, null, 0);
        renderState.submit(poseStack, collector, packedLight, OverlayTexture.NO_OVERLAY, 0);
        poseStack.popPose();
    }

    /**
     * {@code name}, cut short with an ellipsis if it would not fit on the sign.
     */
    private static String cut(String name, Font font) {
        if (font.width(name) < MAX_TEXT_WIDTH) {
            return name;
        }
        StringBuilder sum = new StringBuilder();
        for (int i = 0; i < name.length(); i++) {
            if (font.width(sum.toString() + name.charAt(i) + "...") < MAX_TEXT_WIDTH) {
                sum.append(name.charAt(i));
            } else {
                return sum + "...";
            }
        }
        return sum.toString();
    }
}
