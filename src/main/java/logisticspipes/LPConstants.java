package logisticspipes;

import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

import com.mojang.authlib.GameProfile;

public class LPConstants {

    public static final String ID = "logisticspipes";
    public static final String NAME = "Logistics Pipes";
    private static final String LP_PLAYER = "[" + ID + "]";
    public static final GameProfile FAKE_GAMEPROFILE =
        new GameProfile(UUID.nameUUIDFromBytes(LP_PLAYER.getBytes()), LP_PLAYER);

    private LPConstants() {
    }

    public static ResourceLocation rl(String path) {
        return ResourceLocation.fromNamespaceAndPath(ID, path);
    }

    public static final float FACADE_THICKNESS = 2F / 16F;
    public static final float PIPE_NORMAL_SPEED = 0.01F;
    public static final float PIPE_MIN_POS = 0.1875F;
    public static final float PIPE_MAX_POS = 0.8125F;
    public static final float BC_PIPE_MIN_POS = 0.25F;
    public static final float BC_PIPE_MAX_POS = 0.75F;

    public static final String RS_MOD_ID = "refinedstorage";

    public static final String computerCraftModID = "computercraft";
    public static final String openComputersModID = "opencomputers";
}
