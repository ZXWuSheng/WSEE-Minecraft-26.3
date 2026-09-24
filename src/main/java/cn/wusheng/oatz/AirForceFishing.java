package cn.wusheng.oatz;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.core.particles.ParticleTypes;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Replaces the real fishing loot before FishingHook creates item entities. */
public final class AirForceFishing {
    private static final Identifier AIR_FORCE_ID = Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "air_force");
    private static final Identifier FLYING_HOOK_ID = Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, "flying_hook");
    private static final TagKey<Item> AIR_FORCE_FISH = itemTag("air_force_fish");
    private static final TagKey<Item> AIR_FORCE_UNOBTAINABLE = itemTag("air_force_unobtainable");
    private static final TagKey<Item> AIR_FORCE_ADMIN_BLACKLIST = itemTag("air_force_admin_blacklist");
    private static final TagKey<Item> AIR_FORCE_BLACKLIST = itemTag("air_force_blacklist");

    private AirForceFishing() {
    }

    public static List<ItemStack> replaceLoot(FishingHook hook, ItemStack rod, List<ItemStack> vanillaLoot) {
        Player owner = hook.getPlayerOwner();
        if (owner == null || hook.level().isClientSide() || !rod.is(Items.FISHING_ROD)) {
            return vanillaLoot;
        }

        // Illegal combinations can still be created with commands. Flying Hook always wins and never fishes loot.
        if (hook.getPersistentData().getIntOr("wsee_flying_hook_level", 0) > 0
                || enchantmentLevel(owner, rod, FLYING_HOOK_ID) > 0) {
            return List.of();
        }
        if (enchantmentLevel(owner, rod, AIR_FORCE_ID) <= 0) {
            return vanillaLoot;
        }

        Map<Rarity, List<ItemStack>> pools = new EnumMap<>(Rarity.class);
        for (Rarity rarity : Rarity.values()) {
            pools.put(rarity, new ArrayList<>());
        }
        for (Item item : BuiltInRegistries.ITEM) {
            ItemStack candidate = new ItemStack(item, 1);
            if (!isCandidate(item, candidate)) {
                continue;
            }
            pools.get(candidate.getRarity()).add(candidate);
        }

        int luckLevel = enchantmentLevel(owner, rod, Enchantments.LUCK_OF_THE_SEA);
        List<ItemStack> candidates = poolForRoll(pools, luckLevel, owner.getRandom().nextDouble());
        if (candidates.isEmpty()) {
            candidates = pools.values().stream().flatMap(List::stream).toList();
        }
        if (candidates.isEmpty()) {
            return List.of();
        }

        ItemStack selected = candidates.get(owner.getRandom().nextInt(candidates.size())).copyWithCount(1);
        if (hook.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.ENCHANT,
                    owner.getX(), owner.getY() + 1.0D, owner.getZ(), 4, 0.3D, 0.4D, 0.3D, 0.02D);
            level.playSound(null, owner.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.35F,
                    selected.getRarity() == Rarity.EPIC ? 1.4F
                            : selected.getRarity() == Rarity.RARE ? 1.15F : 1.0F);
        }
        return List.of(selected);
    }

    private static boolean isCandidate(Item item, ItemStack candidate) {
        return item != Items.AIR
                && !candidate.is(ItemTags.FISHES)
                && !candidate.is(AIR_FORCE_FISH)
                && !candidate.is(AIR_FORCE_UNOBTAINABLE)
                && !candidate.is(AIR_FORCE_ADMIN_BLACKLIST)
                && !candidate.is(AIR_FORCE_BLACKLIST)
                && !isBuiltInTechnicalItem(item)
                && !candidate.isEmpty();
    }

    private static List<ItemStack> poolForRoll(Map<Rarity, List<ItemStack>> pools, int luckLevel, double roll) {
        int level = Mth.clamp(luckLevel, 0, 3);
        double epic = level == 0 ? 0.01D : level == 1 ? 0.015D : level == 2 ? 0.025D : 0.04D;
        double rare = level == 0 ? 0.05D : level == 1 ? 0.065D : level == 2 ? 0.085D : 0.11D;
        double uncommon = level == 0 ? 0.12D : level == 1 ? 0.14D : level == 2 ? 0.16D : 0.18D;
        Rarity selected = roll < epic ? Rarity.EPIC : roll < epic + rare ? Rarity.RARE
                : roll < epic + rare + uncommon ? Rarity.UNCOMMON : Rarity.COMMON;
        for (Rarity fallback : new Rarity[]{selected, Rarity.RARE, Rarity.UNCOMMON, Rarity.COMMON}) {
            List<ItemStack> pool = pools.get(fallback);
            if (pool != null && !pool.isEmpty()) {
                return pool;
            }
        }
        return List.of();
    }

    private static boolean isBuiltInTechnicalItem(Item item) {
        return item == Items.BEDROCK || item == Items.BARRIER || item == Items.COMMAND_BLOCK
                || item == Items.CHAIN_COMMAND_BLOCK || item == Items.REPEATING_COMMAND_BLOCK
                || item == Items.COMMAND_BLOCK_MINECART || item == Items.STRUCTURE_BLOCK
                || item == Items.STRUCTURE_VOID || item == Items.JIGSAW || item == Items.DEBUG_STICK
                || item == Items.LIGHT || item == Items.END_PORTAL_FRAME || item == Items.REINFORCED_DEEPSLATE;
    }

    private static int enchantmentLevel(Player owner, ItemStack rod, Identifier id) {
        Holder<Enchantment> enchantment = owner.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id));
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, rod);
    }

    private static int enchantmentLevel(Player owner, ItemStack rod, ResourceKey<Enchantment> key) {
        Holder<Enchantment> enchantment = owner.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(key);
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, rod);
    }

    private static TagKey<Item> itemTag(String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(WseeMod.MOD_ID, path));
    }
}
