package cn.wusheng.oatz.movement;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Rope forces change only radial motion; vanilla movement owns position and collisions. */
public final class MovementMath {
    public static final double MAX_DISTANCE = 32.0D; // Vanilla FishingHook: distance squared <= 1024.
    public static final double PROJECTILE_SPEED_MULTIPLIER = 1.15D;
    public static final double REEL_SPEED = 0.35D;
    public static final double PULL_ACCELERATION = 0.24D;
    public static final double MAX_RADIAL_SPEED = 0.55D;
    public static final double ARRIVAL_DISTANCE = 1.5D;
    public static final double SLOWDOWN_DISTANCE = 3.0D;
    public static final double MIN_TENSION_FACTOR = 0.5D;
    public static final int REEL_TIMEOUT = 120;
    public static final int STUCK_TICKS = 10;

    private MovementMath() {}

    public record Pull(Vec3 velocity, double ropeLength) {}

    public static Vec3 flyingVelocity(Vec3 current, double gravity) {
        return current.add(0.0D, -gravity, 0.0D);
    }

    private static double proximityFactor(double boxGap) {
        return Math.min(1.0D, Math.max(MIN_TENSION_FACTOR,
                (boxGap - ARRIVAL_DISTANCE) / SLOWDOWN_DISTANCE));
    }

    public static double radialSpeedLimit(double boxGap) {
        return MAX_RADIAL_SPEED * proximityFactor(boxGap);
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

    public static Pull reel(Vec3 current, Vec3 toAnchor, double boxGap, double ropeLength) {
        double length = Math.max(ARRIVAL_DISTANCE, ropeLength - REEL_SPEED);
        double distance = toAnchor.length();
        if (distance < 1.0E-8D || distance <= length || boxGap <= ARRIVAL_DISTANCE) {
            return new Pull(current, length);
        }
        Vec3 radial = toAnchor.scale(1.0D / distance);
        double speed = current.dot(radial);
        // 近端保留有限张力，避免在到达边界外渐近停住，或因重力无法完成向上收绳。
        double proximity = proximityFactor(boxGap);
        double cap = radialSpeedLimit(boxGap);
        double extra = Math.min(PULL_ACCELERATION * proximity,
                Math.min((distance - length) * 0.4D, Math.max(0.0D, cap - speed)));
        // The receipt may reduce an authorized increment against actual client
        // speed. The server cannot infer its realized contribution, so near the
        // anchor it only reduces new force and never subtracts existing motion.
        return new Pull(current.add(radial.scale(extra)), length);
    }
}
