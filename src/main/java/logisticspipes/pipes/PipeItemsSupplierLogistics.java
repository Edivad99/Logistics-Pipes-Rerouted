package logisticspipes.pipes;

import java.util.List;

import net.minecraft.world.item.Item;

import org.jspecify.annotations.Nullable;

import logisticspipes.interfaces.routing.IAdditionalTargetInformation;
import logisticspipes.interfaces.routing.IRequestItems;
import logisticspipes.interfaces.routing.IRequireReliableTransport;
import logisticspipes.modules.LogisticsModule.ModulePositionType;
import logisticspipes.modules.ModuleActiveSupplier;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.debug.StatusEntry;
import logisticspipes.textures.PipeTexture;
import logisticspipes.utils.item.ItemIdentifierStack;

public class PipeItemsSupplierLogistics extends CoreRoutedPipe implements IRequestItems, IRequireReliableTransport {

    private final ModuleActiveSupplier supplierModule;

    public PipeItemsSupplierLogistics(Item item) {
        super(item);
        supplierModule = new ModuleActiveSupplier();
        supplierModule.registerHandler(this, this);
        supplierModule.registerPosition(ModulePositionType.IN_PIPE, 0);
    }

    @Override
    public PipeTexture getCenterTexture() {
        return PipeTexture.SUPPLIER;
    }

    @Override
    public ModuleActiveSupplier getLogisticsModule() {
        return supplierModule;
    }

    @Override
    public ItemSendMode getItemSendMode() {
        return ItemSendMode.NORMAL;
    }

    @Override
    public void itemLost(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info) {
        supplierModule.itemLost(item, info);
    }

    @Override
    public void itemArrived(ItemIdentifierStack item, @Nullable IAdditionalTargetInformation info) {
        supplierModule.itemArrived(item, info);
    }

    @Override
    public void addStatusInformation(List<StatusEntry> status) {
        super.addStatusInformation(status);
        supplierModule.addStatusInformation(status);
    }
}
