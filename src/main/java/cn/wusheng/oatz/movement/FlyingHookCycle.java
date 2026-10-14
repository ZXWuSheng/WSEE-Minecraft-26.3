package cn.wusheng.oatz.movement;

import java.util.Objects;
import java.util.UUID;

/** The production cast lifecycle and force gate, independent of world/entity APIs. */
public final class FlyingHookCycle {
    // Keep the former waiting phase's wire ID reserved; new casts skip it.
    public static final byte INACTIVE = 0, FLYING = 1, ANCHORED = 2, REELING = 3;
    private final UUID hook;
    private final Object owner;
    private byte phase = FLYING;
    private boolean hasForceTick;
    private long lastForceTick;

    public FlyingHookCycle(UUID hook, Object owner) {
        this.hook = Objects.requireNonNull(hook);
        this.owner = Objects.requireNonNull(owner);
    }

    public byte phase() {
        return phase;
    }

    public boolean matchesRequest(Object player, UUID expectedHook, byte expectedPhase) {
        // Recall has the same meaning before and after impact. A flying-phase
        // cancellation may arrive after this exact hook has begun auto-reeling.
        return phase != INACTIVE && player == owner && hook.equals(expectedHook)
                && (expectedPhase == phase || expectedPhase == FLYING && phase == REELING);
    }

    public boolean anchor() {
        if (phase != FLYING) return false;
        phase = REELING;
        return true;
    }

    /** A duplicate or reordered tick cannot apply the continuous force again. */
    public boolean takeForceTick(long now) {
        if (phase != REELING || hasForceTick && now <= lastForceTick) return false;
        hasForceTick = true;
        lastForceTick = now;
        return true;
    }

    /** Every recall, completion and invalidation shares this one idempotent end transition. */
    public boolean finish() {
        if (phase == INACTIVE) return false;
        phase = INACTIVE;
        return true;
    }
}
