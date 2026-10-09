package cn.wusheng.oatz.movement;

import java.util.UUID;

/** Exercises the production server cycle without launching Minecraft. */
public final class DoubleJumpRegression {
    private static int checks;

    public static void main(String[] args) {
        impulseCases();
        inputCycle();
        noMidairRefresh();
        lifecycle();
        quickRequestBetweenInputSamples();
        System.out.println("Double Jump regression: " + checks + " assertions passed");
    }

    private static void impulseCases() {
        // Stationary, sprint launch, rising, falling, externally forced and
        // fast-falling movement all use current Y plus exactly one increment.
        for (double y : new double[] {0.0D, 0.42D, 1.1D, -0.3D, 2.5D, -2.0D}) {
            close(y + 0.65D, DoubleJumpController.addVerticalImpulse(y));
        }
        close(-1.35D, DoubleJumpController.addVerticalImpulse(-2.0D));
        // Hook acceleration is additive in either ordering. Its following tick
        // changes motion naturally instead of replacing this jump's velocity.
        close(DoubleJumpController.addVerticalImpulse(-0.2D) + 0.08D,
                DoubleJumpController.addVerticalImpulse(-0.2D + 0.08D));
    }

    private static void inputCycle() {
        DoubleJumpController controller = new DoubleJumpController();
        UUID id = UUID.randomUUID();
        Object player = new Object();
        Object world = new Object();
        check(!controller.consume(id, player, world, true, false));
        check(!controller.tick(id, player, world, true, true, true));
        for (int i = 0; i < 20; i++) {
            check(!controller.tick(id, player, world, true, false, true));
            check(!controller.consume(id, player, world, true, false));
        }
        check(!controller.tick(id, player, world, true, false, false));
        check(controller.tick(id, player, world, true, false, true));
        check(controller.consume(id, player, world, true, false));
        // A server input edge and its client payload may both arrive: only one
        // consume succeeds, so only one impulse and feedback burst are emitted.
        check(!controller.consume(id, player, world, true, false));
        check(!controller.tick(id, player, world, true, false, true));
        check(!controller.tick(id, player, world, true, false, false));
        check(!controller.tick(id, player, world, true, false, true));
        check(!controller.consume(id, player, world, true, false));
        check(!controller.tick(id, player, world, true, true, false));
        check(!controller.consume(id, player, world, true, true));
        check(!controller.tick(id, player, world, true, false, false));
        check(controller.consume(id, player, world, true, false));
    }

    private static void noMidairRefresh() {
        DoubleJumpController controller = new DoubleJumpController();
        UUID id = UUID.randomUUID();
        Object player = new Object();
        Object world = new Object();
        controller.tick(id, player, world, true, true, false);
        controller.tick(id, player, world, true, false, false);
        check(controller.consumeRequest(id, player, world, true, false));
        // Hook cancellation, item switching and contact with entities/wall
        // sides never call genuine-ground reset. Nor does re-equipping boots.
        for (int i = 0; i < 3; i++) {
            controller.tick(id, player, world, true, false, false);
            check(!controller.consumeRequest(id, player, world, true, false));
        }
        controller.tick(id, player, world, false, false, false);
        controller.tick(id, player, world, true, false, false);
        check(!controller.consumeRequest(id, player, world, true, false));
        controller.tick(id, player, world, true, true, false);
        controller.tick(id, player, world, true, false, false);
        check(!controller.consumeRequest(id, player, world, false, false));
        check(controller.consumeRequest(id, player, world, true, false));
        // Equipping boots for the first time while airborne gives no cycle.
        UUID other = UUID.randomUUID();
        controller.tick(other, player, world, true, false, false);
        check(!controller.consumeRequest(other, player, world, true, false));
    }

    private static void lifecycle() {
        DoubleJumpController controller = new DoubleJumpController();
        UUID id = UUID.randomUUID();
        Object player = new Object();
        Object world = new Object();
        Object otherWorld = new Object();
        controller.tick(id, player, world, true, true, false);
        controller.tick(id, player, world, true, false, false);
        check(!controller.consumeRequest(id, player, otherWorld, true, false));
        controller.tick(id, player, otherWorld, true, false, false);
        check(!controller.consumeRequest(id, player, otherWorld, true, false));
        controller.tick(id, player, otherWorld, true, true, false);
        controller.tick(id, player, otherWorld, true, false, false);
        check(!controller.consumeRequest(id, new Object(), otherWorld, true, false));
        controller.clearPlayer(id);
        controller.clearPlayer(id);
        check(!controller.consumeRequest(id, player, otherWorld, true, false));
        controller.tick(id, player, world, true, true, false);
        controller.tick(id, player, world, true, false, false);
        controller.clearAll();
        controller.clearAll();
        check(!controller.consumeRequest(id, player, world, true, false));
    }

    private static void quickRequestBetweenInputSamples() {
        DoubleJumpController controller = new DoubleJumpController();
        UUID id = UUID.randomUUID();
        Object player = new Object();
        Object world = new Object();
        Object otherWorld = new Object();
        controller.tick(id, player, world, true, true, true);
        // Before the server observes leaving the ground, a payload is premature.
        check(!controller.consumeRequest(id, player, world, true, false));
        for (int i = 0; i < 5; i++) {
            // A native held key never produces another trigger by itself.
            check(!controller.tick(id, player, world, true, false, true));
        }
        check(!controller.consume(id, player, world, true, false));
        // The client detects release/repress between two server input samples.
        // Reject wrong entitlement, player and dimension before accepting once.
        check(!controller.consumeRequest(id, player, world, false, false));
        check(!controller.consumeRequest(id, new Object(), world, true, false));
        check(!controller.consumeRequest(id, player, otherWorld, true, false));
        check(!controller.consumeRequest(id, player, world, true, true));
        check(controller.consumeRequest(id, player, world, true, false));
        check(!controller.consumeRequest(id, player, world, true, false));
        check(!controller.tick(id, player, world, true, false, false));
        check(!controller.tick(id, player, world, true, false, true));
        check(!controller.consume(id, player, world, true, false));
        // Conversely, a native trigger followed by its payload still consumes once.
        controller.tick(id, player, world, true, true, false);
        controller.tick(id, player, world, true, false, false);
        check(controller.tick(id, player, world, true, false, true));
        check(controller.consume(id, player, world, true, false));
        check(!controller.consumeRequest(id, player, world, true, false));
        // No request path can create a cycle while first equipping in midair.
        UUID ungrounded = UUID.randomUUID();
        controller.tick(ungrounded, player, world, true, false, false);
        check(!controller.consumeRequest(ungrounded, player, world, true, false));
        controller.clearPlayer(id);
        check(!controller.consumeRequest(id, player, world, true, false));
    }

    private static void close(double expected, double actual) {
        check(Math.abs(expected - actual) <= 1.0E-12D);
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError("Double Jump check " + checks + " failed");
        }
    }
}
