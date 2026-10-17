package logisticspipes.pipes.tubes;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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

public class HSTubeGain extends CoreMultiBlockPipe {

    @Getter
    private TubeGainOrientation orientation;
    private final List<AABB> boxes = new ArrayList<>();

    public HSTubeGain(Item item) {
        super(new PipeMultiBlockTransportLogistics(), item);
    }

    @Override
    public void writeState(FriendlyByteBuf buffer) {
        buffer.writeNullable(orientation, FriendlyByteBuf::writeEnum);
    }

    @Override
    public void readState(FriendlyByteBuf buffer) {
        final TubeGainOrientation read = buffer.readNullable(inner -> inner.readEnum(TubeGainOrientation.class));
        if (read != null) {
            orientation = read;
        }
    }

    @Override
    public List<SubBlock> getSubBlocks() {
        return List.of(
            new SubBlock(new BlockPos(0, 0, -1), SubBlockTypeForShare.GAIN_B),
            new SubBlock(new BlockPos(0, 0, -2), SubBlockTypeForShare.GAIN_A),
            new SubBlock(new BlockPos(0, 1, -1), SubBlockTypeForShare.GAIN_A),
            new SubBlock(new BlockPos(0, 1, -2), SubBlockTypeForShare.GAIN_B),
            new SubBlock(new BlockPos(0, 1, -3), SubBlockTypeForShare.NON_SHARE));
    }

    @Override
    public List<SubBlock> getRotatedSubBlocks() {
        PositionRotation rotation = new PositionRotation();
        orientation.rotatePositions(rotation);
        return getSubBlocks().stream().map(block -> block.rotated(rotation)).toList();
    }

    @Override
    public void serialize(ValueOutput output) {
        super.serialize(output);
        output.putString("orientation", orientation.name());
    }

    @Override
    public void deserialize(ValueInput input) {
        super.deserialize(input);
        orientation = TubeGainOrientation.valueOf(input.getStringOr("orientation", ""));
    }

    @Override
    public void addCollisionBoxesToList(List<AABB> arraylist, @Nullable AABB area) {
        if (boxes.isEmpty()) {
            final BlockPos tubePos = getPos();
            double x = tubePos.getX();
            double y = tubePos.getY();
            double z = tubePos.getZ();
            for (int i = -1; i < 54; i++) {
                double xOne = x;
                double yOne = y;
                double zOne = z;
                double xTwo = x;
                double yTwo = y + 2;
                double zTwo = z;
                switch (orientation.getRenderOrientation()) {
                    case SOUTH -> {
                        zOne += 4.0F * (i - 4) / 50;
                        zTwo += 4.0F * (i + 4) / 50;
                        xOne += 1;
                        xTwo -= 2;
                    }
                    case WEST -> {
                        xOne -= 3;
                        xTwo -= 3;
                        xOne += 4.0F * (i - 4) / 50;
                        xTwo += 4.0F * (i + 4) / 50;
                        zOne -= 1;
                        zTwo += 2;
                    }
                    case NORTH -> {
                        zOne += 1;
                        zTwo += 1;
                        zOne -= 4.0F * (i - 4) / 50;
                        zTwo -= 4.0F * (i + 4) / 50;
                        xOne -= 1;
                        xTwo += 2;
                    }
                    case EAST -> {
                        xOne += 4;
                        xTwo += 4;
                        xOne -= 4.0F * (i - 4) / 50;
                        xTwo -= 4.0F * (i + 4) / 50;
                        zOne -= 1;
                        zTwo += 2;
                    }
                }
                AABB box = TubeCollision.boundsAt(TubeModels.Kind.GAIN, orientation,
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
                    boxes.add(new AABB(minX, minY, minZ, maxX, maxY, maxZ).move(x, y, z));
                }
            }
        }
        arraylist.addAll(boxes.stream()
            .filter(box -> area == null || area.intersects(box))
            .toList());
    }

    @Override
    public AABB getCompleteBox() {
        return TubeCollision.completeBox(TubeModels.Kind.GAIN, orientation);
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
        if (0 < w && w <= halfPI) {
            dir = Direction.EAST;
        } else if (halfPI < w && w <= 2 * halfPI) {
            dir = Direction.NORTH;
        } else if (2 * halfPI < w && w <= 3 * halfPI) {
            dir = Direction.WEST;
        } else if (3 * halfPI < w && w <= 4 * halfPI) {
            dir = Direction.SOUTH;
        }
        for (TubeGainOrientation ori : TubeGainOrientation.values()) {
            if (ori.dir.equals(dir)) {
                return ori;
            }
        }
        return null;
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
        if (orientation.dir.getOpposite() == output) {
            return getContainer().getBlockEntity(output);
        } else {
            PositionRotation rotation = new PositionRotation();
            orientation.rotatePositions(rotation);
            BlockPos offset = rotation.apply(new BlockPos(0, 1, -3));
            BlockEntity subTile = getLevel().getBlockEntity(getPos().offset(offset));
            if (subTile instanceof LogisticsGenericSubMultiBlockBlockEntity genericSubMultiBlockBlockEntity) {
                return genericSubMultiBlockBlockEntity.getTile(output);
            }
        }
        return null;
    }

    @Override
    public int getTextureIndex() {
        return 0;
    }

    @Override
    public boolean actAsNormalPipe() {
        return false;
    }

    @Override
    public @Nullable Vec3 getItemRenderPos(float fPos, LPTravelingItem travelItem) {
        if ((orientation.getDir().getOpposite() == travelItem.input) == (!BlockPos.ZERO.equals(
            orientation.getOffset()))) {
            fPos = transport.getPipeLength() - fPos;
        }

        double x = 0.5;
        double y = 1.5;
        double z = 0.5;
        y += TubeCurveProfile.offset(fPos / transport.getPipeLength() * 3) * transport.getPipeLength() / 3;
        switch (orientation.getRenderOrientation()) {
            case SOUTH -> {
                z -= fPos;
                z += 3.5;
            }
            case NORTH -> {
                z += fPos;
                z -= 3.5;
            }
            case WEST -> {
                x += fPos;
                x -= 3.5;
            }
            case EAST -> {
                x -= fPos;
                x += 3.5;
            }
        }
        return new Vec3(x, y, z);
    }

    @Override
    public double getItemRenderYaw(float fPos, LPTravelingItem travelItem) {
        boolean condition = ((orientation.getRenderOrientation() == TubeGainRenderOrientation.NORTH)
            || (orientation.getRenderOrientation() == TubeGainRenderOrientation.SOUTH));
        return condition ? 0 : 90;
    }

    @Override
    public double getItemRenderPitch(float fPos, LPTravelingItem travelItem) {
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
            case NORTH, WEST -> b;
            case SOUTH, EAST -> -b;
        };
    }

    @Override
    public boolean isHSTube() {
        return true;
    }

    public enum TubeGainOrientation implements ITubeOrientation {
        NORTH(TubeGainRenderOrientation.NORTH, new BlockPos(0, 0, 0), Direction.NORTH),
        SOUTH(TubeGainRenderOrientation.SOUTH, new BlockPos(0, 0, 0), Direction.SOUTH),
        EAST(TubeGainRenderOrientation.EAST, new BlockPos(0, 0, 0), Direction.EAST),
        WEST(TubeGainRenderOrientation.WEST, new BlockPos(0, 0, 0), Direction.WEST);

        @Getter
        final TubeGainRenderOrientation renderOrientation;
        @Getter
        final BlockPos offset;
        @Getter
        final Direction dir;

        TubeGainOrientation(TubeGainRenderOrientation render, BlockPos off, Direction dir) {
            renderOrientation = render;
            offset = off;
            this.dir = dir;
        }

        @Override
        public void rotatePositions(IPositionRotateble set) {
            renderOrientation.rotateOrientation(set);
        }

        @Override
        public void setOnPipe(CoreMultiBlockPipe pipe) {
            ((HSTubeGain) pipe).orientation = this;
        }
    }

    public enum TubeGainRenderOrientation implements ITubeRenderOrientation {
        NORTH(Direction.NORTH),
        SOUTH(Direction.SOUTH),
        WEST(Direction.WEST),
        EAST(Direction.EAST);

        @Getter
        final private Direction dir;

        TubeGainRenderOrientation(Direction dir) {
            this.dir = dir;
        }

        public void rotateOrientation(IPositionRotateble set) {
            switch (this) {
                case NORTH -> {
                }
                case EAST -> set.rotateRight();
                case WEST -> set.rotateLeft();
                case SOUTH -> {
                    set.rotateLeft();
                    set.rotateLeft();
                }
            }
        }
    }
}
