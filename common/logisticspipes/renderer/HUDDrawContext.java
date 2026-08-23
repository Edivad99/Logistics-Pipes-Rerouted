package logisticspipes.renderer;

import javax.annotation.Nullable;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;


import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;

import logisticspipes.client.renderer.LPRenderTypes;

/**
 * The drawing surface the head-up display renders onto.
 *
 * <p>LP's HUD panels hang in the world above a pipe, billboarded towards the camera. Up to 1.21.5
 * they were drawn with a {@link net.minecraft.client.gui.GuiGraphics} whose pose had been loaded
 * with the camera orientation and a 3D translation -- a GUI object used for world geometry. 1.21.6
 * closed that door: {@code GuiGraphics.pose()} is a {@link org.joml.Matrix3x2fStack}, so it cannot
 * express a billboard, and {@code GuiGraphics} now writes into a {@code GuiRenderState} that is
 * rendered in screen space at the end of the frame.</p>
 *
 * <p>Picture-in-Picture is not the replacement: it draws to a texture and submits it to the
 * {@code GuiRenderState}, which also lands in screen space. The replacement is to stop pretending
 * and draw in world space directly, which is what this class does -- the same primitives the HUD
 * actually used, backed by the {@link PoseStack} and {@link MultiBufferSource} the level render
 * stage already provides.</p>
 *
 * <p>Coordinates are the panel-local pixel coordinates the HUD code has always used; the caller
 * sets up the pose so that one unit is one HUD pixel.</p>
 */
public class HUDDrawContext {

    private final PoseStack poseStack;
    private final MultiBufferSource.BufferSource bufferSource;
    private final int packedLight;
    private final Font font;
    /**
     * The render type the last primitive went into. A {@link MultiBufferSource.BufferSource} draws
     * its buffers in an order of its own choosing when the batch ends, not in call order, so two
     * overlapping primitives that use different types come out in an arbitrary order -- a panel
     * background can land on top of the buttons drawn after it. A GUI does not hit this because
     * GuiRenderState sorts by layer; here the fix is to end the batch whenever the type changes,
     * which turns call order back into paint order.
     */
    @Nullable
    private RenderType lastType;

    public HUDDrawContext(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource, int packedLight,
        Font font) {
        this.poseStack = poseStack;
        this.bufferSource = bufferSource;
        this.packedLight = packedLight;
        this.font = font;
    }

    public PoseStack pose() {
        return poseStack;
    }

    public Font font() {
        return font;
    }

    /**
     * Flushes everything drawn so far. The HUD draws one panel at a time and each panel owns its
     * pose, so batches cannot be left pending across panels the way a GUI would.
     */
    public void flush() {
        bufferSource.endBatch();
        lastType = null;
    }

    /** Draws what is pending when the render type changes, so call order is paint order. */
    private VertexConsumer beginning(RenderType type) {
        if (lastType != null && !lastType.equals(type)) {
            bufferSource.endBatch();
        }
        lastType = type;
        return bufferSource.getBuffer(type);
    }

    /** Text and items pick their own render types, so anything pending has to go out first. */
    private void beginningForeign() {
        if (lastType != null) {
            bufferSource.endBatch();
            lastType = null;
        }
    }

    /** Filled rectangle, ARGB. Replaces {@code GuiGraphics#fill}. */
    public void fill(int x0, int y0, int x1, int y1, int color) {
        if (x0 > x1) {
            int swap = x0;
            x0 = x1;
            x1 = swap;
        }
        if (y0 > y1) {
            int swap = y0;
            y0 = y1;
            y1 = swap;
        }
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = beginning(LPRenderTypes.HUD_FILL);
        int a = ARGB.alpha(color);
        int r = ARGB.red(color);
        int g = ARGB.green(color);
        int b = ARGB.blue(color);
        consumer.addVertex(matrix, x0, y1, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(matrix, x1, y1, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(matrix, x1, y0, 0.0f).setColor(r, g, b, a);
        consumer.addVertex(matrix, x0, y0, 0.0f).setColor(r, g, b, a);
    }

    /**
     * Textured rectangle. Replaces the {@code GuiGraphics#blit} overload the HUD used: {@code u}
     * and {@code v} are pixel offsets into a {@code texWidth} x {@code texHeight} sheet.
     */
    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height,
        int texWidth, int texHeight) {
        blit(texture, x, y, u, v, width, height, texWidth, texHeight, -1);
    }

    public void blit(ResourceLocation texture, int x, int y, float u, float v, int width, int height,
        int texWidth, int texHeight, int color) {
        Matrix4f matrix = poseStack.last().pose();
        VertexConsumer consumer = beginning(LPRenderTypes.HUD_TEXTURED.apply(texture));
        float u0 = u / texWidth;
        float u1 = (u + width) / texWidth;
        float v0 = v / texHeight;
        float v1 = (v + height) / texHeight;
        int a = ARGB.alpha(color);
        int r = ARGB.red(color);
        int g = ARGB.green(color);
        int b = ARGB.blue(color);
        consumer.addVertex(matrix, x, y + height, 0.0f).setUv(u0, v1).setColor(r, g, b, a);
        consumer.addVertex(matrix, x + width, y + height, 0.0f).setUv(u1, v1).setColor(r, g, b, a);
        consumer.addVertex(matrix, x + width, y, 0.0f).setUv(u1, v0).setColor(r, g, b, a);
        consumer.addVertex(matrix, x, y, 0.0f).setUv(u0, v0).setColor(r, g, b, a);
    }

    /**
     * Replaces {@code GuiGraphics#drawString}. {@code Font#drawInBatch} is the world-space entry
     * point -- it takes the matrix and buffer source directly, which is exactly what a GUI-less
     * caller has.
     */
    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        beginningForeign();
        font.drawInBatch(text, x, y, color, shadow, poseStack.last().pose(), bufferSource,
            Font.DisplayMode.NORMAL, 0, packedLight);
    }

    /** No-shadow overload, matching the {@code GuiGraphics} signature the HUD called. */
    public void drawString(Font font, String text, int x, int y, int color) {
        drawString(font, text, x, y, color, false);
    }

    public void drawString(String text, int x, int y, int color, boolean shadow) {
        drawString(font, text, x, y, color, shadow);
    }

    /** Replaces {@code GuiGraphics#drawCenteredString}. */
    public void drawCenteredString(Font font, String text, int centerX, int y, int color, boolean shadow) {
        drawString(font, text, centerX - font.width(text) / 2, y, color, shadow);
    }

    /**
     * Replaces {@code GuiGraphics#renderItem}. The GUI version bakes in its own 16x16 projection and
     * lighting; in world space the transform is ours, so the stack is scaled to the same 16 pixels
     * and flipped on Y, since HUD coordinates grow downwards while item models grow upwards.
     */
    public void renderItem(ItemStack stack, int x, int y) {
        if (stack.isEmpty()) {
            return;
        }
        beginningForeign();
        Minecraft minecraft = Minecraft.getInstance();

        // Resolved here rather than through ItemRenderer.renderStatic so the model can be asked
        // whether it is a flat sprite, which decides both of the adjustments below.
        ItemStackRenderState renderState = new ItemStackRenderState();
        minecraft.getItemModelResolver()
            .updateForTopItem(renderState, stack, ItemDisplayContext.GUI, minecraft.level, null, 0);
        boolean flat = !renderState.usesBlockLight();

        poseStack.pushPose();
        poseStack.translate(x + 8.0f, y + 8.0f, 0.0f);
        // A flat item is a sprite that the model generator extrudes into a thin slab. A GUI hides
        // that: it looks at the item head on through an orthographic projection. Here the camera is
        // perspective, so an item away from the centre of the screen is seen at a slight angle and
        // the unlit sides of the slab show up as dark slivers along its edge. Squashing z keeps the
        // sprite intact and makes the extrusion too thin to see; real 3D models keep their depth.
        poseStack.scale(16.0f, -16.0f, flat ? 1.0f : 16.0f);

        // Items need the GUI light set, not the level's. We are inside the world pass, so
        // Lighting.Entry.LEVEL is active and an item model rendered under it comes out dark --
        // vanilla's GuiRenderer switches around exactly this call, and picks between the two item
        // entries on the same usesBlockLight test. The batch has to be drawn while that set is
        // bound, since the lighting lives in a uniform buffer applied at draw time, and the level
        // set is restored afterwards for the rest of the world pass.
        Lighting lighting = minecraft.gameRenderer.getLighting();
        lighting.setupFor(flat ? Lighting.Entry.ITEMS_FLAT : Lighting.Entry.ITEMS_3D);
        renderState.render(poseStack, bufferSource, packedLight, OverlayTexture.NO_OVERLAY);
        bufferSource.endBatch();
        lighting.setupFor(Lighting.Entry.LEVEL);
        poseStack.popPose();
    }

    /**
     * Stack size and damage bar. {@code GuiGraphics#renderItemDecorations} is unavailable here, and
     * only the count is drawn: the damage bar is a GUI-space overlay with its own pipeline, and no
     * HUD panel shows damageable items.
     */
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        if (stack.isEmpty() || stack.getCount() == 1) {
            return;
        }
        String count = String.valueOf(stack.getCount());
        drawString(font, count, x + 19 - 2 - font.width(count), y + 6 + 3, 0xFFFFFFFF, true);
    }

    /** As above, with an explicit label -- LP formats its own counts for large stacks. */
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String label) {
        if (stack.isEmpty()) {
            return;
        }
        String text = label != null ? label : (stack.getCount() == 1 ? null : String.valueOf(stack.getCount()));
        if (text != null) {
            drawString(font, text, x + 19 - 2 - font.width(text), y + 6 + 3, 0xFFFFFFFF, true);
        }
    }

    /** The render type used for flat fills, exposed for callers that batch their own geometry. */
    public RenderType fillRenderType() {
        return LPRenderTypes.HUD_FILL;
    }

    public MultiBufferSource.BufferSource bufferSource() {
        return bufferSource;
    }

    public int packedLight() {
        return packedLight;
    }
}
