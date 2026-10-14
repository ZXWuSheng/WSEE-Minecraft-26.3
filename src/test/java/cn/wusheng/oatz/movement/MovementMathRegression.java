package cn.wusheng.oatz.movement;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tests the production rope force calculation, independent of a running world. */
public final class MovementMathRegression {
    private static int checks;

    public static void main(String[] args) {
        slackAndArrival();
        inertiaAndSpeedLimit();
        diagonalMotion();
        jumpDuringReeling();
        nearAnchorPreservesMotion();
        repeatedReeling();
        realCollisionAnchors();
        System.out.println("Flying Hook motion regression: " + checks + " assertions passed");
    }

    private static void slackAndArrival() {
        Vec3 external = new Vec3(0.75D, 1.4D, -2.1D);
        MovementMath.Pull slack = MovementMath.reel(external, new Vec3(10, 0, 0), 9, 12);
        equal(external, slack.velocity());
        close(11.65D, slack.ropeLength());
        MovementMath.Pull arrival = MovementMath.reel(external, new Vec3(1, 0, 0), 0.7D, 2);
        equal(external, arrival.velocity());
        close(1.65D, arrival.ropeLength());
        equal(external, MovementMath.reel(external, Vec3.ZERO, 0, 2).velocity());
        close(32.0D, MovementMath.MAX_DISTANCE);
    }

    private static void inertiaAndSpeedLimit() {
        Vec3 current = new Vec3(0.10D, 1.4D, -2.1D);
        Vec3 pulled = MovementMath.reel(current, new Vec3(10, 0, 0), 9, 9).velocity();
        close(0.34D, pulled.x);
        close(current.y, pulled.y);
        close(current.z, pulled.z);
        // An unrelated external radial speed above the hook's cap is retained.
        Vec3 fast = new Vec3(2.0D, -0.4D, 3.0D);
        equal(fast, MovementMath.reel(fast, new Vec3(10, 0, 0), 9, 9).velocity());
        Vec3 nearCap = new Vec3(0.52D, -0.1D, 0.8D);
        Vec3 capped = MovementMath.reel(nearCap, new Vec3(10, 0, 0), 9, 9).velocity();
        close(MovementMath.MAX_RADIAL_SPEED, capped.x);
        close(nearCap.y, capped.y);
        close(nearCap.z, capped.z);
    }

    private static void diagonalMotion() {
        Vec3 anchorDirection = new Vec3(10, 5, -6);
        Vec3 normal = anchorDirection.normalize();
        for (Vec3 current : new Vec3[] {
                Vec3.ZERO, new Vec3(-0.7D, 0.4D, 1.8D), new Vec3(2.0D, -3.0D, -0.5D)}) {
            Vec3 after = MovementMath.reel(current, anchorDirection, 10, 9).velocity();
            Vec3 beforeTangent = current.subtract(normal.scale(current.dot(normal)));
            Vec3 afterTangent = after.subtract(normal.scale(after.dot(normal)));
            equal(beforeTangent, afterTangent);
            double suppliedAcceleration = after.subtract(current).length();
            check(suppliedAcceleration <= MovementMath.PULL_ACCELERATION + 1.0E-9D);
        }
    }

    private static void jumpDuringReeling() {
        for (double vertical : new double[] {0, 0.42D, 1.1D, -0.3D, -2.0D, 2.5D}) {
            Vec3 jumped = new Vec3(0.1D, DoubleJumpController.addVerticalImpulse(vertical), 1.3D);
            Vec3 pulled = MovementMath.reel(jumped, new Vec3(10, 0, 0), 9, 9).velocity();
            close(vertical + 0.65D, pulled.y);
            close(jumped.z, pulled.z);
            check(pulled.x > jumped.x);
        }
    }

    private static void nearAnchorPreservesMotion() {
        Vec3 natural = new Vec3(2, 0.8D, -1.2D);
        // Near-anchor force reduces only new increments, even above the local cap.
        equal(natural, MovementMath.reel(natural, new Vec3(3, 0, 0), 2, 2).velocity());
        Vec3 aboveNearCap = new Vec3(0.4D, 0.8D, -1.2D);
        equal(aboveNearCap, MovementMath.reel(aboveNearCap, new Vec3(3, 0, 0), 2, 2).velocity());
        Vec3 atNearCap = new Vec3(MovementMath.radialSpeedLimit(2), 0.8D, -1.2D);
        equal(atNearCap, MovementMath.reel(atNearCap, new Vec3(3, 0, 0), 2, 2).velocity());
        MovementMath.Pull gentle = MovementMath.reel(Vec3.ZERO, new Vec3(3, 0, 0), 2, 2);
        check(gentle.velocity().x > 0 && gentle.velocity().x < MovementMath.PULL_ACCELERATION);
    }

    private static void repeatedReeling() {
        Vec3 velocity = new Vec3(0, 1.1D, -0.7D);
        double length = 20;
        for (int tick = 0; tick < 240; tick++) {
            MovementMath.Pull step = MovementMath.reel(velocity, new Vec3(25, 0, 0), 24, length);
            check(step.velocity().isFinite());
            check(step.velocity().x <= MovementMath.MAX_RADIAL_SPEED + 1.0E-9D);
            check(step.velocity().x >= velocity.x - 1.0E-9D);
            close(velocity.y, step.velocity().y);
            close(velocity.z, step.velocity().z);
            check(step.ropeLength() >= MovementMath.ARRIVAL_DISTANCE);
            check(step.ropeLength() <= length);
            velocity = step.velocity();
            length = step.ropeLength();
        }
    }

    private static void realCollisionAnchors() {
        BlockPos position = new BlockPos(10, 20, 30);
        VoxelShape full = Shapes.block();
        VoxelShape slab = Shapes.box(0, 0, 0, 1, 0.5D, 1);
        VoxelShape stairs = Shapes.or(slab, Shapes.box(0.5D, 0.5D, 0, 1, 1, 1));
        Vec3 wall = new Vec3(10, 20.7D, 30.7D);
        Vec3 slabTop = new Vec3(10.3D, 20.5D, 30.3D);
        Vec3 stairRecess = new Vec3(10.5D, 20.75D, 30.4D);
        check(MovementMath.supportsAnchor(full, position, Direction.WEST, wall));
        check(!MovementMath.supportsAnchor(full, position, Direction.EAST, wall));
        check(MovementMath.supportsAnchor(slab, position, Direction.UP, slabTop));
        check(!MovementMath.supportsAnchor(full, position, Direction.UP, slabTop));
        check(MovementMath.supportsAnchor(stairs, position, Direction.WEST, stairRecess));
        check(!MovementMath.supportsAnchor(slab, position, Direction.WEST, stairRecess));
        check(!MovementMath.supportsAnchor(stairs, position, Direction.WEST, new Vec3(10, 20.75D, 30.4D)));
        check(!MovementMath.supportsAnchor(Shapes.empty(), position, Direction.WEST, wall));
        check(!MovementMath.supportsAnchor(full, position, Direction.WEST, wall.add(0.02D, 0, 0)));
        // Arrival uses the closest point on the real player box, including side
        // and ceiling anchors, instead of requiring the player's center in a wall.
        AABB player = new AABB(-0.3D, 0, -0.3D, 0.3D, 1.8D, 0.3D);
        Vec3 side = new Vec3(0.5D, 0.9D, 0);
        close(0.2D, Math.sqrt(player.distanceToSqr(side)));
        close(0, player.distanceToSqr(new Vec3(0, 1.8D, 0)));
        check(Math.sqrt(player.distanceToSqr(side)) < player.getCenter().distanceTo(side));
    }

    private static void equal(Vec3 expected, Vec3 actual) {
        close(expected.x, actual.x);
        close(expected.y, actual.y);
        close(expected.z, actual.z);
    }

    private static void close(double expected, double actual) {
        checks++;
        if (Math.abs(expected - actual) > 1.0E-9D) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void check(boolean passed) {
        checks++;
        if (!passed) throw new AssertionError("Movement assertion " + checks + " failed");
    }
}
