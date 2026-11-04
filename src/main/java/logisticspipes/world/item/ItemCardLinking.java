package logisticspipes.world.item;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ServerExplosion;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;

import logisticspipes.world.item.component.LPDataComponents;

/**
 * Links blank item cards: an explosion that reaches blank cards lying on the ground together with
 * an ender pearl spares the cards and tunes all of them to one new frequency, using up the pearl.
 *
 * <p>Without a pearl, or on cards that already carry a frequency, the explosion does what it does
 * to any other item.
 */
public class ItemCardLinking {

    @SubscribeEvent
    public void onExplosionDetonate(ExplosionEvent.Detonate event) {
        link(event.getExplosion(), event.getAffectedEntities());
    }

    private static void link(ServerExplosion explosion, List<Entity> affected) {
        List<ItemEntity> cards = new ArrayList<>();
        ItemEntity pearl = null;
        for (Entity entity : affected) {
            if (!(entity instanceof ItemEntity item) || !isReached(explosion, item)) {
                continue;
            }
            ItemStack stack = item.getItem();
            if (stack.is(LPItems.ITEM_CARD.get()) && !stack.has(LPDataComponents.UUID)) {
                cards.add(item);
            } else if (pearl == null && stack.is(Items.ENDER_PEARL)) {
                pearl = item;
            }
        }
        if (cards.isEmpty() || pearl == null) {
            return;
        }

        // Out of the list, the explosion neither hurts nor pushes the cards
        affected.removeAll(cards);
        UUID frequency = UUID.randomUUID();
        for (ItemEntity card : cards) {
            ItemStack stack = card.getItem().copy();
            stack.set(LPDataComponents.UUID, frequency);
            card.setItem(stack);
        }
        // Any other pearl in the stack is left to the explosion
        ItemStack pearls = pearl.getItem().copy();
        pearls.shrink(1);
        if (pearls.isEmpty()) {
            affected.remove(pearl);
            pearl.discard();
        } else {
            pearl.setItem(pearls);
        }
    }

    /**
     * Whether the explosion actually reaches this item: the affected list holds everything in a
     * box twice the radius wide, reached or not.
     */
    private static boolean isReached(ServerExplosion explosion, ItemEntity item) {
        double reach = explosion.radius() * 2.0;
        return item.distanceToSqr(explosion.center()) <= reach * reach
            && ServerExplosion.getSeenPercent(explosion.center(), item) > 0.0F;
    }
}
