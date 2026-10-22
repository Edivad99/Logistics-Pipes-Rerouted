package logisticspipes.integrations.tesseract;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

import com.supermartijn642.tesseract.EnumChannelType;
import com.supermartijn642.tesseract.TesseractBlockEntity;
import com.supermartijn642.tesseract.manager.Channel;
import com.supermartijn642.tesseract.manager.TesseractReference;
import org.jspecify.annotations.Nullable;

import logisticspipes.connection.special.ISpecialBlockEntityConnection;
import logisticspipes.logisticspipes.IRoutedItem;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.proxy.SimpleServiceLocator;
import logisticspipes.utils.item.ItemIdentifierStack;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * Joins two routing pipes through a pair of linked Tesseracts, so they route to each other as if they
 * were neighbors.
 *
 * <p>An item leaves through the Tesseract like any other inventory and comes back out of the other
 * one as a fresh, unrouted item. {@link #transmit} hands its routing information ahead to the pipe on
 * the far side, which gives it back to the item as soon as it arrives.</p>
 *
 * <p>For that to work, the item must be able to end up in only one place. A Tesseract spreads what it
 * receives over every inventory around every other Tesseract on its item channel, so a link only
 * counts when:</p>
 *
 * <ul>
 *   <li>the two are the only loaded Tesseracts on their item channel;</li>
 *   <li>both are set to send and receive items, and neither is switched off by redstone;</li>
 *   <li>each has a single routing pipe next to it, connected to it, and nothing else that takes
 *   items.</li>
 * </ul>
 *
 * <p>The rules hold for both ends alike, so the link always works both ways, as a pipe connection
 * does.</p>
 */
public class TesseractConnection implements ISpecialBlockEntityConnection {

    @Override
    public boolean isType(BlockEntity blockEntity) {
        return blockEntity instanceof TesseractBlockEntity;
    }

    @Override
    public Collection<BlockEntity> getConnections(BlockEntity blockEntity) {
        LogisticsGenericPipeBlockEntity pipe = findLinkedPipe(blockEntity);
        return pipe == null ? List.of() : List.of(pipe);
    }

    @Override
    public boolean needsInformationTransition() {
        return true;
    }

    @Override
    public void transmit(BlockEntity blockEntity, IRoutedItem arrivingItem) {
        LogisticsGenericPipeBlockEntity pipe = findLinkedPipe(blockEntity);
        if (pipe != null && pipe.pipe instanceof CoreRoutedPipe routedPipe) {
            routedPipe.queueUnroutedItemInformation(
                new ItemIdentifierStack(arrivingItem.getItemIdentifierStack()), arrivingItem.getInfo());
        }
    }

    /**
     * The routing pipe on the far side of the link {@code blockEntity} sends into, or null when there is no
     * link that meets the rules in the class comment.
     */
    private static @Nullable LogisticsGenericPipeBlockEntity findLinkedPipe(BlockEntity blockEntity) {
        if (!(blockEntity instanceof TesseractBlockEntity tesseract)
            || blockEntity.getLevel() == null
            || blockEntity.getLevel().isClientSide() || linkedEndPipe(tesseract) == null) {
            return null;
        }
        TesseractReference self = tesseract.getReference();
        Channel channel = self.getChannel(EnumChannelType.ITEMS);
        if (channel == null) {
            return null;
        }

        List<TesseractBlockEntity> others = new ArrayList<>(1);
        for (TesseractReference reference : channel.tesseracts) {
            TesseractBlockEntity other = reference == self ? null : reference.getTesseract();
            if (other != null) {
                others.add(other);
            }
        }
        return others.size() == 1 ? linkedEndPipe(others.getFirst()) : null;
    }

    /**
     * The routing pipe at {@code tesseract}'s end of a link, or null when that end breaks one of the
     * rules in the class comment.
     */
    private static @Nullable LogisticsGenericPipeBlockEntity linkedEndPipe(TesseractBlockEntity tesseract) {
        if (!tesseract.canSend(EnumChannelType.ITEMS) || !tesseract.canReceive(EnumChannelType.ITEMS)) {
            return null;
        }
        LogisticsGenericPipeBlockEntity pipe = adjacentRoutingPipe(tesseract);
        // The pipe's own item handler is the one entry expected here; anything more could take the item.
        if (pipe == null || tesseract.getSurroundingItemCapabilities().size() != 1) {
            return null;
        }
        return pipe;
    }

    /**
     * The one routing pipe next to {@code tesseract} and connected to it, or null when there is none
     * or more than one.
     */
    private static @Nullable LogisticsGenericPipeBlockEntity adjacentRoutingPipe(TesseractBlockEntity tesseract) {
        LogisticsGenericPipeBlockEntity found = null;
        for (Direction direction : Direction.values()) {
            BlockEntity neighbor = tesseract.getLevel().getBlockEntity(tesseract.getBlockPos().relative(direction));
            if (neighbor instanceof LogisticsGenericPipeBlockEntity pipe
                && SimpleServiceLocator.pipeInformationManager.canConnect(tesseract, pipe, direction)) {
                if (found != null || !(pipe.pipe instanceof CoreRoutedPipe)) {
                    return null;
                }
                found = pipe;
            }
        }
        return found;
    }
}
