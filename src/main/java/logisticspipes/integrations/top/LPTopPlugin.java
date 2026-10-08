package logisticspipes.integrations.top;

import java.util.function.Function;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import mcjty.theoneprobe.api.ElementAlignment;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.ProbeMode;
import org.jspecify.annotations.Nullable;

import logisticspipes.LPConstants;
import logisticspipes.integrations.probe.PipeTooltip;
import logisticspipes.integrations.probe.PipeTooltipBuilder;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;

public class LPTopPlugin implements Function<ITheOneProbe, @Nullable Void> {

    @Override
    public @Nullable Void apply(ITheOneProbe probe) {
        probe.registerProvider(new PipeInfoProvider());
        return null;
    }

    private static final class PipeInfoProvider implements IProbeInfoProvider {

        private static final Identifier ID = LPConstants.rl("pipe");

        @Override
        public Identifier getID() {
            return ID;
        }

        @Override
        public void addProbeInfo(ProbeMode mode, IProbeInfo probeInfo, Player player, Level level,
            BlockState blockState, IProbeHitData data) {
            if (!(blockState.getBlock() instanceof LogisticsBlockGenericPipe)) {
                return;
            }
            PipeTooltip tooltip =
                PipeTooltipBuilder.build(level.getBlockEntity(data.getPos()), mode != ProbeMode.NORMAL);
            for (PipeTooltip.Line line : tooltip.lines()) {
                addLine(probeInfo, line);
            }
        }

        private static void addLine(IProbeInfo probeInfo, PipeTooltip.Line line) {
            Component text = line.text();
            boolean hasText = !text.getString().isEmpty();
            if (line.icons().isEmpty()) {
                if (hasText) {
                    probeInfo.text(text);
                }
                return;
            }
            IProbeInfo row = probeInfo.horizontal(
                probeInfo.defaultLayoutStyle().alignment(ElementAlignment.ALIGN_CENTER).spacing(3));
            for (ItemStack icon : line.icons()) {
                row.item(icon);
            }
            if (hasText) {
                row.text(text);
            }
        }
    }
}
