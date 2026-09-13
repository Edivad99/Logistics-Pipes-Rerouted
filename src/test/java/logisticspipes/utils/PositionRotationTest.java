package logisticspipes.utils;

import net.minecraft.core.BlockPos;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PositionRotationTest {

    private static final BlockPos POINT = new BlockPos(1, 7, 3);

    private static BlockPos rotate(String steps, BlockPos point) {
        PositionRotation rotation = new PositionRotation();
        for (char step : steps.toCharArray()) {
            switch (step) {
                case 'L' -> rotation.rotateLeft();
                case 'R' -> rotation.rotateRight();
                case 'X' -> rotation.mirrorX();
                case 'Z' -> rotation.mirrorZ();
                default -> throw new IllegalArgumentException("unknown step " + step);
            }
        }
        return rotation.apply(point);
    }

    @Test
    void eachStepMovesTheHorizontalPlaneAndLeavesTheHeight() {
        assertEquals(new BlockPos(3, 7, -1), rotate("L", POINT));
        assertEquals(new BlockPos(-3, 7, 1), rotate("R", POINT));
        assertEquals(new BlockPos(-1, 7, 3), rotate("X", POINT));
        assertEquals(new BlockPos(1, 7, -3), rotate("Z", POINT));
    }

    @Test
    void stepsComposeInTheOrderTheyAreRecorded() {
        // two quarter turns the same way is a half turn, whichever way round
        assertEquals(new BlockPos(-1, 7, -3), rotate("LL", POINT));
        assertEquals(new BlockPos(-1, 7, -3), rotate("RR", POINT));
        // and mirroring both axes is the same half turn
        assertEquals(new BlockPos(-1, 7, -3), rotate("XZ", POINT));
    }

    @Test
    void oppositeStepsCancel() {
        assertEquals(POINT, rotate("LR", POINT));
        assertEquals(POINT, rotate("RL", POINT));
        assertEquals(POINT, rotate("XX", POINT));
        assertEquals(POINT, rotate("LLLL", POINT));
    }

    @Test
    void aMirrorAfterATurnIsNotTheSameAsBeforeIt() {
        // order matters: this is what a composed transform has to get right
        assertEquals(new BlockPos(-3, 7, -1), rotate("LX", POINT));
        assertEquals(new BlockPos(3, 7, 1), rotate("XL", POINT));
    }
}
