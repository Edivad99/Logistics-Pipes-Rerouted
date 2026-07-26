package logisticspipes.world.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import logisticspipes.LPConstants;

public class LPCreativeModeTabs {

    public static final DeferredRegister<CreativeModeTab> deferredRegister =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, LPConstants.ID);

    public static void register(IEventBus modEventBus) {
        deferredRegister.register(modEventBus);
    }

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MAIN_TAB =
        deferredRegister.register("logistics_pipes", () ->
            CreativeModeTab.builder()
                .icon(LPItems.PIPE_BASIC::toStack)
                .title(Component.translatable("itemGroup.logisticspipes"))
                .displayItems((params, output) -> {
                    LPItems.entries()
                        .forEach(reg -> output.accept(new ItemStack(reg.get())));
                })
                .build());
}
