package logisticspipes.renderer;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.FluidModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;

import net.neoforged.neoforge.client.fluid.FluidTintSource;

import com.mojang.blaze3d.platform.NativeImage;

import logisticspipes.utils.FluidIdentifier;

/**
 * Fluid window rendering for the logistics fluid container item.
 *
 * <p>The item model switches to {@code fluid_container_filled} (via the
 * {@code logisticspipes:fluid} predicate registered here) when the stack holds a
 * fluid; that model's layer1 is LP1's window stencil, tinted by the item color
 * handler below. LP1 drew the actual fluid sprite through the stencil; tinting the
 * stencil with the fluid's dominant color (average of its still texture multiplied
 * by the fluid's tint color) reads the same at item scale.</p>
 */
public class FluidContainerRenderer {

    private static final Map<Fluid, Integer> SPRITE_AVERAGE_CACHE = new HashMap<>();

    public static int getFluidColor(ItemStack stack) {
        FluidIdentifier ident = FluidIdentifier.get(stack);
        if (ident == null) {
            return 0xFFFFFFFF;
        }
        Fluid fluid = ident.getFluid();
        try {
            FluidState state = fluid.defaultFluidState();
            FluidModel model = Minecraft.getInstance().getModelManager()
                .getFluidStateModelSet().get(state);
            int color = SPRITE_AVERAGE_CACHE.computeIfAbsent(fluid,
                ignored -> averageSpriteColor(model.stillMaterial().sprite()));
            FluidTintSource tint = model.fluidTintSource();
            if (tint != null) {
                color = multiplyColors(color, tintColor(tint, state));
            }
            return color;
        } catch (Exception ignored) {
            // Defensive: a broken third-party fluid must not crash item rendering.
            return 0xFFFFFFFF;
        }
    }

    private static int tintColor(FluidTintSource tint, FluidState state) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null && minecraft.player != null) {
            return tint.colorInWorld(state.createLegacyBlock(), minecraft.level,
                minecraft.player.blockPosition());
        }
        return tint.color(state);
    }

    private static int averageSpriteColor(TextureAtlasSprite sprite) {
        NativeImage image = sprite.contents().getOriginalImage();
        try {
            long r = 0, g = 0, b = 0, n = 0;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int argb = image.getPixel(x, y);
                    if (ARGB.alpha(argb) < 128) {
                        continue;
                    }
                    r += ARGB.red(argb);
                    g += ARGB.green(argb);
                    b += ARGB.blue(argb);
                    n++;
                }
            }
            if (n == 0) {
                return 0xFFFFFFFF;
            }
            return 0xFF000000 | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
        } catch (Exception e) {
            return 0xFFFFFFFF;
        }
    }

    private static int multiplyColors(int c1, int c2) {
        int r = (((c1 >> 16) & 0xff) * ((c2 >> 16) & 0xff)) / 255;
        int g = (((c1 >> 8) & 0xff) * ((c2 >> 8) & 0xff)) / 255;
        int b = ((c1 & 0xff) * (c2 & 0xff)) / 255;
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }
}
