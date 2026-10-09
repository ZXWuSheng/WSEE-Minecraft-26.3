package cn.wusheng.oatz.movement;

import cn.wusheng.oatz.MovementImpulsePayload;
import java.util.UUID;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/** Exercises the production receipt gate; no connection or live world is simulated. */
public final class MovementImpulseRegression {
    private static final Identifier OVERWORLD = Identifier.fromNamespaceAndPath("minecraft", "overworld");
    private static final Identifier NETHER = Identifier.fromNamespaceAndPath("minecraft", "the_nether");
    private static int checks;

    public static void main(String[] args) {
        duplicatesAndOrdering();
        retiredSessions();
        changedPlayerAndWorld();
        invalidRequestsDoNotConsume();
        conservativeRadialAllowance();
        rejectedForceCannotSubtractExternalMotion();
        unlimitedIndependentImpulses();
        System.out.println("Movement impulse receipt regression: " + checks + " assertions passed");
    }

    private static void duplicatesAndOrdering() {
        MovementImpulseState state = new MovementImpulseState();
        Object player = new Object(), world = new Object();
        UUID session = UUID.randomUUID();
        int applied = 0;
        for (long sequence : new long[] {1, 1, 3, 2, 3, 4, 1, 5, 4}) {
            if (state.accept(player, world, OVERWORLD, session, sequence)) applied++;
        }
        check(applied == 4); // Each approved increment can be applied only once.
        check(!state.accept(player, world, OVERWORLD, session, 5));
        check(!state.accept(player, world, OVERWORLD, session, 2));
        check(state.accept(player, world, OVERWORLD, session, 20)); // Gaps are allowed.
        check(!state.accept(player, world, OVERWORLD, session, 19));
        check(state.accept(player, world, OVERWORLD, session, 21));
    }

    private static void retiredSessions() {
        MovementImpulseState state = new MovementImpulseState();
        Object player = new Object(), world = new Object();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID(), third = UUID.randomUUID();
        check(state.accept(player, world, OVERWORLD, first, 50));
        check(state.accept(player, world, OVERWORLD, second, 1));
        check(!state.accept(player, world, OVERWORLD, first, 51));
        check(state.accept(player, world, OVERWORLD, second, 2));
        check(!state.accept(player, world, OVERWORLD, first, Long.MAX_VALUE));
        check(state.accept(player, world, OVERWORLD, third, 1));
        check(!state.accept(player, world, OVERWORLD, second, 100));
        check(!state.accept(player, world, OVERWORLD, first, 100));
        check(!state.accept(player, world, OVERWORLD, third, 1));
        check(state.accept(player, world, OVERWORLD, third, 2));
    }

    private static void changedPlayerAndWorld() {
        MovementImpulseState state = new MovementImpulseState();
        Object player = new Object(), world = new Object();
        UUID session = UUID.randomUUID();
        check(state.accept(player, world, OVERWORLD, session, 10));
        check(!state.accept(player, world, OVERWORLD, session, 1));
        // New client entity/world identities start a new receipt context. The
        // production apply() additionally checks the current entity ID/dimension.
        Object replacement = new Object();
        check(state.accept(replacement, world, OVERWORLD, session, 1));
        check(!state.accept(replacement, world, OVERWORLD, session, 1));
        Object replacementWorld = new Object();
        check(state.accept(replacement, replacementWorld, OVERWORLD, session, 1));
        check(!state.accept(replacement, replacementWorld, OVERWORLD, session, 1));
        check(state.accept(replacement, replacementWorld, NETHER, session, 1));
        check(!state.accept(replacement, replacementWorld, NETHER, session, 1));
        state.clear();
        state.clear();
        check(state.accept(replacement, replacementWorld, NETHER, session, 1));
        check(!state.accept(replacement, replacementWorld, NETHER, session, 1));
    }

    private static void invalidRequestsDoNotConsume() {
        MovementImpulseState state = new MovementImpulseState();
        Object player = new Object(), world = new Object();
        UUID session = UUID.randomUUID(), other = UUID.randomUUID();
        for (long invalid : new long[] {0, -1, Long.MIN_VALUE}) {
            check(!state.accept(player, world, OVERWORLD, session, invalid));
        }
        check(!state.accept(null, world, OVERWORLD, session, 1));
        check(!state.accept(player, null, OVERWORLD, session, 1));
        check(!state.accept(player, world, null, session, 1));
        check(!state.accept(player, world, OVERWORLD, null, 1));
        check(state.accept(player, world, OVERWORLD, session, 1));
        check(!state.accept(player, world, OVERWORLD, other, 0));
        check(state.accept(player, world, OVERWORLD, session, 2));
        check(state.accept(player, world, OVERWORLD, other, 1));
        check(!state.accept(player, world, OVERWORLD, session, 3));
    }

    private static MovementImpulsePayload payload(Vec3 delta, double limit) {
        return new MovementImpulsePayload(1, OVERWORLD, UUID.randomUUID(), 1, delta, limit);
    }

    private static Vec3 received(Vec3 current, MovementImpulsePayload payload) {
        return current.add(MovementImpulseState.approvedDelta(current, payload));
    }

    private static void conservativeRadialAllowance() {
        // The accepted server displacement can lag behind actual client speed.
        // Applying only the remaining authorized allowance prevents .53+.1=.63.
        vector(new Vec3(.55D, .37D, -1.8D), received(new Vec3(.53D, .37D, -1.8D),
                payload(new Vec3(.1D, 0, 0), .55D)));
        Vec3 external = new Vec3(2.0D, .5D, -.7D);
        vector(external, received(external, payload(new Vec3(.1D, 0, 0), .55D)));
        vector(new Vec3(.55D, .4D, -.8D), received(new Vec3(.55D, .4D, -.8D),
                payload(new Vec3(.1D, 0, 0), .55D)));
        vector(new Vec3(-.1D, .4D, -.8D), received(new Vec3(-.2D, .4D, -.8D),
                payload(new Vec3(.1D, 0, 0), .55D)));

        Vec3 axis = new Vec3(1, 1, 0).normalize(), tangent = new Vec3(1, -1, 2);
        Vec3 result = received(axis.scale(.53D).add(tangent), payload(axis.scale(.1D), .55D));
        close(.55D, result.dot(axis));
        vector(tangent, result.subtract(axis.scale(result.dot(axis))));
        close(.55D, MovementMath.radialSpeedLimit(10));
        close(.275D, MovementMath.radialSpeedLimit(MovementMath.ARRIVAL_DISTANCE + .01D));
        close(.55D, MovementMath.radialSpeedLimit(MovementMath.ARRIVAL_DISTANCE + MovementMath.SLOWDOWN_DISTANCE));
        vector(Vec3.ZERO, MovementImpulseState.approvedDelta(Vec3.ZERO, payload(Vec3.ZERO, .55D)));
        vector(Vec3.ZERO, MovementImpulseState.approvedDelta(Vec3.ZERO, payload(new Vec3(.1D, 0, 0), Double.NaN)));
    }

    private static void rejectedForceCannotSubtractExternalMotion() {
        // The server observes a stale zero vector while the client already has
        // external radial motion. Its authorized force can be reduced to zero;
        // the next near-anchor update must not treat it as realized contribution.
        for (Vec3 axis : new Vec3[] {new Vec3(1, 0, 0), new Vec3(1, 1, 0).normalize()}) {
            Vec3 tangent = new Vec3(-axis.y, axis.x, -.7D);
            for (double externalSpeed : new double[] {1.2D, 2.0D}) {
                Vec3 external = axis.scale(externalSpeed).add(tangent);
                MovementMath.Pull stale = MovementMath.reel(Vec3.ZERO, axis.scale(10), 9, 9);
                MovementImpulsePayload request = payload(stale.velocity(), MovementMath.radialSpeedLimit(9));
                check(request.delta().length() > 0);
                vector(Vec3.ZERO, MovementImpulseState.approvedDelta(external, request));
                Vec3 actual = received(external, request);
                vector(external, actual);
                MovementMath.Pull near = MovementMath.reel(actual, axis.scale(3), 2, 2);
                vector(actual, near.velocity());
                vector(Vec3.ZERO, near.velocity().subtract(actual));
            }
        }
    }

    private static void unlimitedIndependentImpulses() {
        // A hook cap cannot change the separate upward jump authorization,
        // whether the current vertical motion is rising or fast falling.
        for (double y : new double[] {0, .8D, -2.0D, 2.5D}) {
            MovementImpulsePayload jump = new MovementImpulsePayload(1, OVERWORLD, UUID.randomUUID(), 1,
                    new Vec3(0, DoubleJumpController.VERTICAL_IMPULSE, 0));
            close(-1.0D, jump.radialLimit());
            vector(new Vec3(.7D, y + .65D, -.9D), received(new Vec3(.7D, y, -.9D), jump));
        }
    }

    private static void vector(Vec3 expected, Vec3 actual) {
        close(expected.x, actual.x);
        close(expected.y, actual.y);
        close(expected.z, actual.z);
    }

    private static void close(double expected, double actual) {
        check(Math.abs(expected - actual) <= 1.0E-12D);
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Movement impulse receipt check " + checks + " failed");
    }
}
