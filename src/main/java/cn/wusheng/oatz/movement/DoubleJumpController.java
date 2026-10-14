package cn.wusheng.oatz.movement;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Server-owned ground/release/press cycle; independent of hook and item state. */
public final class DoubleJumpController {
    /** Added once to world-space Y; deliberately no minimum upward velocity. */
    public static final double VERTICAL_IMPULSE = 0.65D;
    public static final double MAX_FALL_COMPENSATION = 0.25D;
    private final Map<UUID, Cycle> cycles = new HashMap<>();

    private static final class Cycle {
        private final Object player;
        private final Object dimension;
        private boolean available;
        private boolean releaseObserved;
        private boolean keyDown;
        private int airborneTicks;

        private Cycle(Object player, Object dimension) {
            this.player = player;
            this.dimension = dimension;
        }

        private boolean belongsTo(Object player, Object dimension) {
            return this.player == player && this.dimension.equals(dimension);
        }
    }

    /** Returns a server input rising edge that may request the same consume path as a payload. */
    public boolean tick(UUID id, Object player, Object dimension, boolean eligible,
            boolean genuineGroundContact, boolean keyDown) {
        Cycle cycle = cycles.get(id);
        if (cycle == null || !cycle.belongsTo(player, dimension)) {
            cycle = new Cycle(player, dimension);
            cycles.put(id, cycle);
        }
        boolean pressed = keyDown && !cycle.keyDown;
        cycle.keyDown = keyDown;
        if (!eligible) {
            // Losing boots or entering a forbidden movement mode can consume the
            // cycle, but can never create a new opportunity while airborne.
            cycle.available = false;
            cycle.releaseObserved = false;
            cycle.airborneTicks = 0;
            return false;
        }
        if (genuineGroundContact) {
            cycle.available = true;
            cycle.releaseObserved = false;
            cycle.airborneTicks = 0;
            return false;
        }
        cycle.airborneTicks = Math.min(cycle.airborneTicks + 1, 2);
        if (!keyDown) {
            cycle.releaseObserved = true;
        }
        return pressed && cycle.airborneTicks > 1 && cycle.releaseObserved && cycle.available;
    }

    /** Strict native input path: the server must have observed an airborne key release. */
    public boolean consume(UUID id, Object player, Object dimension, boolean eligible, boolean onGround) {
        return tryConsume(id, player, dimension, eligible, onGround, true);
    }

    /**
     * A client edge request can represent a release/repress between server input
     * samples. It conveys input only; equipment, ground history and the one-use
     * opportunity are still checked and consumed exclusively by this server.
     */
    public boolean consumeRequest(UUID id, Object player, Object dimension, boolean eligible, boolean onGround) {
        return tryConsume(id, player, dimension, eligible, onGround, false);
    }

    private boolean tryConsume(UUID id, Object player, Object dimension, boolean eligible,
            boolean onGround, boolean requireObservedRelease) {
        Cycle cycle = cycles.get(id);
        if (!eligible || onGround || cycle == null || !cycle.belongsTo(player, dimension)
                || !cycle.available || (requireObservedRelease && !cycle.releaseObserved) || cycle.airborneTicks <= 0) {
            return false;
        }
        cycle.available = false;
        cycle.releaseObserved = false;
        return true;
    }

    public static double addVerticalImpulse(double currentY) {
        return currentY + VERTICAL_IMPULSE + fallCompensation(currentY, MAX_FALL_COMPENSATION);
    }

    /** Consume only the authorized amount of downward speed; never boost an existing rise. */
    public static double fallCompensation(double currentY, double allowance) {
        return Math.min(allowance, Math.max(0.0D, -currentY));
    }

    public static boolean validCompensationAllowance(double allowance) {
        return Double.isFinite(allowance) && allowance >= 0.0D && allowance <= MAX_FALL_COMPENSATION;
    }

    public void clearPlayer(UUID id) {
        cycles.remove(id);
    }

    public void clearAll() {
        cycles.clear();
    }
}
