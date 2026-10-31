package logisticspipes.routing;

public enum PipeRoutingConnectionType {
    CAN_ROUTE_TO,
    CAN_REQUEST_FROM,
    CAN_POWER_FROM,
    CAN_POWER_SUB_SYSTEM_FROM;

    public static PipeRoutingConnectionType[] values = new PipeRoutingConnectionType[] {
        CAN_ROUTE_TO,
        CAN_REQUEST_FROM,
        CAN_POWER_FROM,
        CAN_POWER_SUB_SYSTEM_FROM
    };
}
