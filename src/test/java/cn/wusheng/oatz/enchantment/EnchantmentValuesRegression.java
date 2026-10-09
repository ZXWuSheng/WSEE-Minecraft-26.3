package cn.wusheng.oatz.enchantment;

import cn.wusheng.oatz.tool.RefiningRules;

/** Executable numeric contract without a game client or test dependency. */
public final class EnchantmentValuesRegression {
    private static int assertions;

    private EnchantmentValuesRegression() {
    }

    public static void main(String[] args) {
        double[] shieldPercentages = {0.08D, 0.12D, 0.16D, 0.20D, 0.25D};
        for (int level = 1; level <= 5; level++) {
            equal(0.55D, EnchantmentValues.flyingHookPullSpeed(level));
            equal(32, EnchantmentValues.flyingHookMaxDistance(level));
            equal(1.15D, EnchantmentValues.flyingHookProjectileSpeedMultiplier(level));
            equal(shieldPercentages[level - 1], EnchantmentValues.shieldAbsorptionPercent(level));
            equal(5.0D + level, EnchantmentValues.swordAuraRange(level));
        }

        int[] rainCounts = {6, 9, 12};
        int[] rainDurations = {30, 36, 40};
        double[] rainRadii = {2.25D, 2.75D, 3.25D};
        double[] ringDamage = {0.12D, 0.16D, 0.20D};
        double[][] comboDamage = {{1.0D, 1.05D, 1.12D}, {1.0D, 1.08D, 1.18D},
                {1.0D, 1.10D, 1.25D}};
        int[] momentumStacks = {3, 4, 5};
        double[] momentumPerStack = {0.04D, 0.05D, 0.06D};
        double[] foundationBonus = {0.15D, 0.25D, 0.35D};
        double[] surgeDamage = {1.15D, 1.25D, 1.35D};
        int[] surgeWaterTicks = {6, 5, 4};
        double[] holyHeal = {0.02D, 0.025D, 0.03D};
        double[] impactRadius = {3.0D, 4.0D, 5.0D};
        int[] overchargeTicks = {10, 15, 20};
        double[] overchargeDamage = {0.07D, 0.17D, 0.27D};
        double[] overchargeVelocity = {0.05D, 0.08D, 0.12D};
        for (int level = 1; level <= 3; level++) {
            int index = level - 1;
            equal(rainCounts[index], EnchantmentValues.arrowRainCount(level));
            equal(rainDurations[index], EnchantmentValues.arrowRainDuration(level));
            equal(rainRadii[index], EnchantmentValues.arrowRainRadius(level));
            equal(ringDamage[index], EnchantmentValues.shockRingDamageMultiplier(level));
            for (int stage = 0; stage <= 2; stage++) {
                equal(comboDamage[index][stage], EnchantmentValues.comboDamageMultiplier(level, stage));
            }
            equal(momentumStacks[index], EnchantmentValues.battleMomentumMaxStacks(level));
            equal(momentumPerStack[index], EnchantmentValues.battleMomentumPerStack(level));
            equal(foundationBonus[index], EnchantmentValues.foundationKnockbackBonus(level));
            equal(surgeDamage[index], EnchantmentValues.surgeDamageMultiplier(level));
            equal(surgeWaterTicks[index], EnchantmentValues.surgeRequiredWaterTicks(level));
            equal(holyHeal[index], EnchantmentValues.holyLightHealPercent(level));
            equal(impactRadius[index], EnchantmentValues.fallingStarRadius(level));
            equal(impactRadius[index], EnchantmentValues.shockRingRadius(level));
            equal(overchargeTicks[index], EnchantmentValues.overchargeExtraTicks(level));
            equal(overchargeDamage[index], EnchantmentValues.overchargeDamageBonus(level));
            equal(overchargeVelocity[index], EnchantmentValues.overchargeVelocityBonus(level));
            equal(0.0D, EnchantmentValues.overchargeProgress(level, 20));
            equal((overchargeTicks[index] / 2) / (double) overchargeTicks[index],
                    EnchantmentValues.overchargeProgress(level, 20 + overchargeTicks[index] / 2));
            equal(1.0D, EnchantmentValues.overchargeProgress(level, 20 + overchargeTicks[index]));
            equal(1.0D, EnchantmentValues.overchargeProgress(level, 100));
        }

        int[] marchTicks = {80, 70, 60, 50, 40};
        double[] marchSpeed = {0.06D, 0.09D, 0.12D, 0.15D, 0.18D};
        double[] swordDamage = {0.40D, 0.55D, 0.70D, 0.85D, 1.00D};
        for (int level = 1; level <= 5; level++) {
            equal(marchTicks[level - 1], EnchantmentValues.forcedMarchRequiredTicks(level));
            equal(marchSpeed[level - 1], EnchantmentValues.forcedMarchSpeedBonus(level));
            equal(swordDamage[level - 1], EnchantmentValues.swordAuraDamageMultiplier(level));
        }
        equal(1, RefiningRules.fortunePreservingCount(1, 1));
        equal(4, RefiningRules.fortunePreservingCount(1, 4));
        equal(8, RefiningRules.fortunePreservingCount(2, 4));
        equal(4, RefiningRules.fortunePreservingCount(0, 4));
        equal(4800, EnchantmentValues.shieldRefreshTicks());
        equal(0.20D, EnchantmentValues.holyLightTriggerThreshold());
        equal(300, EnchantmentValues.holyLightDurationTicks());
        equal(20, EnchantmentValues.holyLightHealIntervalTicks());
        equal(7200, EnchantmentValues.holyLightCooldownTicks());
        equal(4.0D, EnchantmentValues.fallingStarFallThreshold());
        equal(120, EnchantmentValues.flyingHookTimeoutTicks());
        equal(0.65D, EnchantmentValues.doubleJumpVerticalImpulse());
        equal(2.0D, EnchantmentValues.doubleJumpSafeFallBonus());
        System.out.println("Enchantment numeric regression: " + assertions + " assertions passed");
    }

    private static void equal(int expected, int actual) {
        assertions++;
        if (expected != actual) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }

    private static void equal(double expected, double actual) {
        assertions++;
        if (Math.abs(expected - actual) > 1.0E-6D) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
