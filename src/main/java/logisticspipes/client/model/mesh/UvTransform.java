package logisticspipes.client.model.mesh;

/**
 * An affine transform in UV space: {@code u' = u * scaleU + offsetU}.
 *
 * <p>Replaces the CodeChickenLib-era operation objects {@code LPUVScale},
 * {@code LPUVTranslation}, {@code LPUVTransformationList} and {@code LPUvOp}, which
 * modelled the same thing as a tagged union walked per vertex per frame. Composition
 * is closed under this form, so an arbitrary chain collapses into a single record at
 * bake time instead of being re-walked for every vertex.</p>
 */
public record UvTransform(float scaleU, float scaleV, float offsetU, float offsetV) {

    public static final UvTransform IDENTITY = new UvTransform(1, 1, 0, 0);

    public static UvTransform scale(float su, float sv) {
        return new UvTransform(su, sv, 0, 0);
    }

    public static UvTransform translate(float du, float dv) {
        return new UvTransform(1, 1, du, dv);
    }

    /**
     * The transform equivalent to applying {@code this} first, then {@code next}.
     */
    public UvTransform andThen(UvTransform next) {
        return new UvTransform(
            scaleU * next.scaleU,
            scaleV * next.scaleV,
            offsetU * next.scaleU + next.offsetU,
            offsetV * next.scaleV + next.offsetV);
    }

    public float applyU(float u) {
        return u * scaleU + offsetU;
    }

    public float applyV(float v) {
        return v * scaleV + offsetV;
    }

    public boolean isIdentity() {
        return scaleU == 1 && scaleV == 1 && offsetU == 0 && offsetV == 0;
    }
}
