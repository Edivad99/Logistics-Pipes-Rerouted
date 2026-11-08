package logisticspipes.inventory;

import java.util.Map;
import java.util.Set;

import logisticspipes.utils.item.ItemIdentifier;

/**
 * An autocrafting system behind an inventory, such as an Applied Energistics 2 network behind an ME
 * Interface, that a provider can ask to make what it does not have. What it makes is expected to
 * show up in that same inventory, where the provider picks it up.
 */
public interface IExternalCrafter {

    /**
     * Everything the system knows how to make right now.
     */
    Set<ItemIdentifier> getCraftables();

    boolean canCraft(ItemIdentifier item);

    /**
     * Works out, without starting anything, how many of {@code amount} the system can make with
     * what it has now. Called on the server thread, while a request is being worked out.
     */
    Calculation calculate(ItemIdentifier item, int amount);

    /**
     * Asks the system to make {@code amount} of {@code item}. The answer may take a few ticks, so
     * the returned job has to be polled.
     */
    Job startCrafting(ItemIdentifier item, int amount);

    /**
     * The outcome of {@link #calculate}.
     *
     * @param craftable how many the system can make, at most the amount asked for
     * @param missing   what the system lacks to make the whole amount; empty when it can, or when
     *                  it did not say
     */
    record Calculation(int craftable, Map<ItemIdentifier, Integer> missing) {

        /**
         * The answer when the system could not be asked: assume it can, and let the job say.
         */
        public static Calculation assumeCraftable(int amount) {
            return new Calculation(amount, Map.of());
        }
    }

    /**
     * A request handed to the system.
     */
    interface Job {

        /**
         * Moves the request along and says where it stands. Called on the server thread.
         */
        State poll();

        /**
         * Withdraws the request if the system has not taken it on yet.
         */
        void cancel();
    }

    enum State {
        /**
         * The system is still working out whether it can make it.
         */
        PENDING,
        /**
         * The system took the request on; the result will show up in the inventory.
         */
        ACCEPTED,
        /**
         * The system cannot make it, for instance because ingredients are missing.
         */
        FAILED
    }
}
