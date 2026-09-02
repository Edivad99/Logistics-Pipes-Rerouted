package logisticspipes.routing;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import logisticspipes.LPConfigs;
import logisticspipes.LogisticsPipes;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.pipes.basic.LogisticsTileGenericPipe;
import logisticspipes.routing.pathfinder.PathFinder;

/**
 * Walks a pipe's routing table and turns it into the beam segments the HUD draws.
 *
 * <p>One builder per request: it keeps the walk's state, which is why this is not a static
 * helper.
 */
public class RoutingLaserBuilder {

    private record WorkItem(LogisticsTileGenericPipe pipe, Direction dir, List<ExitRoute> connectedRouters,
            EnumSet<PipeRoutingConnectionType> connectionType, String logPrefix) {}

    private final List<LaserData> lasers = new ArrayList<>();
    private boolean firstPipe = true;

    /**
     * Every beam segment leaving {@code pipe}, collinear ones already merged.
     */
    public static List<LaserData> buildFor(CoreRoutedPipe pipe) {
        return new RoutingLaserBuilder().build(pipe);
    }

    private List<LaserData> build(CoreRoutedPipe pipe) {
        final IRouter router = pipe.getRouter();

        // Requesting the lasers is also how a player forces a network-wide LSA update.
        router.forceLsaUpdate();

        final Map<Direction, List<ExitRoute>> routers = new HashMap<>();
        for (List<ExitRoute> exit : router.getRouteTable()) {
            if (exit == null) {
                continue;
            }
            for (ExitRoute route : exit) {
                groupByExit(routers, route);
            }
        }
        for (Entry<Direction, List<ExitRoute>> entry : routers.entrySet()) {
            if (entry.getKey() == null) {
                continue;
            }
            walk(pipe.container, entry.getKey(), entry.getValue(),
                    EnumSet.allOf(PipeRoutingConnectionType.class), entry.getKey().name());
        }
        return compress();
    }

    private void walk(LogisticsTileGenericPipe startPipe, Direction startDir, List<ExitRoute> startRouters,
            EnumSet<PipeRoutingConnectionType> startConnectionType, String startPrefix) {
        final List<WorkItem> worklist = new LinkedList<>();
        worklist.add(new WorkItem(startPipe, startDir, startRouters, startConnectionType, startPrefix));
        while (!worklist.isEmpty()) {
            final WorkItem item = worklist.remove(0);
            final LogisticsTileGenericPipe pipe = item.pipe;
            final List<ExitRoute> connectedRouters = item.connectedRouters;
            if (LogisticsPipes.isDEBUG()) {
                System.out.println(item.logPrefix + ": Size: " + connectedRouters.size());
            }
            lasers.add(new LaserData(pipe.getBlockPos(), item.dir, item.connectionType).setStartPipe(firstPipe));
            firstPipe = false;

            final HashMap<CoreRoutedPipe, ExitRoute> reachable = PathFinder.paintAndgetConnectedRoutingPipes(
                    pipe, item.dir,
                    LPConfigs.COMMON.LOGISTICS_DETECTION_COUNT.getAsInt(),
                    LPConfigs.COMMON.LOGISTICS_DETECTION_LENGTH.getAsInt(),
                    (world, laser) -> {
                        if (pipe.getWorld() == world) {
                            lasers.add(laser);
                        }
                    },
                    item.connectionType);

            // Anything the painter already reached needs no beam of its own.
            for (CoreRoutedPipe reachedPipe : reachable.keySet()) {
                final IRouter reachedRouter = reachedPipe.getRouter();
                connectedRouters.removeIf(route -> route.destination == reachedRouter);
            }

            for (Entry<CoreRoutedPipe, List<ExitRoute>> hop : nextHops(connectedRouters, reachable).entrySet()) {
                final Map<Direction, List<ExitRoute>> byExit = new HashMap<>();
                for (ExitRoute exit : hop.getValue()) {
                    groupByExit(byExit, exit);
                }
                for (Entry<Direction, List<ExitRoute>> exit : byExit.entrySet()) {
                    if (exit.getKey() == null) {
                        continue;
                    }
                    worklist.add(new WorkItem(hop.getKey().container, exit.getKey(), exit.getValue(),
                            reachable.get(hop.getKey()).connectionDetails,
                            item.logPrefix + ": " + exit.getKey().name()));
                }
            }
        }
    }

    /**
     * For each router still to be reached, the closest pipe among those just painted that leads
     * there -- so the walk continues from that pipe rather than from the one it started at.
     */
    private static Map<CoreRoutedPipe, List<ExitRoute>> nextHops(List<ExitRoute> connectedRouters,
            Map<CoreRoutedPipe, ExitRoute> reachable) {
        final Map<CoreRoutedPipe, List<ExitRoute>> hops = new HashMap<>();
        for (ExitRoute routeTo : connectedRouters) {
            ExitRoute closest = null;
            CoreRoutedPipe closestPipe = null;
            for (Entry<CoreRoutedPipe, ExitRoute> candidate : reachable.entrySet()) {
                for (ExitRoute distance : candidate.getValue().destination.getDistanceTo(routeTo.destination)) {
                    if (distance.isSameWay(routeTo)
                            && (closest == null || closest.distanceToDestination > distance.distanceToDestination)) {
                        closest = distance;
                        closestPipe = candidate.getKey();
                    }
                }
            }
            if (closest == null) {
                continue;
            }
            final List<ExitRoute> routes = hops.computeIfAbsent(closestPipe, key -> new ArrayList<>());
            if (!routes.contains(closest)) {
                routes.add(closest);
            }
        }
        return hops;
    }

    private static void groupByExit(Map<Direction, List<ExitRoute>> byExit, ExitRoute route) {
        final List<ExitRoute> routes = byExit.computeIfAbsent(route.exitOrientation, key -> new ArrayList<>());
        if (!routes.contains(route)) {
            routes.add(route);
        }
    }

    /**
     * Merges each run of segments that continue straight on with the same connection flags into a
     * single longer one.
     */
    private List<LaserData> compress() {
        final List<LaserData> options = new ArrayList<>(lasers);
        Iterator<LaserData> iLasers = lasers.iterator();
        while (iLasers.hasNext()) {
            boolean compressed = false;
            final LaserData data = iLasers.next();
            BlockPos next = data.getPos().relative(data.getDir(), data.getLength());
            boolean found;
            do {
                found = false;
                final Iterator<LaserData> iOptions = options.iterator();
                while (iOptions.hasNext()) {
                    final LaserData other = iOptions.next();
                    if (!other.getPos().equals(next) || data.getDir() != other.getDir()) {
                        continue;
                    }
                    if (data.getConnectionType().equals(other.getConnectionType())) {
                        data.setLength(data.getLength() + other.getLength());
                        next = next.relative(data.getDir(), data.getLength());
                        found = true;
                        iOptions.remove();
                        lasers.remove(other);
                        compressed = true;
                    } else {
                        data.setFinalPipe(false);
                    }
                }
            } while (found);
            if (compressed) {
                iLasers = lasers.iterator();
            }
        }
        return lasers;
    }
}
