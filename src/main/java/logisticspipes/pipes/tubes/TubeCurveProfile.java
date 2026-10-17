package logisticspipes.pipes.tubes;

/**
 * The S-shaped profile items follow through the S curve (sideways) and the gain tube (upwards), fitted as a quintic
 * over {@code a} from 0 to 3.
 */
final class TubeCurveProfile {

    private TubeCurveProfile() {}

    /**
     * Displacement of the center line, from 0 at {@code a = 0} to about -0.75 at {@code a = 3}.
     */
    static double offset(double a) {
        return -0.030238483815369 * Math.pow(a, 5) +
            0.225914176523007 * Math.pow(a, 4) -
            0.502711673373567 * Math.pow(a, 3) +
            0.233256545765967 * Math.pow(a, 2) -
            0.074807924321475 * a + 0.000099653425518;
    }

    /**
     * Derivative of {@link #offset(double)}: how steeply the center line turns at {@code a}.
     */
    static double slope(double a) {
        return -0.15119241907684 * Math.pow(a, 4) +
            0.903656706092028 * Math.pow(a, 3) -
            1.50813502012070 * Math.pow(a, 2) +
            0.466513091531934 * a - 0.074807924321475;
    }
}
