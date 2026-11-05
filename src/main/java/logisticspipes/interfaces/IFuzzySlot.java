package logisticspipes.interfaces;

import java.util.EnumSet;

import logisticspipes.api.property.IBitSet;
import logisticspipes.utils.FuzzyFlag;

/**
 * A slot whose item can be matched loosely, with flags the player picks on it.
 */
public interface IFuzzySlot {

    /**
     * Which of the four flags this slot actually honours: a screen offers only these, so a module
     * does not advertise matching it will then ignore. All four by default.
     */
    default EnumSet<FuzzyFlag> getUsedFlags() {
        return EnumSet.allOf(FuzzyFlag.class);
    }

    IBitSet getFuzzyFlags();

    int getX();

    int getY();

    int getSlotId();
}
