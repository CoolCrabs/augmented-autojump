package io.github.coolcrabs.augmentedautojump;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
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
        double bpt = Math.sqrt((dx * dx) + (dz * dz)); // Current speed in blocks per tick
        Box currentBox = player.getBoundingBox();
        float yawDeg = -player.getYaw(0) * (float)(Math.PI / 180);
        double yawDeltaX = MathHelper.sin(yawDeg);
        double yawDeltaZ = MathHelper.cos(yawDeg);
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
                    double ydiff = jumpTargetShape.getMax(Axis.Y) - player.getY();
                    if (ydiff > player.stepHeight + 0.001 && ydiff < jumpHeight && hasHeadSpace(player, currentBox, jumpHeight, pos)) {
                        return true;
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
}
