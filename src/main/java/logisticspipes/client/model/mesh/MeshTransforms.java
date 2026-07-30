package logisticspipes.client.model.mesh;

import org.joml.Matrix4f;

/**
 * Factory for the {@link Matrix4f} transforms that the pipe models are assembled from.
 *
 * <p>These replace the {@code logisticspipes.proxy.object3d.operation} classes
 * ({@code LPTranslation}, {@code LPScale}, {@code LPRotation}) one for one. Because they
 * are plain matrices, a chain of operations now composes with {@link Matrix4f#mul} instead
 * of running one full pass over the vertex data per operation.</p>
 */
public final class MeshTransforms {

    private MeshTransforms() {
    }

    public static Matrix4f translation(double x, double y, double z) {
        return new Matrix4f().translation((float) x, (float) y, (float) z);
    }

    public static Matrix4f scale(double factor) {
        return scale(factor, factor, factor);
    }

    public static Matrix4f scale(double x, double y, double z) {
        return new Matrix4f().scaling((float) x, (float) y, (float) z);
    }

    /**
     * Rotation of {@code angleRadians} about the axis {@code (ax, ay, az)} through the
     * <em>origin</em>.
     *
     * <p>Matches CodeChickenLib's {@code new Rotation(angle, x, y, z)}, which is what the
     * 1.12.2 tube renderers were written against. Note that the intermediate port
     * ({@code CCLProxy.getRotation(double, int, int, int)}) diverged from that on two
     * counts: it ran {@code Math.toRadians} over an argument the callers already supply in
     * radians, and it rotated about (0.5, 0.5, 0.5). Both are corrected here, so the HS
     * tube orientations rotate by the intended quarter turns.</p>
     */
    public static Matrix4f rotation(double angleRadians, double ax, double ay, double az) {
        return new Matrix4f().rotation((float) angleRadians, (float) ax, (float) ay, (float) az);
    }

    /**
     * CodeChickenLib's {@code Rotation.sideOrientation(0, meta)}: {@code meta} quarter turns
     * about the Y axis through the origin, mapping {@code (x, z)} to
     * {@code (x·cos − z·sin, x·sin + z·cos)}.
     *
     * <p>Note the handedness: this is the <em>opposite</em> sense to {@link #rotation}, hence
     * the negated angle against JOML's right-handed {@code rotationY}. That is not an
     * oversight — {@code LogisticsNewSolidBlockWorldRenderer.computeRotated} follows each of
     * these with a {@link #translation} that only brings the result back into the unit cube
     * under this convention (e.g. {@code sideOrientation(1)} then {@code translate(1, 0, 0)}
     * maps the unit cube to x ∈ [-1, 0] and back to [0, 1]; the opposite sense would leave it
     * at x ∈ [1, 2]).</p>
     */
    public static Matrix4f sideOrientation(int meta) {
        return new Matrix4f().rotationY((float) (-meta * Math.PI / 2.0));
    }
}
