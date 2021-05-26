package io.github.coolcrabs.augmentedautojump;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.World;

public class AugmentedAutojump {
    private static final double PREDICTION_MULT = 6;

    private AugmentedAutojump() { }

    public static boolean autojumpPlayer(ClientPlayerEntity player, float dx, float dz) {
        if (!player.input.hasForwardMovement()) return false;
        float jumpHeight = 1.2F;
        if (player.hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            jumpHeight += (float)(player.getStatusEffect(StatusEffects.JUMP_BOOST).getAmplifier() + 1) * 0.75F;
        }
        World world = player.getEntityWorld();
        double bpt = MathHelper.clamp(Math.sqrt((dx * dx) + (dz * dz)), 0.1, 0.8); // Current speed in blocks per tick; Clamped to reasonable values for aproximating next location
        Box currentBox = player.getBoundingBox();
        float yawRad = -player.getYaw(0) * (float)(Math.PI / 180);
        double yawDeltaX = MathHelper.sin(yawRad);
        double yawDeltaZ = MathHelper.cos(yawRad);
        double predictionX = yawDeltaX * bpt * PREDICTION_MULT;
        double predictionZ = yawDeltaZ * bpt * PREDICTION_MULT;
        Box predictionBox = currentBox.offset(predictionX, 0, predictionZ);
        int minX = MathHelper.floor(predictionBox.minX);
        int minY = MathHelper.floor(predictionBox.minY);
        int minZ = MathHelper.floor(predictionBox.minZ);
        int maxX = MathHelper.floor(predictionBox.maxX);
        int maxY = MathHelper.floor(predictionBox.maxY);
        int maxZ = MathHelper.floor(predictionBox.maxZ);

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                for (int k = minZ; k <= maxZ; k++) {
                    pos.set(i, j, k);
                    VoxelShape jumpTargetShape = world.getBlockState(pos).getCollisionShape(world, pos).offset(i, j, k);
                    if (jumpTargetShape.isEmpty()) continue;
                    double playerAngle = mcDeg2NormalDeg((yawRad * (-180 / Math.PI)));
                    double ydiff = getCollisionY(angleToDirection(playerAngle).getOpposite(), jumpTargetShape) - player.getY();
                    if (ydiff > player.stepHeight + 0.001 && ydiff < jumpHeight) {
                        double playerToBlockAngle = calcAngle(player.getX(), player.getZ(), i + 0.5, k + 0.5);
                        if (!hasHeadSpace(player, currentBox, jumpHeight, pos)) continue;
                        if (Math.abs(angleDiff(playerToBlockAngle, playerAngle)) < 10 || Math.floorMod((int)playerAngle, 90) < 10 || Math.floorMod((int)playerAngle, 90) > 80) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static boolean hasHeadSpace(ClientPlayerEntity player, Box playerBox, float jumpHeight, BlockPos target) {
        int minX = MathHelper.floor(Math.min(playerBox.minX, target.getX()));
        int minY = MathHelper.floor(player.getY() + jumpHeight);
        int minZ = MathHelper.floor(Math.min(playerBox.minZ, target.getZ()));
        int maxX = MathHelper.floor(Math.max(playerBox.maxX, target.getX()));
        int maxY = MathHelper.floor(player.getY() + playerBox.getYLength() + jumpHeight);
        int maxZ = MathHelper.floor(Math.max(playerBox.maxZ, target.getZ()));

        BlockPos.Mutable pos = new BlockPos.Mutable();

        for (int i = minX; i <= maxX; i++) {
            for (int j = minY; j <= maxY; j++) {
                for (int k = minZ; k <= maxZ; k++) {
                    pos.set(i, j, k);
                    VoxelShape blockingShape = player.getEntityWorld().getBlockState(pos).getCollisionShape(player.getEntityWorld(), pos).offset(i, j, k);
                    if (blockingShape.getMin(Axis.Y) - player.getY() < jumpHeight + 1.7) return false;
                }
            }
        }

        return true;
    }

    public static double getCollisionY(Direction side, VoxelShape shape) {
        boolean positiveDirection = side.getOffsetX() + side.getOffsetZ() > 0;
        double maxDir = Double.NaN;
        double maxY = Double.NaN;
        for (Box box : shape.getBoundingBoxes()) {
            if (box.maxY > maxY || Double.isNaN(maxDir)) {
                if (positiveDirection) {
                    if (Double.isNaN(maxDir) || box.getMax(side.getAxis()) >= maxDir) {
                        maxDir = box.getMax(side.getAxis());
                        maxY = box.maxY;
                    }
                } else {
                    if (Double.isNaN(maxDir) || box.getMin(side.getAxis()) <= maxDir) {
                        maxDir = box.getMin(side.getAxis());
                        maxY = box.maxY;
                    }
                }
            }
        }
        return maxY;
    }

    public static Direction angleToDirection(double deg) {
        if (deg > 0 && deg < 45) {
            return Direction.NORTH;
        } else if (deg >= 45 && deg < 135) {
            return Direction.EAST;
        } else if (deg >= 135 && deg < 225) {
            return Direction.SOUTH;
        } else if (deg >= 225 && deg < 315) {
            return Direction.WEST;
        } else {
            return Direction.NORTH;
        }
    }

    public static double mcDeg2NormalDeg(double a) {
        a += 180;
        while (a < 0) a += 360;
        while (a > 360) a -= 360;
        return a;
    }

    public static double calcAngle(double x, double y, double x1, double y1) {
        return MathHelper.atan2(x - x1, y1 - y) * 180 / Math.PI + 180;
    }

    /**
     * Gets the diffrence between 2 degree angles
     * @param a 0 <= a <= 360
     * @param b 0 <= b <= 360
     * @return diffrence
     */
    public static double angleDiff(double a, double b) {
        double difference = a - b;
        while (difference < -180) difference += 360;
        while (difference > 180) difference -= 360;
        return difference;
    }
}
