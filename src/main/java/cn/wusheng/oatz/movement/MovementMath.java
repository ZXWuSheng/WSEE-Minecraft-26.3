package cn.wusheng.oatz.movement;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Direct grapple steering; vanilla movement still owns position and collisions. */
public final class MovementMath {
    public static final double MAX_DISTANCE = 32.0D; // Vanilla FishingHook: distance squared <= 1024.
    public static final double PROJECTILE_SPEED_MULTIPLIER = 1.15D;
    public static final double MAX_PULL_SPEED = 0.594D;
    public static final double ARRIVAL_DISTANCE = 1.5D;
    public static final double SLOWDOWN_DISTANCE = 3.0D;
    public static final double MIN_PULL_FACTOR = 0.5D;
    public static final int REEL_TIMEOUT = 120;
    public static final int STUCK_TICKS = 10;

    private MovementMath() {}

    public static Vec3 flyingVelocity(Vec3 current, double gravity) {
        return current.add(0.0D, -gravity, 0.0D);
    }

    private static double proximityFactor(double boxGap) {
        return Math.min(1.0D, Math.max(MIN_PULL_FACTOR,
                (boxGap - ARRIVAL_DISTANCE) / SLOWDOWN_DISTANCE));
    }

    public static Vec3 pullTarget(Vec3 toAnchor, double boxGap) {
        if (!toAnchor.isFinite() || !Double.isFinite(boxGap) || boxGap <= ARRIVAL_DISTANCE
                || toAnchor.lengthSqr() < 1.0E-16D) return Vec3.ZERO;
        return toAnchor.normalize().scale(MAX_PULL_SPEED * proximityFactor(boxGap));
    }

    /** The target is authorized by the server; read Y at receipt, after any jump impulse. */
    public static Vec3 directPull(Vec3 current, Vec3 target) {
        if (!current.isFinite() || !target.isFinite()) return current;
        // Horizontal steering removes pendulum motion. Never reduce existing
        // upward motion: a jump can lift the player above even a lower anchor.
        // Gravity consumes that rise naturally; no stored/predicted jump bonus.
        return new Vec3(target.x, Math.max(current.y, target.y), target.z);
    }

    public static boolean supportsAnchor(VoxelShape shape, BlockPos block, Direction face, Vec3 anchor) {
        if (shape.isEmpty()) return false;
        // Inclusive edges are valid hitbox contacts even when a center ray
        // parallel to another face cannot clip the exact edge/corner again.
        Vec3 local = anchor.subtract(block.getX(), block.getY(), block.getZ());
        Direction.Axis axis = face.getAxis();
        double coordinate = axis.choose(local.x, local.y, local.z);
        for (AABB box : shape.toAabbs()) {
            double surface = face.getAxisDirection() == Direction.AxisDirection.POSITIVE ? box.max(axis) : box.min(axis);
            if (Math.abs(coordinate - surface) <= 1.0E-7D
                    && local.x >= box.minX - 1.0E-7D && local.x <= box.maxX + 1.0E-7D
                    && local.y >= box.minY - 1.0E-7D && local.y <= box.maxY + 1.0E-7D
                    && local.z >= box.minZ - 1.0E-7D && local.z <= box.maxZ + 1.0E-7D) return true;
        }
        return false;
    }

}
