package cn.wusheng.oatz.tool;

/** Pure part of Refining's post-Fortune drop conversion. */
public final class RefiningRules {
    private RefiningRules() {
    }

    public static int fortunePreservingCount(int recipeResultCount, int lootDropCount) {
        return Math.max(1, recipeResultCount) * lootDropCount;
    }
}
