package cn.wusheng.oatz.movement;

import net.minecraft.world.phys.Vec3;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Tests direct steering and trajectories, independent of a running world. */
public final class MovementMathRegression {
    private static int checks;

    public static void main(String[] args) {
        targetsAndArrival();
        removesSidewaysSwing();
        jumpDuringReeling();
        repeatedDirectPull();
        realCollisionAnchors();
        System.out.println("Flying Hook motion regression: " + checks + " assertions passed");
    }

    private static void targetsAndArrival() {
        equal(new Vec3(.594D, 0, 0), MovementMath.pullTarget(new Vec3(10, 0, 0), 9));
        equal(Vec3.ZERO, MovementMath.pullTarget(new Vec3(1, 0, 0), .7D));
        equal(Vec3.ZERO, MovementMath.pullTarget(Vec3.ZERO, 3));
        equal(Vec3.ZERO, MovementMath.pullTarget(new Vec3(Double.NaN, 0, 0), 9));
        close(.297D, MovementMath.pullTarget(new Vec3(3, 0, 0), 2).length());
        close(.594D, MovementMath.pullTarget(new Vec3(10, 5, -6), 9).length());
        close(32.0D, MovementMath.MAX_DISTANCE);
    }

    private static void removesSidewaysSwing() {
        for (Vec3 direction : new Vec3[] {new Vec3(10, 0, 0), new Vec3(10, 5, -6), new Vec3(10, -8, 4)}) {
            Vec3 target = MovementMath.pullTarget(direction, 9);
            for (Vec3 current : new Vec3[] {Vec3.ZERO, new Vec3(-.7D, .4D, 1.8D), new Vec3(2, -3, -.5D)}) {
                Vec3 after = MovementMath.directPull(current, target);
                close(target.x, after.x);
                close(target.z, after.z);
                close(0, after.x * direction.z - after.z * direction.x);
                check(after.y >= current.y && after.y >= target.y);
            }
        }
    }

    private static void jumpDuringReeling() {
        for (double vertical : new double[] {0, 0.42D, 1.1D, -0.3D, -2.0D, 2.5D}) {
            Vec3 jumped = new Vec3(0.1D, DoubleJumpController.addVerticalImpulse(vertical), 1.3D);
            // Base impulse plus bounded falling compensation; steering cannot erase the rise.
            close(vertical + .65D + Math.min(.25D, Math.max(0, -vertical)), jumped.y);
            for (Vec3 direction : new Vec3[] {new Vec3(10, 0, 0), new Vec3(10, 8, 0), new Vec3(10, -8, 0)}) {
                Vec3 target = MovementMath.pullTarget(direction, 9);
                Vec3 pulled = MovementMath.directPull(jumped, target);
                check(pulled.y >= jumped.y);
                if (jumped.y >= target.y) close(jumped.y, pulled.y);
                Vec3 fallingNaturally = pulled.add(0, -.08D, 0).multiply(.91D, .98D, .91D);
                Vec3 next = MovementMath.directPull(fallingNaturally, target);
                if (fallingNaturally.y >= target.y) close(fallingNaturally.y, next.y);
            }
        }
    }

    private static void repeatedDirectPull() {
        // This is a free-air numerical regression, not a Minecraft collision test.
        // Include lower/upper anchors and lateral motion that used to cause orbiting.
        for (Vec3 anchor : new Vec3[] {new Vec3(16, 0, 0), new Vec3(12, 12, 6), new Vec3(12, -8, -6)}) {
            for (boolean jump : new boolean[] {false, true}) {
                Vec3 position = Vec3.ZERO, velocity = new Vec3(-.5D, .42D, 1.8D);
                boolean arrived = false;
                for (int tick = 0; tick < MovementMath.REEL_TIMEOUT; tick++) {
                    Vec3 toAnchor = anchor.subtract(position);
                    AABB box = new AABB(position.x - .3D, position.y - .9D, position.z - .3D,
                            position.x + .3D, position.y + .9D, position.z + .3D);
                    double gap = Math.sqrt(box.distanceToSqr(anchor));
                    if (gap <= MovementMath.ARRIVAL_DISTANCE) { arrived = true; break; }
                    if (jump && tick == 8) velocity = new Vec3(velocity.x,
                            DoubleJumpController.addVerticalImpulse(velocity.y), velocity.z);
                    Vec3 pulled = MovementMath.directPull(velocity, MovementMath.pullTarget(toAnchor, gap));
                    close(0, pulled.x * toAnchor.z - pulled.z * toAnchor.x);
                    check(pulled.isFinite());
                    if (jump && tick == 8) check(pulled.y >= velocity.y);
                    position = position.add(pulled);
                    velocity = pulled.add(0, -.08D, 0).multiply(.91D, .98D, .91D);
                }
                check(arrived);
            }
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
