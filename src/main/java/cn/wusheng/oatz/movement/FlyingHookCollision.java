package cn.wusheng.oatz.movement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Sweeps hook/player bounds against the actual block collision shapes. */
final class FlyingHookCollision {
    private static final double EPSILON = 1.0E-7D;

    record Contact(double fraction, BlockHitResult hit) {}

    private FlyingHookCollision() {
    }

    static boolean blocksPull(AABB playerBounds, Vec3 movement, Vec3 actualMovement, boolean onGround,
            Iterable<VoxelShape> blockShapes) {
        if (actualMovement == null) {
            return false;
        }
        for (VoxelShape shape : blockShapes) {
            for (AABB box : shape.toAabbs()) {
                Contact contact = sweep(playerBounds, movement, box, BlockPos.ZERO);
                // A future collision is not proof of obstruction: vanilla may step up
                // or slide on the ground before arriving at the predicted contact.
                if (contact == null || contact.fraction() > EPSILON) {
                    continue;
                }
                Direction.Axis axis = contact.hit().getDirection().getAxis();
                double requested = axis.choose(movement.x, movement.y, movement.z);
                if (axis == Direction.Axis.Y && requested < 0.0D && onGround
                        && movement.horizontalDistanceSqr() > EPSILON * EPSILON) {
                    // A downward cast still pulls across the supporting ground.
                    continue;
                }
                double advanced = axis.choose(actualMovement.x, actualMovement.y, actualMovement.z);
                if (advanced * Math.signum(requested) <= EPSILON) {
                    return true;
                }
            }
        }
        return false;
    }

    static Contact sweep(AABB hookBounds, Vec3 movement, AABB blockBox, BlockPos blockPos) {
        if (movement.lengthSqr() < EPSILON * EPSILON) {
            return null;
        }
        Vec3 center = hookBounds.getCenter();
        AABB expanded = blockBox.inflate(hookBounds.getXsize() / 2.0D,
                hookBounds.getYsize() / 2.0D, hookBounds.getZsize() / 2.0D);
        double entry = Double.NEGATIVE_INFINITY;
        double exit = Double.POSITIVE_INFINITY;
        Direction face = null;
        for (Direction.Axis axis : Direction.Axis.values()) {
            double start = axis.choose(center.x, center.y, center.z);
            double delta = axis.choose(movement.x, movement.y, movement.z);
            double min = expanded.min(axis);
            double max = expanded.max(axis);
            if (Math.abs(delta) < EPSILON) {
                // Tangential travel without overlap cannot collide on this axis.
                if (start <= min + EPSILON || start >= max - EPSILON) {
                    return null;
                }
                continue;
            }
            double near = ((delta > 0.0D ? min : max) - start) / delta;
            double far = ((delta > 0.0D ? max : min) - start) / delta;
            if (near > entry) {
                entry = near;
                face = Direction.fromAxisAndDirection(axis,
                        delta > 0.0D ? Direction.AxisDirection.NEGATIVE : Direction.AxisDirection.POSITIVE);
            }
            exit = Math.min(exit, far);
            if (entry > exit + EPSILON) {
                return null;
            }
        }
        // Do not attach while moving away from an existing overlap or beyond this tick.
        if (face == null || entry < -EPSILON || exit < 0.0D || entry > 1.0D + EPSILON) {
            return null;
        }
        double fraction = Math.clamp(entry, 0.0D, 1.0D);
        Vec3 impactCenter = center.add(movement.scale(fraction));
        // Clamp back onto the original shape: an expanded-box point is not a block anchor.
        Vec3 contact = new Vec3(Math.clamp(impactCenter.x, blockBox.minX, blockBox.maxX),
                Math.clamp(impactCenter.y, blockBox.minY, blockBox.maxY),
                Math.clamp(impactCenter.z, blockBox.minZ, blockBox.maxZ));
        return new Contact(fraction, new BlockHitResult(contact, face, blockPos.immutable(), false));
    }
}
