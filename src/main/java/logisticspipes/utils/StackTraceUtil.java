package logisticspipes.utils;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

public class StackTraceUtil {

    /**
     * Whether to trace at all. Off unless asked for with {@code -Dlogisticspipes.traceTicks=true}:
     * every pipe tick records its stack frame, which takes most of the time LP spends ticking.
     */
    private static final boolean ENABLED = Boolean.getBoolean("logisticspipes.traceTicks");

    private static final Map<Thread, LinkedList<TracedFrame>> informationMap = new HashMap<>();

    /**
     * A stack frame, and what was noted about it while it ran.
     */
    private record TracedFrame(StackTraceElement frame, @Nullable String information) {}

    public static abstract class Info {

        public abstract void end();
    }

    private static class DummyInfo extends Info {

        @Override
        public void end() {
        }
    }

    private static LinkedList<TracedFrame> getList() {
        return StackTraceUtil.informationMap.computeIfAbsent(Thread.currentThread(), k -> new LinkedList<>());
    }

    public static Info addTraceInformation(final Supplier<String> informationSupplier, Info... infos) {
        if (!ENABLED) {
            return new DummyInfo();
        }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        final StackTraceElement calledFrom = trace[2];
        return StackTraceUtil.addTraceInformationFor(calledFrom, informationSupplier.get(), infos);
    }

    public static Info addSuperTraceInformation(final Supplier<String> informationSupplier, Info... infos) {
        if (!ENABLED) {
            return new DummyInfo();
        }
        StackTraceElement[] trace = Thread.currentThread().getStackTrace();
        final StackTraceElement calledFrom = trace[3];
        return StackTraceUtil.addTraceInformationFor(calledFrom, informationSupplier.get(), infos);
    }

    private static Info addTraceInformationFor(final StackTraceElement calledFrom, final String information,
        final Info... infos) {
        synchronized (StackTraceUtil.informationMap) {
            StackTraceUtil.getList().addLast(new TracedFrame(calledFrom, information));
            return new Info() {

                @Override
                public void end() {
                    synchronized (StackTraceUtil.informationMap) {
                        if (StackTraceUtil.getList().isEmpty()) {
                            throw new RuntimeException("There are to many end() calls");
                        } else {
                            TracedFrame pair = StackTraceUtil.getList().getLast();
                            if (!pair.frame().equals(calledFrom)) {
                                System.out.println("Found: " + pair.frame());
                                System.out.println("Looking for: " + calledFrom);
                                throw new RuntimeException("There is an end() call missing");
                            }
                            StackTraceUtil.getList().removeLast();
                        }
                    }
                    if (infos != null) {
                        for (Info info : infos) {
                            info.end();
                        }
                    }
                }
            };
        }
    }

    private static boolean compare(StackTraceElement called, StackTraceElement infoProvider) {
        return called.getClassName().equals(infoProvider.getClassName()) && called.getMethodName()
            .equals(infoProvider.getMethodName()) && called.getFileName().equals(infoProvider.getFileName())
            && called.getLineNumber() >= infoProvider.getLineNumber();
    }
}
