package cn.wusheng.oatz.tool;

import cn.wusheng.oatz.WseeMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.event.level.BlockDropsEvent;

import java.util.Optional;

import static cn.wusheng.oatz.enchantment.EnchantmentUtil.enchantmentLevel;
import static cn.wusheng.oatz.registry.ModEnchantments.REFINING_ID;

public final class RefiningHandler {
    private static final String MOD_ID = WseeMod.MOD_ID;
    private static final TagKey<Block> REFINABLE_ORES = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(MOD_ID, "refinable_ores"));
    /**
     * Converts ore drops through the active server smelting recipe after loot
     * (including Fortune) is resolved.  Looking up the recipe instead of
     * hard-coding raw iron/gold/copper also supports ores supplied by other
     * mods, including recipes that use a custom raw material or output stack.
     */
    public static void onBlockDrops(BlockDropsEvent event) {
        if (!(event.getBreaker() instanceof Player player)
                || !event.getState().is(REFINABLE_ORES)
                || enchantmentLevel(player, event.getTool(), REFINING_ID) <= 0) {
            return;
        }
        Holder<Enchantment> silkTouch = event.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.SILK_TOUCH);
        if (EnchantmentHelper.getItemEnchantmentLevel(silkTouch, event.getTool()) > 0) {
            return;
        }
        boolean converted = false;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack input = drop.getItem();
            if (input.isEmpty()) {
                continue;
            }
            Optional<RecipeHolder<SmeltingRecipe>> recipe = event.getLevel().recipeAccess().getRecipeFor(
                    RecipeType.SMELTING,
                    new SingleRecipeInput(input),
                    event.getLevel());
            if (recipe.isEmpty()) {
                continue;
            }
            ItemStack result = recipe.get().value().assemble(new SingleRecipeInput(input));
            if (result.isEmpty()) {
                continue;
            }
            // The recipe describes one input item. Multiply only after the
            // block loot table has applied Fortune so its quantity is kept.
            int outputCount = RefiningRules.fortunePreservingCount(result.getCount(), input.getCount());
            drop.setItem(result.copyWithCount(outputCount));
            converted = true;
        }
        if (converted) {
            ServerLevel level = event.getLevel();
            BlockPos pos = event.getPos();
            level.sendParticles(ParticleTypes.FLAME, pos.getX() + 0.5D, pos.getY() + 0.65D, pos.getZ() + 0.5D,
                    4, 0.2D, 0.2D, 0.2D, 0.015D);
            level.sendParticles(ParticleTypes.SMOKE, pos.getX() + 0.5D, pos.getY() + 0.7D, pos.getZ() + 0.5D,
                    2, 0.15D, 0.15D, 0.15D, 0.01D);
            level.playSound(null, pos, SoundEvents.FURNACE_FIRE_CRACKLE, SoundSource.BLOCKS, 0.35F, 1.2F);
        }
    }

    private RefiningHandler() {
    }
}
