/*
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.pipes;

import java.util.Collection;
import javax.annotation.Nonnull;
import logisticspipes.world.level.block.entity.LogisticsProgramCompilerBlockEntity;
import logisticspipes.blocks.LogisticsSecurityTileEntity;
import logisticspipes.blocks.powertile.LogisticsPowerJunctionTileEntity;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.modules.ModuleItemSink;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.textures.Textures;
import logisticspipes.textures.Textures.TextureType;
import logisticspipes.transport.PipeTransportLogistics;
import logisticspipes.utils.OrientationsUtil;
import logisticspipes.utils.item.ItemIdentifier;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;

public class PipeItemsBasicLogistics extends CoreRoutedPipe {

	private final ModuleItemSink itemSinkModule;

	public PipeItemsBasicLogistics(Item item) {
		super(new PipeTransportLogistics(true) {

			@Override
			public boolean canPipeConnect(BlockEntity tile, Direction dir) {
				if (super.canPipeConnect(tile, dir)) {
					return true;
				}
				if (tile instanceof LogisticsSecurityTileEntity) {
					Direction ori = OrientationsUtil.getOrientationOfTilewithTile(container, tile);
					return ori != null && ori != Direction.DOWN && ori != Direction.UP;
				}
				if (tile instanceof LogisticsProgramCompilerBlockEntity) {
					Direction ori = OrientationsUtil.getOrientationOfTilewithTile(container, tile);
					return ori != null && ori != Direction.DOWN;
				}
				return false;
			}
		}, item);
		itemSinkModule = new ModuleItemSink();
		itemSinkModule.registerHandler(this, this);
		itemSinkModule.registerPosition(ModulePositionType.IN_PIPE, 0);
	}

	@Override
	public TextureType getNonRoutedTexture(Direction connection) {
		if (isSecurityProvider(connection)) {
			return Textures.LOGISTICSPIPE_SECURITY_TEXTURE;
		}
		return super.getNonRoutedTexture(connection);
	}

	@Override
	public boolean isLockedExit(Direction orientation) {
		if (isPowerJunction(orientation) || isSecurityProvider(orientation)) {
			return true;
		}
		return super.isLockedExit(orientation);
	}

	private boolean isPowerJunction(Direction ori) {
		BlockEntity tilePipe = container.getTile(ori);
		if (tilePipe == null || !container.canPipeConnect(tilePipe, ori)) {
			return false;
		}

		return tilePipe instanceof LogisticsPowerJunctionTileEntity;
	}

	private boolean isSecurityProvider(Direction ori) {
		BlockEntity tilePipe = container.getTile(ori);
		if (tilePipe == null || !container.canPipeConnect(tilePipe, ori)) {
			return false;
		}
		return tilePipe instanceof LogisticsSecurityTileEntity;
	}

	@Override
	public TextureType getCenterTexture() {
		return Textures.LOGISTICSPIPE_TEXTURE;
	}

	@Override
	public @Nonnull ModuleItemSink getLogisticsModule() {
		return itemSinkModule;
	}

	@Override
	public ItemSendMode getItemSendMode() {
		return ItemSendMode.Normal;
	}

	@Override
	public void setTile(BlockEntity tile) {
		super.setTile(tile);
		itemSinkModule.registerPosition(ModulePositionType.IN_PIPE, 0);
	}

	@Override
	public void collectSpecificInterests(@Nonnull Collection<ItemIdentifier> itemIdentifiers) {
		if (!itemSinkModule.isDefaultRoute()) {
			itemSinkModule.collectSpecificInterests(itemIdentifiers);
		}
	}

	@Override
	public boolean hasGenericInterests() {
		return itemSinkModule.isDefaultRoute();
	}
}
