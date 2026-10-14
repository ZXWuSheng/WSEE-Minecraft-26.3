package cn.wusheng.oatz.enchantment;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;

import java.util.function.Predicate;

import static cn.wusheng.oatz.registry.ModEnchantments.*;

public final class EquipmentRules {
    enum Kind {
        SHIELD, BOW, CROSSBOW, SWORD, AXE, SPEAR, TRIDENT, MACE,
        HEAD_ARMOR, CHEST_ARMOR, LEG_ARMOR, FOOT_ARMOR, FISHING_ROD,
        PICKAXE, MELEE_WEAPON_ENCHANTABLE
    }

    private EquipmentRules() {
    }

    /** Runtime backstop for the data-driven primary/supported item restrictions. */
    public static boolean isValidEnchantmentItem(ItemStack stack, Identifier id) {
        if (stack.isEmpty()) {
            return false;
        }
        return isValidEnchantmentItem(id, kind -> matches(stack, kind));
    }

    /** The same decision tree is exercised by regression tests without loading a game client. */
    static boolean isValidEnchantmentItem(Identifier id, Predicate<Kind> matches) {
        if (id.equals(ENCHANTMENT_ID)) {
            return matches.test(Kind.SHIELD);
        }
        if (id.equals(DEFLECT_ID)) {
            return matches.test(Kind.SHIELD);
        }
        if (id.equals(EXPANSION_ID) || id.equals(EXPLOSIVE_ARROW_ID) || id.equals(TRACKING_ID)) {
            return matches.test(Kind.BOW) || matches.test(Kind.CROSSBOW);
        }
        if (id.equals(CLUSTER_ID) || id.equals(ARMOR_PIERCING_ID)) {
            return matches.test(Kind.CROSSBOW);
        }
        if (id.equals(BLEEDING_ID)) {
            return matches.test(Kind.SPEAR);
        }
        if (id.equals(FROSTBITE_ID)) {
            return matches.test(Kind.SWORD) || matches.test(Kind.SPEAR);
        }
        if (id.equals(LONG_EDGE_ID)) {
            return matches.test(Kind.SPEAR);
        }
        if (id.equals(SURGE_ID)) {
            return matches.test(Kind.TRIDENT);
        }
        if (id.equals(TETHER_ID) || id.equals(REBOUND_ID) || id.equals(HYDROSHOCK_ID)) {
            return matches.test(Kind.TRIDENT);
        }
        if (id.equals(OVERCHARGE_ID)) {
            return matches.test(Kind.BOW);
        }
        if (id.equals(COMBO_STRIKE_ID)) {
            return matches.test(Kind.SWORD);
        }
        if (id.equals(ARROW_RAIN_ID)) {
            return matches.test(Kind.BOW);
        }
        if (id.equals(BATTLE_MOMENTUM_ID)) {
            return matches.test(Kind.MELEE_WEAPON_ENCHANTABLE)
                    || matches.test(Kind.SPEAR)
                    || matches.test(Kind.AXE)
                    || matches.test(Kind.TRIDENT)
                    || matches.test(Kind.MACE);
        }
        if (id.equals(FORCED_MARCH_ID)) {
            return matches.test(Kind.FOOT_ARMOR);
        }
        if (id.equals(CLARITY_ID)) {
            return matches.test(Kind.HEAD_ARMOR);
        }
        if (id.equals(EXECUTION_ID)) {
            return matches.test(Kind.AXE);
        }
        if (id.equals(FALLING_STAR_ID)) {
            return matches.test(Kind.MACE);
        }
        if (id.equals(SHOCK_RING_ID)) {
            return matches.test(Kind.MACE);
        }
        if (id.equals(REFINING_ID)) {
            return matches.test(Kind.PICKAXE);
        }
        if (id.equals(FOUNDATION_ID)) {
            return matches.test(Kind.LEG_ARMOR);
        }
        if (id.equals(AIR_FORCE_ID)) {
            return matches.test(Kind.FISHING_ROD);
        }
        if (id.equals(FLYING_HOOK_ID)) {
            return matches.test(Kind.FISHING_ROD);
        }
        if (id.equals(DOUBLE_JUMP_ID)) {
            return matches.test(Kind.FOOT_ARMOR);
        }
        if (id.equals(SHIELD_ID) || id.equals(HOLY_LIGHT_ID)) {
            return matches.test(Kind.CHEST_ARMOR);
        }
        if (id.equals(SWORD_AURA_ID)) {
            return matches.test(Kind.SWORD);
        }
        return false;
    }

    private static boolean matches(ItemStack stack, Kind kind) {
        return switch (kind) {
            case SHIELD -> stack.is(Items.SHIELD);
            case BOW -> stack.getItem() instanceof BowItem;
            case CROSSBOW -> stack.getItem() instanceof CrossbowItem;
            case SWORD -> stack.is(ItemTags.SWORDS);
            case AXE -> stack.is(ItemTags.AXES);
            case SPEAR -> stack.is(ItemTags.SPEARS);
            case TRIDENT -> stack.is(Items.TRIDENT);
            case MACE -> stack.getItem() instanceof MaceItem;
            case HEAD_ARMOR -> stack.is(ItemTags.HEAD_ARMOR);
            case CHEST_ARMOR -> isChestEquipment(stack);
            case LEG_ARMOR -> stack.is(ItemTags.LEG_ARMOR);
            case FOOT_ARMOR -> isFootEquipment(stack);
            case FISHING_ROD -> stack.is(Items.FISHING_ROD);
            case PICKAXE -> stack.is(ItemTags.PICKAXES);
            case MELEE_WEAPON_ENCHANTABLE -> stack.is(ItemTags.MELEE_WEAPON_ENCHANTABLE);
        };
    }

    private static boolean isChestEquipment(ItemStack stack) {
        if (stack.is(ItemTags.CHEST_ARMOR)) {
            return true;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    public static boolean isFootEquipment(ItemStack stack) {
        if (stack.is(ItemTags.FOOT_ARMOR)) {
            return true;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.FEET;
    }

}
