package logisticspipes.routing;

import java.util.List;

import org.jspecify.annotations.Nullable;

public final class AsyncRouting {

    private AsyncRouting() {
    }

    public static @Nullable List<ExitRoute> getDistance(ServerRouter sourceRouter, IRouter destinationRouter) {
        if (sourceRouter.routeTable.size() <= destinationRouter.getSimpleID()) {
            return null;
        }
        return sourceRouter.routeTable.get(destinationRouter.getSimpleID());
    }

    public static void updateServerRouterLsa(ServerRouter router) {
        if (router.connectionNeedsChecking != 0 && router.checkAdjacentUpdate()) {
            router.updateLsa();
        }
    }

    public static boolean needsRoutingTableUpdate(ServerRouter router) {
        return router.lsaVersion > ServerRouter.lastLsaVersion[router.simpleID];
    }

    public static void updateRoutingTable(ServerRouter serverRouter) {
        if (serverRouter.lsaVersion > ServerRouter.lastLsaVersion[serverRouter.simpleID]) {
            serverRouter.CreateRouteTable(serverRouter.lsaVersion);
        }
    }
}
