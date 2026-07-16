package logisticspipes.entity;

import javax.annotation.Nonnull;
import logisticspipes.LPConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.neoforge.common.util.FakePlayer;

public class FakePlayerLP extends FakePlayer {

  private final Component DISPLAY_NAME = Component.literal(
      String.format("[%s]", LPConstants.NAME.replace(" ", "")));

  public FakePlayerLP(ServerLevel world) {
    super(world, LPConstants.FAKE_GAMEPROFILE);
    this.setPos(0, 0, 0);
  }

  @Nonnull
  @Override
  public Component getDisplayName() {
    return DISPLAY_NAME;
  }

  @Override
  public void tick() {
  }
}
