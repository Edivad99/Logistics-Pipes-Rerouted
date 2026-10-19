package logisticspipes.pipes.tubes;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.jspecify.annotations.Nullable;

import logisticspipes.client.model.tube.TubeCollision;
import logisticspipes.client.model.tube.TubeModels;
import logisticspipes.interfaces.ITubeOrientation;
import logisticspipes.interfaces.ITubeRenderOrientation;
import logisticspipes.pipes.basic.CoreMultiBlockPipe;
import logisticspipes.transport.LPTravelingItem;
import logisticspipes.transport.PipeMultiBlockTransportLogistics;
import logisticspipes.utils.IPositionRotateble;
import logisticspipes.utils.PositionRotation;
import logisticspipes.world.level.block.entity.LogisticsGenericSubMultiBlockBlockEntity;

public class HSTubeSCurve extends CoreMultiBlockPipe {

    @Getter
    private CurveSOrientation orientation;
    // Filled lazily and read from the chunk builder threads too, so it is built aside and swapped in whole.
    private volatile List<AABB> boxes = List.of();

    public HSTubeSCurve(Item item) {
        super(new PipeMultiBlockTransportLogistics(), item);
    }

    @Override
    public void writeState(FriendlyByteBuf buffer) {
        buffer.writeNullable(orientation, FriendlyByteBuf::writeEnum);
    }

    @Override
    public void readState(FriendlyByteBuf buffer) {
        final CurveSOrientation read = buffer.readNullable(inner -> inner.readEnum(CurveSOrientation.class));
        if (read != null) {
            orientation = read;
        }
    }

    @Override
    public int getTextureIndex() {
        return 0;
    }

    @Override
    public List<SubBlock> getSubBlocks() {
        return List.of(
            new SubBlock(new BlockPos(0, 0, -1), SubBlockTypeForShare.S_CURVE_B),
            new SubBlock(new BlockPos(0, 0, -2), SubBlockTypeForShare.S_CURVE_A),
            new SubBlock(new BlockPos(1, 0, -1), SubBlockTypeForShare.S_CURVE_A),
            new SubBlock(new BlockPos(1, 0, -2), SubBlockTypeForShare.S_CURVE_B),
            new SubBlock(new BlockPos(1, 0, -3), SubBlockTypeForShare.NON_SHARE));
    }

    @Override
    public List<SubBlock> getRotatedSubBlocks() {
        PositionRotation rotation = new PositionRotation();
        orientation.rotatePositions(rotation);
        return getSubBlocks().stream().map(block -> block.rotated(rotation)).toList();
    }

    @Override
    public void addCollisionBoxesToList(List<AABB> arraylist, @Nullable AABB area) {
        List<AABB> cached = boxes;
        if (cached.isEmpty()) {
            List<AABB> built = new ArrayList<>();
            final BlockPos tubePos = getPos();
            double x = tubePos.getX();
            double y = tubePos.getY();
            double z = tubePos.getZ();
            for (int i = -1; i < 54; i++) {
                double xOne = x;
                double yOne = y;
                double zOne = z;
                double xTwo = x;
                double yTwo = y + 1;
                double zTwo = z;
                switch (orientation.getRenderOrientation()) {
                    case NORTH, NORTH_INV -> {
                        zOne += 1;
                        zTwo += 1;
                        zOne -= 4.0F * (i - 4) / 50;
                        zTwo -= 4.0F * (i + 4) / 50;
                        xOne -= 1;
                        xTwo += 2;
                    }
                    case EAST, EAST_INV -> {
                        xOne += 4;
                        xTwo += 4;
                        xOne -= 4.0F * (i - 4) / 50;
                        xTwo -= 4.0F * (i + 4) / 50;
                        zOne -= 1;
                        zTwo += 2;
                    }
                }
                AABB box = TubeCollision.boundsAt(TubeModels.Kind.SCURVE, orientation,
                    new AABB(Math.min(xOne, xTwo), Math.min(yOne, yTwo), Math.min(zOne, zTwo), Math.max(xOne, xTwo),
                        Math.max(yOne, yTwo),
                        Math.max(zOne, zTwo)).move(-x, -y, -z));
                if (box != null) {
                    Vec3 center = box.getCenter();
                    box = new AABB(center.x - 0.3D, center.y - 0.3D, center.z - 0.3D, center.x + 0.3D,
                        center.y + 0.3D, center.z + 0.3D);
                    AABB cBox = getCompleteBox();
                    double minX = Math.max(box.minX, cBox.minX);
                    double minY = Math.max(box.minY, cBox.minY);
                    double minZ = Math.max(box.minZ, cBox.minZ);
                    double maxX = Math.min(box.maxX, cBox.maxX);
                    double maxY = Math.min(box.maxY, cBox.maxY);
                    double maxZ = Math.min(box.maxZ, cBox.maxZ);
                    built.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ).move(x, y, z));
                }
            }
            cached = List.copyOf(built);
            boxes = cached;
        }
        arraylist.addAll(cached.stream()
            .filter(box -> area == null || area.intersects(box))
            .toList());
    }

    @Override
    public AABB getCompleteBox() {
        return TubeCollision.completeBox(TubeModels.Kind.SCURVE, orientation);
    }

    @Override
    @Nullable
    public ITubeOrientation getTubeOrientation(Player player, int xPos, int zPos) {
        double x = xPos + 0.5 - player.getX();
        double z = zPos + 0.5 - player.getZ();
        double w = Math.atan2(x, z);
        double halfPI = Math.PI / 2;
        double halfhalfPI = halfPI / 2;
        w -= halfhalfPI;
        if (w < 0) {
            w += 2 * Math.PI;
        }
        Direction dir = null;
        Direction dir1 = null;
        Direction dir2 = null;
        double addition = 0;
        if (0 < w && w <= halfPI) {
            dir = Direction.EAST;
            dir1 = Direction.NORTH;
            dir2 = Direction.SOUTH;
            addition = halfPI;
        } else if (halfPI < w && w <= 2 * halfPI) {
            dir = Direction.NORTH;
            dir1 = Direction.EAST;
            dir2 = Direction.WEST;
        } else if (2 * halfPI < w && w <= 3 * halfPI) {
            dir = Direction.WEST;
            dir1 = Direction.NORTH;
            dir2 = Direction.SOUTH;
            addition = halfPI;
        } else if (3 * halfPI < w && w <= 4 * halfPI) {
            dir = Direction.SOUTH;
            dir1 = Direction.EAST;
            dir2 = Direction.WEST;
        }
        w = Math.atan2(player.getLookAngle().x, player.getLookAngle().z);
        w -= addition;
        if (w < 0) {
            w += 2 * Math.PI;
        }
        Direction dir3 = null;
        if (0 < w && w <= 2 * halfPI) {
            dir3 = dir1;
        } else if (2 * halfPI < w && w <= 4 * halfPI) {
            dir3 = dir2;
        }
        for (CurveSOrientation curve : CurveSOrientation.values()) {
            if (curve.dir.equals(dir)) {
                if (curve.looking.equals(dir3)) {
                    return curve;
                }
            }
        }
        return null;
    }

    @Override
    public void serialize(ValueOutput output) {
        super.serialize(output);
        output.putString("orientation", orientation.name());
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        orientation = CurveSOrientation.valueOf(input.getStringOr("orientation", ""));
    }

    @Override
    public float getPipeLength() {
        return 4;
    }

    @Override
    public @Nullable Direction getExitForInput(Direction comingFrom) {
        if (orientation.dir.getOpposite() == comingFrom) {
            return orientation.dir;
        }
        if (orientation.dir == comingFrom) {
            return orientation.dir.getOpposite();
        }
        return null;
    }

    @Override
    @Nullable
    public BlockEntity getConnectedEndTile(Direction output) {
        boolean useOwn;
        if (!BlockPos.ZERO.equals(orientation.getOffset())) {
            if (orientation.dir.getOpposite() == output) {
                useOwn = false;
            } else if (orientation.dir == output) {
                useOwn = true;
            } else {
                return null;
            }
        } else {
            if (orientation.dir.getOpposite() == output) {
                useOwn = true;
            } else if (orientation.dir == output) {
                useOwn = false;
            } else {
                return null;
            }
        }
        if (useOwn) {
            return getContainer().getBlockEntity(output);
        } else {
            PositionRotation rotation = new PositionRotation();
            orientation.rotatePositions(rotation);
            BlockPos offset = rotation.apply(new BlockPos(1, 0, -3));
            BlockEntity subTile = getLevel().getBlockEntity(getPos().offset(offset));
            if (subTile instanceof LogisticsGenericSubMultiBlockBlockEntity genericSubMultiBlockBlockEntity) {
                return genericSubMultiBlockBlockEntity.getTile(output);
            }
        }
        return null;
    }

    @Override
    public boolean actAsNormalPipe() {
        return false;
    }

    @Override
    public @Nullable Vec3 getItemRenderPos(float fPos, LPTravelingItem travelItem) {
        if ((orientation.getDir().getOpposite() == travelItem.input) ==
            (!BlockPos.ZERO.equals(orientation.getOffset()))) {
            fPos = transport.getPipeLength() - fPos;
        }

        double x = 0.5;
        double y = 0.5;
        double z = 0.5;
        double sideOffset =
            TubeCurveProfile.offset(fPos / transport.getPipeLength() * 3) * transport.getPipeLength() / 3;
        switch (orientation.getRenderOrientation()) {
            case NORTH -> {
                z -= fPos;
                z += 0.5;
                x += sideOffset;
            }
            case NORTH_INV -> {
                z -= fPos;
                z += 0.5;
                x -= sideOffset;
            }
            case EAST -> {
                x -= fPos;
                x += 3.5;
                z -= 1;
                z -= sideOffset;
            }
            case EAST_INV -> {
                x -= fPos;
                x += 3.5;
                z += 1;
                z += sideOffset;
            }
        }
        return new Vec3(x, y, z);
    }

    @Override
    public double getItemRenderYaw(float fPos, LPTravelingItem travelItem) {
        if ((orientation.getDir().getOpposite() == travelItem.input) ==
            (!BlockPos.ZERO.equals(orientation.getOffset()))) {
            fPos = transport.getPipeLength() - fPos;
        }
        double b;
        if (fPos < 0.5) {
            b = TubeCurveProfile.slope(0.5 / transport.getPipeLength() * 3);
            b = b * transport.getPipeLength() * -13;
            b = b * fPos / 0.5;
        } else if (fPos < 3.5) {
            b = TubeCurveProfile.slope(fPos / transport.getPipeLength() * 3);
            b = b * transport.getPipeLength() * -13;
        } else {
            b = TubeCurveProfile.slope(3.5 / transport.getPipeLength() * 3);
            b = b * transport.getPipeLength() * -13;
            b = b * (transport.getPipeLength() - fPos) / (transport.getPipeLength() - 3.5);
        }
        return switch (orientation.getRenderOrientation()) {
            case NORTH -> b;
            case NORTH_INV -> -b;
            case EAST -> 90 + b;
            case EAST_INV -> 90 - b;
        };
    }

    @Override
    public boolean isHSTube() {
        return true;
    }

    @AllArgsConstructor
    public enum CurveSOrientation implements ITubeOrientation {
        // Name: Placement from  _ TurnDirection
        NORTH_EAST(TurnSDirection.NORTH_INV, new BlockPos(0, 0, 0), Direction.NORTH, Direction.EAST),
        NORTH_WEST(TurnSDirection.NORTH, new BlockPos(0, 0, 0), Direction.NORTH, Direction.WEST),
        EAST_SOUTH(TurnSDirection.EAST_INV, new BlockPos(0, 0, 0), Direction.EAST, Direction.SOUTH),
        EAST_NORTH(TurnSDirection.EAST, new BlockPos(0, 0, 0), Direction.EAST, Direction.NORTH),
        SOUTH_WEST(TurnSDirection.NORTH_INV, new BlockPos(-1, 0, 3), Direction.SOUTH, Direction.WEST),
        SOUTH_EAST(TurnSDirection.NORTH, new BlockPos(1, 0, 3), Direction.SOUTH, Direction.EAST),
        WEST_NORTH(TurnSDirection.EAST_INV, new BlockPos(-3, 0, -1), Direction.WEST, Direction.NORTH),
        WEST_SOUTH(TurnSDirection.EAST, new BlockPos(-3, 0, 1), Direction.WEST, Direction.SOUTH);

        @Getter
        final TurnSDirection renderOrientation;
        @Getter
        final BlockPos offset;
        @Getter
        final Direction dir;
        @Getter
        final Direction looking;

        @Override
        public void rotatePositions(IPositionRotateble set) {
            renderOrientation.rotatePositions(set);
        }

        @Override
        public void setOnPipe(CoreMultiBlockPipe pipe) {
            ((HSTubeSCurve) pipe).orientation = this;
        }
    }

    @AllArgsConstructor
    public enum TurnSDirection implements ITubeRenderOrientation {
        NORTH(Direction.NORTH),
        EAST(Direction.EAST),
        NORTH_INV(Direction.SOUTH),
        EAST_INV(Direction.WEST);

        @Getter
        private final Direction dir1;

        public void rotatePositions(IPositionRotateble set) {
            switch (this) {
                case NORTH -> set.mirrorX();
                case EAST -> {
                    set.mirrorX();
                    set.rotateRight();
                }
                case NORTH_INV -> {
                }
                case EAST_INV -> set.rotateRight();
            }
        }
    }
}
