package cn.wusheng.oatz.enchantment;

import net.minecraft.resources.Identifier;

import java.util.EnumSet;
import java.util.Set;

import static cn.wusheng.oatz.enchantment.EquipmentRules.Kind.*;
import static cn.wusheng.oatz.registry.ModEnchantments.*;

/** Exercises the production decision tree with representative equipment categories. */
public final class EquipmentRulesRegression {
    private static int assertions;

    private EquipmentRulesRegression() {
    }

    public static void main(String[] args) {
        accepts(SWORD_AURA_ID, SWORD);
        accepts(COMBO_STRIKE_ID, SWORD);
        accepts(EXECUTION_ID, AXE);
        accepts(BLEEDING_ID, SPEAR);
        accepts(LONG_EDGE_ID, SPEAR);
        accepts(FROSTBITE_ID, SWORD, SPEAR);
        accepts(SURGE_ID, TRIDENT);
        accepts(TETHER_ID, TRIDENT);
        accepts(REBOUND_ID, TRIDENT);
        accepts(HYDROSHOCK_ID, TRIDENT);
        accepts(OVERCHARGE_ID, BOW);
        accepts(EXPLOSIVE_ARROW_ID, BOW, CROSSBOW);
        accepts(TRACKING_ID, BOW, CROSSBOW);
        accepts(ARROW_RAIN_ID, BOW);
        accepts(EXPANSION_ID, BOW, CROSSBOW);
        accepts(CLUSTER_ID, CROSSBOW);
        accepts(ARMOR_PIERCING_ID, CROSSBOW);
        accepts(ENCHANTMENT_ID, SHIELD);
        accepts(DEFLECT_ID, SHIELD);
        accepts(FALLING_STAR_ID, MACE);
        accepts(SHOCK_RING_ID, MACE);
        accepts(FORCED_MARCH_ID, FOOT_ARMOR);
        accepts(DOUBLE_JUMP_ID, FOOT_ARMOR);
        accepts(FOUNDATION_ID, LEG_ARMOR);
        accepts(CLARITY_ID, HEAD_ARMOR);
        accepts(SHIELD_ID, CHEST_ARMOR);
        accepts(HOLY_LIGHT_ID, CHEST_ARMOR);
        accepts(AIR_FORCE_ID, FISHING_ROD);
        accepts(FLYING_HOOK_ID, FISHING_ROD);
        accepts(REFINING_ID, PICKAXE);
        accepts(BATTLE_MOMENTUM_ID, MELEE_WEAPON_ENCHANTABLE, AXE,
                SPEAR, TRIDENT, MACE);
        acceptsTraits(BATTLE_MOMENTUM_ID, EnumSet.of(SWORD, MELEE_WEAPON_ENCHANTABLE));
        for (Identifier id : OATZ_ENCHANTMENT_IDS) {
            rejects(id);
        }
        rejects(SWORD_AURA_ID, AXE);
        rejects(COMBO_STRIKE_ID, AXE);
        rejects(EXECUTION_ID, SWORD);
        rejects(BLEEDING_ID, SWORD);
        rejects(FROSTBITE_ID, BOW);
        rejects(OVERCHARGE_ID, CROSSBOW);
        rejects(ARROW_RAIN_ID, CROSSBOW);
        rejects(CLUSTER_ID, BOW);
        rejects(ARMOR_PIERCING_ID, BOW);
        rejects(FORCED_MARCH_ID, LEG_ARMOR);
        rejects(FOUNDATION_ID, FOOT_ARMOR);
        rejects(CLARITY_ID, CHEST_ARMOR);
        rejects(SHIELD_ID, HEAD_ARMOR);
        rejects(REFINING_ID, AXE);
        rejects(BATTLE_MOMENTUM_ID, BOW);
        System.out.println("Equipment rules regression: " + assertions + " assertions passed");
    }

    private static void accepts(Identifier id, EquipmentRules.Kind... kinds) {
        for (EquipmentRules.Kind kind : kinds) {
            assertions++;
            if (!EquipmentRules.isValidEnchantmentItem(id, Set.of(kind)::contains)) {
                throw new AssertionError(id + " should accept " + kind);
            }
        }
    }

    private static void acceptsTraits(Identifier id, Set<EquipmentRules.Kind> kinds) {
        assertions++;
        if (!EquipmentRules.isValidEnchantmentItem(id, kinds::contains)) {
            throw new AssertionError(id + " should accept " + kinds);
        }
    }

    private static void rejects(Identifier id, EquipmentRules.Kind... kinds) {
        assertions++;
        Set<EquipmentRules.Kind> item = kinds.length == 0
                ? EnumSet.noneOf(EquipmentRules.Kind.class)
                : EnumSet.copyOf(Set.of(kinds));
        if (EquipmentRules.isValidEnchantmentItem(id, item::contains)) {
            throw new AssertionError(id + " should reject " + item);
        }
    }
}
