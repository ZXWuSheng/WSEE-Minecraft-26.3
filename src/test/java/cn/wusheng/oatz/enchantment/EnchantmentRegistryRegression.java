package cn.wusheng.oatz.enchantment;

import cn.wusheng.oatz.registry.ModDamageTypes;
import cn.wusheng.oatz.registry.ModEnchantments;
import net.minecraft.resources.Identifier;

import java.util.HashSet;
import java.util.Set;

/** Executable checks for the Java registry list and damage type keys. */
public final class EnchantmentRegistryRegression {
    private EnchantmentRegistryRegression() {
    }

    public static void main(String[] args) {
        String[] names = {
                "sword_aura", "shield_bash", "deflect", "frostbite", "expansion", "cluster",
                "armor_piercing", "bleeding", "long_edge", "surge", "tether", "rebound",
                "hydroshock", "overcharge", "combo_strike", "explosive_arrow", "arrow_rain",
                "tracking", "battle_momentum", "forced_march", "clarity", "execution",
                "falling_star", "shock_ring", "shield", "holy_light", "refining", "foundation",
                "air_force", "flying_hook", "double_jump"
        };
        Set<Identifier> expected = new HashSet<>();
        for (String name : names) {
            expected.add(Identifier.fromNamespaceAndPath("wsee", name));
        }
        var actual = ModEnchantments.OATZ_ENCHANTMENT_IDS;
        equal(31, names.length);
        equal(31, expected.size());
        equal(31, actual.size());
        equal(31, new HashSet<>(actual).size());
        equal(expected, new HashSet<>(actual));
        equal("wsee:bleeding", ModDamageTypes.BLEEDING_DAMAGE_TYPE.identifier().toString());
        equal("wsee:explosive_arrow", ModDamageTypes.EXPLOSIVE_ARROW_DAMAGE_TYPE.identifier().toString());
        equal("wsee:shock_ring", ModDamageTypes.SHOCK_RING_DAMAGE_TYPE.identifier().toString());
        System.out.println("Enchantment registry regression: 8 assertions passed");
    }

    private static void equal(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected " + expected + ", got " + actual);
        }
    }
}
