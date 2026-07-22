package logisticspipes.items;

import logisticspipes.blocks.LogisticsSolidBlock;
import logisticspipes.interfaces.ILogisticsItem;
import lombok.Getter;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;

import java.util.function.Consumer;

public class LogisticsSolidBlockItem extends BlockItem implements ILogisticsItem {

	/** meta → item map used by the legacy {@link logisticspipes.datafixer.DataFixerSolidBlockItems}. */
	public static final java.util.Map<Integer, LogisticsSolidBlockItem> updateItemMap = new java.util.HashMap<>();

	@Getter
	private final LogisticsSolidBlock.Type type;

	public LogisticsSolidBlockItem(LogisticsSolidBlock block, Properties properties) {
		super(block, properties);
		type = block.getType();
		updateItemMap.put(type.getMeta(), this);
	}

	public net.minecraft.network.chat.Component getHoverName(ItemStack itemstack) {
		return net.minecraft.network.chat.Component.literal(I18n.get(getDescriptionId(itemstack) + ".name"));
	}

	@Override
	public void initializeClient(Consumer<IClientItemExtensions> consumer) {
		consumer.accept(ClientExtensionsHolder.EXTENSIONS);
	}

	/** Client-only BEWLR holder, loaded lazily so dedicated servers don't touch it. */
	private static final class ClientExtensionsHolder {
		static final IClientItemExtensions EXTENSIONS =
			new IClientItemExtensions() {
				@Override
				public net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer getCustomRenderer() {
					return logisticspipes.renderer.LogisticsSolidBlockItemRenderer.instance();
				}
			};
	}
}
