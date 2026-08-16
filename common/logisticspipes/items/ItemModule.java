package logisticspipes.items;

import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import logisticspipes.interfaces.IPipeServiceProvider;
import logisticspipes.interfaces.IWorldProvider;
import logisticspipes.logisticspipes.ItemModuleInformationManager;
import logisticspipes.modules.LogisticsModule;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.pipes.basic.CoreUnroutedPipe;
import logisticspipes.pipes.basic.LogisticsBlockGenericPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.proxy.MainProxy;
import logisticspipes.utils.DummyLevelProvider;
import logisticspipes.utils.item.ItemIdentifierInventory;
import logisticspipes.utils.item.ItemIdentifierStack;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import network.rs485.logisticspipes.module.Gui;
import network.rs485.logisticspipes.util.TextUtil;

public class ItemModule extends LogisticsItem {

	private static class Module {

		private final Supplier<? extends LogisticsModule> moduleConstructor;
		private final Class<? extends LogisticsModule> moduleClass;

		private Module(Supplier<? extends LogisticsModule> moduleConstructor) {
			this.moduleConstructor = moduleConstructor;
			this.moduleClass = moduleConstructor.get().getClass();
		}

		private LogisticsModule getILogisticsModule() {
			if (moduleConstructor == null) {
				return null;
			}
			return moduleConstructor.get();
		}

		private Class<? extends LogisticsModule> getILogisticsModuleClass() {
			return moduleClass;
		}

	}

	private final Module moduleType;

	private ItemModule(Module moduleType, Properties properties) {
		super(properties);
		this.moduleType = moduleType;
	}

	/** Factory for use with DeferredRegister. */
	public static ItemModule of(Supplier<? extends LogisticsModule> moduleConstructor, Properties properties) {
		return new ItemModule(new Module(moduleConstructor), properties);
	}

	@Nullable
	public static LogisticsModule getLogisticsModule(Player player, int invSlot) {
		ItemStack item = player.getInventory().items.get(invSlot);
		if (item.isEmpty() || !(item.getItem() instanceof ItemModule itemModule)) {
            return null;
        }
		LogisticsModule module = itemModule.getModuleForItem(item, null, new DummyLevelProvider(player.level()), null);
		if (module == null) {
            return null;
        }
		module.registerPosition(ModulePositionType.IN_HAND, invSlot);
		ItemModuleInformationManager.readInformation(item, module);
		return module;
	}

	private void openConfigGui(ItemStack stack, Player player, Level level) {
		LogisticsModule module = getModuleForItem(stack, null, new DummyLevelProvider(level), null);
		if (module instanceof Gui && !stack.isEmpty()) {
			module.registerPosition(ModulePositionType.IN_HAND, player.getInventory().selected);
			ItemModuleInformationManager.readInformation(stack, module);
			Gui.getInHandGuiProvider((Gui) module).open(player);
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		LogisticsModule module = getModuleForItem(stack, null, null, null);
		if (module != null) {
			if (stack.getCount() > 0) {
				return module.hasEffect();
			}
		}
		return false;
	}

	@Override
    public InteractionResultHolder<ItemStack> use(final Level level, final Player player,
			final InteractionHand hand) {
		if (MainProxy.isServer(player.level())) {
			openConfigGui(player.getItemInHand(hand), player, level);
		}
		return super.use(level, player, hand);
	}

	@Override
    public InteractionResult useOn(UseOnContext context) {
		Player player = context.getPlayer();
		Level level = context.getLevel();
		BlockPos pos = context.getClickedPos();
		InteractionHand hand = context.getHand();
		if (player != null && MainProxy.isServer(player.level())) {
			BlockEntity tile = level.getBlockEntity(pos);
			if (tile instanceof LogisticsTileGenericPipe) {
				if (player.getDisplayName().getString()
						.equals("ComputerCraft")) { // Allow turtle to place modules in pipes.
					CoreUnroutedPipe pipe = LogisticsBlockGenericPipe.getPipe(level, pos);
					if (LogisticsBlockGenericPipe.isValid(pipe)) {
						pipe.blockActivated(player);
					}
				}
				return InteractionResult.PASS;
			}
			openConfigGui(player.getItemInHand(hand), player, level);
		}
		return InteractionResult.PASS;
	}

	@Nullable
	public LogisticsModule getModule(
			@Nullable LogisticsModule currentModule,
			@Nullable IWorldProvider world,
			@Nullable IPipeServiceProvider service
	) {
		if (currentModule != null) {
			if (moduleType.getILogisticsModuleClass().equals(currentModule.getClass())) {
				return currentModule;
			}
		}
		LogisticsModule newModule = moduleType.getILogisticsModule();
		if (newModule == null) {
			return null;
		}
		newModule.registerHandler(world, service);
		return newModule;
	}

	@Nullable
	public LogisticsModule getModuleForItem(
			ItemStack itemStack,
			@Nullable LogisticsModule currentModule,
			@Nullable IWorldProvider world,
			@Nullable IPipeServiceProvider service
	) {

		if (itemStack.isEmpty()) {
			return null;
		}
		if (itemStack.getItem() != this) {
			return null;
		}
		return getModule(currentModule, world, service);
	}

	@Override
	public String getModelSubdir() {
		return "module";
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltipComponents, TooltipFlag tooltipFlag) {
		if (stack.has(DataComponents.CUSTOM_DATA)) {
			CompoundTag nbt = Objects.requireNonNull(stack.get(DataComponents.CUSTOM_DATA)).copyTag();

			if (nbt.contains("informationList")) {
				if (Screen.hasShiftDown()) {
					ListTag nbttaglist = nbt.getList("informationList", 8);
					for (int i = 0; i < nbttaglist.size(); i++) {
						Tag nbtTag = nbttaglist.get(i);
						String data = nbtTag.getAsString();
						if (data.equals("<inventory>") && i + 1 < nbttaglist.size()) {
							nbtTag = nbttaglist.get(i + 1);
							data = nbtTag.getAsString();
							HolderLookup.Provider registries = context.registries();
							if (data.startsWith("<that>") && registries != null) {
								String prefix = data.substring(6);
								CompoundTag module = nbt.getCompound("moduleInformation");
								int size = module.getList(prefix + "items", module.getId()).size();
								if (module.contains(prefix + "itemsCount")) {
									size = module.getInt(prefix + "itemsCount");
								}
								ItemIdentifierInventory inv =
                                    new ItemIdentifierInventory(size, "InformationTempInventory", Integer.MAX_VALUE);
								inv.readFromNBT(module, registries, prefix);
								for (int pos = 0; pos < inv.getContainerSize(); pos++) {
									ItemIdentifierStack identStack = inv.getIDStackInSlot(pos);
									if (identStack != null) {
										if (identStack.getStackSize() > 1) {
											tooltipComponents.add(Component.literal("  " + identStack.getStackSize() + "x " + identStack.getFriendlyName()));
										} else {
											tooltipComponents.add(Component.literal("  " + identStack.getFriendlyName()));
										}
									}
								}
							}
							i++;
						} else {
							tooltipComponents.add(Component.literal(data));
						}
					}
				} else {
					TextUtil.addTooltipInformation(stack, tooltipComponents, false);
				}
			} else {
				TextUtil.addTooltipInformation(stack, tooltipComponents, Screen.hasShiftDown());
			}
		} else {
			TextUtil.addTooltipInformation(stack, tooltipComponents, Screen.hasShiftDown());
		}
	}
}
