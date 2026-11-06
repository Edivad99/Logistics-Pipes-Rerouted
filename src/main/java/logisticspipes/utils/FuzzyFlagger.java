package logisticspipes.utils;

/**
 * Whether a flag is set, without saying where the bits live.
 */
@FunctionalInterface
public interface FuzzyFlagger {

    boolean test(FuzzyFlag flag);
}
