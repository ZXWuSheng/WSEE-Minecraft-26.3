package cn.wusheng.oatz.registry;

import cn.wusheng.oatz.WseeMod;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.function.Supplier;

public final class ModRegistries {
    private static final String MOD_ID = WseeMod.MOD_ID;
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MOD_ID);
    public static final DeferredRegister<MobEffect> MOB_EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, MOD_ID);
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, MOD_ID);
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SWORD_WAVE_PARTICLE =
            PARTICLE_TYPES.register("sword_wave", () -> new SimpleParticleType(true));
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING_EFFECT = MOB_EFFECTS.register(
            "bleeding", () -> new MobEffect(MobEffectCategory.HARMFUL, 0xB51A2B) {
                @Override
                public boolean shouldApplyEffectTickThisTick(int tickCount, int amplifier) {
                    return tickCount % 20 == 0;
                }
            });
    public static final DeferredHolder<MobEffect, MobEffect> BATTLE_MOMENTUM_EFFECT = MOB_EFFECTS.register(
            "battle_momentum", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0xF59B23) {
            });
    public static final Supplier<CreativeModeTab> OA_ENCHANTMENTS_TAB = CREATIVE_MODE_TABS.register(
            "oa_enchantments", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.wsee.enchantments").withStyle(ChatFormatting.LIGHT_PURPLE))
                    .icon(() -> new ItemStack(Items.ENCHANTED_BOOK))
                    .displayItems((parameters, output) -> {
                        var enchantments = parameters.holders().lookupOrThrow(Registries.ENCHANTMENT);
                        Holder<Enchantment> shieldBash = enchantments.getOrThrow(
                                ResourceKey.create(Registries.ENCHANTMENT,
                                        Identifier.fromNamespaceAndPath(MOD_ID, "shield_bash")));
                        Holder<Enchantment> frostbite = enchantments.getOrThrow(
                                ResourceKey.create(Registries.ENCHANTMENT,
                                        Identifier.fromNamespaceAndPath(MOD_ID, "frostbite")));
                        Holder<Enchantment> deflect = enchantments.getOrThrow(
                                ResourceKey.create(Registries.ENCHANTMENT,
                                        Identifier.fromNamespaceAndPath(MOD_ID, "deflect")));
                        Holder<Enchantment> swordAura = enchantments.getOrThrow(
                                ResourceKey.create(Registries.ENCHANTMENT,
                                        Identifier.fromNamespaceAndPath(MOD_ID, "sword_aura")));
                        Holder<Enchantment> expansion = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "expansion")));
                        Holder<Enchantment> cluster = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "cluster")));
                        Holder<Enchantment> armorPiercing = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "armor_piercing")));
                        Holder<Enchantment> bleedingEnchantment = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "bleeding")));
                        Holder<Enchantment> longEdge = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "long_edge")));
                        Holder<Enchantment> surge = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "surge")));
                        Holder<Enchantment> tether = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "tether")));
                        Holder<Enchantment> rebound = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "rebound")));
                        Holder<Enchantment> hydroshock = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "hydroshock")));
                        Holder<Enchantment> overcharge = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "overcharge")));
                        Holder<Enchantment> comboStrike = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "combo_strike")));
                        Holder<Enchantment> explosiveArrow = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "explosive_arrow")));
                        Holder<Enchantment> arrowRain = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "arrow_rain")));
                        Holder<Enchantment> tracking = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "tracking")));
                        Holder<Enchantment> battleMomentum = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "battle_momentum")));
                        Holder<Enchantment> forcedMarch = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "forced_march")));
                        Holder<Enchantment> clarity = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "clarity")));
                        Holder<Enchantment> execution = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "execution")));
                        Holder<Enchantment> fallingStar = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "falling_star")));
                        Holder<Enchantment> shockRing = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "shock_ring")));
                        Holder<Enchantment> shield = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "shield")));
                        Holder<Enchantment> holyLight = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "holy_light")));
                        Holder<Enchantment> refining = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "refining")));
                        Holder<Enchantment> foundation = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "foundation")));
                        Holder<Enchantment> airForce = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "air_force")));
                        Holder<Enchantment> flyingHook = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "flying_hook")));
                        Holder<Enchantment> doubleJump = enchantments.getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                                Identifier.fromNamespaceAndPath(MOD_ID, "double_jump")));
                        // Match vanilla-style creative ordering: group by equipment type,
                        // then list each enchantment's levels from I upward.
                        for (int level = 1; level <= 5; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(swordAura, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(comboStrike, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(battleMomentum, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(execution, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(fallingStar, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(shockRing, level)));
                        }
                        output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(shieldBash, 1)));
                        output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(deflect, 1)));
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(frostbite, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(bleedingEnchantment, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(longEdge, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(surge, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(tether, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(rebound, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(hydroshock, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(overcharge, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(explosiveArrow, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(arrowRain, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(tracking, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(expansion, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(cluster, level)));
                        }
                        for (int level = 1; level <= 4; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(armorPiercing, level)));
                        }
                        output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(refining, 1)));
                        output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(clarity, 1)));
                        for (int level = 1; level <= 5; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(forcedMarch, level)));
                        }
                        for (int level = 1; level <= 5; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(shield, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(holyLight, level)));
                        }
                        for (int level = 1; level <= 3; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(foundation, level)));
                        }
                        output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(airForce, 1)));
                        output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(flyingHook, 1)));
                        output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(doubleJump, 1)));
                    })
                    .build());

    private ModRegistries() {
    }

    public static void register(IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
        MOB_EFFECTS.register(modBus);
        PARTICLE_TYPES.register(modBus);
    }
}
