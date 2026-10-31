package logisticspipes.routing.sink;

public record FluidSinkReply(FixedFluidPriority fixedPriority, long sinkAmount) {

    public enum FixedFluidPriority {
        TERMINUS,
        FLUID_SINK
    }

    public int getSinkAmountInt() {
        return Math.max(0, (int) Math.min(Integer.MAX_VALUE, sinkAmount));
    }

}
