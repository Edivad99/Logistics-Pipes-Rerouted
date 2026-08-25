package logisticspipes.renderer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.renderer.ImmediateSubmitCollector;
import logisticspipes.client.renderer.LPRenderTypes;

/**
 * The drawing surface the head-up display renders onto.
 *
 * <p>LP's HUD panels hang in the world above a pipe, billboarded towards the camera. Up to 1.21.5
 * they were drawn with a {@link net.minecraft.client.gui.GuiGraphicsExtractor} whose pose had been loaded
 * with the camera orientation and a 3D translation -- a GUI object used for world geometry. 1.21.6
 * closed that door: {@code GuiGraphicsExtractor.pose()} is a {@link org.joml.Matrix3x2fStack}, so it cannot
 * express a billboard, and {@code GuiGraphicsExtractor} now writes into a {@code GuiRenderState} that is
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

    /**
     * How far towards the camera the stack-size label sits, in HUD pixels. An item is scaled by 16
     * on every axis, so a block model rotated into the GUI display pose reaches about 14 units
     * either side of the panel plane; 16 clears it. The panel itself is scaled to 0.008 blocks per
     * unit, so this is about 13 centimetres in world terms -- far too little to read as detached.
     */
    private static final float ITEM_LABEL_DEPTH = 16.0f;

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

    /** Filled rectangle, ARGB. Replaces {@code GuiGraphicsExtractor#fill}. */
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
     * Textured rectangle. Replaces the {@code GuiGraphicsExtractor#blit} overload the HUD used: {@code u}
     * and {@code v} are pixel offsets into a {@code texWidth} x {@code texHeight} sheet.
     */
    public void blit(Identifier texture, int x, int y, float u, float v, int width, int height,
        int texWidth, int texHeight) {
        blit(texture, x, y, u, v, width, height, texWidth, texHeight, -1);
    }

    public void blit(Identifier texture, int x, int y, float u, float v, int width, int height,
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
     * Replaces {@code GuiGraphicsExtractor#drawString}. {@code Font#drawInBatch} is the world-space entry
     * point -- it takes the matrix and buffer source directly, which is exactly what a GUI-less
     * caller has.
     */
    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        beginningForeign();
        font.drawInBatch(text, x, y, color, shadow, poseStack.last().pose(), bufferSource,
            Font.DisplayMode.NORMAL, 0, packedLight);
    }

    /** No-shadow overload, matching the {@code GuiGraphicsExtractor} signature the HUD called. */
    public void drawString(Font font, String text, int x, int y, int color) {
        drawString(font, text, x, y, color, false);
    }

    public void drawString(String text, int x, int y, int color, boolean shadow) {
        drawString(font, text, x, y, color, shadow);
    }

    /** Replaces {@code GuiGraphicsExtractor#drawCenteredString}. */
    public void drawCenteredString(Font font, String text, int centerX, int y, int color, boolean shadow) {
        drawString(font, text, centerX - font.width(text) / 2, y, color, shadow);
    }

    /**
     * Replaces {@code GuiGraphicsExtractor#renderItem}. The GUI version bakes in its own 16x16 projection and
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
        // Negative on y *and* z, which together are a 180 degree turn about x rather than a
        // mirror. A GUI gets away with scale(s, -s, s) because its projection is built with
        // invertY -- see GuiItemAtlas.render -- so the two mirrorings cancel; out here the
        // projection is the ordinary perspective camera, and a lone negative left the whole item
        // reflected: back-face culling kept the far faces, so a chest showed its own hollow
        // interior, and every normal pointed the wrong way, so lit-from-above surfaces came out
        // dark. The panel's +z points away from the camera, so flipping z is also what turns the
        // model around to face the viewer.
        //
        // A flat item is a sprite the model generator extrudes into a thin slab. A GUI hides that
        // by looking at it head on through an orthographic projection; here the camera is
        // perspective, so an item away from the centre of the screen is seen at a slight angle and
        // the unlit sides of the slab show up as dark slivers along its edge. Squashing z keeps the
        // sprite intact and makes the extrusion too thin to see; real 3D models keep their depth.
        poseStack.scale(16.0f, -16.0f, flat ? -1.0f : -16.0f);

        // Lighting stays on Lighting.Entry.LEVEL, the world pass's own set, rather than switching
        // to the ITEMS_* sets a GUI uses. Those are not neutral: ITEMS_3D bakes its light
        // directions through scaling(1, -1, 1) plus the GUI's own item rotation, so they only make
        // sense for geometry mirrored the GUI way and viewed from the GUI's angle. Applied to an
        // upright item in the world they light it from underneath. LEVEL's directions are the
        // plain ones, coming from above, which is what an item standing upright in the world wants.
        //
        // 1.21.9 routes item drawing through a SubmitNodeCollector, which a level-stage listener
        // has no way to obtain; ImmediateSubmitCollector adapts back onto the buffer source we do
        // have, and the drawing itself is still vanilla's.
        renderState.submit(poseStack, new ImmediateSubmitCollector(bufferSource), packedLight,
            OverlayTexture.NO_OVERLAY, 0);
        // Drawn now so the item lands in call order, like every other primitive here.
        bufferSource.endBatch();
        poseStack.popPose();
    }

    /**
     * Stack size and damage bar. {@code GuiGraphicsExtractor#renderItemDecorations} is unavailable here, and
     * only the count is drawn: the damage bar is a GUI-space overlay with its own pipeline, and no
     * HUD panel shows damageable items.
     */
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y) {
        if (stack.isEmpty() || stack.getCount() == 1) {
            return;
        }
        String count = String.valueOf(stack.getCount());
        drawCountLabel(font, count, x, y);
    }

    /** As above, with an explicit label -- LP formats its own counts for large stacks. */
    public void renderItemDecorations(Font font, ItemStack stack, int x, int y, @Nullable String label) {
        if (stack.isEmpty()) {
            return;
        }
        String text = label != null ? label : (stack.getCount() == 1 ? null : String.valueOf(stack.getCount()));
        if (text != null) {
            drawCountLabel(font, text, x, y);
        }
    }

    /**
     * A GUI puts the count on top of the item by opening a new stratum, which does not exist
     * outside GuiRenderState. Here the item is real geometry that writes depth and, being a block
     * model scaled by 16, reaches several units towards the camera -- so a label drawn on the
     * panel plane loses the depth test against the very item it belongs to and comes out sunk into
     * it. Moving the label towards the viewer past the item's own depth is what puts it in front.
     */
    private void drawCountLabel(Font font, String text, int x, int y) {
        poseStack.pushPose();
        poseStack.translate(0.0f, 0.0f, -ITEM_LABEL_DEPTH);
        drawString(font, text, x + 19 - 2 - font.width(text), y + 6 + 3, 0xFFFFFFFF, true);
        poseStack.popPose();
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
