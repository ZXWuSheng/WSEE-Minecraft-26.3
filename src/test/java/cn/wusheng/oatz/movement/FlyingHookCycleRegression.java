package cn.wusheng.oatz.movement;

import java.util.UUID;

/** Exercises the lifecycle used by FlyingHookController; no live world is simulated. */
public final class FlyingHookCycleRegression {
    private static int checks;

    public static void main(String[] args) {
        flightAndAutomaticReeling();
        requestsAndForceTicks();
        cancellationAcrossImpact();
        sharedCleanup();
        System.out.println("Flying Hook cycle regression: " + checks + " assertions passed");
    }

    private static void flightAndAutomaticReeling() {
        Object owner = new Object();
        UUID hook = UUID.randomUUID();
        FlyingHookCycle cycle = new FlyingHookCycle(hook, owner);
        phase(FlyingHookCycle.FLYING, cycle);
        check(cycle.matchesRequest(owner, hook, FlyingHookCycle.FLYING));
        for (long tick = 0; tick < 10; tick++) check(!cycle.takeForceTick(tick));
        check(cycle.anchor());
        // Impact itself enables pulling; no second rod-use action is needed.
        phase(FlyingHookCycle.REELING, cycle);
        check(cycle.takeForceTick(10));
        check(!cycle.anchor());
        check(!cycle.takeForceTick(10));
        check(cycle.takeForceTick(11));
    }

    private static void requestsAndForceTicks() {
        String owner = new String("same-account");
        UUID hook = UUID.randomUUID();
        FlyingHookCycle cycle = new FlyingHookCycle(hook, owner);
        check(!cycle.matchesRequest(new String("same-account"), hook, FlyingHookCycle.FLYING));
        check(!cycle.matchesRequest(owner, UUID.randomUUID(), FlyingHookCycle.FLYING));
        check(!cycle.matchesRequest(owner, null, FlyingHookCycle.FLYING));
        check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.INACTIVE));
        check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.REELING));
        check(cycle.anchor());
        check(!cycle.matchesRequest(owner, hook, FlyingHookCycle.ANCHORED));
        check(!cycle.matchesRequest(owner, hook, (byte) 4));
        check(cycle.matchesRequest(owner, hook, FlyingHookCycle.FLYING));
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

    private static void cancellationAcrossImpact() {
        Object owner = new Object();
        UUID hook = UUID.randomUUID();
        FlyingHookCycle cycle = new FlyingHookCycle(hook, owner);
        byte phaseWhenUseWasPressed = cycle.phase();
        check(cycle.anchor()); // The server receives the use packet after impact.
        check(cycle.matchesRequest(owner, hook, phaseWhenUseWasPressed));
        check(cycle.finish());
        check(!cycle.takeForceTick(1));
        check(!cycle.matchesRequest(owner, hook, phaseWhenUseWasPressed));
        check(!cycle.finish());
        FlyingHookCycle nextCast = new FlyingHookCycle(UUID.randomUUID(), owner);
        check(nextCast.anchor());
        check(!nextCast.matchesRequest(owner, hook, phaseWhenUseWasPressed));
        check(nextCast.takeForceTick(1));
    }

    private static void sharedCleanup() {
        // Each production completion/invalidation calls the same finish path.
        // These reasons name the scenarios; they do not claim to execute world
        // death, chunk availability, collision or dimension event integration.
        for (String reason : new String[] {
                "flight recall", "cancel", "arrival", "anchor removed",
                "owner death", "dimension change", "lost rod", "hook removed"}) {
            Object owner = new Object();
            UUID hook = UUID.randomUUID();
            FlyingHookCycle cycle = new FlyingHookCycle(hook, owner);
            if (!reason.equals("flight recall")) check(cycle.anchor());
            if (!reason.equals("flight recall")) {
                check(cycle.takeForceTick(1));
            }
            check(cycle.finish());
            phase(FlyingHookCycle.INACTIVE, cycle);
            check(!cycle.finish());
            check(!cycle.anchor());
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
