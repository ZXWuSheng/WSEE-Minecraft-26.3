package cn.wusheng.oatz.movement;

import java.util.UUID;

/** Exercises the lifecycle used by FlyingHookController; no live world is simulated. */
public final class FlyingHookCycleRegression {
    private static int checks;

    public static void main(String[] args) {
        flightAndAnchorWait();
        requestsAndForceTicks();
        sharedCleanup();
        System.out.println("Flying Hook cycle regression: " + checks + " assertions passed");
    }

    private static void flightAndAnchorWait() {
        Object owner = new Object();
        UUID hook = UUID.randomUUID();
        FlyingHookCycle cycle = new FlyingHookCycle(hook, owner);
        phase(FlyingHookCycle.FLYING, cycle);
        check(cycle.matchesRequest(owner, hook, FlyingHookCycle.FLYING));
        check(!cycle.beginReeling());
        for (long tick = 0; tick < 10; tick++) check(!cycle.takeForceTick(tick));
        check(cycle.anchor());
        phase(FlyingHookCycle.ANCHORED, cycle);
        check(!cycle.anchor());
        check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.FLYING));
        for (long tick = 10; tick < 30; tick++) check(!cycle.takeForceTick(tick));
        // Fixing a real impact cannot by itself begin continuous pulling.
        phase(FlyingHookCycle.ANCHORED, cycle);
        check(cycle.beginReeling());
        phase(FlyingHookCycle.REELING, cycle);
        check(!cycle.beginReeling());
        check(!cycle.anchor());
        check(cycle.takeForceTick(30));
    }

    private static void requestsAndForceTicks() {
        String owner = new String("same-account");
        UUID hook = UUID.randomUUID();
        FlyingHookCycle cycle = new FlyingHookCycle(hook, owner);
        check(!cycle.matchesRequest(new String("same-account"), hook, FlyingHookCycle.FLYING));
        check(!cycle.matchesRequest(owner, UUID.randomUUID(), FlyingHookCycle.FLYING));
        check(!cycle.matchesRequest(owner, null, FlyingHookCycle.FLYING));
        check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.INACTIVE));
        check(cycle.anchor());
        check(cycle.matchesRequest(owner, hook, FlyingHookCycle.ANCHORED));
        check(cycle.beginReeling());
        // The duplicate anchored request cannot cancel the newly started reel.
        check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.ANCHORED));
        check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.FLYING));
        check(cycle.matchesRequest(owner, hook, FlyingHookCycle.REELING));
        int forces = 0;
        for (long tick : new long[] {100, 100, 99, 101, 100, 101, 102, 102}) {
            if (cycle.takeForceTick(tick)) forces++;
        }
        check(forces == 3);
        check(cycle.finish());
        check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.REELING));
        check(!cycle.takeForceTick(103));
        check(!cycle.finish());
        FlyingHookCycle replacement = new FlyingHookCycle(UUID.randomUUID(), owner);
        check(!replacement.matchesRequest(owner, hook, FlyingHookCycle.FLYING));
        check(!replacement.matchesRequest(owner, hook, FlyingHookCycle.REELING));
    }

    private static void sharedCleanup() {
        // Each production completion/invalidation calls the same finish path.
        // These reasons name the scenarios; they do not claim to execute world
        // death, chunk availability, collision or dimension event integration.
        for (String reason : new String[] {
                "flight recall", "fixed recall", "cancel", "arrival", "anchor removed",
                "owner death", "dimension change", "lost rod", "hook removed"}) {
            Object owner = new Object();
            UUID hook = UUID.randomUUID();
            FlyingHookCycle cycle = new FlyingHookCycle(hook, owner);
            if (!reason.equals("flight recall")) check(cycle.anchor());
            if (!reason.equals("flight recall") && !reason.equals("fixed recall")) {
                check(cycle.beginReeling());
                check(cycle.takeForceTick(1));
            }
            check(cycle.finish());
            phase(FlyingHookCycle.INACTIVE, cycle);
            check(!cycle.finish());
            check(!cycle.anchor());
            check(!cycle.beginReeling());
            check(!cycle.takeForceTick(2));
            check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.INACTIVE));
            check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.FLYING));
            check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.ANCHORED));
            check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.REELING));
        }
    }

    private static void phase(byte expected, FlyingHookCycle cycle) {
        check(expected == cycle.phase());
    }

    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("Flying Hook lifecycle check " + checks + " failed");
    }
}
