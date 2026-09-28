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

import static cn.wusheng.oatz.registry.ModEnchantments.*;

public final class EquipmentRules {
    private EquipmentRules() {
    }

    /** Runtime backstop for the data-driven primary/supported item restrictions. */
    public static boolean isValidEnchantmentItem(ItemStack stack, Identifier id) {
        if (stack.isEmpty()) {
            return false;
        }
        if (id.equals(ENCHANTMENT_ID)) {
            return stack.is(Items.SHIELD);
        }
        if (id.equals(DEFLECT_ID)) {
            return stack.is(Items.SHIELD);
        }
        if (id.equals(EXPANSION_ID) || id.equals(EXPLOSIVE_ARROW_ID) || id.equals(TRACKING_ID)) {
            return isRangedWeapon(stack);
        }
        if (id.equals(CLUSTER_ID) || id.equals(ARMOR_PIERCING_ID)) {
            return stack.getItem() instanceof CrossbowItem;
        }
        if (id.equals(BLEEDING_ID)) {
            return stack.is(ItemTags.SPEARS);
        }
        if (id.equals(FROSTBITE_ID)) {
            return stack.is(ItemTags.SWORDS) || stack.is(ItemTags.SPEARS);
        }
        if (id.equals(LONG_EDGE_ID)) {
            return stack.is(ItemTags.SPEARS);
        }
        if (id.equals(SURGE_ID)) {
            return stack.is(Items.TRIDENT);
        }
        if (id.equals(TETHER_ID) || id.equals(REBOUND_ID) || id.equals(HYDROSHOCK_ID)) {
            return stack.is(Items.TRIDENT);
        }
        if (id.equals(OVERCHARGE_ID)) {
            return stack.getItem() instanceof BowItem;
        }
        if (id.equals(COMBO_STRIKE_ID)) {
            return stack.is(ItemTags.SWORDS);
        }
        if (id.equals(ARROW_RAIN_ID)) {
            return stack.getItem() instanceof BowItem;
        }
        if (id.equals(BATTLE_MOMENTUM_ID)) {
            return isBattleMomentumWeapon(stack);
        }
        if (id.equals(FORCED_MARCH_ID)) {
            return isFootEquipment(stack);
        }
        if (id.equals(CLARITY_ID)) {
            return stack.is(ItemTags.HEAD_ARMOR);
        }
        if (id.equals(EXECUTION_ID)) {
            return stack.is(ItemTags.AXES);
        }
        if (id.equals(FALLING_STAR_ID)) {
            return stack.getItem() instanceof MaceItem;
        }
        if (id.equals(SHOCK_RING_ID)) {
            return stack.getItem() instanceof MaceItem;
        }
        if (id.equals(REFINING_ID)) {
            return stack.is(ItemTags.PICKAXES);
        }
        if (id.equals(FOUNDATION_ID)) {
            return stack.is(ItemTags.LEG_ARMOR);
        }
        if (id.equals(AIR_FORCE_ID)) {
            return stack.is(Items.FISHING_ROD);
        }
        if (id.equals(FLYING_HOOK_ID)) {
            return stack.is(Items.FISHING_ROD);
        }
        if (id.equals(DOUBLE_JUMP_ID)) {
            return isFootEquipment(stack);
        }
        if (id.equals(SHIELD_ID) || id.equals(HOLY_LIGHT_ID)) {
            return isChestEquipment(stack);
        }
        if (id.equals(SWORD_AURA_ID)) {
            return stack.is(ItemTags.SWORDS);
        }
        return false;
    }

    private static boolean isRangedWeapon(ItemStack stack) {
        return stack.getItem() instanceof BowItem || stack.getItem() instanceof CrossbowItem;
    }

    private static boolean isBattleMomentumWeapon(ItemStack stack) {
        return stack.is(ItemTags.MELEE_WEAPON_ENCHANTABLE)
                || stack.is(ItemTags.SPEARS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE);
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
