package logisticspipes.world.item;

import java.util.Set;
import logisticspipes.LPConstants;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

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
            // Exclude BC (MJ) and IC2 (EU) power items — those mods are not ported to 1.20.1.
            Set<String> hidden = Set.of(
                "power_provider_eu", "power_provider_mj",
                "power_supplier_mj",
                "power_supplier_eu_lv", "power_supplier_eu_mv",
                "power_supplier_eu_hv", "power_supplier_eu_ev");
            LPItems.entries().stream()
                .filter(reg -> !hidden.contains(reg.getId().getPath()))
                .forEach(reg -> output.accept(new ItemStack(reg.get())));
          })
          .build());
}
