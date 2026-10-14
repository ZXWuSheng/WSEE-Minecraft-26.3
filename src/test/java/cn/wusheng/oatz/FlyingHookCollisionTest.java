package cn.wusheng.oatz;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/** Geometric regression fixtures for real hook hitbox contacts; no game is launched. */
public final class FlyingHookCollisionTest {
    private static final AABB BLOCK = new AABB(0, 0, 0, 1, 1, 1);

    public static void main(String[] args) {
        Vec3 edgeStart = new Vec3(1.05, 0.5, -1);
        Vec3 motion = new Vec3(0, 0, 2);
        expect(BLOCK.clip(edgeStart, edgeStart.add(motion)).isEmpty(), "Reproduce a center-ray miss");
        var edge = contact(edgeStart, motion, BLOCK);
        expect(edge != null, "The 0.25-block hook hitbox must still hit the edge");
        expect(edge.hit().getDirection() == Direction.NORTH, "Keep the actual entry face");
        expect(edge.hit().getLocation().distanceTo(new Vec3(1, 0.5, 0)) < 1.0E-7,
                "The anchor must be on the original block edge, not its expanded bounds");

        var upperEdge = contact(new Vec3(0.5, 1.1, -1), motion, BLOCK);
        expect(upperEdge != null && Math.abs(upperEdge.hit().getLocation().y - 1.0) < 1.0E-7,
                "Catch the top edge even when the center passes above it");
        expect(contact(new Vec3(1.2, 0.5, -1), motion, BLOCK) == null,
                "A near miss outside the real hook width must remain a miss");
        expect(contact(new Vec3(1.125, 0.5, -1), motion, BLOCK) == null,
                "Do not snag tangential travel with no physical overlap");

        AABB slab = new AABB(0, 0, 0, 1, 0.5, 1);
        var slabEdge = contact(new Vec3(0.5, 0.6, -1), motion, slab);
        expect(slabEdge != null && Math.abs(slabEdge.hit().getLocation().y - 0.5) < 1.0E-7,
                "Use the slab collision shape rather than treating it as a full cube");
        expect(contact(new Vec3(0.5, 0.8, -1), motion, slab) == null, "Respect empty space above slabs");

        Vec3 fallingStart = new Vec3(0.5, 1.14, -1);
        expect(contact(fallingStart, motion, BLOCK) == null, "The old straight path passes over the edge");
        var falling = contact(fallingStart, motion.add(0, -0.03F, 0), BLOCK);
        expect(falling != null && falling.hit().getDirection() == Direction.UP,
                "Gravity turns this near miss into a real top-face hitbox contact");

        for (Direction face : Direction.values()) {
            Vec3 normal = new Vec3(face.getStepX(), face.getStepY(), face.getStepZ());
            var hit = contact(BLOCK.getCenter().add(normal.scale(2)), normal.scale(-3), BLOCK);
            expect(hit != null && hit.hit().getDirection() == face, "Hit the " + face + " block face");
            expect(Math.abs(hit.fraction() - 1.375 / 3.0) < 1.0E-7, "Choose earliest contact along " + face);
        }
        Vec3 touching = new Vec3(0.5, 0.5, -0.125);
        expect(contact(touching, new Vec3(0, 0, -1), BLOCK) == null, "Do not attach while moving away");
        var forward = contact(touching, new Vec3(0, 0, 1), BLOCK);
        expect(forward != null && forward.fraction() == 0.0, "Recognize contact at the start of a tick");
        var endpoint = contact(new Vec3(0.5, 0.5, -1), new Vec3(0, 0, 0.875), BLOCK);
        expect(endpoint != null && endpoint.fraction() == 1.0, "Recognize contact exactly at the tick endpoint");
        expect(contact(edgeStart, Vec3.ZERO, BLOCK) == null, "A stationary hook cannot hit a distant block");
        checkBlockedPulls();
        System.out.println("Flying Hook collision checks passed (hook contacts and blocked player pulls)");
    }

    private static void checkBlockedPulls() {
        AABB player = new AABB(-0.3, 0, -0.3, 0.3, 1.8, 0.3);
        VoxelShape wall = Shapes.box(0.7, 0, -1, 1.7, 3, 1);
        expect(!blocked(player, new Vec3(0.95, 0, 0), wall), "Do not stop before the player has reached an obstacle");
        expect(!blocked(player, new Vec3(0.2, 0, 0), wall), "A wall beyond this tick's movement is not yet blocking");

        VoxelShape touchingWall = Shapes.box(0.3, 0, -1, 1.3, 3, 1);
        expect(!FlyingHookCollision.blocksPull(player, new Vec3(0.3, 0.4, 0), null, true, List.of(touchingWall)),
                "The first pull must get a movement attempt before deciding it is blocked");
        expect(!FlyingHookCollision.blocksPull(player, new Vec3(0.3, 0.4, 0), new Vec3(0.2, 0, 0), true, List.of(touchingWall)),
                "Arriving at a wall while still advancing is not yet a stalled pull");
        expect(blocked(player, new Vec3(0.3, 0.4, 0), touchingWall), "Stop a pull after the contacted wall prevented inward progress");
        expect(!blocked(player, new Vec3(-0.3, 0, 0), touchingWall), "Allow pulling away from a wall");
        expect(!blocked(player, new Vec3(0, 0.5, 0), touchingWall), "Allow pulling upward along a wall");
        expect(!blocked(player, new Vec3(0, 0, 0.5), touchingWall), "Allow pulling sideways along a wall");

        VoxelShape ceiling = Shapes.box(-1, 2, -1, 1, 3, 1);
        expect(!blocked(player, new Vec3(0, 0.55, 0), ceiling), "Allow movement until reaching the ceiling");
        expect(blocked(player.move(0, 0.2, 0), new Vec3(0, 0.55, 0), ceiling), "Stop when the contacted ceiling blocks upward movement");
        VoxelShape floor = Shapes.box(-2, -1, -2, 2, 0, 2);
        expect(blocked(player, new Vec3(0, -0.55, 0), floor), "Stop downward pulling into the floor");
        expect(!blocked(player, new Vec3(0.5, 0, 0), floor), "Standing on the floor must not block horizontal pulling");
        expect(!blocked(player, new Vec3(0.5, -0.2, 0), floor), "A downward cast must still pull horizontally across supporting ground");
        expect(!blocked(player, new Vec3(0.4, 0.6, 0), floor), "Allow lifting off the ground");

        VoxelShape edge = Shapes.box(0.3, 0, 0.25, 1.3, 1, 1.25);
        expect(new AABB(0.3, 0, 0.25, 1.3, 1, 1.25).clip(
                player.getCenter(), player.getCenter().add(0.95, 0, 0)).isEmpty(), "The center line misses this block edge");
        expect(blocked(player, new Vec3(0.95, 0, 0), edge), "The player's shoulder is blocked by the contacted edge");

        VoxelShape slab = Shapes.box(0.3, 0, -1, 1.3, 0.5, 1);
        expect(blocked(player, new Vec3(0.95, 0, 0), slab), "Respect the solid half of a slab");
        expect(!FlyingHookCollision.blocksPull(player, new Vec3(0.5, -0.2, 0), null, true, List.of(floor, slab)),
                "Downward casting on an uphill step must not retract before vanilla movement runs");
        expect(!FlyingHookCollision.blocksPull(player.move(0.4, 0.5, 0), new Vec3(0.5, -0.2, 0),
                        new Vec3(0.4, 0.5, 0), true, List.of(floor, slab)),
                "After vanilla steps up the slab, its top is supporting ground, not an obstruction");
        expect(!blocked(player.move(0, 0.5, 0), new Vec3(0.95, 0, 0), slab), "Allow passing above the slab's empty half");
        expect(!blocked(player, new Vec3(0.95, 0, 0), Shapes.empty()), "Grass, vines and other empty collision shapes must not stop pulling");
        expect(!blocked(player, Vec3.ZERO, wall), "No movement cannot create a collision with a distant wall");
    }

    private static boolean blocked(AABB player, Vec3 movement, VoxelShape... blocks) {
        return FlyingHookCollision.blocksPull(player, movement, Vec3.ZERO, true, List.of(blocks));
    }

    private static FlyingHookCollision.Contact contact(Vec3 center, Vec3 movement, AABB shape) {
        return FlyingHookCollision.sweep(AABB.ofSize(center, 0.25, 0.25, 0.25), movement, shape, BlockPos.ZERO);
    }

    private static void expect(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
