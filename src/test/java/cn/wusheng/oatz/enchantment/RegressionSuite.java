package cn.wusheng.oatz.enchantment;

/** Runs the executable regression checks from Gradle's regressionTest task. */
public final class RegressionSuite {
    private RegressionSuite() {
    }

    public static void main(String[] args) {
        EnchantmentRegistryRegression.main(args);
        EquipmentRulesRegression.main(args);
        EnchantmentValuesRegression.main(args);
        cn.wusheng.oatz.movement.DoubleJumpRegression.main(args);
        cn.wusheng.oatz.movement.MovementMathRegression.main(args);
        cn.wusheng.oatz.movement.FlyingHookCycleRegression.main(args);
        cn.wusheng.oatz.movement.MovementImpulseRegression.main(args);
    }
}
