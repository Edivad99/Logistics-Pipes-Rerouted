package logisticspipes.entity;

import java.util.UUID;
import javax.annotation.Nonnull;
import com.mojang.authlib.GameProfile;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;

@SuppressWarnings("EntityConstructor")
public class FakePlayerLP extends FakePlayer {

	public static GameProfile LPPLAYER = new GameProfile(UUID.fromString("e7d8e347-3828-4f39-b76f-ea519857c004"), "[LogisticsPipes]");

	public String myName = "[LogisticsPipes]";

	public FakePlayerLP(ServerLevel world) {
		super(world, LPPLAYER);
		this.setPos(0, 0, 0);
	}

	@Nonnull
	@Override
	public Component getDisplayName() {
		return Component.literal(myName);
	}

	@Override
	public void tick() { }
}
