package cn.wusheng.oatz.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

public final class EnchantmentUtil {
    private EnchantmentUtil() {
    }

    public static int enchantmentLevel(LivingEntity entity, ItemStack stack, Identifier id) {
        if (!EquipmentRules.isValidEnchantmentItem(stack, id)) {
            return 0;
        }
        Holder<Enchantment> enchantment = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id));
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }

}
