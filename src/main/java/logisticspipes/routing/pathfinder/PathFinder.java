/*
 * Copyright (c) Krapht, 2011
 * "LogisticsPipes" is distributed under the terms of the Minecraft Mod Public
 * License 1.0, or MMPL. Please check the contents of the license located in
 * http://www.mod-buildcraft.com/MMPL-1.0.txt
 */

package logisticspipes.routing.pathfinder;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

import net.neoforged.neoforge.capabilities.Capabilities;

import org.jspecify.annotations.Nullable;

import logisticspipes.api.provider.ILogisticsPowerProvider;
import logisticspipes.connection.special.SpecialBlockEntityConnection;
import logisticspipes.connection.special.SpecialPipeConnection;
import logisticspipes.connection.special.SpecialPipeConnection.ConnectionInformation;
import logisticspipes.interfaces.ISubSystemPowerProvider;
import logisticspipes.interfaces.routing.FilteredRoute;
import logisticspipes.interfaces.routing.IChannelRoutingConnection;
import logisticspipes.pipes.basic.CoreRoutedPipe;
import logisticspipes.routing.ExitRoute;
import logisticspipes.routing.PipeRoutingConnectionType;
import logisticspipes.routing.RouterManager;
import logisticspipes.routing.laser.ILaserPainter;
import logisticspipes.routing.laser.LaserData;
import logisticspipes.routing.pathfinder.changedetection.IBlockEntityChangeListener;
import logisticspipes.utils.OneList;
import logisticspipes.world.level.block.entity.LogisticsGenericPipeBlockEntity;

/**
 * Examines all pipe connections and their forks to locate all connected routers
 */
public class PathFinder {

    /**
     * Recurse through all exists of a pipe to find instances of
     * PipeItemsRouting. maxVisited and maxLength are safeguards for recursion
     * runaways.
     *
     * @param startPipe  - The BlockEntity to start the search from
     * @param maxVisited - The maximum number of pipes to visit, regardless of
     *                   recursion level
     * @param maxLength  - The maximum recurse depth, i.e. the maximum length pipe that
     *                   is supported
     */
    public static HashMap<CoreRoutedPipe, ExitRoute> findAndPaintConnectedRoutingPipes(BlockEntity startPipe,
        Direction startOrientation, int maxVisited, int maxLength, ILaserPainter pathPainter,
        EnumSet<PipeRoutingConnectionType> connectionType) {
        IPipeInformationProvider startProvider = PipeInformationManager.getInformationProviderFor(startPipe);
        if (startProvider == null) {
            return new HashMap<>();
        }
        PathFinder newSearch = new PathFinder(maxVisited, maxLength, pathPainter);
        BlockPos p = startProvider.getPos();
        newSearch.setVisited.add(p);
        p = p.relative(startOrientation);
        BlockEntity entity = startProvider.getLevel().getBlockEntity(p);
        IPipeInformationProvider provider = PipeInformationManager.getInformationProviderFor(entity);
        if (provider == null) {
            return new HashMap<>();
        }
        return newSearch.getConnectedRoutingPipes(provider, connectionType, startOrientation);
    }

    public PathFinder(@Nullable IPipeInformationProvider startPipe, int maxVisited, int maxLength,
        IBlockEntityChangeListener changeListener) {
        this(maxVisited, maxLength, null);
        if (startPipe == null) {
            result = new HashMap<>();
            return;
        }
        this.changeListener = changeListener;
        result = getConnectedRoutingPipes(startPipe, EnumSet.allOf(PipeRoutingConnectionType.class), null);
    }

    public PathFinder(IPipeInformationProvider startPipe, int maxVisited, int maxLength, Direction side) {
        this(maxVisited, maxLength, null);
        result = getConnectedRoutingPipes(startPipe, EnumSet.allOf(PipeRoutingConnectionType.class), side);
    }

    private PathFinder(int maxVisited, int maxLength, @Nullable ILaserPainter pathPainter) {
        this.maxVisited = maxVisited;
        this.maxLength = maxLength;
        setVisited = new HashSet<>();
        distances = new HashMap<>();
        this.pathPainter = pathPainter;
    }

    private final int maxVisited;
    private final int maxLength;
    private final HashSet<BlockPos> setVisited;
    /**
     * How much of the length budget each step has eaten. A pipe reached through a special
     * connection gets its own entry alongside the pipe's own, which is what the old key did by
     * offsetting the position to the block centre -- a distinct key by accident rather than by
     * intent, and easy to flatten by mistake when both became plain positions.
     */
    private final HashMap<DistanceKey, Double> distances;

    private record DistanceKey(BlockPos pos, boolean viaSpecialConnection) {}

    private final @Nullable ILaserPainter pathPainter;
    private double pipesVisited;

    public @Nullable List<FilteredRoute<ILogisticsPowerProvider>> powerNodes;
    public @Nullable List<FilteredRoute<ISubSystemPowerProvider>> subPowerProvider;
    public HashMap<CoreRoutedPipe, ExitRoute> result = new HashMap<>();

    public @Nullable IBlockEntityChangeListener changeListener;
    public Set<List<IBlockEntityChangeListener>> listenedPipes = new HashSet<>();
    public Set<LogisticsGenericPipeBlockEntity> touchedPipes = new HashSet<>();

    private HashMap<CoreRoutedPipe, ExitRoute> getConnectedRoutingPipes(IPipeInformationProvider startPipe,
        EnumSet<PipeRoutingConnectionType> connectionFlags, @Nullable Direction side) {
        HashMap<CoreRoutedPipe, ExitRoute> foundPipes = new HashMap<>();

        final int setVisitedSize = setVisited.size();

        boolean root = setVisitedSize == 0;

        //Reset visited count at top level
        if (setVisitedSize == 1) {
            pipesVisited = 0;
        }

        //Break recursion if we have visited a set number of pipes, to prevent client hang if pipes are weirdly configured
        pipesVisited += startPipe.getDistanceWeight() > 0 ? startPipe.getDistanceWeight() : 1;
        if (pipesVisited > maxVisited) {
            return foundPipes;
        }

        //Break recursion after certain amount of nodes visited
        if (setVisitedSize > maxLength * 10) {
            return foundPipes;
        }

        //Break recursion after certain length of nodes visited
        //Maximize to 1 so we don't stop at routes with resistor pipes
        //Check size of setVisited first to speed up the process, so we don't sum the distances all the time
        if (setVisitedSize > maxLength
            && distances.values().stream().mapToDouble(i -> Math.max(Math.min(i, 1), 0)).sum() > maxLength) {
            return foundPipes;
        }

        if (!startPipe.isRouterInitialized()) {
            return foundPipes;
        }

        //Break recursion if we end up on a routing pipe, unless its the first one. Will break if matches the first call
        if (startPipe.isRoutingPipe() && setVisitedSize != 0) {
            CoreRoutedPipe rp = startPipe.getRoutingPipe();
            if (rp.stillNeedReplace()) {
                return foundPipes;
            }
            double size = 0;
            for (Double dis : distances.values()) {
                size += dis;
            }

            if (!rp.getUpgradeManager().hasPowerPassUpgrade()) {
                connectionFlags.remove(PipeRoutingConnectionType.CAN_POWER_SUB_SYSTEM_FROM);
            }

            foundPipes.put(rp,
                new ExitRoute(null, rp.getRouter(), null, side.getOpposite(), Math.max(1, size), connectionFlags,
                    distances.size()));

            return foundPipes;
        }

        //Visited is checked after, so we can reach the same target twice to allow to keep the shortest path
        setVisited.add(startPipe.getPos());
        distances.put(new DistanceKey(startPipe.getPos(), false),
            startPipe.getDistance() * startPipe.getDistanceWeight());

        // first check specialPipeConnections (tesseracts, teleports, other connectors)
        List<ConnectionInformation> pipez = SpecialPipeConnection.get()
            .getConnectedPipes(startPipe, connectionFlags, side);
        for (ConnectionInformation specialConnection : pipez) {
            if (setVisited.contains(specialConnection.getConnectedPipe().getPos())) {
                //Don't go where we have been before
                continue;
            }
            distances.put(new DistanceKey(startPipe.getPos(), true), specialConnection.getDistance());
            HashMap<CoreRoutedPipe, ExitRoute> result = getConnectedRoutingPipes(specialConnection.getConnectedPipe(),
                specialConnection.getConnectionFlags(), specialConnection.getInsertOrientation());
            distances.remove(new DistanceKey(startPipe.getPos(), true));
            for (Entry<CoreRoutedPipe, ExitRoute> pipe : result.entrySet()) {
                pipe.getValue().exitOrientation = specialConnection.getExitOrientation();
                ExitRoute foundPipe = foundPipes.get(pipe.getKey());
                if (foundPipe == null || (pipe.getValue().distanceToDestination < foundPipe.distanceToDestination)) {
                    // New path OR 	If new path is better, replace old path
                    foundPipes.put(pipe.getKey(), pipe.getValue());
                }
            }
        }

        ArrayDeque<Connection> connections = new ArrayDeque<>();

        //Recurse in all directions
        for (Direction direction : Direction.values()) {
            if (root && side != null && !direction.equals(side)) {
                continue;
            }

            // tile may be up to 1 second old, but any neighbour pipe change will cause an immidiate update here, so we know that if it has changed, it isn't a pipe that has done so.
            BlockEntity tile = startPipe.getNextConnectedTile(direction);

            if (tile == null) {
                continue;
            }
            if (root && (direction.getAxis() == Direction.Axis.X || direction.getAxis() == Direction.Axis.Z)) {
                if (tile instanceof ILogisticsPowerProvider) {
                    if (powerNodes == null) {
                        powerNodes = new ArrayList<>();
                    }
                    //If we are a FireWall pipe add our filter to the pipes
                    if (startPipe.isFirewallPipe()) {
                        powerNodes.add(new FilteredRoute<>((ILogisticsPowerProvider) tile,
                            new OneList<>(startPipe.getFirewallFilter())));
                    } else {
                        powerNodes.add(new FilteredRoute<>((ILogisticsPowerProvider) tile,
                            Collections.unmodifiableList(new ArrayList<>(0))));
                    }
                } else if (tile instanceof ISubSystemPowerProvider) {
                    if (subPowerProvider == null) {
                        subPowerProvider = new ArrayList<>();
                    }
                    //If we are a FireWall pipe add our filter to the pipes
                    if (startPipe.isFirewallPipe()) {
                        subPowerProvider.add(new FilteredRoute<>((ISubSystemPowerProvider) tile,
                            new OneList<>(startPipe.getFirewallFilter())));
                    } else {
                        subPowerProvider.add(new FilteredRoute<>((ISubSystemPowerProvider) tile,
                            Collections.unmodifiableList(new ArrayList<>(0))));
                    }
                }
            }
            connections.add(new Connection(tile, direction, 0, false));
        }

        while (!connections.isEmpty()) {
            Connection connection = connections.pollFirst();
            BlockEntity tile = connection.tile();
            Direction direction = connection.direction();
            int resistance = connection.resistance();
            boolean isDirectConnection = connection.isDirectConnection();
            EnumSet<PipeRoutingConnectionType> nextConnectionFlags = EnumSet.copyOf(connectionFlags);

            if (root) {
                Collection<BlockEntity> list = SpecialBlockEntityConnection.get().getConnectedPipes(tile);
                if (!list.isEmpty()) {
                    connections.addAll(list.stream().map(pipe -> new Connection(pipe, direction, 0, false)).toList());
                    listTileEntity(tile);
                    continue;
                }
                if (!startPipe.getRoutingPipe().getUpgradeManager().hasPowerPassUpgrade()) {
                    nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_POWER_SUB_SYSTEM_FROM);
                }
            }

            if (!PipeInformationManager.isPipe(tile) &&
                tile.getLevel() != null &&
                tile.getLevel().getCapability(Capabilities.Item.BLOCK, tile.getBlockPos(), direction.getOpposite())
                    != null &&
                startPipe.isRoutingPipe() &&
                startPipe.getRoutingPipe() instanceof IChannelRoutingConnection &&
                startPipe.canConnect(tile, direction, false)) {
                if (RouterManager.get().hasChannelConnection(startPipe.getRoutingPipe().getRouter())) {
                    List<CoreRoutedPipe> connectedPipes = RouterManager.get()
                        .getConnectedPipes(startPipe.getRoutingPipe().getRouter());
                    connections.addAll(connectedPipes.stream()
                        .map(CoreRoutedPipe::getContainer)
                        .filter(Objects::nonNull)
                        .map(container -> new Connection(container, direction,
                            ((IChannelRoutingConnection) startPipe.getRoutingPipe()).getConnectionResistance(), true))
                        .toList());
                    if (!connectedPipes.isEmpty()) {
                        continue;
                    }
                }
            }

            IPipeInformationProvider currentPipe = PipeInformationManager.getInformationProviderFor(tile);

            if (currentPipe != null && currentPipe.isRouterInitialized() && (isDirectConnection
                || PipeInformationManager.canConnect(startPipe, currentPipe, direction, true))) {

                listTileEntity(tile);

                if (currentPipe.isMultiBlock()) {
                    currentPipe.getPartsOfPipe().forEach(this::listTileEntity);
                }

                if (setVisited.contains(currentPipe.getPos())) {
                    //Don't go where we have been before
                    continue;
                }
                if (side != direction && !root) { //Only straight connections for subsystem power
                    nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_POWER_SUB_SYSTEM_FROM);
                }
                if (isDirectConnection) { //ISC doesn't pass power
                    nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_POWER_FROM);
                    nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_POWER_SUB_SYSTEM_FROM);
                }
                //Iron, obsidean and liquid pipes will separate networks
                if (currentPipe.divideNetwork()) {
                    continue;
                }
                if (currentPipe.powerOnly()) {
                    nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_ROUTE_TO);
                    nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_REQUEST_FROM);
                }
                if (startPipe.isOnewayPipe()) {
                    if (startPipe.isOutputClosed(direction)) {
                        nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_ROUTE_TO);
                    }
                }
                if (currentPipe.isOnewayPipe()) {
                    nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_POWER_SUB_SYSTEM_FROM);
                    if (currentPipe.isOutputClosed(direction.getOpposite())) {
                        nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_REQUEST_FROM);
                        nextConnectionFlags.remove(PipeRoutingConnectionType.CAN_POWER_FROM);
                    }
                }

                if (nextConnectionFlags.isEmpty()) { //don't bother going somewhere we can't do anything with
                    continue;
                }

                int beforeRecurseCount = foundPipes.size();
                HashMap<CoreRoutedPipe, ExitRoute> result = getConnectedRoutingPipes(currentPipe, nextConnectionFlags,
                    direction);
                for (Entry<CoreRoutedPipe, ExitRoute> pipeEntry : result.entrySet()) {
                    //Update Result with the direction we took
                    pipeEntry.getValue().exitOrientation = direction;
                    ExitRoute foundPipe = foundPipes.get(pipeEntry.getKey());
                    if (foundPipe == null) {
                        // New path
                        foundPipes.put(pipeEntry.getKey(), pipeEntry.getValue());
                        //Add resistance
                        pipeEntry.getValue().distanceToDestination += resistance;
                    } else if (pipeEntry.getValue().distanceToDestination + resistance
                        < foundPipe.distanceToDestination) {
                        //If new path is better, replace old path, otherwise do nothing
                        foundPipes.put(pipeEntry.getKey(), pipeEntry.getValue());
                        //Add resistance
                        pipeEntry.getValue().distanceToDestination += resistance;
                    }
                }
                if (foundPipes.size() > beforeRecurseCount && pathPainter != null) {
                    pathPainter.addLaser(startPipe.getLevel(),
                        new LaserData(startPipe.getPos(), direction, connectionFlags));
                }
            }
        }
        setVisited.remove(startPipe.getPos());
        distances.remove(new DistanceKey(startPipe.getPos(), false));
        if (startPipe.isRoutingPipe()) { // ie, has the recursion returned to the pipe it started from?
            for (ExitRoute e : foundPipes.values()) {
                e.root = (startPipe.getRoutingPipe()).getRouter();
            }
        }
        //If we are a FireWall pipe add our filter to the pipes
        if (startPipe.isFirewallPipe() && root) {
            for (ExitRoute e : foundPipes.values()) {
                e.filters = new OneList<>(startPipe.getFirewallFilter());
            }
        }
        return foundPipes;
    }

    private void listTileEntity(@Nullable BlockEntity tile) {
        if (changeListener != null && tile instanceof LogisticsGenericPipeBlockEntity lpTile
            && lpTile.isChangeDetectionActive()) {
            if (!lpTile.changeListeners.contains(changeListener)) {
                lpTile.changeListeners.add(changeListener);
            }
            listenedPipes.add(lpTile.changeListeners);
            touchedPipes.add(lpTile);
        }
    }

    public static int measureDistanceToNextRoutedPipe(BlockPos lpPosition, Direction exitOrientation, Level level) {
        int dis = 1;
        BlockEntity tile = level.getBlockEntity(lpPosition);
        if (tile instanceof LogisticsGenericPipeBlockEntity) {
            tile = ((LogisticsGenericPipeBlockEntity) tile).getNextConnectedTile(exitOrientation);
        }
        if (tile == null) {
            return 0;
        }
        IPipeInformationProvider info = PipeInformationManager.getInformationProviderFor(tile);
        while (info != null && !info.isRoutingPipe()) {
            tile = info.getNextConnectedTile(exitOrientation);
            if (tile == null) {
                info = null;
                continue;
            }
            info = PipeInformationManager.getInformationProviderFor(tile);
            dis++;
        }
        return dis;
    }

    /**
     * A block entity still to look at, the side it was reached from, and for a channel link its
     * resistance; a direct connection skips the usual can-connect check.
     */
    private record Connection(BlockEntity tile, Direction direction, int resistance, boolean isDirectConnection) {}
}
