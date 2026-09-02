package logisticspipes.world.item;

import java.util.UUID;
import java.util.function.Consumer;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

import logisticspipes.proxy.SimpleServiceLocator;

/**
 * A card carrying the id of a security station, handed out by that station.
 *
 * <p>It cannot be dropped: a card lying on the ground would hand its holder's authorisation to
 * whoever picked it up. Throwing one puts it back in the inventory rather than losing it -- see
 * {@code LogisticsEventListener.onItemToss}.
 */
public class LogisticsSecurityCard extends LogisticsItemCard {

    public LogisticsSecurityCard(Properties properties) {
        super(properties);
    }

    @Override
    protected void appendDetails(ItemStack stack, UUID id, Consumer<Component> tooltipAdder) {
        tooltipAdder.accept(Component.literal("Authorization: "
            + (SimpleServiceLocator.securityStationManager.isAuthorized(id) ? "Authorized" : "Unauthorized")));
    }

    @Override
    public boolean canExistInWorld(ItemStack stack) {
        return false;
    }
}
