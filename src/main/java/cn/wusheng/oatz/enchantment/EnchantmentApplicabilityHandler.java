package cn.wusheng.oatz.enchantment;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;

import static cn.wusheng.oatz.enchantment.EquipmentRules.isValidEnchantmentItem;
import static cn.wusheng.oatz.registry.ModEnchantments.OATZ_ENCHANTMENT_IDS;

/** Server-side backstops for custom enchantment equipment restrictions. */
public final class EnchantmentApplicabilityHandler {
    private EnchantmentApplicabilityHandler() {
    }

    /** Creative players can normally bypass vanilla anvil applicability checks. */
    public static void onAnvilUpdate(AnvilUpdateEvent event) {
        ItemStack output = event.getOutput();
        if (output.isEmpty() || output.is(Items.ENCHANTED_BOOK)) {
            return;
        }
        var enchantments = event.getPlayer().level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (Identifier id : OATZ_ENCHANTMENT_IDS) {
            Holder<Enchantment> holder = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id));
            if (EnchantmentHelper.getEnchantmentsForCrafting(output).getLevel(holder) > 0
                    && !isValidEnchantmentItem(output, id)) {
                event.setOutput(ItemStack.EMPTY);
                event.setXpCost(0);
                event.setMaterialCost(0);
                event.setCanceled(true);
                return;
            }
        }
    }

    /** Final server-side validation after enchanting-table results are applied. */
    public static void onPlayerEnchantItem(PlayerEnchantItemEvent event) {
        ItemStack item = event.getEnchantedItem();
        if (item.isEmpty() || item.is(Items.ENCHANTED_BOOK)) {
            return;
        }
        var enchantments = event.getEntity().level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        EnchantmentHelper.updateEnchantments(item, mutable -> {
            for (Identifier id : OATZ_ENCHANTMENT_IDS) {
                Holder<Enchantment> holder = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id));
                if (mutable.getLevel(holder) > 0 && !isValidEnchantmentItem(item, id)) {
                    mutable.set(holder, 0);
                }
            }
        });
    }
}
