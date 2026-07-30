package logisticspipes.client.model.mesh;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;

/**
 * A parsed Wavefront OBJ file: one {@link ObjMesh} per {@code g} line, plus two indices
 * that make part lookup a hash lookup instead of a string scan.
 *
 * <p>LP's OBJ files put several names on a single group line, e.g.</p>
 * <pre>g Mesh332 Side_Texture_Plate_Side3 Texture_Side_N</pre>
 * <p>The 1.12.2 loader kept the whole line as a map key and found parts by running
 * {@code key.contains(" " + name + " ") || key.endsWith(" " + name)} over all 512 groups,
 * once per part, for roughly fifteen part families — a quadratic pile of string scans on
 * every startup, and the numbered variants ({@code Texture_Side_N1}, {@code _N2}, ...) were
 * picked apart by inspecting the character after the match.</p>
 *
 * <p>Both access patterns are preserved exactly, as two indices:</p>
 * <ul>
 *   <li>{@link #byName(String)} — exact token match, replacing
 *       {@code contains(" name ") || endsWith(" name")}.</li>
 *   <li>{@link #byNamePrefix(String)} — base-name match returning every numbered variant,
 *       replacing {@code contains(" name")} plus the manual suffix parsing. A token's
 *       trailing digits become {@link Part#variant}, so {@code Texture_Side_N2} is variant
 *       2 of base {@code Texture_Side_N} and an unsuffixed {@code Texture_Side_N} is
 *       variant 0.</li>
 * </ul>
 */
public final class ObjModel {

    /**
     * One group's mesh, together with the numeric suffix of the token it was found under.
     */
    public record Part(String group, ObjMesh mesh, int variant) {}

    private final Map<String, ObjMesh> byGroupLine;
    private final Map<String, List<Part>> byName;
    private final Map<String, List<Part>> byBaseName;

    ObjModel(Map<String, ObjMesh> byGroupLine, Map<String, List<Part>> byName, Map<String, List<Part>> byBaseName) {
        this.byGroupLine = Collections.unmodifiableMap(byGroupLine);
        this.byName = Collections.unmodifiableMap(byName);
        this.byBaseName = Collections.unmodifiableMap(byBaseName);
    }

    public static ObjModel empty() {
        return new ObjModel(new LinkedHashMap<>(), Map.of(), Map.of());
    }

    /**
     * Every group, keyed by its full {@code g} line, in file order.
     */
    public Map<String, ObjMesh> groups() {
        return byGroupLine;
    }

    /**
     * The mesh of the group whose {@code g} line is exactly {@code groupLine}, or null.
     */
    public ObjMesh group(String groupLine) {
        return byGroupLine.get(groupLine);
    }

    /**
     * Every group carrying {@code name} as one of its whitespace-separated tokens.
     */
    public List<Part> byName(String name) {
        return byName.getOrDefault(name, List.of());
    }

    /**
     * Every group carrying a token that is {@code baseName} optionally followed by digits.
     * The returned parts carry those digits in {@link Part#variant} (0 when absent).
     */
    public List<Part> byNamePrefix(String baseName) {
        return byBaseName.getOrDefault(baseName, List.of());
    }

    /**
     * All meshes matching {@link #byName(String)}, for callers that don't need the variant.
     */
    public List<ObjMesh> meshesNamed(String name) {
        List<Part> parts = byName(name);
        List<ObjMesh> meshes = new ArrayList<>(parts.size());
        for (Part part : parts) {
            meshes.add(part.mesh());
        }
        return meshes;
    }

    /**
     * Applies {@code operator} to every group's mesh, keeping the indices consistent.
     */
    public ObjModel mapMeshes(UnaryOperator<ObjMesh> operator) {
        Map<String, ObjMesh> groups = new LinkedHashMap<>(byGroupLine.size());
        for (Map.Entry<String, ObjMesh> entry : byGroupLine.entrySet()) {
            groups.put(entry.getKey(), operator.apply(entry.getValue()));
        }
        return ObjModelIndexer.index(groups);
    }
}
