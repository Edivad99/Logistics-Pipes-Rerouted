package logisticspipes.api.connection;

public enum ConnectionType {
    ITEM, FLUID, MULTIBLOCK, UNDEFINED;

    public boolean isItem() {
        return this == ITEM || this == UNDEFINED;
    }

    public boolean isFluid() {
        return this == FLUID || this == UNDEFINED;
    }
}
