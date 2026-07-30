package logisticspipes.client.model.mesh;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Builds {@link ObjModel}'s two lookup indices from a group-line → mesh map.
 *
 * @see ObjModel for why both an exact-token and a base-token index are needed
 */
final class ObjModelIndexer {

    private ObjModelIndexer() {
    }

    static ObjModel index(Map<String, ObjMesh> groups) {
        Map<String, List<ObjModel.Part>> byName = new HashMap<>();
        Map<String, List<ObjModel.Part>> byBaseName = new HashMap<>();

        for (Map.Entry<String, ObjMesh> entry : groups.entrySet()) {
            String groupLine = entry.getKey();
            ObjMesh mesh = entry.getValue();
            for (String token : groupLine.split("\\s+")) {
                if (token.isEmpty()) {
                    continue;
                }
                int variant = trailingNumber(token);
                String base = variant == 0 ? token : token.substring(0, token.length() - digitCount(token));
                ObjModel.Part part = new ObjModel.Part(groupLine, mesh, variant);
                byName.computeIfAbsent(token, key -> new ArrayList<>()).add(part);
                if (!base.equals(token)) {
                    byBaseName.computeIfAbsent(base, key -> new ArrayList<>()).add(part);
                }
                // A token with no numeric suffix is variant 0 of itself, so it must also
                // appear under its own base name — that is what makes byNamePrefix("Texture_Side_N")
                // return the unsuffixed group alongside _N1 / _N2 / _N3, matching the old
                // `contains(" " + name)` behaviour.
                byBaseName.computeIfAbsent(token, key -> new ArrayList<>()).add(part);
            }
        }
        return new ObjModel(groups, byName, byBaseName);
    }

    /**
     * Digits at the end of {@code token} as an int, or 0 when there are none.
     */
    private static int trailingNumber(String token) {
        int digits = digitCount(token);
        if (digits == 0 || digits == token.length()) {
            return 0;
        }
        try {
            return Integer.parseInt(token.substring(token.length() - digits));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static int digitCount(String token) {
        int count = 0;
        for (int i = token.length() - 1; i >= 0 && Character.isDigit(token.charAt(i)); i--) {
            count++;
        }
        return count;
    }
}
