package logisticspipes.client.model.mesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Wavefront OBJ parser covering the subset LP's models use.
 *
 * <p>Supports {@code v}, {@code vt}, {@code vn}, {@code g} and {@code f} with triangle or
 * quad faces in {@code v/vt/vn} form; {@code s}, {@code o}, {@code mtllib} and
 * {@code usemtl} are ignored. Triangles are expanded to degenerate quads by duplicating
 * the last vertex — {@link ObjMesh#withComputedNormals()} has a fallback for the resulting
 * zero-area cross product.</p>
 *
 * <p>OBJ indices are 1-based and are normalized to 0-based here.</p>
 *
 * <p>Group keys are the entire rest of the {@code g} line, because LP's files list several
 * part names per group; {@link ObjModel} indexes the individual tokens.</p>
 */
public final class ObjParser {

    private ObjParser() {
    }

    /**
     * Parses an OBJ stream. The stream is closed on return.
     */
    public static ObjModel parse(InputStream in) throws IOException {
        List<float[]> positions = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<float[]> normals = new ArrayList<>();
        Map<String, ObjMeshBuilder> builders = new LinkedHashMap<>();
        String currentGroup = "default";

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.charAt(0) == '#') {
                    continue;
                }
                String[] tok = line.split("\\s+");
                switch (tok[0]) {
                    case "v" -> positions.add(new float[] {
                        Float.parseFloat(tok[1]), Float.parseFloat(tok[2]), Float.parseFloat(tok[3])
                    });
                    case "vt" ->
                        // OBJ's V axis runs bottom-up, Minecraft textures run top-down. Flip V.
                        uvs.add(new float[] {
                            Float.parseFloat(tok[1]), 1.0f - Float.parseFloat(tok[2])
                        });
                    case "vn" -> normals.add(new float[] {
                        Float.parseFloat(tok[1]), Float.parseFloat(tok[2]), Float.parseFloat(tok[3])
                    });
                    case "g" -> {
                        String name = line.length() > 2 ? line.substring(2).trim() : "";
                        currentGroup = name.isEmpty() ? "default" : name;
                        builders.computeIfAbsent(currentGroup, key -> new ObjMeshBuilder());
                    }
                    case "f" -> parseFace(tok, positions, uvs, normals,
                        builders.computeIfAbsent(currentGroup, key -> new ObjMeshBuilder()));
                    default -> { /* s, o, mtllib, usemtl: ignored */ }
                }
            }
        }

        Map<String, ObjMesh> groups = new LinkedHashMap<>(builders.size());
        for (Map.Entry<String, ObjMeshBuilder> entry : builders.entrySet()) {
            groups.put(entry.getKey(), entry.getValue().build());
        }
        return ObjModelIndexer.index(groups);
    }

    private static void parseFace(String[] tok, List<float[]> positions, List<float[]> uvs, List<float[]> normals,
        ObjMeshBuilder out) {
        int vertexCount = tok.length - 1;
        if (vertexCount < 3 || vertexCount > 4) {
            return; // only triangles and quads
        }

        int[] posIdx = new int[4];
        int[] uvIdx = new int[4];
        int[] normalIdx = new int[4];
        for (int i = 0; i < vertexCount; i++) {
            String[] parts = tok[i + 1].split("/");
            posIdx[i] = Integer.parseInt(parts[0]) - 1;
            uvIdx[i] = (parts.length > 1 && !parts[1].isEmpty()) ? Integer.parseInt(parts[1]) - 1 : -1;
            normalIdx[i] = (parts.length > 2 && !parts[2].isEmpty()) ? Integer.parseInt(parts[2]) - 1 : -1;
        }
        if (vertexCount == 3) {
            posIdx[3] = posIdx[2];
            uvIdx[3] = uvIdx[2];
            normalIdx[3] = normalIdx[2];
        }

        float[] quadPos = new float[12];
        float[] quadUv = new float[8];
        float[] quadNormal = new float[12];
        for (int i = 0; i < 4; i++) {
            float[] p = positions.get(posIdx[i]);
            quadPos[i * 3] = p[0];
            quadPos[i * 3 + 1] = p[1];
            quadPos[i * 3 + 2] = p[2];
            if (uvIdx[i] >= 0 && uvIdx[i] < uvs.size()) {
                float[] texture = uvs.get(uvIdx[i]);
                quadUv[i * 2] = texture[0];
                quadUv[i * 2 + 1] = texture[1];
            }
            if (normalIdx[i] >= 0 && normalIdx[i] < normals.size()) {
                float[] n = normals.get(normalIdx[i]);
                quadNormal[i * 3] = n[0];
                quadNormal[i * 3 + 1] = n[1];
                quadNormal[i * 3 + 2] = n[2];
            }
        }
        out.addQuad(quadPos, quadUv, quadNormal);
    }
}
