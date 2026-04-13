package logisticspipes.interfaces;

/**
 * Replaces the removed net.minecraft.util.ITickable from 1.12.2.
 * Block entities that need per-tick work implement this; their block class
 * must override getTicker() to call update() each tick.
 */
public interface ITickable {
    void update();
}
