package cn.wusheng.oatz;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.Arrow;
import net.minecraft.world.entity.projectile.arrow.SpectralArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.AnvilUpdateEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEnchantItemEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import net.neoforged.neoforge.event.level.BlockDropsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.EnumMap;
import java.util.function.Supplier;
import java.lang.reflect.Field;

@Mod(WseeMod.MOD_ID)
public final class WseeMod {
    public static final String MOD_ID = "wsee";
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
                        for (int level = 1; level <= 5; level++) {
                            output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(flyingHook, level)));
                        }
                        output.accept(EnchantmentHelper.createBook(new EnchantmentInstance(doubleJump, 1)));
                    })
                    .build());
    private static final Identifier ENCHANTMENT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shield_bash");
    private static final Identifier DEFLECT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "deflect");
    private static final Identifier FROSTBITE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "frostbite");
    private static final Identifier SWORD_AURA_ID = Identifier.fromNamespaceAndPath(MOD_ID, "sword_aura");
    private static final Identifier EXPANSION_ID = Identifier.fromNamespaceAndPath(MOD_ID, "expansion");
    private static final Identifier CLUSTER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "cluster");
    private static final Identifier ARMOR_PIERCING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "armor_piercing");
    private static final Identifier BLEEDING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "bleeding");
    private static final Identifier LONG_EDGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "long_edge");
    private static final Identifier SURGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "surge");
    private static final Identifier TETHER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "tether");
    private static final Identifier REBOUND_ID = Identifier.fromNamespaceAndPath(MOD_ID, "rebound");
    private static final Identifier HYDROSHOCK_ID = Identifier.fromNamespaceAndPath(MOD_ID, "hydroshock");
    private static final Identifier OVERCHARGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "overcharge");
    private static final Identifier COMBO_STRIKE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "combo_strike");
    private static final Identifier EXPLOSIVE_ARROW_ID = Identifier.fromNamespaceAndPath(MOD_ID, "explosive_arrow");
    private static final Identifier ARROW_RAIN_ID = Identifier.fromNamespaceAndPath(MOD_ID, "arrow_rain");
    private static final Identifier TRACKING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "tracking");
    private static final Identifier BATTLE_MOMENTUM_ID = Identifier.fromNamespaceAndPath(MOD_ID, "battle_momentum");
    private static final Identifier FORCED_MARCH_ID = Identifier.fromNamespaceAndPath(MOD_ID, "forced_march");
    private static final Identifier CLARITY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "clarity");
    private static final Identifier EXECUTION_ID = Identifier.fromNamespaceAndPath(MOD_ID, "execution");
    private static final Identifier FALLING_STAR_ID = Identifier.fromNamespaceAndPath(MOD_ID, "falling_star");
    private static final Identifier SHOCK_RING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shock_ring");
    private static final Identifier SHIELD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shield");
    private static final Identifier HOLY_LIGHT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "holy_light");
    private static final Identifier REFINING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "refining");
    private static final Identifier FOUNDATION_ID = Identifier.fromNamespaceAndPath(MOD_ID, "foundation");
    private static final Identifier AIR_FORCE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "air_force");
    private static final Identifier FLYING_HOOK_ID = Identifier.fromNamespaceAndPath(MOD_ID, "flying_hook");
    private static final Identifier DOUBLE_JUMP_ID = Identifier.fromNamespaceAndPath(MOD_ID, "double_jump");
    private static final TagKey<Block> REFINABLE_ORES = TagKey.create(Registries.BLOCK,
            Identifier.fromNamespaceAndPath(MOD_ID, "refinable_ores"));
    private static final List<Identifier> OATZ_ENCHANTMENT_IDS = List.of(
            SWORD_AURA_ID, ENCHANTMENT_ID, DEFLECT_ID, FROSTBITE_ID, EXPANSION_ID, CLUSTER_ID,
            ARMOR_PIERCING_ID, BLEEDING_ID, LONG_EDGE_ID, SURGE_ID, TETHER_ID, REBOUND_ID, HYDROSHOCK_ID,
            OVERCHARGE_ID, COMBO_STRIKE_ID, EXPLOSIVE_ARROW_ID, ARROW_RAIN_ID, TRACKING_ID, BATTLE_MOMENTUM_ID, FORCED_MARCH_ID, CLARITY_ID, EXECUTION_ID,
            FALLING_STAR_ID, SHOCK_RING_ID, SHIELD_ID, HOLY_LIGHT_ID, REFINING_ID, FOUNDATION_ID, AIR_FORCE_ID, FLYING_HOOK_ID, DOUBLE_JUMP_ID);
    private static final TagKey<net.minecraft.world.item.Item> AIR_FORCE_FISH = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "air_force_fish"));
    private static final TagKey<net.minecraft.world.item.Item> AIR_FORCE_UNOBTAINABLE = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "air_force_unobtainable"));
    private static final TagKey<net.minecraft.world.item.Item> AIR_FORCE_ADMIN_BLACKLIST = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "air_force_admin_blacklist"));
    private static final TagKey<net.minecraft.world.item.Item> AIR_FORCE_BLACKLIST = TagKey.create(Registries.ITEM,
            Identifier.fromNamespaceAndPath(MOD_ID, "air_force_blacklist"));
    private static final ResourceKey<DamageType> BLEEDING_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "bleeding"));
    private static final ResourceKey<DamageType> EXPLOSIVE_ARROW_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "explosive_arrow"));
    private static final ResourceKey<DamageType> SHOCK_RING_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "shock_ring"));
    private static final String BLEEDING_ATTACKER_KEY = "oatz_bleeding_attacker";
    private static final String SHIELD_NEXT_REFRESH_KEY = "oatz_shield_next_refresh";
    private static final String HOLY_LIGHT_NEXT_TRIGGER_KEY = "oatz_holy_light_next_trigger";
    private static final String SHIELD_AMOUNT_KEY = "oatz_shield_amount";
    private static final String SHIELD_REMAINING_KEY = "oatz_shield_remaining";
    private static final String SHIELD_EFFECT_KEY = "oatz_shield_effect";
    private static final String SHIELD_VERSION_KEY = "oatz_shield_version";
    private static final String EXPLOSIVE_ARROW_BOSS_TAG = "wsee:explosive_arrow_boss";
    private static final String DEFLECTED_KEY = "wsee_deflected";
    private static final String ARROW_RAIN_PROJECTILE_KEY = "wsee_arrow_rain_projectile";
    private static final String FLYING_HOOK_LEVEL_KEY = "wsee_flying_hook_level";
    private static final Field ARROW_BASE_DAMAGE_FIELD = findArrowBaseDamageField();
    private static final Identifier SLOW_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shield_bash_slow");
    private static final Identifier SHIELD_ABSORPTION_LIMIT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shield_absorption_limit");
    private static final Identifier BATTLE_MOMENTUM_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "battle_momentum_attack_speed");
    private static final Identifier FORCED_MARCH_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "forced_march_movement_speed");
    private static final Identifier FOUNDATION_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "foundation_knockback_resistance");
    private static final int SURGE_HOLD_TICKS = 20;
    private static final int BASH_COOLDOWN_TICKS = 50;
    private static final int SLOW_TICKS = 10;
    private static final int MARCH_GRACE_TICKS = 15;
    private static final double RANGE = 2.0D;
    private static final double HALF_WIDTH = 0.75D;
    private static final double HALF_ANGLE_COS = Math.cos(Math.atan2(HALF_WIDTH, RANGE));
    private static final double SWORD_AURA_HALF_WIDTH = 1.25D;
    private final Map<UUID, Integer> cooldowns = new HashMap<>();
    private final Map<UUID, Long> slowedTargets = new HashMap<>();
    private final Map<UUID, Long> swordAuraTicks = new HashMap<>();
    private final Map<UUID, SwordWaveVisual> swordWaves = new HashMap<>();
    private final Map<UUID, ArrayDeque<QueuedProjectile>> delayedCrossbowShots = new HashMap<>();
    private final Map<UUID, Long> expansionShotTicks = new HashMap<>();
    private final Set<UUID> generatedClusterArrows = new HashSet<>();
    private final Set<UUID> generatedExpansionArrows = new HashSet<>();
    private final Map<UUID, UUID> bleedingAttackers = new HashMap<>();
    private final Map<UUID, Long> bleedingNextDamage = new HashMap<>();
    private final Set<UUID> bleedingDamage = new HashSet<>();
    private final Map<UUID, OverchargeCharge> overchargeCharges = new HashMap<>();
    private final Map<UUID, Double> bowDrawRatios = new HashMap<>();
    private final Map<UUID, HolyField> holyFields = new HashMap<>();
    private final Set<UUID> overchargeStarted = new HashSet<>();
    private final Set<UUID> overchargeFullPlayed = new HashSet<>();
    private final Map<UUID, Integer> overchargeStages = new HashMap<>();
    private final Set<UUID> overchargeOverlayShown = new HashSet<>();
    private final Map<UUID, SurgeState> surgeStates = new HashMap<>();
    private final Map<UUID, BattleMomentumState> battleMomentumStates = new HashMap<>();
    private final Map<UUID, PendingMeleeAttack> pendingMeleeAttacks = new HashMap<>();
    private final Map<UUID, PendingFrostbiteAttack> pendingFrostbiteAttacks = new HashMap<>();
    private final Map<UUID, PendingComboAttack> pendingComboAttacks = new HashMap<>();
    private final Map<UUID, ComboState> comboStates = new HashMap<>();
    private final Map<UUID, Integer> comboOverlayStates = new HashMap<>();
    private final Map<UUID, ForcedMarchState> forcedMarchStates = new HashMap<>();
    private final Set<UUID> clarityActive = new HashSet<>();
    private final Map<UUID, ExplosiveArrowState> explosiveArrows = new HashMap<>();
    private final Map<UUID, ArrowRainState> arrowRainStates = new HashMap<>();
    private final Map<UUID, TrackingState> trackingStates = new HashMap<>();
    private final Map<ResourceKey<Level>, Long> explosiveStateTicks = new HashMap<>();
    private final Map<UUID, FlyingHookState> flyingHooks = new HashMap<>();
    private final Map<UUID, ClientFlyingHookAnchor> clientFlyingHookAnchors = new HashMap<>();
    private final Map<UUID, Boolean> doubleJumpAvailable = new HashMap<>();
    private final Map<UUID, Boolean> doubleJumpKeyDown = new HashMap<>();
    private final Map<UUID, FallingStarHit> fallingStarHits = new HashMap<>();
    private final Map<UUID, ShockRingHit> shockRingHits = new HashMap<>();
    private final List<ShockRingInstance> shockRings = new ArrayList<>();
    private static final class FlyingHookState {
        private final UUID hook;
        private final int level;
        private final ResourceKey<Level> dimension;
        private final InteractionHand hand;
        private Vec3 anchor;
        private Entity anchorEntity;
        private Vec3 entityAnchorOffset;
        private BlockPos anchorBlock;
        private int durabilityCost;
        private int ticks;
        private int stuckTicks;
        private double lastDistance = Double.NaN;
        private boolean pulling;

        private FlyingHookState(UUID hook, int level, ResourceKey<Level> dimension, InteractionHand hand) {
            this.hook = hook;
            this.level = level;
            this.dimension = dimension;
            this.hand = hand;
        }
    }

    private record QueuedProjectile(Projectile projectile, long dueTick) {}
    private record OverchargeCharge(double damageMultiplier, double velocityMultiplier) {}
    private record PendingMeleeAttack(UUID target, long tick) {}
    private record PendingFrostbiteAttack(UUID target, long tick, ItemStack weapon) {}
    private record PendingComboAttack(UUID target, long tick, ItemStack weapon, int level, int stage) {}
    private record FallingStarHit(UUID target, long tick, int level, float fallDistance) {}
    private record ShockRingHit(UUID target, long tick, int level, float baseDamage) {}
    private static final class ShockRingInstance {
        private final UUID owner;
        private final Vec3 center;
        private final int level;
        private final double maxRadius;
        private final double expansionSpeed;
        private final double baseDamage;
        private final double knockbackStrength;
        private final ResourceKey<Level> dimension;
        private final Set<UUID> hitEntityUUIDs = new HashSet<>();
        private double currentRadius;
        private double previousRadius;
        private int age;

        private ShockRingInstance(UUID owner, Vec3 center, int level, double baseDamage,
                ResourceKey<Level> dimension, UUID directTarget) {
            this.owner = owner;
            this.center = center;
            this.level = level;
            this.maxRadius = switch (level) {
                case 1 -> 3.0D;
                case 2 -> 4.0D;
                default -> 5.0D;
            };
            this.expansionSpeed = switch (level) {
                case 1 -> 0.60D;
                case 2 -> 0.75D;
                default -> 0.90D;
            };
            this.baseDamage = baseDamage;
            this.knockbackStrength = switch (level) {
                case 1 -> 0.30D;
                case 2 -> 0.45D;
                default -> 0.60D;
            };
            this.dimension = dimension;
            this.hitEntityUUIDs.add(owner);
            this.hitEntityUUIDs.add(directTarget);
        }
    }

    private record ClientFlyingHookAnchor(Vec3 anchor, Entity entity, Vec3 entityOffset, BlockPos block) {}
    private static final class ComboState {
        private UUID target;
        private ItemStack weapon;
        private int stage;
        private ResourceKey<Level> dimension;

        private ComboState(UUID target, ItemStack weapon, int stage, ResourceKey<Level> dimension) {
            this.target = target;
            this.weapon = weapon;
            this.stage = stage;
            this.dimension = dimension;
        }
    }
    private static final class BattleMomentumState {
        private int stacks;
        private int level;
        private long expiresAt;
        private long lastHitTick = Long.MIN_VALUE;
        private ResourceKey<Level> dimension;
        private boolean commandGranted;
        private int appliedStacks = -1;
        private int appliedLevel = -1;
        private boolean wasAtMax;

        private BattleMomentumState(int stacks, int level, long expiresAt, ResourceKey<Level> dimension,
                boolean commandGranted) {
            this.stacks = stacks;
            this.level = level;
            this.expiresAt = expiresAt;
            this.dimension = dimension;
            this.commandGranted = commandGranted;
        }
    }
    private static final class ForcedMarchState {
        private int sprintTicks;
        private int graceTicks;
        private int level;
        private boolean active;
        private boolean activationPlayed;
        private int appliedLevel = -1;
        private ResourceKey<Level> dimension;

        private ForcedMarchState(int level, ResourceKey<Level> dimension) {
            this.level = level;
            this.dimension = dimension;
        }
    }
    private static final class ExplosiveArrowState {
        private final UUID owner;
        private final int level;
        private final double attackDamage;
        private long detonationTick;
        private Vec3 lastPosition;
        private UUID attachedEntity;
        private boolean armed;
        private boolean detonated;

        private ExplosiveArrowState(UUID owner, int level, double attackDamage) {
            this.owner = owner;
            this.level = level;
            this.attackDamage = attackDamage;
        }
    }
    private static final class ArrowRainState {
        private final UUID owner;
        private final int level;
        private final double damageBase;
        private final ItemStack weapon;
        private final ResourceKey<Level> dimension;
        private Vec3 center;
        private UUID trackingTarget;
        private long warningUntil;
        private long expiresAt;
        private long nextSpawnAt;
        private int spawned;
        private boolean armed;
        private boolean started;

        private ArrowRainState(UUID owner, int level, double damageBase, ItemStack weapon, ResourceKey<Level> dimension) {
            this.owner = owner;
            this.level = level;
            this.damageBase = damageBase;
            this.weapon = weapon.copy();
            this.dimension = dimension;
        }
    }
    private static final class TrackingState {
        private final UUID owner;
        private final int level;
        private final Vec3 initialDirection;
        private final ResourceKey<Level> dimension;
        private UUID target;
        private final int targetEntityId;
        private int ticks;
        private boolean active = true;

        private TrackingState(UUID owner, int level, Vec3 initialDirection, ResourceKey<Level> dimension,
                UUID target, int targetEntityId) {
            this.owner = owner;
            this.level = level;
            this.initialDirection = initialDirection;
            this.dimension = dimension;
            this.target = target;
            this.targetEntityId = targetEntityId;
        }
    }
    private static final class SurgeState {
        private int surgeLevel;
        private int tetherLevel;
        private int reboundLevel;
        private int hydroshockLevel;
        private int waterFlightTicks;
        private boolean active;
        private boolean outbound = true;
        private boolean tetherTriggered;
        private boolean shockwaveTriggered;
        private long lastSurgeAdvanceTick = Long.MIN_VALUE;
        private final Set<UUID> returnHitEntities = new HashSet<>();
        private long activeUntil;

        private SurgeState(int surgeLevel, int tetherLevel, int reboundLevel, int hydroshockLevel) {
            this.surgeLevel = surgeLevel;
            this.tetherLevel = tetherLevel;
            this.reboundLevel = reboundLevel;
            this.hydroshockLevel = hydroshockLevel;
        }
    }
    private static final class SwordWaveVisual {
        private final UUID owner;
        private final int level;
        private final Vec3 start;
        private final Vec3 direction;
        private final double distance;
        private final int steps;
        private final double attackDamage;
        private final Set<UUID> hitTargets = new HashSet<>();
        private int step;

        private SwordWaveVisual(UUID owner, int level, Vec3 start, Vec3 end, double attackDamage) {
            this.owner = owner;
            this.level = level;
            this.start = start;
            Vec3 delta = end.subtract(start);
            this.distance = delta.length();
            this.direction = delta.normalize();
            this.steps = Math.max(1, (int) Math.ceil(distance / 1.5D));
            this.attackDamage = attackDamage;
        }
    }
    private record HolyField(Vec3 center, long expiresAt, long nextHealAt, int level) {}
    public WseeMod(IEventBus modBus) {
        CREATIVE_MODE_TABS.register(modBus);
        MOB_EFFECTS.register(modBus);
        PARTICLE_TYPES.register(modBus);
        modBus.addListener(this::registerPayloads);
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(this::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(this::onPlayerEnchantItem);
        NeoForge.EVENT_BUS.addListener(this::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(this::onLeftClickEmpty);
        NeoForge.EVENT_BUS.addListener(this::onUseItemFinish);
        NeoForge.EVENT_BUS.addListener(this::onUseItemStop);
        NeoForge.EVENT_BUS.addListener(this::onProjectileJoin);
        NeoForge.EVENT_BUS.addListener(this::onFishingHookJoin);
        NeoForge.EVENT_BUS.addListener(this::onEntityLeave);
        NeoForge.EVENT_BUS.addListener(this::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(this::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(this::onItemFished);
        NeoForge.EVENT_BUS.addListener(this::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(this::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(this::onEntityTick);
        NeoForge.EVENT_BUS.addListener(this::onEntityTickPost);
        NeoForge.EVENT_BUS.addListener(this::onLivingFall);
    }

    private void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar("1");
        registrar.playToServer(ShieldBashPayload.TYPE, ShieldBashPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        shieldBash(player);
                    }
                }));
        registrar.playToServer(SwordAuraPayload.TYPE, SwordAuraPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        triggerSwordAura(player);
                    }
                }));
        registrar.playToServer(DoubleJumpPayload.TYPE, DoubleJumpPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> {
                    if (context.player() instanceof ServerPlayer player) {
                        tryDoubleJump(player);
                    }
                }));
        registrar.playToClient(TrackingTargetPayload.TYPE, TrackingTargetPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() -> ClientHooks.handleTrackingTarget(payload)));
        registrar.playToClient(FlyingHookAnchorPayload.TYPE, FlyingHookAnchorPayload.STREAM_CODEC,
                (payload, context) -> context.enqueueWork(() ->
                        handleFlyingHookAnchor(payload, context.player())));
    }

    private void onAttackEntity(AttackEntityEvent event) {
        // AttackEntityEvent is posted on both logical sides. Effects and damage must run on the server.
        if (event.getEntity().level().isClientSide()) {
            return;
        }
        Player attacker = event.getEntity();
        ItemStack weapon = attacker.getMainHandItem();
        if (event.getTarget() instanceof LivingEntity target && target != attacker) {
            if (enchantmentLevel(attacker, weapon, BATTLE_MOMENTUM_ID) > 0) {
                pendingMeleeAttacks.put(attacker.getUUID(),
                        new PendingMeleeAttack(target.getUUID(), attacker.level().getGameTime()));
            }
            if (enchantmentLevel(attacker, weapon, FROSTBITE_ID) > 0
                    && (weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.SPEARS))) {
                pendingFrostbiteAttacks.put(attacker.getUUID(),
                        new PendingFrostbiteAttack(target.getUUID(), attacker.level().getGameTime(), weapon));
            }
            recordComboAttempt(attacker, target, weapon);
        } else {
            clearCombo(attacker);
        }
        triggerSwordAura(event.getEntity());
    }

    private void onLeftClickEmpty(PlayerInteractEvent.LeftClickEmpty event) {
        if (!event.getEntity().level().isClientSide()) {
            clearCombo(event.getEntity());
        }
    }

    private void recordComboAttempt(Player attacker, LivingEntity target, ItemStack weapon) {
        int level = enchantmentLevel(attacker, weapon, COMBO_STRIKE_ID);
        if (level <= 0 || !weapon.is(ItemTags.SWORDS)) {
            clearCombo(attacker);
            return;
        }
        UUID id = attacker.getUUID();
        ComboState state = comboStates.get(id);
        if (state == null || state.target == null || !state.target.equals(target.getUUID())
                || state.weapon != weapon || !state.dimension.equals(attacker.level().dimension())) {
            state = new ComboState(target.getUUID(), weapon, 0, attacker.level().dimension());
            comboStates.put(id, state);
        }
        int stage = Math.min(2, Math.max(0, state.stage));
        pendingComboAttacks.put(id, new PendingComboAttack(target.getUUID(), attacker.level().getGameTime(),
                weapon, level, stage));
    }

    /**
     * Creative players can normally bypass vanilla anvil applicability checks.
     * OATZ enchantments remain exclusive to their declared weapon or armor type.
     */
    private void onAnvilUpdate(AnvilUpdateEvent event) {
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

    /**
     * Enchanting-table results are validated after vanilla applies them. This
     * is a final server-side backstop for custom providers and creative paths.
     */
    private void onPlayerEnchantItem(PlayerEnchantItemEvent event) {
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

    /** Converts tagged ore drops using the vanilla smelting recipe after loot (including Fortune) is resolved. */
    private void onBlockDrops(BlockDropsEvent event) {
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
        net.minecraft.world.item.Item blockResult = event.getState().is(Blocks.IRON_ORE)
                || event.getState().is(Blocks.DEEPSLATE_IRON_ORE) ? Items.IRON_INGOT
                : event.getState().is(Blocks.GOLD_ORE) || event.getState().is(Blocks.DEEPSLATE_GOLD_ORE) ? Items.GOLD_INGOT
                : event.getState().is(Blocks.COPPER_ORE) || event.getState().is(Blocks.DEEPSLATE_COPPER_ORE) ? Items.COPPER_INGOT : null;
        for (ItemEntity drop : event.getDrops()) {
            ItemStack input = drop.getItem();
            if (input.isEmpty()) {
                continue;
            }
            net.minecraft.world.item.Item resultItem = input.is(Items.RAW_IRON) ? Items.IRON_INGOT
                    : input.is(Items.RAW_GOLD) ? Items.GOLD_INGOT
                    : input.is(Items.RAW_COPPER) ? Items.COPPER_INGOT : null;
            if (resultItem == null) {
                resultItem = blockResult;
            }
            if (resultItem == null) {
                continue;
            }
            ItemStack result = new ItemStack(resultItem);
            if (result.isEmpty()) {
                continue;
            }
            int outputCount = Math.max(1, result.getCount()) * input.getCount();
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

    private void onItemFished(ItemFishedEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()) {
            return;
        }
        ItemStack rod = player.getMainHandItem().is(Items.FISHING_ROD)
                ? player.getMainHandItem()
                : player.getOffhandItem();
        if (!rod.is(Items.FISHING_ROD)) {
            return;
        }
        if (enchantmentLevel(player, rod, FLYING_HOOK_ID) > 0) {
            event.getDrops().clear();
            return;
        }
        Holder<Enchantment> airForce = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, AIR_FORCE_ID));
        if (EnchantmentHelper.getItemEnchantmentLevel(airForce, rod) <= 0) {
            return;
        }
        Map<Rarity, List<ItemStack>> pools = new EnumMap<>(Rarity.class);
        for (Rarity rarity : Rarity.values()) {
            pools.put(rarity, new ArrayList<>());
        }
        for (net.minecraft.world.item.Item item : BuiltInRegistries.ITEM) {
            ItemStack candidate = new ItemStack(item, 1);
            if (item == Items.AIR || item == Items.COD || item == Items.SALMON
                    || item == Items.TROPICAL_FISH || item == Items.PUFFERFISH
                    || candidate.is(AIR_FORCE_FISH) || candidate.is(AIR_FORCE_UNOBTAINABLE)
                    || candidate.is(AIR_FORCE_ADMIN_BLACKLIST) || candidate.is(AIR_FORCE_BLACKLIST)
                    || isAirForceBuiltInBlacklist(item)) {
                continue;
            }
            if (!candidate.isEmpty()) {
                pools.get(candidate.getRarity()).add(candidate);
            }
        }
        int luckLevel = EnchantmentHelper.getItemEnchantmentLevel(
                player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.LUCK_OF_THE_SEA), rod);
        List<ItemStack> candidates = airForcePoolForRoll(pools, luckLevel, player.getRandom().nextDouble());
        if (candidates.isEmpty()) {
            // A modded registry may expose an unusual rarity set. Keep Air
            // Force functional by falling back to every validated candidate.
            candidates = pools.values().stream().flatMap(List::stream).toList();
            if (candidates.isEmpty()) {
                return;
            }
        }
        ItemStack selected = candidates.get(player.getRandom().nextInt(candidates.size()));
        event.getDrops().clear();
        event.getDrops().add(selected);
        if (player.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT,
                    player.getX(), player.getY() + 1.0D, player.getZ(), 4, 0.3D, 0.4D, 0.3D, 0.02D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP,
                    SoundSource.PLAYERS, 0.35F,
                    selected.getRarity() == Rarity.EPIC ? 1.4F : selected.getRarity() == Rarity.RARE ? 1.15F : 1.0F);
        }
    }

    private static List<ItemStack> airForcePoolForRoll(Map<Rarity, List<ItemStack>> pools, int luckLevel, double roll) {
        int level = Mth.clamp(luckLevel, 0, 3);
        double epic = level == 0 ? 0.01D : level == 1 ? 0.015D : level == 2 ? 0.025D : 0.04D;
        double rare = level == 0 ? 0.05D : level == 1 ? 0.065D : level == 2 ? 0.085D : 0.11D;
        double uncommon = level == 0 ? 0.12D : level == 1 ? 0.14D : level == 2 ? 0.16D : 0.18D;
        Rarity selected = roll < epic ? Rarity.EPIC : roll < epic + rare ? Rarity.RARE
                : roll < epic + rare + uncommon ? Rarity.UNCOMMON : Rarity.COMMON;
        for (Rarity fallback : new Rarity[] { selected, Rarity.RARE, Rarity.UNCOMMON, Rarity.COMMON }) {
            List<ItemStack> pool = pools.get(fallback);
            if (pool != null && !pool.isEmpty()) {
                return pool;
            }
        }
        return List.of();
    }

    private static boolean isAirForceBuiltInBlacklist(net.minecraft.world.item.Item item) {
        return item == Items.BEDROCK || item == Items.BARRIER || item == Items.COMMAND_BLOCK
                || item == Items.CHAIN_COMMAND_BLOCK || item == Items.REPEATING_COMMAND_BLOCK
                || item == Items.COMMAND_BLOCK_MINECART || item == Items.STRUCTURE_BLOCK
                || item == Items.STRUCTURE_VOID || item == Items.JIGSAW || item == Items.DEBUG_STICK
                || item == Items.LIGHT || item == Items.END_PORTAL_FRAME || item == Items.REINFORCED_DEEPSLATE;
    }

    private void tickFlyingHook(Player player) {
        UUID id = player.getUUID();
        FlyingHookState state = flyingHooks.get(id);
        if (state == null) {
            return;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            flyingHooks.remove(id);
            return;
        }
        if (!state.dimension.equals(level.dimension())) {
            ServerLevel hookLevel = level.getServer().getLevel(state.dimension);
            Entity oldHook = hookLevel == null ? null : hookLevel.getEntity(state.hook);
            if (oldHook instanceof FishingHook hook) {
                finishFlyingHook(player, state, hook);
            } else {
                flyingHooks.remove(id);
            }
            return;
        }
        Entity hookEntity = level.getEntity(state.hook);
        if (!(hookEntity instanceof FishingHook hook) || !hook.isAlive()) {
            flyingHooks.remove(id);
            return;
        }
        if (player.isDeadOrDying() || player.isPassenger() || player.isFallFlying()
                || player.getAbilities().flying) {
            finishFlyingHook(player, state, hook);
            return;
        }
        int levelValue = enchantmentLevel(player, player.getMainHandItem(), FLYING_HOOK_ID);
        if (levelValue <= 0 && enchantmentLevel(player, player.getOffhandItem(), FLYING_HOOK_ID) <= 0) {
            finishFlyingHook(player, state, hook);
            return;
        }
        Vec3 anchor = state.pulling ? resolveFishingAnchor(hook, state) : hook.position();
        if (anchor == null) {
            finishFlyingHook(player, state, hook);
            return;
        }
        double distance = player.position().distanceTo(anchor);
        int maxDistance = flyingHookMaxDistance(state.level);
        if (distance > maxDistance) {
            finishFlyingHook(player, state, hook);
            return;
        }
        if (!state.pulling) {
            return;
        }
        state.anchor = anchor;
        if (!Double.isNaN(state.lastDistance) && state.lastDistance - distance < 0.1D) {
            state.stuckTicks++;
        } else {
            state.stuckTicks = 0;
        }
        state.lastDistance = distance;
        if (distance <= 1.5D || ++state.ticks >= 60 || state.stuckTicks >= 10) {
            finishFlyingHook(player, state, hook);
            return;
        }
        Vec3 direction = anchor.subtract(player.position());
        if (direction.lengthSqr() < 1.0E-6D) {
            finishFlyingHook(player, state, hook);
            return;
        }
        Vec3 velocity = direction.normalize().scale(flyingHookPullSpeed(state.level));
        player.setDeltaMovement(velocity);
        player.syncVelocity = true;
        player.fallDistance = 0.0F;
        if (state.ticks % 4 == 0) {
            level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.5D, player.getZ(),
                    1 + state.level / 2, 0.12D, 0.12D, 0.12D, 0.01D);
        }
    }

    private static double flyingHookPullSpeed(int level) {
        return switch (level) {
            case 1 -> 0.55D;
            case 2 -> 0.65D;
            case 3 -> 0.75D;
            case 4 -> 0.85D;
            default -> 0.95D;
        };
    }

    private static int flyingHookMaxDistance(int level) {
        return switch (level) {
            case 1 -> 24;
            case 2 -> 30;
            case 3 -> 36;
            case 4 -> 42;
            default -> 48;
        };
    }

    private static Vec3 resolveFishingAnchor(FishingHook hook, FlyingHookState state) {
        if (state.anchorEntity != null) {
            Entity entity = state.anchorEntity;
            if (entity.isRemoved() || !entity.canInteractWithLevel() || entity.level() != hook.level()) {
                return null;
            }
            Vec3 offset = state.entityAnchorOffset == null ? Vec3.ZERO : state.entityAnchorOffset;
            return entity.position().add(offset);
        }
        if (state.anchorBlock != null
                && hook.level().getBlockState(state.anchorBlock).getShape(hook.level(), state.anchorBlock).isEmpty()) {
            return null;
        }
        return state.anchor;
    }

    private void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        tickLongEdgeRange(player);
        if (!player.level().isClientSide()) {
            cooldowns.computeIfPresent(player.getUUID(), (id, ticks) -> ticks <= 1 ? null : ticks - 1);
            tickSlowModifier(player.level());
            expansionShotTicks.remove(player.getUUID());
            releaseQueuedShot(player);
            tickChestEnchantments(player);
            tickOverchargeParticles(player);
            tickSwordWave(player);
            if (player.level() instanceof ServerLevel serverLevel) {
                long gameTime = serverLevel.getGameTime();
                if (!gameTimeEquals(explosiveStateTicks.get(serverLevel.dimension()), gameTime)) {
                    explosiveStateTicks.put(serverLevel.dimension(), gameTime);
                    tickDetachedExplosiveStates(serverLevel);
                    tickArrowRainStates(serverLevel);
                    tickShockRings(serverLevel);
                }
            }
            tickBattleMomentum(player);
            tickForcedMarch(player);
            tickFoundation(player);
            tickFlyingHook(player);
            tickDoubleJump(player);
            tickClarity(player);
            tickComboState(player);
            PendingMeleeAttack pending = pendingMeleeAttacks.get(player.getUUID());
            if (pending != null && pending.tick() < player.level().getGameTime()) {
                pendingMeleeAttacks.remove(player.getUUID());
            }
            PendingComboAttack pendingCombo = pendingComboAttacks.get(player.getUUID());
            if (pendingCombo != null && pendingCombo.tick() < player.level().getGameTime()) {
                pendingComboAttacks.remove(player.getUUID());
                clearCombo(player);
            }
        }
    }

    /** Keeps the spear's item attack range in sync on both logical sides. */
    private void tickLongEdgeRange(Player player) {
        ItemStack spear = player.getMainHandItem();
        int level = enchantmentLevel(player, spear, LONG_EDGE_ID);
        AttackRange current = spear.get(DataComponents.ATTACK_RANGE);
        if (level <= 0) {
            if (isLongEdgeRange(current)) {
                spear.set(DataComponents.ATTACK_RANGE, vanillaSpearAttackRange());
            }
            return;
        }
        double bonus = switch (level) {
            case 1 -> 0.25D;
            case 2 -> 0.625D;
            default -> 1.0D;
        };
        AttackRange expected = new AttackRange(2.0F, (float) (4.5D + bonus),
                2.0F, (float) (6.5D + bonus), 0.125F, 0.5F);
        if (!expected.equals(current)) {
            spear.set(DataComponents.ATTACK_RANGE, expected);
        }
    }

    private void tickDoubleJump(Player player) {
        UUID id = player.getUUID();
        boolean keyDown = player.isJumping();
        boolean jumpPressed = keyDown && !doubleJumpKeyDown.getOrDefault(id, false);
        doubleJumpKeyDown.put(id, keyDown);
        int level = enchantmentLevel(player, player.getItemBySlot(EquipmentSlot.FEET), DOUBLE_JUMP_ID);
        if (level <= 0 || player.isDeadOrDying() || player.isSpectator()
                || player.isSwimming() || player.isPassenger() || player.isFallFlying()
                || player.getAbilities().flying) {
            doubleJumpAvailable.remove(id);
            doubleJumpKeyDown.remove(id);
            return;
        }
        if (player.onGround()) {
            doubleJumpAvailable.put(id, true);
        } else if (!doubleJumpAvailable.containsKey(id)) {
            doubleJumpAvailable.put(id, true);
        }
        if (jumpPressed && player instanceof ServerPlayer serverPlayer) {
            tryDoubleJump(serverPlayer);
        }
    }

    private void tryDoubleJump(ServerPlayer player) {
        int level = enchantmentLevel(player, player.getItemBySlot(EquipmentSlot.FEET), DOUBLE_JUMP_ID);
        if (level <= 0 || player.onGround() || player.isSpectator() || player.isSwimming()
                || player.isPassenger() || player.isFallFlying() || player.getAbilities().flying
                || !doubleJumpAvailable.getOrDefault(player.getUUID(), false)) {
            return;
        }
        Vec3 current = player.getDeltaMovement();
        player.setDeltaMovement(current.x, 0.65D, current.z);
        player.syncVelocity = true;
        player.connection.send(new ClientboundSetEntityMotionPacket(player));
        player.fallDistance = 0.0F;
        doubleJumpAvailable.put(player.getUUID(), false);
        if (player.level() instanceof ServerLevel levelWorld) {
            levelWorld.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY(), player.getZ(),
                    6, 0.25D, 0.08D, 0.25D, 0.03D);
            levelWorld.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                    SoundSource.PLAYERS, 0.55F, 1.35F);
        }
    }

    private void onLivingFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player) || player.level().isClientSide()
                || enchantmentLevel(player, player.getItemBySlot(EquipmentSlot.FEET), DOUBLE_JUMP_ID) <= 0) {
            return;
        }
        event.setDistance(Math.max(0.0F, event.getDistance() - 2.0F));
    }

    private static AttackRange vanillaSpearAttackRange() {
        return new AttackRange(2.0F, 4.5F, 2.0F, 6.5F, 0.125F, 0.5F);
    }

    private static boolean isLongEdgeRange(AttackRange range) {
        if (range == null || range.minReach() != 2.0F || range.minCreativeReach() != 2.0F
                || range.hitboxMargin() != 0.125F || range.mobFactor() != 0.5F) {
            return false;
        }
        return (range.maxReach() == 4.75F || range.maxReach() == 5.125F || range.maxReach() == 5.5F)
                && (range.maxCreativeReach() == 6.75F || range.maxCreativeReach() == 7.125F
                        || range.maxCreativeReach() == 7.5F);
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("wsee")
                .then(Commands.literal("reset_cooldown")
                        .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                        .then(Commands.literal("shield")
                                .executes(context -> resetCooldown(context.getSource(), "shield")))
                        .then(Commands.literal("holy_light")
                                .executes(context -> resetCooldown(context.getSource(), "holy_light")))
                        .then(Commands.literal("shield_bash")
                                .executes(context -> resetCooldown(context.getSource(), "shield_bash")))));
    }

    private int resetCooldown(net.minecraft.commands.CommandSourceStack source, String enchantment) {
        if (!(source.getEntity() instanceof Player player)) {
            source.sendFailure(Component.translatable("commands.wsee.reset_cooldown.player_only"));
            return 0;
        }
        switch (enchantment) {
            case "shield" -> {
                player.getPersistentData().putLong(SHIELD_NEXT_REFRESH_KEY, 0L);
                clearShield(player);
            }
            case "holy_light" -> player.getPersistentData().putLong(HOLY_LIGHT_NEXT_TRIGGER_KEY, 0L);
            case "shield_bash" -> cooldowns.remove(player.getUUID());
            default -> {
                source.sendFailure(Component.translatable("commands.wsee.reset_cooldown.unknown"));
                return 0;
            }
        }
        source.sendSuccess(() -> Component.translatable("commands.wsee.reset_cooldown.success", enchantment), true);
        return 1;
    }

    private void tickOverchargeParticles(Player player) {
        UUID id = player.getUUID();
        if (!player.isUsingItem() || !(player.getUseItem().getItem() instanceof BowItem)) {
            clearOverchargeState(player);
            return;
        }
        ItemStack bow = player.getUseItem();
        int level = enchantmentLevel(player, bow, OVERCHARGE_ID);
        int heldTicks = player.getTicksUsingItem();
        if (level <= 0 || heldTicks < 20 || !(player.level() instanceof ServerLevel serverLevel)) {
            clearOverchargeState(player);
            return;
        }
        int maxExtraTicks = switch (level) {
            case 1 -> 10;
            case 2 -> 15;
            default -> 20;
        };
        double ratio = Mth.clamp((heldTicks - 20.0D) / maxExtraTicks, 0.0D, 1.0D);
        Vec3 center = player.position().add(player.getLookAngle().scale(0.45D)).add(0.0D, player.getBbHeight() * 0.62D, 0.0D);
        if (overchargeStarted.add(id)) {
            serverLevel.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z,
                    5 + level * 2, 0.3D, 0.3D, 0.3D, 0.03D);
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS, 0.22F, 0.9F + level * 0.06F);
        }
        int stage = ratio >= 1.0D ? 4 : ratio >= 0.75D ? 3 : ratio >= 0.5D ? 2 : ratio >= 0.25D ? 1 : 0;
        int previousStage = overchargeStages.getOrDefault(id, -1);
        if (stage > previousStage && stage > 0) {
            serverLevel.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 0.22F + stage * 0.025F, 0.95F + stage * 0.08F);
        }
        overchargeStages.put(id, stage);
        if (heldTicks % 5 == 0) {
            int displayStage = Math.min(4, (int) Math.floor(ratio * 4.0D) + 1);
            int velocityPercent = (int) Math.round((switch (level) {
                case 1 -> 0.05D;
                case 2 -> 0.08D;
                default -> 0.12D;
            }) * ratio * 100.0D);
            overchargeOverlayShown.add(id);
            player.sendOverlayMessage(overchargeChargeOverlay(displayStage, velocityPercent));
        }
        int interval = level == 1 ? 5 : level == 2 ? 3 : 2;
        if (heldTicks % interval == 0) {
            int count = level == 1 ? 1 : level == 2 ? 2 : 3;
            if (ratio >= 0.75D) count++;
            serverLevel.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z,
                    count, 0.25D + ratio * 0.25D, 0.25D + ratio * 0.25D, 0.25D + ratio * 0.25D, 0.02D + ratio * 0.03D);
            if (ratio >= 0.5D) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                        Math.min(2, level), 0.18D, 0.18D, 0.18D, 0.01D);
            }
        }
        if (ratio >= 1.0D && overchargeFullPlayed.add(id)) {
            serverLevel.sendParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z,
                    8 + level * 4, 0.35D, 0.35D, 0.35D, 0.04D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z,
                    4 + level * 3, 0.25D, 0.25D, 0.25D, 0.03D);
        }
    }

    private void clearOverchargeState(Player player) {
        UUID id = player.getUUID();
        overchargeStarted.remove(id);
        overchargeFullPlayed.remove(id);
        overchargeStages.remove(id);
        if (overchargeOverlayShown.remove(id)) {
            player.sendOverlayMessage(Component.empty());
        }
    }

    private static Component overchargeChargeOverlay(int stage, int velocityPercent) {
        int filled = Mth.clamp(stage, 1, 4);
        ChatFormatting[] colors = {
                ChatFormatting.RED,
                ChatFormatting.GOLD,
                ChatFormatting.YELLOW,
                ChatFormatting.GREEN
        };
        var message = Component.translatable("wsee.overcharge.charge");
        for (int index = 0; index < 4; index++) {
            boolean isFilled = index < filled;
            ChatFormatting color = isFilled ? colors[index] : ChatFormatting.DARK_GRAY;
            message.append(Component.literal(isFilled ? "■" : "□").withStyle(color));
        }
        message.append(Component.translatable("wsee.overcharge.velocity", velocityPercent)
                .withStyle(ChatFormatting.GRAY));
        return message;
    }

    private void tickChestEnchantments(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int shieldLevel = directShieldLevel(player, chest);
        if (shieldLevel <= 0) {
            clearShield(player);
            holyFields.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= player.level().getGameTime());
            return;
        }

        // Invalidate state written by the earlier shield implementation once,
        // so an already-equipped chestplate receives the new vanilla effect.
        if (player.getPersistentData().getIntOr(SHIELD_VERSION_KEY, 0) != 2) {
            player.getPersistentData().remove(SHIELD_EFFECT_KEY);
            player.getPersistentData().remove(SHIELD_AMOUNT_KEY);
            player.getPersistentData().putInt(SHIELD_VERSION_KEY, 2);
        }
        long now = player.level().getGameTime();
        long nextRefresh = player.getPersistentData().getLongOr(SHIELD_NEXT_REFRESH_KEY, 0L);
        if (nextRefresh <= now) {
            applyShield(player, shieldLevel, now);
        } else {
            reconcileShieldAfterReload(player, shieldLevel, nextRefresh);
        }
        holyFields.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= player.level().getGameTime());
    }

    private int directShieldLevel(Player player, ItemStack chest) {
        if (!isValidEnchantmentItem(chest, SHIELD_ID)) {
            return 0;
        }
        Holder<Enchantment> enchantment = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, SHIELD_ID));
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, chest);
    }

    private void applyShield(Player player, int shieldLevel, long now) {
        double percent = switch (shieldLevel) {
            case 1 -> 0.08D;
            case 2 -> 0.12D;
            case 3 -> 0.16D;
            case 4 -> 0.20D;
            default -> 0.25D;
        };
        float shield = (float) (player.getMaxHealth() * percent);
        float previousShield = (float) player.getPersistentData().getDoubleOr(
                SHIELD_REMAINING_KEY,
                player.getPersistentData().getDoubleOr(SHIELD_AMOUNT_KEY, 0.0D));
        float preservedAbsorption = Math.max(0.0F, player.getAbsorptionAmount() - previousShield);
        ensureShieldAbsorptionLimit(player, shield + preservedAbsorption);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 4800, shieldLevel - 1,
                false, true, true));
        player.setAbsorptionAmount(preservedAbsorption + shield);
        player.getPersistentData().putBoolean(SHIELD_EFFECT_KEY, true);
        player.getPersistentData().putDouble(SHIELD_AMOUNT_KEY, shield);
        player.getPersistentData().putDouble(SHIELD_REMAINING_KEY, shield);
        player.getPersistentData().putLong(SHIELD_NEXT_REFRESH_KEY, now + 4800L);
        if (player.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 1.0D, player.getZ(),
                    24, 0.6D, 0.9D, 0.6D, 0.04D);
            level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                    12, 0.55D, 0.8D, 0.55D, 0.02D);
            level.playSound(null, player.blockPosition(), SoundEvents.BEACON_POWER_SELECT,
                    SoundSource.PLAYERS, 0.75F, 1.0F + shieldLevel * 0.06F);
        }
    }

    /**
     * Transient attribute modifiers are not serialized by Minecraft. Restore the
     * shield's saved remaining absorption after a player is loaded without
     * refreshing its cooldown or creating a new shield.
     */
    private void reconcileShieldAfterReload(Player player, int shieldLevel, long nextRefresh) {
        var data = player.getPersistentData();
        if (!data.getBooleanOr(SHIELD_EFFECT_KEY, false)) {
            return;
        }
        float fullShield = (float) data.getDoubleOr(SHIELD_AMOUNT_KEY, 0.0D);
        if (fullShield <= 0.0F) {
            return;
        }

        var maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        boolean hasLimitModifier = maxAbsorption != null && maxAbsorption.hasModifier(SHIELD_ABSORPTION_LIMIT_ID);
        boolean hasSavedRemaining = data.contains(SHIELD_REMAINING_KEY);
        float savedRemaining = hasSavedRemaining
                ? Mth.clamp((float) data.getDoubleOr(SHIELD_REMAINING_KEY, 0.0D), 0.0F, fullShield)
                : fullShield;
        float previousRemaining = hasSavedRemaining ? savedRemaining : fullShield;
        float currentAbsorption = Math.max(0.0F, player.getAbsorptionAmount());

        // While the modifier is present, only record damage taken from the
        // pooled absorption value. Positive changes are treated as external
        // absorption and do not replenish OATZ's shield.
        if (hasLimitModifier) {
            savedRemaining = Math.min(previousRemaining, currentAbsorption);
        } else {
            // A missing modifier is the reload case. The current value may
            // already have been clamped to vanilla's 20-point limit, so use the
            // persisted remaining value instead of accepting that truncated
            // value as the new shield state.
            float preservedAbsorption = Math.max(0.0F, currentAbsorption - previousRemaining);
            float restoredAbsorption = savedRemaining + preservedAbsorption;
            if (restoredAbsorption > 0.0F) {
                ensureShieldAbsorptionLimit(player, restoredAbsorption);
                player.setAbsorptionAmount(restoredAbsorption);
            }
            if (player.getEffect(MobEffects.ABSORPTION) == null && savedRemaining > 0.0F) {
                long remainingCooldown = Math.max(1L, nextRefresh - player.level().getGameTime());
                int duration = (int) Math.min(Integer.MAX_VALUE, remainingCooldown);
                player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, duration, shieldLevel - 1,
                        false, true, true));
            }
        }

        data.putDouble(SHIELD_REMAINING_KEY, savedRemaining);
    }

    private static void ensureShieldAbsorptionLimit(Player player, float requiredAbsorption) {
        var maxAbsorption = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (maxAbsorption == null) {
            return;
        }
        maxAbsorption.removeModifier(SHIELD_ABSORPTION_LIMIT_ID);
        double extraLimit = Math.max(0.0D, requiredAbsorption - player.getMaxAbsorption());
        if (extraLimit > 0.0D) {
            maxAbsorption.addTransientModifier(new AttributeModifier(SHIELD_ABSORPTION_LIMIT_ID,
                    extraLimit, AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private void clearShield(Player player) {
        var data = player.getPersistentData();
        float ownedShield = (float) data.getDoubleOr(SHIELD_REMAINING_KEY,
                data.getDoubleOr(SHIELD_AMOUNT_KEY, 0.0D));
        boolean owned = data.getBooleanOr(SHIELD_EFFECT_KEY, false);
        if (!owned && ownedShield <= 0.0F) {
            return;
        }
        player.setAbsorptionAmount(Math.max(0.0F, player.getAbsorptionAmount() - ownedShield));
        player.removeEffect(MobEffects.ABSORPTION);
        if (player.getAttribute(Attributes.MAX_ABSORPTION) != null) {
            player.getAttribute(Attributes.MAX_ABSORPTION).removeModifier(SHIELD_ABSORPTION_LIMIT_ID);
        }
        data.remove(SHIELD_EFFECT_KEY);
        data.remove(SHIELD_AMOUNT_KEY);
        data.remove(SHIELD_REMAINING_KEY);
    }

    private void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack crossbow = event.getResultStack();
        if (!(crossbow.getItem() instanceof CrossbowItem)) {
            return;
        }
        int level = enchantmentLevel(player, crossbow, EXPANSION_ID);
        if (level <= 0) {
            return;
        }
        ChargedProjectiles charged = crossbow.getOrDefault(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                ChargedProjectiles.EMPTY);
        if (charged.isEmpty()) {
            return;
        }
        List<ItemStack> projectiles = charged.itemCopies().toList();
        int wanted = Math.min(4, level + 1);
        while (projectiles.size() < wanted) {
            ItemStack extra = takeArrow(player);
            if (extra.isEmpty()) {
                break;
            }
            projectiles.add(extra);
        }
        crossbow.set(net.minecraft.core.component.DataComponents.CHARGED_PROJECTILES,
                ChargedProjectiles.ofNonEmpty(projectiles));
        event.setResultStack(crossbow);
    }

    private void onUseItemStop(LivingEntityUseItemEvent.Stop event) {
        if (!(event.getEntity() instanceof ServerPlayer player)
                || !(event.getItem().getItem() instanceof BowItem)) {
            return;
        }
        int useDuration = event.getItem().getUseDuration(player);
        if (useDuration > 0) {
            int heldTicks = Math.max(0, useDuration - event.getDuration());
            // Vanilla bows reach their full draw at 20 ticks even though the
            // generic item use duration is much longer (usually 72,000 ticks).
            bowDrawRatios.put(player.getUUID(), Mth.clamp(heldTicks / 20.0D, 0.0D, 1.0D));
        }
        int level = enchantmentLevel(player, event.getItem(), OVERCHARGE_ID);
        if (level <= 0) {
            return;
        }
        int heldTicks = Math.max(0, useDuration - event.getDuration());
        int maxExtraTicks = switch (level) {
            case 1 -> 10;
            case 2 -> 15;
            default -> 20;
        };
        double ratio = Mth.clamp((heldTicks - 20.0D) / maxExtraTicks, 0.0D, 1.0D);
        double maxDamageBonus = switch (level) {
            case 1 -> 0.07D;
            case 2 -> 0.17D;
            default -> 0.27D;
        };
        double maxVelocityBonus = switch (level) {
            case 1 -> 0.05D;
            case 2 -> 0.08D;
            default -> 0.12D;
        };
        overchargeCharges.put(player.getUUID(), new OverchargeCharge(
                1.0D + maxDamageBonus * ratio,
                1.0D + maxVelocityBonus * ratio));
        if (player.level() instanceof ServerLevel serverLevel && ratio > 0.0D) {
            Vec3 center = player.position().add(player.getLookAngle().scale(0.5D))
                    .add(0.0D, player.getBbHeight() * 0.62D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.ENCHANT, center.x, center.y, center.z,
                    2 + (int) Math.round(ratio * 6.0D), 0.2D, 0.2D, 0.2D, 0.04D);
            if (ratio >= 1.0D) {
                serverLevel.sendParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z,
                        4 + level * 2, 0.2D, 0.2D, 0.2D, 0.05D);
            }
        }
        if (ratio <= 0.0D) {
            // A normal full-bow shot has no overcharge state to apply. Avoid
            // retaining a stale entry if the use event is cancelled afterward.
            overchargeCharges.remove(player.getUUID());
        }
        clearOverchargeState(player);
    }

    private ItemStack takeArrow(ServerPlayer player) {
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.getItem() instanceof ArrowItem) {
                ItemStack result = stack.copyWithCount(1);
                stack.shrink(1);
                return result;
            }
        }
        return ItemStack.EMPTY;
    }

    private void onProjectileJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        // Capture trident enchantments as soon as the projectile is spawned.  A
        // returning trident can temporarily expose an empty weapon stack while
        // vanilla is updating its owner reference, so later ticks must use the
        // captured levels instead of treating that transient state as removal.
        if (arrow instanceof ThrownTrident trident) {
            captureTridentState(trident, serverLevel);
        }
        if (arrow.getPersistentData().getBoolean(ARROW_RAIN_PROJECTILE_KEY).orElse(false)) {
            return;
        }
        if (generatedExpansionArrows.remove(arrow.getUUID())) {
            return;
        }
        if (generatedClusterArrows.remove(arrow.getUUID())) {
            return;
        }
        ItemStack weapon = arrow.getWeaponItem();
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) {
            return;
        }
        if (weapon.getItem() instanceof BowItem) {
            int explosiveLevel = enchantmentLevel(shooter, weapon, EXPLOSIVE_ARROW_ID);
            if (explosiveLevel > 0) {
                explosiveArrows.put(arrow.getUUID(), new ExplosiveArrowState(shooter.getUUID(), explosiveLevel,
                        shooter.getAttributeValue(Attributes.ATTACK_DAMAGE)));
            }
            Double recordedDrawRatio = bowDrawRatios.remove(shooter.getUUID());
            // The release event normally runs before projectile creation. Keep
            // a vanilla-physics fallback for integrations that reorder those
            // callbacks: a fully drawn bow launches at roughly 3 blocks/tick.
            double drawRatio = recordedDrawRatio != null
                    ? recordedDrawRatio
                    : Mth.clamp(arrow.getDeltaMovement().length() / 3.0D, 0.0D, 1.0D);
            int arrowRainLevel = enchantmentLevel(shooter, weapon, ARROW_RAIN_ID);
            if (arrowRainLevel > 0 && drawRatio >= 0.90D) {
                arrowRainStates.put(arrow.getUUID(), new ArrowRainState(shooter.getUUID(), arrowRainLevel,
                        readArrowBaseDamage(arrow), weapon, serverLevel.dimension()));
            }
            int trackingLevel = enchantmentLevel(shooter, weapon, TRACKING_ID);
            if (trackingLevel > 0 && drawRatio >= 0.80D) {
                acquireTrackingTarget(arrow, shooter, trackingLevel, serverLevel);
            }
        }
        OverchargeCharge charge = overchargeCharges.remove(shooter.getUUID());
        if (charge != null) {
            applyOvercharge(arrow, charge, serverLevel);
            return;
        }
        if (!(weapon.getItem() instanceof CrossbowItem)) {
            return;
        }
        // EntityJoinLevelEvent also fires when an old arrow is restored from a
        // world save. Only a genuinely new crossbow shot may create follow-ups.
        if (event.loadedFromDisk()) {
            return;
        }
        int expansion = enchantmentLevel(shooter, weapon, EXPANSION_ID);
        if (expansion > 0) {
            UUID shooterId = shooter.getUUID();
            long tick = serverLevel.getGameTime();
            if (expansionShotTicks.putIfAbsent(shooterId, tick) == null) {
                ArrayDeque<QueuedProjectile> queue = delayedCrossbowShots.computeIfAbsent(shooterId,
                        id -> new ArrayDeque<>());
                int extraCount = Math.min(3, expansion);
                ItemStack projectileStack = arrow.getPickupItemStackOrigin();
                Vec3 velocity = arrow.getDeltaMovement();
                for (int i = 1; i <= extraCount; i++) {
                    if (!(projectileStack.getItem() instanceof ArrowItem arrowItem)) {
                        break;
                    }
                    AbstractArrow extra = arrowItem.createArrow(serverLevel, projectileStack, shooter, weapon);
                    extra.setPos(arrow.position());
                    extra.shoot(velocity.x, velocity.y, velocity.z, (float) velocity.length(), 0.0F);
                    extra.setCritArrow(arrow.isCritArrow());
                    generatedExpansionArrows.add(extra.getUUID());
                    queue.add(new QueuedProjectile(extra, tick + i * 5L));
                }
            }
        }

        int cluster = enchantmentLevel(shooter, weapon, CLUSTER_ID);
        if (cluster <= 0 || !(arrow.getPickupItemStackOrigin().getItem() instanceof ArrowItem arrowItem)) {
            return;
        }
        float damageMultiplier = switch (cluster) {
            case 1 -> 0.70F;
            case 2 -> 0.55F;
            default -> 0.45F;
        };
        arrow.setBaseDamage(2.0D * damageMultiplier);
        int count = cluster + 1;
        float angle = (float) Math.toRadians(cluster == 1 ? 4.0D : cluster == 2 ? 3.0D : 2.0D);
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        Vec3 base = velocity.normalize();
        for (int i = 1; i < count; i++) {
            double offset = -angle + (2.0D * angle * i / (count - 1));
            Vec3 direction = rotateAroundY(base, offset);
            AbstractArrow extra = arrowItem.createArrow(serverLevel, arrow.getPickupItemStackOrigin(), shooter, weapon);
            extra.setPos(arrow.position());
            extra.shoot(direction.x, direction.y, direction.z, (float) speed, 0.0F);
            extra.setBaseDamage(2.0D * damageMultiplier);
            extra.setCritArrow(arrow.isCritArrow());
            generatedClusterArrows.add(extra.getUUID());
        serverLevel.addFreshEntity(extra);
    }
    }

    private void onFishingHookJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof FishingHook hook)
                || !(hook.getOwner() instanceof Player owner) || !(hook.level() instanceof ServerLevel level)) {
            return;
        }
        InteractionHand hand = InteractionHand.MAIN_HAND;
        int flyingLevel = enchantmentLevel(owner, owner.getMainHandItem(), FLYING_HOOK_ID);
        if (flyingLevel <= 0) {
            hand = InteractionHand.OFF_HAND;
            flyingLevel = enchantmentLevel(owner, owner.getOffhandItem(), FLYING_HOOK_ID);
        }
        if (flyingLevel > 0) {
            hook.setDeltaMovement(hook.getDeltaMovement().scale(1.0D + flyingLevel * 0.15D));
            hook.setNoGravity(true);
            hook.getPersistentData().putInt(FLYING_HOOK_LEVEL_KEY, Mth.clamp(flyingLevel, 1, 5));
            flyingHooks.put(owner.getUUID(),
                    new FlyingHookState(hook.getUUID(), flyingLevel, level.dimension(), hand));
        }
    }

    private void onEntityLeave(EntityLeaveLevelEvent event) {
        if (!(event.getEntity() instanceof FishingHook hook)) {
            return;
        }
        clientFlyingHookAnchors.remove(hook.getUUID());
        if (!event.getLevel().isClientSide() && hook.getOwner() instanceof Player owner) {
            FlyingHookState state = flyingHooks.get(owner.getUUID());
            if (state != null && state.hook.equals(hook.getUUID())) {
                flyingHooks.remove(owner.getUUID());
            }
        }
    }

    private void onProjectileImpact(ProjectileImpactEvent event) {
        if (anchorFlyingHook(event)) {
            return;
        }
        if (deflectArrow(event)) {
            return;
        }
        if (event.getProjectile() instanceof AbstractArrow arrow
                && arrow.level() instanceof ServerLevel arrowLevel) {
            armExplosiveArrow(arrow, event, arrowLevel);
            armArrowRain(arrow, event, arrowLevel);
        }
        if (!(event.getProjectile() instanceof ThrownTrident trident)
                || !(trident.level() instanceof ServerLevel level)
                || trident.noPhysics) {
            return;
        }
        captureTridentState(trident, level);
        SurgeState state = surgeStates.get(trident.getUUID());
        if (state == null || !state.outbound || state.shockwaveTriggered || state.hydroshockLevel <= 0) {
            return;
        }
        // ProjectileImpactEvent is raised from inside the vanilla projectile
        // tick, before the next EntityTickEvent.Pre. Advance Surge here as
        // well so the impact tick itself cannot be lost at the water boundary.
        if (state.surgeLevel > 0 && isThrownTridentFlightState(trident)) {
            advanceSurgeWaterState(trident, level, state, tridentTouchesWater(trident, level)
                    || level.getFluidState(BlockPos.containing(event.getRayTraceResult().getLocation()))
                    .is(FluidTags.WATER));
        }
        Entity owner = trident.getOwner();
        if (!(owner instanceof LivingEntity shooter)
                || enchantmentLevel(shooter, tridentItemStack(trident), HYDROSHOCK_ID) <= 0) {
            return;
        }
        Vec3 impact = event.getRayTraceResult().getLocation();
        if (!trident.isInWater()
                && !level.getFluidState(net.minecraft.core.BlockPos.containing(impact)).is(net.minecraft.tags.FluidTags.WATER)) {
            return;
        }
        state.shockwaveTriggered = true;
        LivingEntity directTarget = event.getRayTraceResult() instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity living ? living : null;
        triggerHydroshock(trident, level, impact, directTarget, state.hydroshockLevel);
    }

    private boolean anchorFlyingHook(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof FishingHook hook)
                || !(hook.getOwner() instanceof Player owner)) {
            return false;
        }
        if (hook.level().isClientSide()) {
            if (!isClientFlyingHook(hook, owner) || !isFlyingHookImpact(hook, event.getRayTraceResult())) {
                return false;
            }
            anchorClientFlyingHook(hook, event.getRayTraceResult());
            event.setCanceled(true);
            return true;
        }
        if (!(hook.level() instanceof ServerLevel level)) {
            return false;
        }
        FlyingHookState state = flyingHooks.get(owner.getUUID());
        if (state == null || !state.hook.equals(hook.getUUID())
                || !state.dimension.equals(level.dimension())) {
            return false;
        }
        if (!state.pulling && !anchorFlyingHook(hook, state, event.getRayTraceResult())) {
            return false;
        }
        event.setCanceled(true);
        return true;
    }

    private boolean anchorFlyingHook(FishingHook hook, FlyingHookState state, HitResult hit) {
        if (!isFlyingHookImpact(hook, hit)) {
            return false;
        }
        if (!(hook.level() instanceof ServerLevel level) || !(hook.getOwner() instanceof Player owner)) {
            return false;
        }
        Vec3 hitLocation = hit.getLocation();
        state.anchor = hitLocation;
        state.anchorEntity = null;
        state.entityAnchorOffset = null;
        state.anchorBlock = null;
        state.durabilityCost = 2;
        if (hit instanceof EntityHitResult entityHit) {
            Entity target = entityHit.getEntity();
            state.anchorEntity = target;
            state.entityAnchorOffset = hitLocation.subtract(target.position());
            state.durabilityCost = target instanceof ItemEntity ? 3 : 5;
        } else if (hit instanceof BlockHitResult blockHit) {
            state.anchorBlock = blockHit.getBlockPos().immutable();
        }
        state.pulling = true;
        hook.setPos(hitLocation);
        hook.setDeltaMovement(Vec3.ZERO);
        hook.syncPosition = true;
        hook.syncVelocity = true;
        sendFlyingHookAnchor(hook, hitLocation);
        owner.fallDistance = 0.0F;
        level.playSound(null, BlockPos.containing(hitLocation), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.PLAYERS, 0.75F, 1.2F);
        level.sendParticles(ParticleTypes.CLOUD, hitLocation.x, hitLocation.y, hitLocation.z,
                4 + state.level, 0.25D, 0.25D, 0.25D, 0.03D);
        return true;
    }

    private void handleFlyingHookAnchor(FlyingHookAnchorPayload payload, Player receiver) {
        Entity entity = receiver.level().getEntity(payload.hookEntityId());
        if (!(entity instanceof FishingHook hook)) {
            return;
        }
        Vec3 anchor = new Vec3(payload.x(), payload.y(), payload.z());
        clientFlyingHookAnchors.put(hook.getUUID(),
                new ClientFlyingHookAnchor(anchor, null, null, null));
        hook.setPos(anchor);
        hook.setDeltaMovement(Vec3.ZERO);
    }

    private static void sendFlyingHookAnchor(FishingHook hook, Vec3 anchor) {
        PacketDistributor.sendToPlayersTrackingEntity(hook,
                new FlyingHookAnchorPayload(hook.getId(), anchor.x, anchor.y, anchor.z));
    }

    private static boolean isFlyingHookImpact(FishingHook hook, HitResult hit) {
        if (hit == null) {
            return false;
        }
        if (hit.getType() == HitResult.Type.ENTITY) {
            return hit instanceof EntityHitResult;
        }
        if (hit.getType() != HitResult.Type.BLOCK || !(hit instanceof BlockHitResult blockHit)) {
            return false;
        }
        return !hook.level().getBlockState(blockHit.getBlockPos()).isAir();
    }

    private void anchorClientFlyingHook(FishingHook hook, HitResult hit) {
        if (!isFlyingHookImpact(hook, hit)) {
            return;
        }
        Entity entity = hit instanceof EntityHitResult entityHit ? entityHit.getEntity() : null;
        Vec3 offset = entity == null ? null : hit.getLocation().subtract(entity.position());
        BlockPos block = hit instanceof BlockHitResult blockHit ? blockHit.getBlockPos().immutable() : null;
        clientFlyingHookAnchors.put(hook.getUUID(),
                new ClientFlyingHookAnchor(hit.getLocation(), entity, offset, block));
        hook.setPos(hit.getLocation());
        hook.setDeltaMovement(Vec3.ZERO);
    }

    private boolean deflectArrow(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || !(arrow instanceof Arrow || arrow instanceof SpectralArrow)
                || !(arrow.level() instanceof ServerLevel level)
                || explosiveArrows.containsKey(arrow.getUUID())
                || arrow.getPersistentData().getBoolean(DEFLECTED_KEY).orElse(false)
                || !(event.getRayTraceResult() instanceof EntityHitResult entityHit)
                || !(entityHit.getEntity() instanceof ServerPlayer player)
                || !player.isBlocking()) {
            return false;
        }
        ItemStack shield = player.getItemBlockingWith();
        int deflectLevel = enchantmentLevel(player, shield, DEFLECT_ID);
        if (deflectLevel <= 0) {
            return false;
        }
        Vec3 look = player.getLookAngle();
        Vec3 toArrow = arrow.position().subtract(player.getEyePosition());
        if (toArrow.lengthSqr() < 1.0E-6D || look.dot(toArrow.normalize()) <= 0.0D) {
            return false;
        }
        Vec3 originalVelocity = arrow.getDeltaMovement();
        double originalSpeed = originalVelocity.length();
        if (originalSpeed <= 1.0E-6D) {
            return false;
        }

        arrow.getPersistentData().putBoolean(DEFLECTED_KEY, true);
        arrow.setOwner(player);
        arrow.setBaseDamage(Math.max(0.0D, readArrowBaseDamage(arrow) * 0.80D));
        Vec3 reflectedVelocity = look.normalize().scale(originalSpeed * 0.90D);
        Vec3 launchPosition = player.getEyePosition().add(look.normalize().scale(0.75D));
        arrow.setPos(launchPosition.x, launchPosition.y, launchPosition.z);
        arrow.setDeltaMovement(reflectedVelocity);
        arrow.syncVelocity = true;

        // Cancelling the impact prevents the original projectile from applying
        // damage while allowing the same arrow entity to continue flying.
        event.setCanceled(true);
        InteractionHand hand = player.getUsedItemHand();
        if (shield != player.getUseItem()) {
            hand = player.getMainHandItem() == shield ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND;
        }
        if (!player.isCreative() && shield.isDamageableItem()) {
            shield.hurtAndBreak(1, player, hand);
        }
        int particles = 4 + player.getRandom().nextInt(5);
        level.sendParticles(ParticleTypes.CRIT, player.getX() + look.x * 0.65D,
                player.getEyeY() - 0.25D + look.y * 0.35D, player.getZ() + look.z * 0.65D,
                particles, 0.18D, 0.18D, 0.18D, 0.08D);
        level.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(),
                SoundSource.PLAYERS, 0.8F, 1.35F);
        return true;
    }

    private static Field findArrowBaseDamageField() {
        try {
            Field field = AbstractArrow.class.getDeclaredField("baseDamage");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    private static double readArrowBaseDamage(AbstractArrow arrow) {
        if (ARROW_BASE_DAMAGE_FIELD != null) {
            try {
                return ARROW_BASE_DAMAGE_FIELD.getDouble(arrow);
            } catch (IllegalAccessException ignored) {
                // Fall through to vanilla's default when access is unavailable.
            }
        }
        return 2.0D;
    }

    private void acquireTrackingTarget(AbstractArrow arrow, LivingEntity shooter, int enchantmentLevel,
            ServerLevel level) {
        Vec3 motion = arrow.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }
        Vec3 initial = motion.normalize();
        double range = switch (enchantmentLevel) {
            case 1 -> 24.0D;
            case 2 -> 48.0D;
            case 3 -> 72.0D;
            default -> 72.0D;
        };
        double lockAngle = switch (enchantmentLevel) {
            case 1 -> 25.0D;
            case 2 -> 35.0D;
            default -> 50.0D;
        };
        double minDot = Math.cos(lockAngle * Mth.DEG_TO_RAD);
        Vec3 origin = arrow.position();
        LivingEntity selected = level.getEntitiesOfClass(LivingEntity.class,
                        shooter.getBoundingBox().inflate(range), target -> isTrackingCandidate(shooter, target))
                .stream()
                .filter(target -> {
                    Vec3 targetPoint = target.getPosition(1.0F).add(0.0D, target.getBbHeight() * 0.60D, 0.0D);
                    Vec3 toTarget = targetPoint.subtract(origin);
                    return toTarget.lengthSqr() > 1.0E-6D && toTarget.lengthSqr() <= range * range
                            && initial.dot(toTarget.normalize()) >= minDot
                            && hasTrackingLineOfSight(level, origin, targetPoint, arrow);
                })
                .min(Comparator.comparingDouble(target -> {
                    Vec3 toTarget = target.getPosition(1.0F).add(0.0D, target.getBbHeight() * 0.60D, 0.0D)
                            .subtract(origin).normalize();
                    double anglePenalty = 1.0D - initial.dot(toTarget);
                    return anglePenalty * 100.0D + target.distanceToSqr(origin);
                }))
                .orElse(null);
        if (selected != null) {
            trackingStates.put(arrow.getUUID(),
                    new TrackingState(shooter.getUUID(), enchantmentLevel, initial, level.dimension(), selected.getUUID(),
                            selected.getId()));
            sendTrackingIndicator(level, shooter, selected.getId(), true);
            level.sendParticles(ParticleTypes.ENCHANT, arrow.getX(), arrow.getY(), arrow.getZ(),
                    2 + enchantmentLevel, 0.10D, 0.10D, 0.10D, 0.02D);
            level.sendParticles(ParticleTypes.END_ROD, selected.getX(), selected.getY() + selected.getBbHeight() * 0.6D,
                    selected.getZ(), 4, 0.25D, selected.getBbHeight() * 0.35D, 0.25D, 0.01D);
        }
    }

    private void clearTrackingState(UUID arrowId, ServerLevel level) {
        TrackingState state = trackingStates.remove(arrowId);
        if (state == null) {
            return;
        }
        Entity owner = level.getEntity(state.owner);
        if (owner instanceof ServerPlayer shooter) {
            shooter.connection.send(new TrackingTargetPayload(state.targetEntityId, false));
        }
    }

    private static void sendTrackingIndicator(ServerLevel level, LivingEntity shooter, int targetEntityId,
            boolean glowing) {
        if (shooter instanceof ServerPlayer player) {
            player.connection.send(new TrackingTargetPayload(targetEntityId, glowing));
        }
    }

    private static boolean isTrackingCandidate(LivingEntity shooter, LivingEntity target) {
        if (target == shooter || !target.isAlive() || target.isSpectator() || target instanceof ArmorStand
                || !target.isPickable() || shooter.isAlliedTo(target)) {
            return false;
        }
        if (target instanceof Player playerTarget && shooter instanceof Player playerShooter
                && !playerShooter.canHarmPlayer(playerTarget)) {
            return false;
        }
        return true;
    }

    private static boolean hasTrackingLineOfSight(ServerLevel level, Vec3 from, Vec3 to, Entity ignored) {
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, ignored));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < 0.01D;
    }

    private void tickTracking(AbstractArrow arrow, ServerLevel level) {
        TrackingState state = trackingStates.get(arrow.getUUID());
        if (state == null) {
            return;
        }
        if (!state.active || !state.dimension.equals(level.dimension()) || arrow.isRemoved()
                || state.ticks >= 40) {
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        state.ticks++;
        Entity ownerEntity = level.getEntity(state.owner);
        Entity targetEntity = level.getEntity(state.target);
        if (!(ownerEntity instanceof LivingEntity owner) || !(targetEntity instanceof LivingEntity target)
                || !target.isAlive() || !isTrackingCandidate(owner, target)) {
            state.active = false;
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        Vec3 currentVelocity = arrow.getDeltaMovement();
        double speed = currentVelocity.length();
        if (speed < 1.0E-6D) {
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        Vec3 currentDirection = currentVelocity.scale(1.0D / speed);
        Vec3 targetPoint = target.getPosition(1.0F).add(0.0D, target.getBbHeight() * 0.60D, 0.0D);
        Vec3 desired = targetPoint.subtract(arrow.position());
        if (desired.lengthSqr() < 1.0E-6D || !hasTrackingLineOfSight(level, arrow.position(), targetPoint, arrow)) {
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        double initialLimit = Math.cos(70.0D * Mth.DEG_TO_RAD);
        if (state.initialDirection.dot(desired.normalize()) < initialLimit) {
            clearTrackingState(arrow.getUUID(), level);
            return;
        }
        double maxTurn = switch (state.level) {
            case 1 -> 3.0D;
            case 2 -> 5.0D;
            default -> 7.0D;
        } * Mth.DEG_TO_RAD;
        Vec3 desiredDirection = desired.normalize();
        double angle = Math.acos(Mth.clamp(currentDirection.dot(desiredDirection), -1.0D, 1.0D));
        double ratio = angle <= maxTurn ? 1.0D : maxTurn / angle;
        Vec3 adjusted = currentDirection.scale(1.0D - ratio).add(desiredDirection.scale(ratio)).normalize();
        arrow.setDeltaMovement(adjusted.scale(speed));
        arrow.syncVelocity = true;
        if (arrow.tickCount % 5 == 0) {
            level.sendParticles(ParticleTypes.ENCHANT, arrow.getX(), arrow.getY(), arrow.getZ(),
                    1, 0.03D, 0.03D, 0.03D, 0.0D);
        }
    }

    private void armArrowRain(AbstractArrow arrow, ProjectileImpactEvent event, ServerLevel level) {
        if (arrow.getPersistentData().getBoolean(ARROW_RAIN_PROJECTILE_KEY).orElse(false)) {
            return;
        }
        ArrowRainState state = arrowRainStates.get(arrow.getUUID());
        if (state == null || state.armed || !(arrow.getOwner() instanceof Player)) {
            return;
        }
        Vec3 impact = event.getRayTraceResult().getLocation();
        state.center = impact;
        if (event.getRayTraceResult() instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity target) {
            state.trackingTarget = target.getUUID();
            state.center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        }
        long now = level.getGameTime();
        state.warningUntil = now + 10L;
        state.expiresAt = state.warningUntil + arrowRainDuration(state.level);
        state.nextSpawnAt = state.warningUntil;
        state.armed = true;
        level.sendParticles(ParticleTypes.ENCHANT, state.center.x, state.center.y + 0.15D, state.center.z,
                8 + state.level * 3, arrowRainRadius(state.level), 0.05D, arrowRainRadius(state.level), 0.01D);
        level.playSound(null, BlockPos.containing(state.center), SoundEvents.ENCHANTMENT_TABLE_USE,
                SoundSource.PLAYERS, 0.35F, 0.7F + state.level * 0.08F);
    }

    private void tickArrowRainStates(ServerLevel level) {
        long now = level.getGameTime();
        for (var entry : new ArrayList<>(arrowRainStates.entrySet())) {
            ArrowRainState state = entry.getValue();
            if (!state.dimension.equals(level.dimension())) {
                continue;
            }
            Entity ownerEntity = level.getEntity(state.owner);
            if (!(ownerEntity instanceof Player owner)) {
                arrowRainStates.remove(entry.getKey());
                continue;
            }
            if (!state.armed) {
                if (level.getEntity(entry.getKey()) == null) {
                    arrowRainStates.remove(entry.getKey());
                }
                continue;
            }
            if (now >= state.expiresAt) {
                arrowRainStates.remove(entry.getKey());
                continue;
            }
            if (state.trackingTarget != null && now < state.warningUntil) {
                Entity target = level.getEntity(state.trackingTarget);
                if (target != null && !target.isRemoved()) {
                    state.center = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
                }
            }
            if (!state.started && now >= state.warningUntil) {
                state.started = true;
                state.nextSpawnAt = now;
                level.playSound(null, BlockPos.containing(state.center), SoundEvents.ARROW_SHOOT,
                        SoundSource.PLAYERS, 0.45F, 0.65F);
            }
            if (!state.started || now < state.nextSpawnAt) {
                continue;
            }
            int total = arrowRainCount(state.level);
            int remaining = total - state.spawned;
            int ticksLeft = (int) Math.max(1L, state.expiresAt - now);
            int spawnNow = Math.max(1, (int) Math.ceil((double) remaining / ticksLeft));
            for (int i = 0; i < spawnNow && state.spawned < total; i++) {
                spawnArrowRainArrow(level, owner, state);
                state.spawned++;
            }
            state.nextSpawnAt = now + Math.max(1L, arrowRainDuration(state.level) / total);
            if (state.spawned >= total && now + 1L >= state.expiresAt) {
                arrowRainStates.remove(entry.getKey());
            }
        }
    }

    private void spawnArrowRainArrow(ServerLevel level, Player owner, ArrowRainState state) {
        double radius = arrowRainRadius(state.level);
        double angle = owner.getRandom().nextDouble() * Math.PI * 2.0D;
        double distance = radius * Math.sqrt(owner.getRandom().nextDouble());
        double x = state.center.x + Math.cos(angle) * distance;
        double z = state.center.z + Math.sin(angle) * distance;
        double spawnY = findArrowRainSpawnY(level, x, state.center.y, z, owner);
        if (Double.isNaN(spawnY)) {
            return;
        }
        ArrowItem arrowItem = (ArrowItem) Items.ARROW;
        AbstractArrow rainArrow = arrowItem.createArrow(level, new ItemStack(Items.ARROW), owner, state.weapon);
        rainArrow.setPos(x, spawnY, z);
        rainArrow.shoot((owner.getRandom().nextDouble() - 0.5D) * 0.08D, -1.0D,
                (owner.getRandom().nextDouble() - 0.5D) * 0.08D, 1.5F + owner.getRandom().nextFloat() * 0.3F, 0.0F);
        double multiplier = switch (state.level) {
            case 1 -> 0.18D;
            case 2 -> 0.20D;
            default -> 0.22D;
        };
        rainArrow.setBaseDamage(state.damageBase * multiplier);
        rainArrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        rainArrow.getPersistentData().putBoolean(ARROW_RAIN_PROJECTILE_KEY, true);
        level.addFreshEntity(rainArrow);
    }

    private static double findArrowRainSpawnY(ServerLevel level, double x, double centerY, double z, Player owner) {
        int baseY = Mth.floor(centerY + 0.5D);
        for (int y = baseY; y <= baseY + 8; y++) {
            BlockPos pos = BlockPos.containing(x, y, z);
            if (!level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()) {
                double candidate = y - 0.2D;
                BlockPos air = BlockPos.containing(x, candidate, z);
                if (level.getBlockState(air).getCollisionShape(level, air).isEmpty()) {
                    return candidate;
                }
                return Double.NaN;
            }
        }
        return centerY + 6.0D + owner.getRandom().nextDouble() * 2.0D;
    }

    private static int arrowRainCount(int level) {
        return switch (level) {
            case 1 -> 6;
            case 2 -> 9;
            default -> 12;
        };
    }

    private static int arrowRainDuration(int level) {
        return switch (level) {
            case 1 -> 30;
            case 2 -> 36;
            default -> 40;
        };
    }

    private static double arrowRainRadius(int level) {
        return switch (level) {
            case 1 -> 2.25D;
            case 2 -> 2.75D;
            default -> 3.25D;
        };
    }

    private void armExplosiveArrow(AbstractArrow arrow, ProjectileImpactEvent event, ServerLevel level) {
        ExplosiveArrowState state = explosiveArrows.get(arrow.getUUID());
        if (state == null || state.armed) {
            return;
        }
        Vec3 impact = event.getRayTraceResult().getLocation();
        state.lastPosition = impact;
        if (event.getRayTraceResult() instanceof EntityHitResult entityHit
                && entityHit.getEntity() instanceof LivingEntity target) {
            state.attachedEntity = target.getUUID();
            state.lastPosition = target.position().add(0.0D, target.getBbHeight() * 0.5D, 0.0D);
        }
        state.armed = true;
        state.detonationTick = level.getGameTime() + 20L;
        level.sendParticles(ParticleTypes.FLAME, state.lastPosition.x, state.lastPosition.y,
                state.lastPosition.z, 2, 0.10D, 0.10D, 0.10D, 0.01D);
        level.playSound(null, arrow.blockPosition(), SoundEvents.FLINTANDSTEEL_USE,
                SoundSource.PLAYERS, 0.35F, 1.4F);
    }

    private void tickExplosiveArrow(AbstractArrow arrow, ServerLevel level) {
        ExplosiveArrowState state = explosiveArrows.get(arrow.getUUID());
        if (state == null || !state.armed || state.detonated || arrow.isRemoved()) {
            return;
        }

        tickExplosiveState(state, level, arrow);
        if (state.detonated) {
            explosiveArrows.remove(arrow.getUUID());
        }
    }

    /**
     * Entity-hit arrows can be discarded by vanilla immediately after impact.
     * Keep their armed state ticking so the delayed explosion still occurs.
     */
    private void tickDetachedExplosiveStates(ServerLevel level) {
        long now = level.getGameTime();
        for (var entry : new ArrayList<>(explosiveArrows.entrySet())) {
            UUID arrowId = entry.getKey();
            ExplosiveArrowState state = entry.getValue();
            if (state.detonated) {
                explosiveArrows.remove(arrowId);
                continue;
            }
            Entity entity = level.getEntity(arrowId);
            if (entity instanceof AbstractArrow arrow && !arrow.isRemoved()) {
                continue;
            }
            if (!state.armed) {
                if (entity == null) {
                    explosiveArrows.remove(arrowId);
                }
                continue;
            }
            tickExplosiveState(state, level, null);
            if (state.detonated || now >= state.detonationTick) {
                explosiveArrows.remove(arrowId);
            }
        }
    }

    private static boolean gameTimeEquals(Long previous, long current) {
        return previous != null && previous == current;
    }

    private void tickExplosiveState(ExplosiveArrowState state, ServerLevel level, AbstractArrow arrow) {
        if (state.detonated) {
            return;
        }

        Entity attached = state.attachedEntity == null ? null : level.getEntity(state.attachedEntity);
        if (attached != null && !attached.isRemoved()) {
            state.lastPosition = attached.position().add(0.0D, attached.getBbHeight() * 0.5D, 0.0D);
        } else if (state.lastPosition == null) {
            state.lastPosition = arrow != null ? arrow.position() : Vec3.ZERO;
        }

        long remaining = state.detonationTick - level.getGameTime();
        long particleTick = arrow != null ? arrow.tickCount : level.getGameTime();
        if (remaining > 0L && particleTick % (remaining <= 4L ? 1 : remaining <= 10L ? 2 : 5) == 0) {
            Vec3 position = state.lastPosition;
            level.sendParticles(ParticleTypes.FLAME, position.x, position.y, position.z,
                    remaining <= 4L ? 2 : 1, 0.10D, 0.10D, 0.10D, 0.01D);
        }
        if (remaining <= 0L) {
            state.detonated = true;
            detonateExplosiveArrow(state, level);
            if (arrow != null) {
                arrow.discard();
            }
        }
    }

    private void detonateExplosiveArrow(ExplosiveArrowState state, ServerLevel level) {
        Vec3 center = state.lastPosition == null ? Vec3.ZERO : state.lastPosition;
        double radius = switch (state.level) {
            case 1 -> 2.0D;
            case 2 -> 2.5D;
            default -> 3.0D;
        };
        double explosionPercent = switch (state.level) {
            case 1 -> 0.25D;
            case 2 -> 0.45D;
            default -> 0.65D;
        };
        double knockbackStrength = switch (state.level) {
            case 1 -> 0.25D;
            case 2 -> 0.35D;
            default -> 0.45D;
        };
        Entity owner = level.getEntity(state.owner);
        AABB box = new AABB(center, center).inflate(radius);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, box,
                living -> living.isAlive() && !living.isSpectator() && living != owner)) {
            double distance = target.getBoundingBox().getCenter().distanceTo(center);
            if (distance > radius) {
                continue;
            }
            double distanceMultiplier = 1.0D - 0.65D * Mth.clamp(distance / radius, 0.0D, 1.0D);
            double targetMultiplier = target instanceof Player ? 0.50D : 1.0D;
            if (target.entityTags().contains(EXPLOSIVE_ARROW_BOSS_TAG)) {
                targetMultiplier *= 0.65D;
            }
            float damage = (float) Math.min(state.attackDamage * explosionPercent
                    * distanceMultiplier * targetMultiplier, Float.MAX_VALUE);
            if (damage > 0.0F) {
                DamageSource source = owner != null
                        ? level.damageSources().source(EXPLOSIVE_ARROW_DAMAGE_TYPE, owner)
                        : level.damageSources().source(EXPLOSIVE_ARROW_DAMAGE_TYPE);
                target.hurtServer(level, source, damage);
            }

            Vec3 offset = target.getBoundingBox().getCenter().subtract(center);
            Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
            if (horizontal.lengthSqr() < 1.0E-6D && owner != null) {
                horizontal = target.position().subtract(owner.position());
                horizontal = new Vec3(horizontal.x, 0.0D, horizontal.z);
            }
            if (horizontal.lengthSqr() > 1.0E-6D) {
                double resistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
                double directTargetScale = state.attachedEntity != null
                        && state.attachedEntity.equals(target.getUUID()) ? 0.5D : 1.0D;
                double push = knockbackStrength * distanceMultiplier * directTargetScale * (1.0D - resistance);
                Vec3 direction = horizontal.normalize();
                Vec3 current = target.getDeltaMovement();
                target.setDeltaMovement(current.x + direction.x * push,
                        current.y + Math.min(0.12D, push * 0.25D), current.z + direction.z * push);
                target.syncVelocity = true;
            }
        }
        level.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z,
                1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z,
                8 + state.level * 6, radius * 0.30D, radius * 0.25D, radius * 0.30D, 0.04D);
        level.playSound(null, net.minecraft.core.BlockPos.containing(center), SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.PLAYERS, 0.65F, 1.15F);
    }

    private void triggerHydroshock(ThrownTrident trident, ServerLevel level, Vec3 center,
            LivingEntity directTarget, int levelValue) {
        double radius = switch (levelValue) {
            case 1 -> 2.0D;
            case 2 -> 3.0D;
            default -> 4.0D;
        };
        double strength = switch (levelValue) {
            case 1 -> 0.35D;
            case 2 -> 0.50D;
            default -> 0.65D;
        };
        double vertical = switch (levelValue) {
            case 1 -> 0.08D;
            case 2 -> 0.12D;
            default -> 0.16D;
        };
        AABB box = new AABB(center, center).inflate(radius);
        level.getEntitiesOfClass(LivingEntity.class, box, living -> living.isAlive()
                        && !living.isSpectator() && living != directTarget)
                .forEach(living -> {
                    Vec3 offset = living.getBoundingBox().getCenter().subtract(center);
                    double horizontalLength = Math.sqrt(offset.x * offset.x + offset.z * offset.z);
                    double distance = offset.length();
                    if (distance > radius || horizontalLength < 1.0E-6D) {
                        return;
                    }
                    double resistance = Mth.clamp(living.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
                    double scale = 1.0D - resistance;
                    Vec3 direction = new Vec3(offset.x / horizontalLength, 0.0D, offset.z / horizontalLength);
                    Vec3 current = living.getDeltaMovement();
                    living.setDeltaMovement(current.x + direction.x * strength * scale,
                            current.y + vertical * scale, current.z + direction.z * strength * scale);
                    living.syncVelocity = true;
                });
        int particles = switch (levelValue) {
            case 1 -> 10;
            case 2 -> 17;
            default -> 25;
        };
        level.sendParticles(ParticleTypes.SPLASH, center.x, center.y, center.z,
                particles, radius * 0.35D, 0.20D, radius * 0.35D, 0.18D);
        level.sendParticles(ParticleTypes.BUBBLE, center.x, center.y, center.z,
                Math.max(4, particles / 2), radius * 0.25D, 0.20D, radius * 0.25D, 0.08D);
        int wavePoints = 10 + levelValue * 4;
        for (int ring = 1; ring <= 2; ring++) {
            double ringRadius = radius * ring / 2.0D;
            for (int point = 0; point < wavePoints; point++) {
                double angle = (Math.PI * 2.0D * point / wavePoints) + (ring == 2 ? Math.PI / wavePoints : 0.0D);
                double x = center.x + Math.cos(angle) * ringRadius;
                double z = center.z + Math.sin(angle) * ringRadius;
                level.sendParticles(ParticleTypes.SPLASH, x, center.y + 0.08D, z,
                        1, Math.cos(angle) * 0.03D, 0.04D, Math.sin(angle) * 0.03D, 0.02D);
            }
        }
        level.playSound(null, trident.blockPosition(), SoundEvents.GENERIC_SPLASH,
                SoundSource.PLAYERS, 0.7F, 0.85F + levelValue * 0.08F);
    }

    private static Vec3 rotateAroundY(Vec3 vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vector.x * cos - vector.z * sin, vector.y, vector.x * sin + vector.z * cos).normalize();
    }

    private static void applyOvercharge(AbstractArrow arrow, OverchargeCharge charge, ServerLevel level) {
        Vec3 velocity = arrow.getDeltaMovement();
        double speed = velocity.length();
        if (speed <= 1.0E-6D) {
            return;
        }
        // Keep the configured damage bonus independent from the velocity bonus:
        // this version exposes only a setter for arrow base damage, so use the
        // vanilla base value and compensate the speed-derived portion here.
        arrow.setBaseDamage(2.0D * charge.damageMultiplier() / charge.velocityMultiplier());
        Vec3 boostedVelocity = velocity.scale(charge.velocityMultiplier());
        arrow.setDeltaMovement(boostedVelocity);
        Vec3 direction = boostedVelocity.normalize();
        Vec3 origin = arrow.position().add(direction.scale(0.15D));
        int particles = (int) Math.max(2.0D, Math.round((charge.velocityMultiplier() - 1.0D) * 50.0D));
        level.sendParticles(ParticleTypes.END_ROD, origin.x, origin.y, origin.z,
                Math.min(8, particles), 0.08D, 0.08D, 0.08D, 0.02D);
    }

    private void releaseQueuedShot(Player player) {
        ArrayDeque<QueuedProjectile> queue = delayedCrossbowShots.get(player.getUUID());
        if (queue == null || queue.isEmpty()) {
            return;
        }
        QueuedProjectile next = queue.peek();
        if (player.level().getGameTime() < next.dueTick()) {
            return;
        }
        Projectile projectile = next.projectile();
        // A reconnect or dimension/player replacement creates a new Player
        // instance. Never let that session inherit shots queued by the old one.
        if (projectile.getOwner() != player || projectile.level() != player.level()) {
            clearQueuedCrossbowShots(player.getUUID());
            return;
        }
        queue.remove();
        double speed = projectile.getDeltaMovement().length();
        Vec3 direction = player.getViewVector(1.0F);
        projectile.setPos(player.getX(), player.getEyeY() - 0.1D, player.getZ());
        projectile.shoot(direction.x, direction.y, direction.z, (float) speed, 0.0F);
        if (player.level().addFreshEntity(projectile)) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.CROSSBOW_SHOOT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (queue.isEmpty()) {
            delayedCrossbowShots.remove(player.getUUID());
        }
    }

    private void finishFlyingHook(Player player, FlyingHookState state, FishingHook hook) {
        flyingHooks.remove(player.getUUID());
        ItemStack rod = player.getItemInHand(state.hand);
        if (state.durabilityCost > 0 && rod.is(Items.FISHING_ROD)
                && enchantmentLevel(player, rod, FLYING_HOOK_ID) > 0 && !player.isCreative()) {
            rod.hurtAndBreak(state.durabilityCost, player, state.hand);
            state.durabilityCost = 0;
        }
        hook.discard();
    }

    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUUID();
        FlyingHookState state = flyingHooks.get(playerId);
        if (state != null && player.level() instanceof ServerLevel currentLevel) {
            ServerLevel hookLevel = currentLevel.getServer().getLevel(state.dimension);
            Entity entity = hookLevel == null ? null : hookLevel.getEntity(state.hook);
            if (entity instanceof FishingHook hook) {
                finishFlyingHook(player, state, hook);
            } else {
                flyingHooks.remove(playerId);
            }
        }
        clearQueuedCrossbowShots(playerId);
        expansionShotTicks.remove(playerId);
    }

    private void clearQueuedCrossbowShots(UUID playerId) {
        ArrayDeque<QueuedProjectile> removed = delayedCrossbowShots.remove(playerId);
        if (removed != null) {
            removed.forEach(shot -> generatedExpansionArrows.remove(shot.projectile().getUUID()));
        }
    }

    private void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (event.getSource().is(DamageTypes.MACE_SMASH)
                && event.getSource().getDirectEntity() instanceof ServerPlayer attacker
                && event.getEntity() instanceof LivingEntity target
                && attacker.getMainHandItem().getItem() instanceof MaceItem
                && event.getAmount() > 0.0F) {
            ItemStack weapon = attacker.getMainHandItem();
            int fallingStarLevel = enchantmentLevel(attacker, weapon, FALLING_STAR_ID);
            if (fallingStarLevel > 0 && attacker.fallDistance >= 4.0F) {
                // Replace stale or cancelled attack context before the vanilla mace handler consumes fallDistance.
                fallingStarHits.put(attacker.getUUID(),
                        new FallingStarHit(target.getUUID(), attacker.level().getGameTime(),
                                fallingStarLevel, (float) attacker.fallDistance));
            }
            int shockRingLevel = enchantmentLevel(attacker, weapon, SHOCK_RING_ID);
            if (shockRingLevel > 0) {
                shockRingHits.put(attacker.getUUID(),
                        new ShockRingHit(target.getUUID(), attacker.level().getGameTime(),
                                shockRingLevel, event.getAmount()));
            }
        }
        if (event.getSource().getDirectEntity() instanceof Player attacker
                && event.getEntity() != attacker) {
            PendingComboAttack pending = pendingComboAttacks.get(attacker.getUUID());
            ComboState state = comboStates.get(attacker.getUUID());
            long now = attacker.level().getGameTime();
            if (pending != null && state != null && pending.tick() == now
                    && pending.target().equals(event.getEntity().getUUID())
                    && pending.weapon() == attacker.getMainHandItem()
                    && state.weapon == attacker.getMainHandItem()) {
                event.setAmount(event.getAmount()
                        * (float) comboDamageMultiplier(pending.level(), pending.stage()));
            }
        }
        if (event.getSource().getDirectEntity() instanceof ThrownTrident trident
                && trident.level() instanceof ServerLevel level
                && isThrownTridentFlightState(trident)) {
            captureTridentState(trident, level);
            SurgeState state = surgeStates.get(trident.getUUID());
            if (state != null && state.active && state.outbound && state.surgeLevel > 0) {
                event.setAmount(event.getAmount() * surgeDamageMultiplier(state.surgeLevel));
            }
        }
        if (!event.getSource().is(EXPLOSIVE_ARROW_DAMAGE_TYPE)
                && event.getSource().getDirectEntity() instanceof Player attacker
                && attacker.getMainHandItem().is(ItemTags.AXES)) {
            int execution = enchantmentLevel(attacker, attacker.getMainHandItem(), EXECUTION_ID);
            LivingEntity target = event.getEntity();
            if (execution > 0 && target.getHealth() + target.getAbsorptionAmount() <= target.getMaxHealth() * 0.30F) {
                float multiplier = switch (execution) {
                    case 1 -> 1.15F;
                    case 2 -> 1.25F;
                    default -> 1.35F;
                };
                event.setAmount(event.getAmount() * multiplier);
            }
        }
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)
                || !(arrow.getWeaponItem().getItem() instanceof CrossbowItem)
                || !(event.getEntity().level() instanceof ServerLevel)) {
            return;
        }
        Entity owner = arrow.getOwner();
        if (!(owner instanceof LivingEntity shooter)) {
            return;
        }
        int level = enchantmentLevel(shooter, arrow.getWeaponItem(), ARMOR_PIERCING_ID);
        if (level > 0) {
            float amount = event.getAmount();
            float armor = event.getEntity().getArmorValue();
            float toughness = (float) event.getEntity().getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            float normal = CombatRules.getDamageAfterAbsorb(event.getEntity(), amount, event.getSource(), armor, toughness);
            float reducedArmor = armor * (1.0F - level * 0.10F);
            float pierced = CombatRules.getDamageAfterAbsorb(event.getEntity(), amount, event.getSource(), reducedArmor, toughness);
            if (normal > 0.0F && pierced > normal) {
                event.setAmount(amount * pierced / normal);
            }
        }
        int cluster = enchantmentLevel(shooter, arrow.getWeaponItem(), CLUSTER_ID);
        int expansion = enchantmentLevel(shooter, arrow.getWeaponItem(), EXPANSION_ID);
        if (cluster > 0 || expansion > 0) {
            // Extra arrows from cluster and expansion shots can arrive before
            // vanilla's ten-tick hurt cooldown expires.
            event.getEntity().setInvulnerableTime(0);
            event.getEntity().hurtTime = 0;
        }
    }

    private void onLivingDamagePost(LivingDamageEvent.Post event) {
        triggerFallingStar(event);
        triggerShockRing(event);
        if (event.getHealthDamage() > 0.0F
                && event.getSource().getDirectEntity() instanceof ThrownTrident trident
                && trident.level() instanceof ServerLevel level
                && isThrownTridentFlightState(trident)) {
            captureTridentState(trident, level);
            SurgeState state = surgeStates.get(trident.getUUID());
            if (state != null && state.active && state.outbound && state.surgeLevel > 0) {
                Vec3 motion = trident.getDeltaMovement();
                Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
                if (horizontal.lengthSqr() > 1.0E-6D) {
                    double knockback = switch (state.surgeLevel) {
                        case 1 -> 0.10D;
                        case 2 -> 0.20D;
                        default -> 0.30D;
                    };
                    Vec3 push = horizontal.normalize().scale(knockback);
                    LivingEntity target = event.getEntity();
                    Vec3 current = target.getDeltaMovement();
                    target.setDeltaMovement(current.x + push.x, current.y, current.z + push.z);
                    target.syncVelocity = true;
                }
                level.sendParticles(ParticleTypes.SPLASH, trident.getX(), trident.getY(), trident.getZ(),
                        4 + state.surgeLevel * 4, 0.25D, 0.25D, 0.25D, 0.10D);
                level.playSound(null, trident.blockPosition(), SoundEvents.GENERIC_SPLASH,
                        SoundSource.PLAYERS, 0.55F, 1.0F + state.surgeLevel * 0.05F);
            }
            if (state != null && state.outbound && state.tetherLevel > 0
                    && !state.tetherTriggered && event.getEntity().isAlive()
                    && trident.getOwner() instanceof LivingEntity shooter) {
                state.tetherTriggered = true;
                applyTether(event.getEntity(), shooter, level, state.tetherLevel);
                level.playSound(null, event.getEntity().blockPosition(), SoundEvents.PLAYER_SPLASH,
                        SoundSource.PLAYERS, 0.45F, 1.35F);
            }
        }
        if (event.getEntity() instanceof ServerPlayer player && player.getHealth() > 0.0F) {
            tryTriggerHolyLight(player);
        }
        if (event.getSource().is(EXPLOSIVE_ARROW_DAMAGE_TYPE)
                || event.getSource().is(SHOCK_RING_DAMAGE_TYPE)) {
            return;
        }
        applyFrostbite(event);
        recordComboStrikeHit(event);
        recordBattleMomentumHit(event);
        if (event.getHealthDamage() <= 0.0F || bleedingDamage.contains(event.getEntity().getUUID())) {
            return;
        }
        if (!(event.getSource().getDirectEntity() instanceof Player attacker)) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!weapon.is(ItemTags.SPEARS)) {
            weapon = attacker.getOffhandItem();
        }
        int level = enchantmentLevel(attacker, weapon, BLEEDING_ID);
        if (level <= 0) {
            return;
        }
        MobEffectInstance existing = event.getEntity().getEffect(BLEEDING_EFFECT);
        // Vanilla effect updates allow hidden lower-level effects. Prevent that so a
        // lower-level hit never replaces or queues behind a stronger bleeding effect.
        if (existing != null && existing.getAmplifier() > level - 1) {
            return;
        }
        int duration = 60 + level * 20;
        event.getEntity().addEffect(new MobEffectInstance(BLEEDING_EFFECT, duration, level - 1,
                false, true, true));
        UUID targetId = event.getEntity().getUUID();
        bleedingAttackers.put(targetId, attacker.getUUID());
        bleedingNextDamage.put(targetId, event.getEntity().level().getGameTime() + 20L);
        event.getEntity().getPersistentData().putString(BLEEDING_ATTACKER_KEY, attacker.getUUID().toString());
    }

    private void triggerFallingStar(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F
                || !event.getSource().is(DamageTypes.MACE_SMASH)
                || !(event.getSource().getDirectEntity() instanceof ServerPlayer attacker)) {
            return;
        }
        FallingStarHit hit = fallingStarHits.remove(attacker.getUUID());
        if (hit == null || hit.tick() != attacker.level().getGameTime()
                || !hit.target().equals(event.getEntity().getUUID())
                || hit.fallDistance() < 4.0F) {
            return;
        }
        LivingEntity impactTarget = event.getEntity();
        double radius = switch (hit.level()) {
            case 1 -> 3.0D;
            case 2 -> 4.0D;
            default -> 5.0D;
        };
        int duration = hit.level() == 1 ? 40 : 60;
        int amplifier = hit.level() == 3 ? 1 : 0;
        AABB area = impactTarget.getBoundingBox().inflate(radius);
        for (LivingEntity target : attacker.level().getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && !living.isSpectator()
                        && living != attacker && !attacker.isAlliedTo(living)
                        && living.getBoundingBox().getCenter().distanceTo(impactTarget.position()) <= radius)) {
            MobEffectInstance existing = target.getEffect(MobEffects.SLOWNESS);
            if (existing == null || existing.getAmplifier() < amplifier
                    || (existing.getAmplifier() == amplifier && existing.getDuration() < duration)) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amplifier,
                        false, true, true));
            }
        }
        Vec3 center = impactTarget.position();
        int particles = switch (hit.level()) {
            case 1 -> 10;
            case 2 -> 16;
            default -> 24;
        };
        attacker.level().sendParticles(ParticleTypes.END_ROD, center.x, center.y + 4.0D, center.z,
                particles, radius * 0.55D, 1.5D, radius * 0.55D, 0.30D);
        attacker.level().sendParticles(ParticleTypes.CRIT, center.x, center.y + 0.2D, center.z,
                Math.max(2, particles / 4), radius * 0.35D, 0.25D, radius * 0.35D, 0.12D);
        attacker.level().playSound(null, impactTarget.blockPosition(), SoundEvents.MACE_SMASH_AIR,
                SoundSource.PLAYERS, 0.45F, 1.25F);
    }

    private void triggerShockRing(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F
                || !event.getSource().is(DamageTypes.MACE_SMASH)
                || !(event.getSource().getDirectEntity() instanceof ServerPlayer attacker)
                || !(event.getEntity() instanceof LivingEntity target)) {
            return;
        }
        ShockRingHit hit = shockRingHits.remove(attacker.getUUID());
        if (hit == null || hit.tick() != attacker.level().getGameTime()
                || !hit.target().equals(target.getUUID()) || hit.baseDamage() <= 0.0F) {
            return;
        }
        shockRings.add(new ShockRingInstance(attacker.getUUID(), target.position(), hit.level(),
                hit.baseDamage(), attacker.level().dimension(), target.getUUID()));
        attacker.level().playSound(null, target.blockPosition(), SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 0.55F, 0.85F);
    }

    private void tickShockRings(ServerLevel level) {
        Iterator<ShockRingInstance> iterator = shockRings.iterator();
        while (iterator.hasNext()) {
            ShockRingInstance ring = iterator.next();
            if (!ring.dimension.equals(level.dimension())) {
                continue;
            }
            if (tickShockRing(ring, level)) {
                iterator.remove();
            }
        }
    }

    private boolean tickShockRing(ShockRingInstance ring, ServerLevel level) {
        ring.previousRadius = ring.currentRadius;
        ring.currentRadius = Math.min(ring.maxRadius, ring.currentRadius + ring.expansionSpeed);
        ring.age++;
        spawnShockRingParticles(ring, level);

        Entity ownerEntity = level.getServer().getPlayerList().getPlayer(ring.owner);
        double thickness = 0.65D;
        AABB area = new AABB(ring.center, ring.center)
                .inflate(ring.currentRadius + thickness, 1.75D, ring.currentRadius + thickness);
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, area,
                living -> living.isAlive() && !living.isSpectator()
                        && !ring.hitEntityUUIDs.contains(living.getUUID())
                        && canShockRingAffect(ownerEntity, ring.owner, living))) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            double dx = targetCenter.x - ring.center.x;
            double dz = targetCenter.z - ring.center.z;
            double horizontalDistance = Math.sqrt(dx * dx + dz * dz);
            if (horizontalDistance <= ring.previousRadius - thickness
                    || horizontalDistance > ring.currentRadius + thickness
                    || Math.abs(targetCenter.y - ring.center.y) > 1.75D) {
                continue;
            }
            ring.hitEntityUUIDs.add(target.getUUID());
            if (!hasShockRingLineOfSight(level, ring.center, target, ownerEntity)) {
                continue;
            }
            float damage = (float) Math.min(ring.baseDamage * shockRingDamageMultiplier(ring.level), Float.MAX_VALUE);
            if (damage <= 0.0F) {
                continue;
            }
            Holder<DamageType> shockRingType = level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                    .getOrThrow(SHOCK_RING_DAMAGE_TYPE);
            DamageSource source = new DamageSource(shockRingType, ownerEntity, ownerEntity, ring.center);
            boolean damaged = target.hurtServer(level, source, damage);
            if (target.isAlive()) {
                applyShockRingKnockback(target, ring, dx, dz, damaged ? 1.0D : 0.30D);
            }
        }
        return ring.currentRadius >= ring.maxRadius || ring.age >= 8;
    }

    private static boolean canShockRingAffect(Entity ownerEntity, UUID ownerId, LivingEntity target) {
        if (target.getUUID().equals(ownerId)) {
            return false;
        }
        if (ownerEntity instanceof LivingEntity livingOwner && livingOwner.isAlliedTo(target)) {
            return false;
        }
        return !(ownerEntity instanceof Player player && target instanceof Player other)
                || player.canHarmPlayer(other);
    }

    private static boolean hasShockRingLineOfSight(ServerLevel level, Vec3 center,
            LivingEntity target, Entity ownerEntity) {
        Vec3 from = center.add(0.0D, 0.20D, 0.0D);
        Vec3 to = target.getBoundingBox().getCenter();
        BlockHitResult hit = level.clip(new ClipContext(from, to, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, ownerEntity));
        return hit.getType() == HitResult.Type.MISS || hit.getLocation().distanceToSqr(to) < 0.01D;
    }

    private static double shockRingDamageMultiplier(int level) {
        return switch (level) {
            case 1 -> 0.12D;
            case 2 -> 0.16D;
            default -> 0.20D;
        };
    }

    private static void applyShockRingKnockback(LivingEntity target, ShockRingInstance ring,
            double dx, double dz, double effectScale) {
        double horizontalLength = Math.sqrt(dx * dx + dz * dz);
        if (horizontalLength <= 1.0E-6D) {
            return;
        }
        double resistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        double scale = ring.knockbackStrength * effectScale * (1.0D - resistance);
        Vec3 current = target.getDeltaMovement();
        target.setDeltaMovement(current.x + dx / horizontalLength * scale,
                current.y + 0.08D * effectScale * (1.0D - resistance),
                current.z + dz / horizontalLength * scale);
        target.syncVelocity = true;
    }

    private static void spawnShockRingParticles(ShockRingInstance ring, ServerLevel level) {
        double radius = Math.max(0.25D, ring.currentRadius);
        int count = Math.min(48, Math.max(6, (int) Math.ceil((Math.PI * 2.0D * radius) / 0.60D)));
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2.0D * i / count + ring.age * 0.08D;
            double x = ring.center.x + Math.cos(angle) * radius;
            double z = ring.center.z + Math.sin(angle) * radius;
            level.sendParticles(ParticleTypes.POOF, x, ring.center.y + 0.15D, z,
                    1, 0.06D, 0.04D, 0.06D, 0.01D);
            if ((i & 3) == 0) {
                level.sendParticles(ParticleTypes.CRIT, x, ring.center.y + 0.20D, z,
                        1, 0.03D, 0.03D, 0.03D, 0.01D);
            }
        }
    }

    private void applyFrostbite(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F || !(event.getSource().getDirectEntity() instanceof Player attacker)) {
            return;
        }
        PendingFrostbiteAttack pending = pendingFrostbiteAttacks.remove(attacker.getUUID());
        long now = attacker.level().getGameTime();
        if (pending == null || pending.tick() != now || !pending.target().equals(event.getEntity().getUUID())
                || pending.weapon() != attacker.getMainHandItem()) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        if (!(weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.SPEARS))) {
            return;
        }
        int level = enchantmentLevel(attacker, weapon, FROSTBITE_ID);
        if (level <= 0) {
            return;
        }
        LivingEntity target = event.getEntity();
        if (target.canFreeze()) {
            int amount = switch (level) {
                case 1 -> 30;
                case 2 -> 45;
                default -> 60;
            };
            target.setTicksFrozen(Math.min(target.getTicksRequiredToFreeze(), target.getTicksFrozen() + amount));
        }
        int duration = level == 1 ? 40 : 60;
        int amplifier = level == 3 ? 1 : 0;
        MobEffectInstance existing = target.getEffect(MobEffects.SLOWNESS);
        if (existing == null || existing.getAmplifier() <= amplifier || existing.getDuration() < duration) {
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, duration, amplifier,
                    false, true, true));
        }
        if (target.level() instanceof ServerLevel serverLevel) {
            int particles = switch (level) {
                case 1 -> 4;
                case 2 -> 6;
                default -> 9;
            };
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE, target.getX(), target.getY() + target.getBbHeight() * 0.6D,
                    target.getZ(), particles, 0.28D, 0.45D, 0.28D, 0.02D);
        }
    }

    private void recordBattleMomentumHit(LivingDamageEvent.Post event) {
        if (event.getHealthDamage() <= 0.0F || !(event.getSource().getDirectEntity() instanceof Player attacker)
                || event.getEntity() == attacker) {
            return;
        }
        PendingMeleeAttack pending = pendingMeleeAttacks.remove(attacker.getUUID());
        long now = attacker.level().getGameTime();
        if (pending == null || pending.tick() != now || !pending.target().equals(event.getEntity().getUUID())) {
            return;
        }
        ItemStack weapon = attacker.getMainHandItem();
        int level = enchantmentLevel(attacker, weapon, BATTLE_MOMENTUM_ID);
        if (level <= 0) {
            return;
        }
        BattleMomentumState state = battleMomentumStates.get(attacker.getUUID());
        if (state == null || !state.dimension.equals(attacker.level().dimension())) {
            state = new BattleMomentumState(0, level, now, attacker.level().dimension(), false);
            battleMomentumStates.put(attacker.getUUID(), state);
        }
        if (state.lastHitTick == now) {
            return;
        }
        state.lastHitTick = now;
        state.commandGranted = false;
        state.level = level;
        state.stacks = Math.min(battleMomentumMaxStacks(level), state.stacks + 1);
        state.expiresAt = now + 80L;
        boolean reachedMax = !state.wasAtMax && state.stacks >= battleMomentumMaxStacks(level);
        state.wasAtMax = state.wasAtMax || state.stacks >= battleMomentumMaxStacks(level);
        attacker.addEffect(new MobEffectInstance(BATTLE_MOMENTUM_EFFECT, 80, state.stacks - 1,
                false, false, true));
        updateBattleMomentumModifier(attacker, state, level);

        if (attacker.level() instanceof ServerLevel serverLevel) {
            Vec3 center = attacker.position().add(0.0D, attacker.getBbHeight() * 0.65D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.FLAME, center.x, center.y, center.z,
                    Math.min(6, 1 + state.stacks), 0.25D, 0.35D, 0.25D, 0.015D);
            if (reachedMax) {
                serverLevel.sendParticles(ParticleTypes.FIREWORK, center.x, center.y, center.z,
                        6, 0.25D, 0.35D, 0.25D, 0.03D);
            }
        }
    }

    private void tickBattleMomentum(Player player) {
        UUID id = player.getUUID();
        BattleMomentumState state = battleMomentumStates.get(id);
        MobEffectInstance commandEffect = player.getEffect(BATTLE_MOMENTUM_EFFECT);
        if (state == null && commandEffect != null) {
            int stacks = Mth.clamp(commandEffect.getAmplifier() + 1, 1, 5);
            state = new BattleMomentumState(stacks, 3,
                    player.level().getGameTime() + commandEffect.getDuration(),
                    player.level().dimension(), true);
            battleMomentumStates.put(id, state);
        }
        if (state == null) {
            removeBattleMomentumModifier(player);
            return;
        }
        long now = player.level().getGameTime();
        if (player.isDeadOrDying() || !state.dimension.equals(player.level().dimension())
                || now >= state.expiresAt || (state.commandGranted && commandEffect == null)) {
            clearBattleMomentum(player);
            return;
        }

        int currentLevel = enchantmentLevel(player, player.getMainHandItem(), BATTLE_MOMENTUM_ID);
        if (!state.commandGranted && currentLevel > 0) {
            int maxStacks = battleMomentumMaxStacks(currentLevel);
            if (state.stacks > maxStacks) {
                state.stacks = maxStacks;
                state.wasAtMax = true;
            }
            state.level = currentLevel;
        }
        if (!state.commandGranted && commandEffect == null && state.stacks > 0) {
            int duration = (int) Math.min(80L, Math.max(1L, state.expiresAt - now));
            player.addEffect(new MobEffectInstance(BATTLE_MOMENTUM_EFFECT, duration, state.stacks - 1,
                    false, false, true));
        }
        updateBattleMomentumModifier(player, state, currentLevel);
    }

    private void updateBattleMomentumModifier(Player player, BattleMomentumState state, int currentLevel) {
        var attribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute == null) {
            return;
        }
        boolean active = state.stacks > 0 && (state.commandGranted || currentLevel > 0);
        int effectiveLevel = state.commandGranted ? state.level : currentLevel;
        if (state.appliedStacks == state.stacks && state.appliedLevel == effectiveLevel
                && (active || state.appliedStacks < 0)) {
            return;
        }
        attribute.removeModifier(BATTLE_MOMENTUM_MODIFIER_ID);
        if (active) {
            double bonus = state.stacks * battleMomentumPerStack(effectiveLevel);
            attribute.addTransientModifier(new AttributeModifier(BATTLE_MOMENTUM_MODIFIER_ID, bonus,
                    AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            state.appliedStacks = state.stacks;
            state.appliedLevel = effectiveLevel;
        } else {
            state.appliedStacks = -1;
            state.appliedLevel = -1;
        }
    }

    private static void removeBattleMomentumModifier(Player player) {
        var attribute = player.getAttribute(Attributes.ATTACK_SPEED);
        if (attribute != null) {
            attribute.removeModifier(BATTLE_MOMENTUM_MODIFIER_ID);
        }
    }

    private void clearBattleMomentum(Player player) {
        battleMomentumStates.remove(player.getUUID());
        pendingMeleeAttacks.remove(player.getUUID());
        removeBattleMomentumModifier(player);
        player.removeEffect(BATTLE_MOMENTUM_EFFECT);
    }

    private void tickComboState(Player player) {
        ComboState state = comboStates.get(player.getUUID());
        if (state == null) {
            if (enchantmentLevel(player, player.getMainHandItem(), COMBO_STRIKE_ID) > 0) {
                sendComboOverlay(player, 0, false);
            } else {
                comboOverlayStates.remove(player.getUUID());
            }
            return;
        }
        ItemStack weapon = player.getMainHandItem();
        if (player.isDeadOrDying() || !state.dimension.equals(player.level().dimension())
                || state.weapon != weapon || !weapon.is(ItemTags.SWORDS)
                || enchantmentLevel(player, weapon, COMBO_STRIKE_ID) <= 0) {
            clearCombo(player);
        }
    }

    private void clearCombo(Player player) {
        clearCombo(player, true);
    }

    private void clearCombo(Player player, boolean clearOverlay) {
        UUID id = player.getUUID();
        boolean hadState = comboStates.remove(id) != null;
        pendingComboAttacks.remove(id);
        if (hadState && clearOverlay) {
            if (enchantmentLevel(player, player.getMainHandItem(), COMBO_STRIKE_ID) > 0) {
                sendComboOverlay(player, 0, false);
            } else {
                comboOverlayStates.remove(id);
                player.sendOverlayMessage(Component.empty());
            }
        }
    }

    private static double comboDamageMultiplier(int level, int stage) {
        if (stage <= 0) {
            return 1.0D;
        }
        if (stage == 1) {
            return switch (level) {
                case 1 -> 1.05D;
                case 2 -> 1.08D;
                default -> 1.10D;
            };
        }
        return switch (level) {
            case 1 -> 1.12D;
            case 2 -> 1.18D;
            default -> 1.25D;
        };
    }

    private void recordComboStrikeHit(LivingDamageEvent.Post event) {
        if (!(event.getSource().getDirectEntity() instanceof Player attacker)
                || event.getEntity() == attacker) {
            return;
        }
        UUID id = attacker.getUUID();
        PendingComboAttack pending = pendingComboAttacks.remove(id);
        ComboState state = comboStates.get(id);
        long now = attacker.level().getGameTime();
        if (pending == null || pending.tick() != now || state == null
                || !pending.target().equals(event.getEntity().getUUID())
                || pending.weapon() != attacker.getMainHandItem()
                || state.weapon != attacker.getMainHandItem()) {
            return;
        }
        // A finishing hit may kill the target, but it still needs to show the
        // third charge so the player sees that this attack was the finisher.
        if (event.getHealthDamage() <= 0.0F) {
            clearCombo(attacker);
            return;
        }
        int newStage = pending.stage() + 1;
        boolean finishingStrike = newStage >= 3;
        if (!event.getEntity().isAlive() && !finishingStrike) {
            clearCombo(attacker);
            return;
        }
        // The bar previews the next swing: after two hits the third (finisher)
        // is already fully charged, even though it has not been performed yet.
        if (!finishingStrike) {
            int displayedCharge = Math.min(2, newStage);
            sendComboOverlay(attacker, displayedCharge, displayedCharge >= 2);
        }
        if (attacker.level() instanceof ServerLevel level) {
            Vec3 center = event.getEntity().position().add(0.0D, event.getEntity().getBbHeight() * 0.55D, 0.0D);
            int particleCount = newStage >= 3 ? 8 : 3;
            level.sendParticles(ParticleTypes.CRIT, center.x, center.y, center.z,
                    particleCount, 0.28D, 0.35D, 0.28D, 0.05D);
            if (finishingStrike) {
                level.playSound(null, event.getEntity().blockPosition(), SoundEvents.PLAYER_ATTACK_CRIT,
                        SoundSource.PLAYERS, 0.45F, 1.05F);
            }
        }
        if (finishingStrike) {
            applyComboKnockback(attacker, event.getEntity());
            clearCombo(attacker);
        } else {
            state.stage = newStage;
            state.target = event.getEntity().getUUID();
        }
    }

    private static Component comboChargeOverlay(int stage, boolean finishingStrike) {
        int filled = Mth.clamp(stage, 0, 2);
        var message = Component.translatable("wsee.combo_strike.charge")
                .withStyle(ChatFormatting.WHITE);
        for (int index = 0; index < 2; index++) {
            ChatFormatting color = index >= filled
                    ? ChatFormatting.WHITE
                    : index == 0 ? ChatFormatting.YELLOW : ChatFormatting.RED;
            message.append(Component.literal(index < filled ? "■" : "□").withStyle(color));
        }
        return message;
    }

    private void sendComboOverlay(Player player, int stage, boolean finishingStrike) {
        int encoded = Mth.clamp(stage, 0, 2) | (finishingStrike ? 4 : 0);
        UUID id = player.getUUID();
        Integer previous = comboOverlayStates.put(id, encoded);
        if (previous == null || previous != encoded) {
            player.sendOverlayMessage(comboChargeOverlay(stage, finishingStrike));
        }
    }

    private static void applyComboKnockback(Player attacker, LivingEntity target) {
        Vec3 motion = target.getDeltaMovement();
        Vec3 horizontal = new Vec3(motion.x, 0.0D, motion.z);
        if (horizontal.lengthSqr() > 1.0E-8D) {
            Vec3 boosted = horizontal.scale(1.10D);
            target.setDeltaMovement(boosted.x, motion.y, boosted.z);
            target.syncVelocity = true;
        }
    }

    private static int battleMomentumMaxStacks(int level) {
        return switch (level) {
            case 1 -> 3;
            case 2 -> 4;
            default -> 5;
        };
    }

    private static double battleMomentumPerStack(int level) {
        return switch (level) {
            case 1 -> 0.04D;
            case 2 -> 0.05D;
            default -> 0.06D;
        };
    }

    private void tickForcedMarch(Player player) {
        UUID id = player.getUUID();
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        int level = enchantmentLevel(player, boots, FORCED_MARCH_ID);
        ForcedMarchState state = forcedMarchStates.get(id);
        if (level <= 0 || player.isDeadOrDying() || player.isSpectator()
                || player.isSwimming() || player.isPassenger() || player.isFallFlying()
                || player.getAbilities().flying) {
            if (state != null) {
                clearForcedMarch(player);
            } else {
                removeForcedMarchModifier(player);
            }
            return;
        }
        if (state == null || !state.dimension.equals(player.level().dimension())) {
            state = new ForcedMarchState(level, player.level().dimension());
            forcedMarchStates.put(id, state);
        }
        state.level = level;
        if (player.isSprinting()) {
            state.graceTicks = 0;
            state.sprintTicks = Math.min(state.sprintTicks + 1, 80);
            if (!state.active && state.sprintTicks >= forcedMarchRequiredTicks(level)) {
                state.active = true;
                state.activationPlayed = false;
            }
        } else {
            state.graceTicks++;
            if (state.graceTicks >= MARCH_GRACE_TICKS) {
                clearForcedMarch(player);
                return;
            }
        }
        updateForcedMarchModifier(player, state);
        if (!(player.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (state.active && !state.activationPlayed) {
            state.activationPlayed = true;
            Vec3 center = player.position().add(0.0D, 0.08D, 0.0D);
            serverLevel.sendParticles(ParticleTypes.CLOUD, center.x, center.y, center.z,
                    4 + level, 0.30D, 0.06D, 0.30D, 0.03D);
            serverLevel.sendParticles(ParticleTypes.END_ROD, center.x, center.y + 0.12D, center.z,
                    Math.max(1, level / 2), 0.20D, 0.08D, 0.20D, 0.01D);
        }
        if (state.active && player.isSprinting() && player.tickCount % 8 == 0) {
            Vec3 look = player.getLookAngle();
            Vec3 trail = player.position().add(-look.x * 0.45D, 0.12D, -look.z * 0.45D);
            serverLevel.sendParticles(ParticleTypes.CLOUD, trail.x, trail.y, trail.z,
                    level >= 4 ? 2 : 1, 0.12D, 0.08D, 0.12D, 0.02D);
        }
    }

    private void updateForcedMarchModifier(Player player, ForcedMarchState state) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        if (!state.active) {
            removeForcedMarchModifier(player);
            state.appliedLevel = -1;
            return;
        }
        if (state.appliedLevel == state.level) {
            return;
        }
        attribute.removeModifier(FORCED_MARCH_MODIFIER_ID);
        attribute.addTransientModifier(new AttributeModifier(FORCED_MARCH_MODIFIER_ID,
                forcedMarchSpeedBonus(state.level), AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        state.appliedLevel = state.level;
    }

    private static void removeForcedMarchModifier(Player player) {
        var attribute = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute != null) {
            attribute.removeModifier(FORCED_MARCH_MODIFIER_ID);
        }
    }

    private void tickFoundation(Player player) {
        var attribute = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (attribute == null) {
            return;
        }
        int level = enchantmentLevel(player, player.getItemBySlot(EquipmentSlot.LEGS), FOUNDATION_ID);
        if (level <= 0 || player.isDeadOrDying()) {
            attribute.removeModifier(FOUNDATION_MODIFIER_ID);
            return;
        }
        double bonus = foundationKnockbackBonus(level);
        var modifier = attribute.getModifier(FOUNDATION_MODIFIER_ID);
        if (modifier == null || modifier.amount() != bonus
                || modifier.operation() != AttributeModifier.Operation.ADD_VALUE) {
            attribute.removeModifier(FOUNDATION_MODIFIER_ID);
            attribute.addTransientModifier(new AttributeModifier(FOUNDATION_MODIFIER_ID, bonus,
                    AttributeModifier.Operation.ADD_VALUE));
        }
    }

    private static double foundationKnockbackBonus(int level) {
        return switch (level) {
            case 1 -> 0.15D;
            case 2 -> 0.25D;
            default -> 0.35D;
        };
    }

    private void clearForcedMarch(Player player) {
        forcedMarchStates.remove(player.getUUID());
        removeForcedMarchModifier(player);
    }

    private static int forcedMarchRequiredTicks(int level) {
        return switch (level) {
            case 1 -> 80;
            case 2 -> 70;
            case 3 -> 60;
            case 4 -> 50;
            default -> 40;
        };
    }

    private static double forcedMarchSpeedBonus(int level) {
        return switch (level) {
            case 1 -> 0.06D;
            case 2 -> 0.09D;
            case 3 -> 0.12D;
            case 4 -> 0.15D;
            default -> 0.18D;
        };
    }

    private void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof FishingHook hook && prepareFlyingHookTick(hook)) {
            event.setCanceled(true);
            return;
        }
        if (event.getEntity() instanceof ThrownTrident trident
                && trident.level() instanceof ServerLevel level) {
            tickSurge(trident, level);
        }
        if (event.getEntity() instanceof AbstractArrow arrow
                && arrow.level() instanceof ServerLevel level) {
            tickTracking(arrow, level);
            tickExplosiveArrow(arrow, level);
            if (arrow.getOwner() instanceof LivingEntity shooter) {
                OverchargeCharge charge = overchargeCharges.remove(shooter.getUUID());
                if (charge != null) {
                    applyOvercharge(arrow, charge, level);
                }
            }
            return;
        }
        if (!(event.getEntity() instanceof LivingEntity target)
                || !(target.level() instanceof ServerLevel level)) {
            return;
        }
        if (target instanceof ServerPlayer player) {
            tickHolyField(player, level);
        }
        MobEffectInstance effect = target.getEffect(BLEEDING_EFFECT);
        if (effect == null) {
            bleedingAttackers.remove(target.getUUID());
            bleedingNextDamage.remove(target.getUUID());
            target.getPersistentData().remove(BLEEDING_ATTACKER_KEY);
            return;
        }
        long now = level.getGameTime();
        if (now < bleedingNextDamage.getOrDefault(target.getUUID(), now + 20L)) {
            return;
        }
        UUID attackerId = bleedingAttackers.get(target.getUUID());
        if (attackerId == null) {
            String storedAttacker = target.getPersistentData().getStringOr(BLEEDING_ATTACKER_KEY, "");
            try {
                attackerId = storedAttacker.isEmpty() ? null : UUID.fromString(storedAttacker);
            } catch (IllegalArgumentException ignored) {
                attackerId = null;
            }
            if (attackerId != null) {
                bleedingAttackers.put(target.getUUID(), attackerId);
            }
        }
        Entity attacker = attackerId == null ? null : level.getServer().getPlayerList().getPlayer(attackerId);
        double bleedingPercent = switch (effect.getAmplifier()) {
            case 0 -> 0.005D;
            case 1 -> 0.0075D;
            default -> 0.01D;
        };
        float damage = percentMaxHealthDamage(target, bleedingPercent);
        bleedingDamage.add(target.getUUID());
        try {
            if (attacker instanceof Player player) {
                target.hurtServer(level, level.damageSources().source(BLEEDING_DAMAGE_TYPE, player), damage);
            } else {
                target.hurtServer(level, level.damageSources().source(BLEEDING_DAMAGE_TYPE), damage);
            }
        } finally {
            bleedingDamage.remove(target.getUUID());
        }
        bleedingNextDamage.put(target.getUUID(), now + 20L);
    }

    private boolean prepareFlyingHookTick(FishingHook hook) {
        if (!hook.isNoGravity()) {
            if (hook.level().isClientSide()) {
                clientFlyingHookAnchors.remove(hook.getUUID());
            }
            return false;
        }
        if (!(hook.getOwner() instanceof Player owner)) {
            return false;
        }
        if (hook.level().isClientSide()) {
            if (!isClientFlyingHook(hook, owner)) {
                clientFlyingHookAnchors.remove(hook.getUUID());
                return false;
            }
            ClientFlyingHookAnchor state = clientFlyingHookAnchors.get(hook.getUUID());
            if (state != null) {
                Vec3 anchor = resolveClientFishingAnchor(hook, state);
                if (anchor == null) {
                    clientFlyingHookAnchors.remove(hook.getUUID());
                } else {
                    hook.setPos(anchor);
                }
                hook.setDeltaMovement(Vec3.ZERO);
                return false;
            }
            HitResult hit = findFlyingHookImpact(hook, owner);
            if (isFlyingHookImpact(hook, hit)) {
                anchorClientFlyingHook(hook, hit);
            }
            return false;
        }
        FlyingHookState state = flyingHooks.get(owner.getUUID());
        if (state == null || !state.hook.equals(hook.getUUID())) {
            return false;
        }
        if (!state.pulling) {
            anchorFlyingHook(hook, state, findFlyingHookImpact(hook, owner));
        }
        if (!state.pulling) {
            return false;
        }
        Vec3 anchor = resolveFishingAnchor(hook, state);
        if (anchor == null) {
            finishFlyingHook(owner, state, hook);
            return true;
        }
        boolean moved = state.anchor == null || state.anchor.distanceToSqr(anchor) > 1.0E-8D;
        state.anchor = anchor;
        hook.setPos(anchor);
        hook.setDeltaMovement(Vec3.ZERO);
        hook.syncPosition = true;
        hook.syncVelocity = true;
        if (moved) {
            sendFlyingHookAnchor(hook, anchor);
        }
        return true;
    }

    private void onEntityTickPost(EntityTickEvent.Post event) {
        if (!(event.getEntity() instanceof FishingHook hook) || !hook.level().isClientSide()) {
            return;
        }
        ClientFlyingHookAnchor state = clientFlyingHookAnchors.get(hook.getUUID());
        if (state == null) {
            return;
        }
        Vec3 anchor = resolveClientFishingAnchor(hook, state);
        if (anchor == null) {
            clientFlyingHookAnchors.remove(hook.getUUID());
            return;
        }
        hook.setPos(anchor);
        hook.setDeltaMovement(Vec3.ZERO);
    }

    private static Vec3 resolveClientFishingAnchor(FishingHook hook, ClientFlyingHookAnchor state) {
        if (state.entity() != null) {
            Entity entity = state.entity();
            if (entity.isRemoved() || !entity.canInteractWithLevel() || entity.level() != hook.level()) {
                return null;
            }
            return entity.position().add(state.entityOffset() == null ? Vec3.ZERO : state.entityOffset());
        }
        if (state.block() != null
                && hook.level().getBlockState(state.block()).getShape(hook.level(), state.block()).isEmpty()) {
            return null;
        }
        return state.anchor();
    }

    private static boolean isClientFlyingHook(FishingHook hook, Player owner) {
        return hook.isNoGravity()
                && (enchantmentLevel(owner, owner.getMainHandItem(), FLYING_HOOK_ID) > 0
                || enchantmentLevel(owner, owner.getOffhandItem(), FLYING_HOOK_ID) > 0);
    }

    private static HitResult findFlyingHookImpact(FishingHook hook, Player owner) {
        return ProjectileUtil.getHitResultOnMoveVector(hook,
                target -> target != owner && !target.isRemoved() && !target.isSpectator()
                        && !owner.isPassengerOfSameVehicle(target),
                ClipContext.Block.OUTLINE);
    }

    private void tickSurge(ThrownTrident trident, ServerLevel level) {
        ItemStack weapon = tridentItemStack(trident);
        Entity owner = trident.getOwner();
        if (!(owner instanceof LivingEntity shooter)) {
            surgeStates.remove(trident.getUUID());
            return;
        }
        int surgeLevel = enchantmentLevel(shooter, weapon, SURGE_ID);
        int tetherLevel = enchantmentLevel(shooter, weapon, TETHER_ID);
        int reboundLevel = enchantmentLevel(shooter, weapon, REBOUND_ID);
        int hydroshockLevel = enchantmentLevel(shooter, weapon, HYDROSHOCK_ID);
        SurgeState state = surgeStates.get(trident.getUUID());
        if (state == null && surgeLevel <= 0 && tetherLevel <= 0 && reboundLevel <= 0 && hydroshockLevel <= 0) {
            return;
        }
        if (trident.isRemoved()) {
            surgeStates.remove(trident.getUUID());
            return;
        }
        UUID id = trident.getUUID();
        state = surgeStates.computeIfAbsent(id,
                ignored -> new SurgeState(surgeLevel, tetherLevel, reboundLevel, hydroshockLevel));
        // Never overwrite a captured enchantment with zero when vanilla briefly
        // returns an empty stack during projectile state transitions.
        if (surgeLevel > 0) state.surgeLevel = surgeLevel;
        if (tetherLevel > 0) state.tetherLevel = tetherLevel;
        if (reboundLevel > 0) state.reboundLevel = reboundLevel;
        if (hydroshockLevel > 0) state.hydroshockLevel = hydroshockLevel;
        if (state.outbound && trident.noPhysics) {
            state.outbound = false;
            state.active = false;
            state.waterFlightTicks = 0;
        }
        if (!state.outbound) {
            tickRebound(trident, shooter, state, level);
            return;
        }
        if (state.surgeLevel <= 0) {
            return;
        }
        long now = level.getGameTime();
        boolean inWater = tridentTouchesWater(trident, level);
        if (state.active) {
            if (inWater) {
                state.activeUntil = now + SURGE_HOLD_TICKS;
            } else if (now >= state.activeUntil) {
                state.active = false;
                state.waterFlightTicks = 0;
            }
        } else if (inWater) {
            advanceSurgeWaterState(trident, level, state, true);
        } else {
            state.waterFlightTicks = 0;
        }
        if (state.active && trident.tickCount % 2 == 0) {
            int count = state.surgeLevel == 1 ? 1 : 1 + (state.surgeLevel / 3);
            level.sendParticles(ParticleTypes.BUBBLE, trident.getX(), trident.getY(), trident.getZ(),
                    count, 0.12D, 0.12D, 0.12D, 0.03D);
        } else if (!state.active && inWater && trident.tickCount % Math.max(2, 6 - state.surgeLevel) == 0) {
            level.sendParticles(ParticleTypes.BUBBLE, trident.getX(), trident.getY(), trident.getZ(),
                    state.surgeLevel, 0.10D, 0.10D, 0.10D, 0.02D);
        }
    }

    private static boolean isThrownTridentFlightState(ThrownTrident trident) {
        // noPhysics is set by vanilla only after a hit, when Loyalty begins
        // the return path. A melee trident never reaches this entity path.
        return !trident.isRemoved() && !trident.noPhysics;
    }

    private static boolean tridentTouchesWater(ThrownTrident trident, ServerLevel level) {
        if (trident.isInWater()) {
            return true;
        }
        // During the first tick after entering water, the entity fluid flag
        // can still be stale. Check the small voxel volume occupied by the
        // trident so a boundary crossing is observed reliably.
        AABB box = trident.getBoundingBox().inflate(0.05D);
        BlockPos min = BlockPos.containing(box.minX, box.minY, box.minZ);
        BlockPos max = BlockPos.containing(box.maxX, box.maxY, box.maxZ);
        for (int x = min.getX(); x <= max.getX(); x++) {
            for (int y = min.getY(); y <= max.getY(); y++) {
                for (int z = min.getZ(); z <= max.getZ(); z++) {
                    if (level.getFluidState(new BlockPos(x, y, z)).is(FluidTags.WATER)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void advanceSurgeWaterState(ThrownTrident trident, ServerLevel level,
            SurgeState state, boolean inWater) {
        if (state.surgeLevel <= 0 || !state.outbound || !isThrownTridentFlightState(trident)) {
            return;
        }
        long now = level.getGameTime();
        if (state.lastSurgeAdvanceTick == now) {
            return;
        }
        state.lastSurgeAdvanceTick = now;
        if (state.active) {
            if (inWater) {
                state.activeUntil = now + SURGE_HOLD_TICKS;
            } else if (now >= state.activeUntil) {
                state.active = false;
                state.waterFlightTicks = 0;
            }
            return;
        }
        if (!inWater) {
            state.waterFlightTicks = 0;
            return;
        }
        state.waterFlightTicks++;
        int required = switch (state.surgeLevel) {
            case 1 -> 6;
            case 2 -> 5;
            default -> 4;
        };
        if (state.waterFlightTicks >= required) {
            state.active = true;
            state.activeUntil = now + SURGE_HOLD_TICKS;
            level.sendParticles(ParticleTypes.SPLASH, trident.getX(), trident.getY(), trident.getZ(),
                    4 + state.surgeLevel * 4, 0.25D, 0.25D, 0.25D, 0.12D);
            level.playSound(null, trident.blockPosition(), SoundEvents.PLAYER_SPLASH,
                    SoundSource.PLAYERS, 0.45F, 1.1F);
        }
    }

    private void captureTridentState(ThrownTrident trident, ServerLevel level) {
        Entity owner = trident.getOwner();
        if (!(owner instanceof LivingEntity shooter)) {
            return;
        }
        ItemStack weapon = tridentItemStack(trident);
        int surge = enchantmentLevel(shooter, weapon, SURGE_ID);
        int tether = enchantmentLevel(shooter, weapon, TETHER_ID);
        int rebound = enchantmentLevel(shooter, weapon, REBOUND_ID);
        int hydroshock = enchantmentLevel(shooter, weapon, HYDROSHOCK_ID);
        if (surge <= 0 && tether <= 0 && rebound <= 0 && hydroshock <= 0) {
            return;
        }
        SurgeState state = surgeStates.computeIfAbsent(trident.getUUID(),
                ignored -> new SurgeState(surge, tether, rebound, hydroshock));
        state.surgeLevel = Math.max(state.surgeLevel, surge);
        state.tetherLevel = Math.max(state.tetherLevel, tether);
        state.reboundLevel = Math.max(state.reboundLevel, rebound);
        state.hydroshockLevel = Math.max(state.hydroshockLevel, hydroshock);
    }

    private static ItemStack tridentItemStack(ThrownTrident trident) {
        ItemStack pickup = trident.getPickupItemStackOrigin();
        if (pickup != null && !pickup.isEmpty()) {
            return pickup;
        }
        ItemStack weapon = trident.getWeaponItem();
        return weapon == null ? ItemStack.EMPTY : weapon;
    }

    private static float surgeDamageMultiplier(int level) {
        return switch (level) {
            case 1 -> 1.15F;
            case 2 -> 1.25F;
            default -> 1.35F;
        };
    }

    private static void applyTether(LivingEntity target, LivingEntity shooter, ServerLevel level, int levelValue) {
        Vec3 offset = shooter.getPosition(1.0F).subtract(target.getPosition(1.0F));
        Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
        double horizontalLength = horizontal.length();
        if (horizontalLength < 1.0E-6D) {
            return;
        }
        double pullStrength = switch (levelValue) {
            case 1 -> 0.35D;
            case 2 -> 0.50D;
            default -> 0.65D;
        };
        double resistance = Mth.clamp(target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        double scale = 1.0D - resistance;
        Vec3 direction = horizontal.scale(1.0D / horizontalLength);
        double vertical = Mth.clamp(offset.y / Math.max(1.0D, horizontalLength), -0.25D, 0.25D);
        Vec3 current = target.getDeltaMovement();
        target.setDeltaMovement(current.x + direction.x * pullStrength * scale,
                current.y + vertical * scale, current.z + direction.z * pullStrength * scale);
        target.syncVelocity = true;
        int particles = switch (levelValue) {
            case 1 -> 5;
            case 2 -> 8;
            default -> 12;
        };
        level.sendParticles(ParticleTypes.DRIPPING_WATER, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                target.getZ(), particles, 0.25D, 0.35D, 0.25D, 0.04D);
    }

    private void tickRebound(ThrownTrident trident, LivingEntity shooter, SurgeState state, ServerLevel level) {
        if (state.reboundLevel <= 0 || !(shooter instanceof Player)
                || EnchantmentHelper.getItemEnchantmentLevel(
                        level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.LOYALTY), tridentItemStack(trident)) <= 0) {
            return;
        }
        Vec3 motion = trident.getDeltaMovement();
        if (motion.lengthSqr() < 1.0E-6D) {
            return;
        }
        AABB path = trident.getBoundingBox().expandTowards(motion).inflate(0.35D);
        int maxHits = state.reboundLevel;
        if (state.returnHitEntities.size() >= maxHits) {
            return;
        }
        level.getEntitiesOfClass(LivingEntity.class, path, target -> target != shooter
                        && target.isAlive() && !target.isSpectator()
                        && !state.returnHitEntities.contains(target.getUUID()))
                .stream()
                .sorted(Comparator.comparingDouble(target -> target.distanceToSqr(trident)))
                .limit(maxHits - state.returnHitEntities.size())
                .forEach(target -> {
                    DamageSource source = level.damageSources().trident(trident, shooter);
                    float normalDamage = EnchantmentHelper.modifyDamage(level, tridentItemStack(trident), target, source, 8.0F);
                    float returnMultiplier = switch (state.reboundLevel) {
                        case 1 -> 0.30F;
                        case 2 -> 0.45F;
                        default -> 0.60F;
                    };
                    if (target.hurtServer(level, source, normalDamage * returnMultiplier)) {
                        state.returnHitEntities.add(target.getUUID());
                        level.sendParticles(ParticleTypes.SPLASH, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                                target.getZ(), 4 + state.reboundLevel * 3, 0.20D, 0.30D, 0.20D, 0.08D);
                        level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.5D,
                                target.getZ(), 2 + state.reboundLevel, 0.15D, 0.20D, 0.15D, 0.02D);
                        level.playSound(null, target.blockPosition(), SoundEvents.TRIDENT_RETURN,
                                SoundSource.PLAYERS, 0.45F, 1.15F);
                    }
                });
    }

    private void tickHolyField(ServerPlayer player, ServerLevel level) {
        HolyField field = holyFields.get(player.getUUID());
        if (field == null) return;
        long now = level.getGameTime();
        if (now >= field.expiresAt()) {
            holyFields.remove(player.getUUID());
            return;
        }
        if (now < field.nextHealAt()) return;
        holyFields.put(player.getUUID(), new HolyField(field.center(), field.expiresAt(), now + 20L, field.level()));
        double percent = switch (field.level()) {
            case 1 -> 0.02D;
            case 2 -> 0.025D;
            default -> 0.03D;
        };
        player.heal((float) (player.getMaxHealth() * percent));
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0D, player.getZ(),
                3, 0.35D, 0.55D, 0.35D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.8D, player.getZ(),
                2, 0.25D, 0.4D, 0.25D, 0.01D);
    }

    private static int enchantmentLevel(LivingEntity entity, ItemStack stack, Identifier id) {
        if (!isValidEnchantmentItem(stack, id)) {
            return 0;
        }
        Holder<Enchantment> enchantment = entity.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, id));
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack);
    }

    /** Runtime backstop for the data-driven primary/supported item restrictions. */
    private static boolean isValidEnchantmentItem(ItemStack stack, Identifier id) {
        if (stack.isEmpty()) {
            return false;
        }
        if (id.equals(ENCHANTMENT_ID)) {
            return stack.is(Items.SHIELD);
        }
        if (id.equals(DEFLECT_ID)) {
            return stack.is(Items.SHIELD);
        }
        if (id.equals(EXPANSION_ID) || id.equals(CLUSTER_ID) || id.equals(ARMOR_PIERCING_ID)) {
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
        if (id.equals(EXPLOSIVE_ARROW_ID)) {
            return stack.getItem() instanceof BowItem;
        }
        if (id.equals(ARROW_RAIN_ID)) {
            return stack.getItem() instanceof BowItem;
        }
        if (id.equals(TRACKING_ID)) {
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

    private static boolean isBattleMomentumWeapon(ItemStack stack) {
        return stack.is(ItemTags.MELEE_WEAPON_ENCHANTABLE)
                || stack.is(ItemTags.SPEARS)
                || stack.is(ItemTags.AXES)
                || stack.is(Items.TRIDENT)
                || stack.is(Items.MACE);
    }

    private void tickClarity(Player player) {
        UUID id = player.getUUID();
        int level = enchantmentLevel(player, player.getItemBySlot(EquipmentSlot.HEAD), CLARITY_ID);
        if (level <= 0) {
            clarityActive.remove(id);
            return;
        }
        clarityActive.add(id);
        player.removeEffect(MobEffects.DARKNESS);
        player.removeEffect(MobEffects.BLINDNESS);
        player.removeEffect(MobEffects.NAUSEA);
    }

    private static boolean isChestEquipment(ItemStack stack) {
        if (stack.is(ItemTags.CHEST_ARMOR)) {
            return true;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.CHEST;
    }

    private static boolean isFootEquipment(ItemStack stack) {
        if (stack.is(ItemTags.FOOT_ARMOR)) {
            return true;
        }
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return equippable != null && equippable.slot() == EquipmentSlot.FEET;
    }

    private void tickSlowModifier(Level level) {
        slowedTargets.entrySet().removeIf(entry -> {
            Entity entity = level instanceof ServerLevel serverLevel ? serverLevel.getEntity(entry.getKey()) : null;
            if (!(entity instanceof LivingEntity living) || level.getGameTime() >= entry.getValue()) {
                if (entity instanceof LivingEntity living && living.getAttribute(Attributes.MOVEMENT_SPEED) != null) {
                    living.getAttribute(Attributes.MOVEMENT_SPEED).removeModifier(SLOW_MODIFIER_ID);
                }
                return true;
            }
            return false;
        });
    }

    private void shieldBash(Player player) {
        Level level = player.level();
        if (level.isClientSide() || player.isSpectator() || cooldowns.containsKey(player.getUUID())) {
            return;
        }

        InteractionHand hand = findShieldHand(player);
        if (hand == null) {
            return;
        }
        ItemStack shield = hand == InteractionHand.MAIN_HAND ? player.getMainHandItem() : player.getOffhandItem();

        Vec3 origin = player.position().add(0.0D, player.getBbHeight() * 0.5D, 0.0D);
        Vec3 look = player.getLookAngle();
        Vec3 horizontalDirection = new Vec3(look.x, 0.0D, look.z);
        if (horizontalDirection.lengthSqr() < 1.0E-6D) {
            return;
        }
        horizontalDirection = horizontalDirection.normalize();
        final Vec3 bashDirection = horizontalDirection;
        ServerLevel serverLevel = (ServerLevel) level;
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.SHIELD_BLOCK.value(),
                SoundSource.PLAYERS, 1.0F, 1.15F);
        serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                origin.x + bashDirection.x * 0.8D, origin.y, origin.z + bashDirection.z * 0.8D,
                6, 0.55D, 0.45D, 0.55D, 0.0D);
        serverLevel.sendParticles(ParticleTypes.CRIT,
                origin.x + bashDirection.x * 0.9D, origin.y, origin.z + bashDirection.z * 0.9D,
                20, 0.75D, 0.5D, 0.75D, 0.15D);
        AABB searchBox = player.getBoundingBox().expandTowards(bashDirection.scale(RANGE)).inflate(HALF_WIDTH, 0.75D, HALF_WIDTH);
        List<LivingEntity> targets = level.getEntities(player, searchBox, entity ->
                entity instanceof LivingEntity living && living.isAlive() && !living.isSpectator() && inBashArea(origin, bashDirection, living))
                .stream().map(LivingEntity.class::cast).toList();
        if (targets.isEmpty()) {
            // A shield bash attempt consumes its cooldown even when it hits air.
            cooldowns.put(player.getUUID(), BASH_COOLDOWN_TICKS);
            return;
        }

        boolean hit = false;
        for (LivingEntity target : targets) {
            hit = true;
            target.hurt(level.damageSources().playerAttack(player), percentMaxHealthDamage(target, 0.015D));
            applySlow(target);
            double knockback = player.isSprinting() ? 2.0D + player.getRandom().nextDouble() : 1.0D;
            Vec3 push = bashDirection.scale(knockback);
            target.setDeltaMovement(push.x, Math.max(0.2D, target.getDeltaMovement().y), push.z);
            target.syncVelocity = true;
        }

        if (!hit) {
            return;
        }
        cooldowns.put(player.getUUID(), BASH_COOLDOWN_TICKS);
        if (!player.isCreative() && shield.isDamageableItem()) {
            shield.hurtAndBreak(2, player, hand);
        }
    }

    private void tryTriggerHolyLight(ServerPlayer player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        int level = enchantmentLevel(player, chest, HOLY_LIGHT_ID);
        if (level <= 0 || player.getHealth() > player.getMaxHealth() * 0.20F) return;
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLongOr(HOLY_LIGHT_NEXT_TRIGGER_KEY, 0L)) return;
        player.getPersistentData().putLong(HOLY_LIGHT_NEXT_TRIGGER_KEY, now + 7200L);
        holyFields.put(player.getUUID(), new HolyField(player.position(), now + 300L, now + 20L, level));
        if (player.level() instanceof ServerLevel levelWorld) {
            levelWorld.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, player.getX(), player.getY() + 0.8D, player.getZ(),
                    14, 0.55D, 0.8D, 0.55D, 0.03D);
            levelWorld.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0D, player.getZ(),
                    8, 0.55D, 0.75D, 0.55D, 0.01D);
            levelWorld.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE,
                    SoundSource.PLAYERS, 0.8F, 1.2F);
        }
    }

    public static boolean hasShieldBash(Player player) {
        return findShieldHand(player) != null;
    }

    private static InteractionHand findShieldHand(Player player) {
        if (player.isUsingItem() && hasShieldBash(player, player.getUseItem())) {
            return player.getUsedItemHand();
        }
        if (hasShieldBash(player, player.getMainHandItem())) {
            return InteractionHand.MAIN_HAND;
        }
        if (hasShieldBash(player, player.getOffhandItem())) {
            return InteractionHand.OFF_HAND;
        }
        return null;
    }

    private static boolean hasShieldBash(Player player, ItemStack stack) {
        if (!stack.is(Items.SHIELD)) {
            return false;
        }
        Holder<Enchantment> enchantment = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, ENCHANTMENT_ID));
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, stack) > 0;
    }

    public static boolean hasSwordAura(Player player) {
        ItemStack sword = player.getMainHandItem();
        if (!sword.is(ItemTags.SWORDS)) {
            return false;
        }
        Holder<Enchantment> enchantment = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT,
                        Identifier.fromNamespaceAndPath(MOD_ID, "sword_aura")));
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, sword) > 0;
    }

    public static boolean hasDoubleJump(Player player) {
        ItemStack boots = player.getItemBySlot(EquipmentSlot.FEET);
        if (!isFootEquipment(boots)) {
            return false;
        }
        Holder<Enchantment> enchantment = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(ResourceKey.create(Registries.ENCHANTMENT, DOUBLE_JUMP_ID));
        return EnchantmentHelper.getItemEnchantmentLevel(enchantment, boots) > 0;
    }

    private void triggerSwordAura(Player player) {
        Level level = player.level();
        if (level.isClientSide()) {
            return;
        }
        long gameTime = level.getGameTime();
        if (swordAuraTicks.getOrDefault(player.getUUID(), Long.MIN_VALUE) == gameTime) {
            return;
        }
        ItemStack sword = player.getMainHandItem();
        if (player.isSpectator() || !sword.is(ItemTags.SWORDS)
                || (player.isUsingItem() && player.getUseItem().is(Items.SHIELD))) {
            return;
        }
        Holder<Enchantment> enchantment = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(net.minecraft.resources.ResourceKey.create(Registries.ENCHANTMENT,
                        Identifier.fromNamespaceAndPath(MOD_ID, "sword_aura")));
        int levelValue = EnchantmentHelper.getItemEnchantmentLevel(enchantment, sword);
        if (levelValue <= 0) {
            return;
        }
        if (player.getAttackStrengthScale(0.5F) < 0.999F) {
            return;
        }
        swordAuraTicks.put(player.getUUID(), gameTime);
        ServerLevel serverLevel = (ServerLevel) level;
        Vec3 start = player.position().add(0.0D, player.getBbHeight() * 0.55D, 0.0D);
        // The aura follows the player's horizontal facing, rather than the crosshair pitch.
        float yaw = player.getYRot() * Mth.DEG_TO_RAD;
        Vec3 direction = new Vec3(-Mth.sin(yaw), 0.0D, Mth.cos(yaw));
        // I-V reach 6, 7, 8, 9, and 10 blocks respectively.
        double waveRange = 5.0D + levelValue;
        Vec3 end = start.add(direction.scale(waveRange));
        BlockHitResult blockHit = level.clip(new ClipContext(start, end, ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE, player));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            end = blockHit.getLocation();
        }
        AABB corridor = new AABB(start, end).inflate(SWORD_AURA_HALF_WIDTH, 0.75D, SWORD_AURA_HALF_WIDTH);
        final Vec3 waveEnd = end;
        // Store the visual as a travelling wave. Damage is still resolved once
        // at release time, while the sweep particle advances one row per tick.
        swordWaves.put(player.getUUID(), new SwordWaveVisual(player.getUUID(), levelValue, start, waveEnd,
                player.getAttributeValue(Attributes.ATTACK_DAMAGE)));
        serverLevel.playSound(null, player.blockPosition(), SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0F, 0.75F + levelValue * 0.05F);
        if (!player.isCreative() && sword.isDamageableItem()) {
            sword.hurtAndBreak(1, player, InteractionHand.MAIN_HAND);
        }
    }

    private static boolean isInWave(Vec3 start, Vec3 direction, Vec3 end, LivingEntity target) {
        Vec3 offset = target.getBoundingBox().getCenter().subtract(start);
        double along = offset.dot(direction);
        if (along < 0.0D || along > start.distanceTo(end)) {
            return false;
        }
        return target.getBoundingBox().inflate(SWORD_AURA_HALF_WIDTH, 0.75D, SWORD_AURA_HALF_WIDTH)
                .intersects(start, end);
    }

    private static float percentMaxHealthDamage(LivingEntity target, double percent) {
        return percentMaxHealthDamageForPercent(target.getMaxHealth(), percent);
    }

    private static float percentMaxHealthDamageForPercent(double maxHealth, double percent) {
        return (float) Math.min(maxHealth * Math.min(percent, 0.08D), Float.MAX_VALUE);
    }

    private static double swordAuraDamageMultiplier(int level) {
        return switch (level) {
            case 1 -> 0.40D;
            case 2 -> 0.55D;
            case 3 -> 0.70D;
            case 4 -> 0.85D;
            default -> 1.00D;
        };
    }

    private void tickSwordWave(Player player) {
        SwordWaveVisual wave = swordWaves.get(player.getUUID());
        if (wave == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (wave.step > wave.steps) {
            swordWaves.remove(player.getUUID());
            return;
        }
        double distance = Math.min(wave.distance, wave.step * 1.5D);
        Vec3 point = wave.start.add(wave.direction.scale(distance));
        level.sendParticles(SWORD_WAVE_PARTICLE.get(), point.x, point.y - 0.15D, point.z,
                1, 0.02D, 0.05D, 0.02D, 0.0D);
        Entity owner = level.getEntity(wave.owner);
        if (owner instanceof Player attacker) {
            AABB contact = new AABB(point, point).inflate(SWORD_AURA_HALF_WIDTH, 0.9D, SWORD_AURA_HALF_WIDTH);
            int maxTargets = wave.level;
            float waveDamage = (float) Math.min(wave.attackDamage * swordAuraDamageMultiplier(wave.level),
                    Float.MAX_VALUE);
            level.getEntities(attacker, contact, entity -> entity instanceof LivingEntity living
                            && living.isAlive() && !living.isSpectator()
                            && !wave.hitTargets.contains(living.getUUID()))
                    .stream().map(LivingEntity.class::cast)
                    .sorted(Comparator.comparingDouble(target -> target.position().distanceToSqr(point)))
                    .limit(Math.max(0, maxTargets - wave.hitTargets.size()))
                    .forEach(target -> {
                        // Sword Aura is a direct player attack for Frostbite purposes. Record
                        // the target immediately before the synchronous damage event so the
                        // normal post-damage handler can apply Frostbite exactly once.
                        ItemStack weapon = attacker.getMainHandItem();
                        if (enchantmentLevel(attacker, weapon, FROSTBITE_ID) > 0
                                && (weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.SPEARS))) {
                            pendingFrostbiteAttacks.put(attacker.getUUID(),
                                    new PendingFrostbiteAttack(target.getUUID(), level.getGameTime(), weapon));
                        }
                        boolean damaged = target.hurtServer(level, level.damageSources().playerAttack(attacker),
                                waveDamage);
                        if (damaged) {
                            wave.hitTargets.add(target.getUUID());
                        } else {
                            pendingFrostbiteAttacks.remove(attacker.getUUID());
                        }
                    });
        }
        wave.step++;
    }

    private static boolean inBashArea(Vec3 origin, Vec3 direction, LivingEntity target) {
        Vec3 offset = target.getBoundingBox().getCenter().subtract(origin);
        if (Math.abs(offset.y) > 0.75D + target.getBbHeight() * 0.5D) {
            return false;
        }
        Vec3 horizontal = new Vec3(offset.x, 0.0D, offset.z);
        double distance = horizontal.length();
        return distance >= 1.0E-6D && distance <= RANGE && horizontal.normalize().dot(direction) >= HALF_ANGLE_COS;
    }

    private void applySlow(LivingEntity target) {
        var attribute = target.getAttribute(Attributes.MOVEMENT_SPEED);
        if (attribute == null) {
            return;
        }
        attribute.removeModifier(SLOW_MODIFIER_ID);
        attribute.addTransientModifier(new AttributeModifier(SLOW_MODIFIER_ID, -0.25D,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        slowedTargets.put(target.getUUID(), target.level().getGameTime() + SLOW_TICKS);
    }
}
