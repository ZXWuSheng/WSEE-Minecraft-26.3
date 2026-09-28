package cn.wusheng.oatz.enchantment;

/** Numeric gameplay rules, kept independent of loader and world state. */
public final class EnchantmentValues {
    private EnchantmentValues() {
    }

    public static int overchargeExtraTicks(int level) {
        return switch (level) {
            case 1 -> 10;
            case 2 -> 15;
            default -> 20;
        };
    }

    public static double overchargeDamageBonus(int level) {
        return switch (level) {
            case 1 -> 0.07D;
            case 2 -> 0.17D;
            default -> 0.27D;
        };
    }

    public static double overchargeVelocityBonus(int level) {
        return switch (level) {
            case 1 -> 0.05D;
            case 2 -> 0.08D;
            default -> 0.12D;
        };
    }

    public static double overchargeProgress(int level, int heldTicks) {
        double progress = (heldTicks - 20.0D) / overchargeExtraTicks(level);
        return Math.max(0.0D, Math.min(1.0D, progress));
    }

    public static double flyingHookPullSpeed(int level) {
        return switch (level) {
            case 1 -> 0.55D;
            case 2 -> 0.65D;
            case 3 -> 0.75D;
            case 4 -> 0.85D;
            default -> 0.95D;
        };
    }

    public static int flyingHookMaxDistance(int level) {
        return switch (level) {
            case 1 -> 24;
            case 2 -> 30;
            case 3 -> 36;
            case 4 -> 42;
            default -> 48;
        };
    }

    public static int arrowRainCount(int level) {
        return switch (level) {
            case 1 -> 6;
            case 2 -> 9;
            default -> 12;
        };
    }

    public static int arrowRainDuration(int level) {
        return switch (level) {
            case 1 -> 30;
            case 2 -> 36;
            default -> 40;
        };
    }

    public static double arrowRainRadius(int level) {
        return switch (level) {
            case 1 -> 2.25D;
            case 2 -> 2.75D;
            default -> 3.25D;
        };
    }

    public static double shockRingDamageMultiplier(int level) {
        return switch (level) {
            case 1 -> 0.12D;
            case 2 -> 0.16D;
            default -> 0.20D;
        };
    }

    public static double comboDamageMultiplier(int level, int stage) {
        if (stage <= 0) {
            return 1.0D;
        }
        if (stage == 1) {
            return switch (level) {
                case 1 -> 1.05D;
                case 2 -> 1.08D;
                default -> 1.10D;
            };
        }
        return switch (level) {
            case 1 -> 1.12D;
            case 2 -> 1.18D;
            default -> 1.25D;
        };
    }

    public static int battleMomentumMaxStacks(int level) {
        return switch (level) {
            case 1 -> 3;
            case 2 -> 4;
            default -> 5;
        };
    }

    public static double battleMomentumPerStack(int level) {
        return switch (level) {
            case 1 -> 0.04D;
            case 2 -> 0.05D;
            default -> 0.06D;
        };
    }

    public static double foundationKnockbackBonus(int level) {
        return switch (level) {
            case 1 -> 0.15D;
            case 2 -> 0.25D;
            default -> 0.35D;
        };
    }

    public static int forcedMarchRequiredTicks(int level) {
        return switch (level) {
            case 1 -> 80;
            case 2 -> 70;
            case 3 -> 60;
            case 4 -> 50;
            default -> 40;
        };
    }

    public static double forcedMarchSpeedBonus(int level) {
        return switch (level) {
            case 1 -> 0.06D;
            case 2 -> 0.09D;
            case 3 -> 0.12D;
            case 4 -> 0.15D;
            default -> 0.18D;
        };
    }

    public static float surgeDamageMultiplier(int level) {
        return switch (level) {
            case 1 -> 1.15F;
            case 2 -> 1.25F;
            default -> 1.35F;
        };
    }

    public static double swordAuraDamageMultiplier(int level) {
        return switch (level) {
            case 1 -> 0.40D;
            case 2 -> 0.55D;
            case 3 -> 0.70D;
            case 4 -> 0.85D;
            default -> 1.00D;
        };
    }

    public static double swordAuraRange(int level) {
        return 5.0D + level;
    }

    public static double shieldAbsorptionPercent(int level) {
        return switch (level) {
            case 1 -> 0.08D;
            case 2 -> 0.12D;
            case 3 -> 0.16D;
            case 4 -> 0.20D;
            default -> 0.25D;
        };
    }

    public static int shieldRefreshTicks() {
        return 4800;
    }

    public static float holyLightTriggerThreshold() {
        return 0.20F;
    }

    public static int holyLightDurationTicks() {
        return 300;
    }

    public static int holyLightHealIntervalTicks() {
        return 20;
    }

    public static double holyLightHealPercent(int level) {
        return switch (level) {
            case 1 -> 0.02D;
            case 2 -> 0.025D;
            default -> 0.03D;
        };
    }

    public static int holyLightCooldownTicks() {
        return 7200;
    }

    public static double fallingStarRadius(int level) {
        return switch (level) {
            case 1 -> 3.0D;
            case 2 -> 4.0D;
            default -> 5.0D;
        };
    }

    public static float fallingStarFallThreshold() {
        return 4.0F;
    }

    public static double shockRingRadius(int level) {
        return switch (level) {
            case 1 -> 3.0D;
            case 2 -> 4.0D;
            default -> 5.0D;
        };
    }

    public static int surgeRequiredWaterTicks(int level) {
        return switch (level) {
            case 1 -> 6;
            case 2 -> 5;
            default -> 4;
        };
    }

    public static double flyingHookProjectileSpeedMultiplier(int level) {
        return 1.0D + level * 0.15D;
    }

    public static int flyingHookTimeoutTicks() {
        return 60;
    }

    public static double doubleJumpVerticalVelocity() {
        return 0.65D;
    }

    public static float doubleJumpSafeFallBonus() {
        return 2.0F;
    }

}
