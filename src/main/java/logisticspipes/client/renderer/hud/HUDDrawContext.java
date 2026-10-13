package logisticspipes.client.renderer.hud;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeStorage;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Style;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.StringDecomposer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.vertex.PoseStack;
import org.jspecify.annotations.Nullable;

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
 * actually used, backed by the level render stage's {@link PoseStack} and a
 * {@link SubmitNodeStorage} of the HUD's own. 26.2 removed {@code MultiBufferSource}: geometry is
 * submitted to a storage and drawn when the feature dispatcher renders it.</p>
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
    private final SubmitNodeStorage storage;
    private final int packedLight;
    private final Font font;
    /**
     * The render type the last primitive went into. The feature dispatcher batches a collection's
     * submissions by render type and draws its text before its custom geometry, not in call order,
     * so two overlapping primitives that use different types come out in an arbitrary order -- a
     * panel background can land on top of the buttons drawn after it. A GUI does not hit this
     * because GuiRenderState sorts by layer; here the fix is to move to the next {@link #order}
     * whenever the type changes, which turns call order back into paint order.
     */
    @Nullable
    private RenderType lastType;
    /** The storage order primitives are currently submitted under; orders are drawn ascending. */
    private int order;

    public HUDDrawContext(PoseStack poseStack, SubmitNodeStorage storage, int packedLight, Font font) {
        this.poseStack = poseStack;
        this.storage = storage;
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
        Minecraft.getInstance().gameRenderer.featureRenderDispatcher().renderAllFeatures(storage);
        lastType = null;
        order = 0;
    }

    /** Moves to the next order when the render type changes, so call order is paint order. */
    private void beginning(RenderType type) {
        if (lastType != null && !lastType.equals(type)) {
            order++;
        }
        lastType = type;
    }

    /**
     * Text picks its own render types and is drawn ahead of the custom geometry sharing its order,
     * so it has to start a new one to land on top of what is pending.
     */
    private void beginningForeign() {
        if (lastType != null) {
            order++;
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
        beginning(LPRenderTypes.HUD_FILL);
        int a = ARGB.alpha(color);
        int r = ARGB.red(color);
        int g = ARGB.green(color);
        int b = ARGB.blue(color);
        // The collector snapshots the pose and runs this when the storage is rendered.
        final int left = x0, top = y0, right = x1, bottom = y1;
        storage.order(order).submitCustomGeometry(poseStack, LPRenderTypes.HUD_FILL, (pose, consumer) -> {
            consumer.addVertex(pose, left, bottom, 0.0f).setColor(r, g, b, a);
            consumer.addVertex(pose, right, bottom, 0.0f).setColor(r, g, b, a);
            consumer.addVertex(pose, right, top, 0.0f).setColor(r, g, b, a);
            consumer.addVertex(pose, left, top, 0.0f).setColor(r, g, b, a);
        });
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
        RenderType type = LPRenderTypes.HUD_TEXTURED.apply(texture);
        beginning(type);
        float u0 = u / texWidth;
        float u1 = (u + width) / texWidth;
        float v0 = v / texHeight;
        float v1 = (v + height) / texHeight;
        int a = ARGB.alpha(color);
        int r = ARGB.red(color);
        int g = ARGB.green(color);
        int b = ARGB.blue(color);
        storage.order(order).submitCustomGeometry(poseStack, type, (pose, consumer) -> {
            consumer.addVertex(pose, x, y + height, 0.0f).setUv(u0, v1).setColor(r, g, b, a);
            consumer.addVertex(pose, x + width, y + height, 0.0f).setUv(u1, v1).setColor(r, g, b, a);
            consumer.addVertex(pose, x + width, y, 0.0f).setUv(u1, v0).setColor(r, g, b, a);
            consumer.addVertex(pose, x, y, 0.0f).setUv(u0, v0).setColor(r, g, b, a);
        });
    }

    /**
     * Replaces {@code GuiGraphicsExtractor#drawString}. 26.2 removed {@code Font#drawInBatch}; text
     * is a submission like any other and the text feature renderer draws it with the game font, so
     * {@code font} only measures here.
     */
    public void drawString(Font font, String text, int x, int y, int color, boolean shadow) {
        beginningForeign();
        // What Font#prepareText does to a String: shaped if the language needs it, legacy formatting
        // codes honoured.
        String shaped = font.isBidirectional() ? font.bidirectionalShaping(text) : text;
        FormattedCharSequence sequence = sink -> StringDecomposer.iterateFormatted(shaped, Style.EMPTY, sink);
        storage.order(order).submitText(poseStack, x, y, sequence, shadow,
            Font.DisplayMode.NORMAL, packedLight, color, 0, 0);
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
        // Drawn on its own so the item lands in call order, like every other primitive here: an
        // item is sorted into the solid or translucent phase by its own quads, not by order.
        flush();
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
        renderState.submit(poseStack, storage, packedLight, OverlayTexture.NO_OVERLAY, 0);
        flush();
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

    public int packedLight() {
        return packedLight;
    }
}
