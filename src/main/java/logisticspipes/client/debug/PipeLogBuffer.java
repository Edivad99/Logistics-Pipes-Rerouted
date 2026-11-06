package logisticspipes.client.debug;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

import org.jspecify.annotations.Nullable;

import logisticspipes.pipes.basic.debug.StatusEntry;

/**
 * What a watched pipe has told this client, kept so the log survives closing the screen.
 *
 * <p>Everything here is touched from the client thread only: the payload handlers that fill it run
 * there, and so does the screen that reads it.
 */
public final class PipeLogBuffer {

    /** Beyond this the oldest lines are dropped; a busy pipe writes several lines a tick. */
    private static final int MAX_LINES = 2000;

    private static final Map<Integer, PipeLogBuffer> BUFFERS = new LinkedHashMap<>();
    private static int mostRecentId = -1;

    private final Deque<String> lines = new ArrayDeque<>();
    private List<StatusEntry> status = List.of();
    private String title = "";

    private PipeLogBuffer() {}

    /** The buffer for {@code id}, created on first use. */
    public static PipeLogBuffer of(int id) {
        mostRecentId = id;
        return BUFFERS.computeIfAbsent(id, ignored -> new PipeLogBuffer());
    }

    /** The buffer of the pipe watched last, or null if none has been watched yet. */
    public static @Nullable PipeLogBuffer mostRecent() {
        return BUFFERS.get(mostRecentId);
    }

    public String title() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void addLine(String line) {
        lines.addLast(line);
        while (lines.size() > MAX_LINES) {
            lines.removeFirst();
        }
    }

    public void setStatus(List<StatusEntry> status) {
        this.status = status;
    }

    /** The console, oldest line first. */
    public List<Component> logLines() {
        return lines.stream().map(line -> (Component) Component.literal(line)).toList();
    }

    /**
     * The status tree, one line per entry, indented by depth.
     */
    public List<Component> statusLines() {
        final List<Component> out = new ArrayList<>();
        flatten(status, 0, out);
        return out;
    }

    private static void flatten(List<StatusEntry> entries, int depth, List<Component> out) {
        for (StatusEntry entry : entries) {
            out.add(Component.literal("  ".repeat(depth) + entry.name())
                    .withStyle(depth == 0 ? ChatFormatting.WHITE : ChatFormatting.GRAY));
            flatten(entry.subEntries(), depth + 1, out);
        }
    }
}
