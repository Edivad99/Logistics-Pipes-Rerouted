package logisticspipes.integrations.jade;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.ui.JadeUI;

import logisticspipes.integrations.probe.PipeTooltip;

public enum PipeComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        PipeDataProvider.INSTANCE.decodeFromData(accessor).ifPresent(data -> {
            for (PipeTooltip.Line line : data.lines()) {
                appendLine(tooltip, line);
            }
        });
    }

    private static void appendLine(ITooltip tooltip, PipeTooltip.Line line) {
        // A lone row of icons is the whole line, so it gets full-size items.
        boolean iconsOnly = line.text().getString().isEmpty();
        boolean first = true;
        for (ItemStack icon : line.icons()) {
            var element = iconsOnly ? JadeUI.item(icon) : JadeUI.smallItem(icon);
            if (first) {
                tooltip.add(element);
                first = false;
            } else {
                tooltip.append(element);
            }
        }
        if (iconsOnly) {
            return;
        }
        Component text = line.text();
        if (first) {
            tooltip.add(text);
        } else {
            tooltip.append(text);
        }
    }

    @Override
    public Identifier getUid() {
        return PipeDataProvider.UID;
    }
}
