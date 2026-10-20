package logisticspipes.client.model.tube;

/**
 * A placed tube's geometry as the baked model caches it: which tube, at which orientation.
 * The orientation is the render orientation's {@code name()}, as {@link TubeModels} keys it.
 */
public record TubeKey(TubeModels.Kind kind, String orientation) {
}
