package cn.wusheng.oatz.movement;

import cn.wusheng.oatz.MovementImpulsePayload;
import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
        directSteeringAndJumpOrder();
        unlimitedIndependentImpulses();
        fallingCompensationReceipt();
        compensationCodec();
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
        vector(Vec3.ZERO, MovementImpulseState.approvedDelta(Vec3.ZERO, payload(Vec3.ZERO, .55D)));
        vector(Vec3.ZERO, MovementImpulseState.approvedDelta(Vec3.ZERO, payload(new Vec3(.1D, 0, 0), Double.NaN)));
    }

    private static void directSteeringAndJumpOrder() {
        check(MovementImpulseState.validPullTarget(new Vec3(.594D, 0, 0)));
        check(!MovementImpulseState.validPullTarget(new Vec3(.595D, 0, 0)));
        check(!MovementImpulseState.validPullTarget(new Vec3(Double.NaN, 0, 0)));
        check(!MovementImpulseState.validPullTarget(new Vec3(0, Double.POSITIVE_INFINITY, 0)));
        for (Vec3 target : new Vec3[] {new Vec3(.594D, 0, 0), new Vec3(.3D, .3D, 0), new Vec3(.3D, -.3D, 0)}) {
            for (boolean jumpFirst : new boolean[] {true, false}) {
                MovementImpulseState gate = new MovementImpulseState();
                Object player = new Object(), world = new Object();
                UUID session = UUID.randomUUID();
                Vec3 actual = new Vec3(-.7D, .4D, 1.3D);
                MovementImpulsePayload jump = jumpPayload(.25D);
                for (long sequence : new long[] {1, 1, 2, 1, 2, 3, 3, 4}) {
                    if (!gate.accept(player, world, OVERWORLD, session, sequence)) continue;
                    if (sequence == (jumpFirst ? 1 : 2)) actual = received(actual, jump);
                    else actual = MovementMath.directPull(actual, target);
                }
                // Repeated steering reads live Y, never a stale pre-jump snapshot.
                vector(new Vec3(target.x, 1.05D, target.z), actual);
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

    private static MovementImpulsePayload jumpPayload(double allowance) {
        return new MovementImpulsePayload(1, OVERWORLD, UUID.randomUUID(), 1,
                new Vec3(0, .65D, 0), -1, allowance);
    }

    private static void fallingCompensationReceipt() {
        MovementImpulsePayload jump = jumpPayload(.25D);
        double[][] cases = {{0, .65D}, {.42D, 1.07D}, {-.1D, .65D}, {-.25D, .65D},
                {-.4D, .5D}, {-.8D, .1D}, {-2, -1.1D}, {2.5D, 3.15D}};
        for (double[] sample : cases) {
            vector(new Vec3(.7D, sample[1], -.9D), received(new Vec3(.7D, sample[0], -.9D), jump));
        }
        // A receipt after the hook starts pulling upward must not use an old fall sample.
        Vec3 pulled = MovementMath.directPull(new Vec3(-.7D, -.4D, 1.3D), new Vec3(.3D, .3D, 0));
        vector(new Vec3(.3D, .95D, 0), received(pulled, jump));
        Vec3 jumped = received(new Vec3(-.7D, -.4D, 1.3D), jump);
        vector(new Vec3(.3D, .5D, 0), MovementMath.directPull(jumped, new Vec3(.3D, .3D, 0)));
        // Partial authorization can never be expanded by the recipient.
        vector(new Vec3(.7D, .35D, -.9D), received(new Vec3(.7D, -.4D, -.9D), jumpPayload(.1D)));
        for (double invalid : new double[] {-.01D, .26D, Double.NaN, Double.POSITIVE_INFINITY}) {
            vector(Vec3.ZERO, MovementImpulseState.approvedDelta(new Vec3(0, -.4D, 0), jumpPayload(invalid)));
        }
        // Replayed authorization does not compensate twice, even if Y has changed again.
        MovementImpulseState gate = new MovementImpulseState();
        Object player = new Object(), world = new Object();
        Vec3 velocity = new Vec3(.7D, -.4D, -.9D);
        for (int i = 0; i < 3; i++) {
            if (gate.accept(player, world, OVERWORLD, jump.sessionUuid(), jump.sequence())) velocity = received(velocity, jump);
        }
        vector(new Vec3(.7D, .5D, -.9D), velocity);
    }

    private static void compensationCodec() {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);
        try {
            MovementImpulsePayload expected = jumpPayload(.25D);
            MovementImpulsePayload.STREAM_CODEC.encode(buffer, expected);
            check(expected.equals(MovementImpulsePayload.STREAM_CODEC.decode(buffer)));
            check(!buffer.isReadable());
            close(0, payload(new Vec3(0, .65D, 0), -1).fallCompensation());
        } finally {
            buffer.release();
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
