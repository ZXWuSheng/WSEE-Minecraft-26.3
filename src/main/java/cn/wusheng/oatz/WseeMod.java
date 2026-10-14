package cn.wusheng.oatz;

import cn.wusheng.oatz.movement.DoubleJumpController;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
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
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.item.component.AttackRange;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.effect.MobEffect;
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
import net.minecraft.util.Mth;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.AttackEntityEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
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
import java.util.function.Supplier;
import cn.wusheng.oatz.movement.FlyingHookController;
import cn.wusheng.oatz.movement.FlyingHookClientState;
import cn.wusheng.oatz.registry.ModRegistries;
import cn.wusheng.oatz.registry.ModNetworking;
import cn.wusheng.oatz.projectile.TrackingHandler;
import cn.wusheng.oatz.projectile.ArrowRainHandler;
import cn.wusheng.oatz.projectile.ExplosiveArrowHandler;
import cn.wusheng.oatz.combat.FrostbiteHandler;
import cn.wusheng.oatz.projectile.CrossbowEnchantmentHandler;
import cn.wusheng.oatz.projectile.TridentEnchantmentHandler;
import static cn.wusheng.oatz.projectile.ArrowRainHandler.ARROW_RAIN_PROJECTILE_KEY;
import static cn.wusheng.oatz.projectile.ProjectileRules.readArrowBaseDamage;
import cn.wusheng.oatz.enchantment.EnchantmentApplicabilityHandler;
import cn.wusheng.oatz.tool.RefiningHandler;
import static cn.wusheng.oatz.registry.ModEnchantments.*;
import static cn.wusheng.oatz.registry.ModDamageTypes.*;
import static cn.wusheng.oatz.enchantment.EnchantmentUtil.enchantmentLevel;
import static cn.wusheng.oatz.enchantment.EquipmentRules.isValidEnchantmentItem;
import static cn.wusheng.oatz.enchantment.EquipmentRules.isFootEquipment;
import static cn.wusheng.oatz.enchantment.EnchantmentValues.*;

@Mod(WseeMod.MOD_ID)
public final class WseeMod {
    public static final String MOD_ID = "wsee";
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = ModRegistries.CREATIVE_MODE_TABS;
    public static final DeferredRegister<MobEffect> MOB_EFFECTS = ModRegistries.MOB_EFFECTS;
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES = ModRegistries.PARTICLE_TYPES;
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SWORD_WAVE_PARTICLE = ModRegistries.SWORD_WAVE_PARTICLE;
    public static final DeferredHolder<MobEffect, MobEffect> BLEEDING_EFFECT = ModRegistries.BLEEDING_EFFECT;
    public static final DeferredHolder<MobEffect, MobEffect> BATTLE_MOMENTUM_EFFECT = ModRegistries.BATTLE_MOMENTUM_EFFECT;
    public static final Supplier<CreativeModeTab> OA_ENCHANTMENTS_TAB = ModRegistries.OA_ENCHANTMENTS_TAB;
    private static final String BLEEDING_ATTACKER_KEY = "oatz_bleeding_attacker";
    private static final String SHIELD_NEXT_REFRESH_KEY = "oatz_shield_next_refresh";
    private static final String HOLY_LIGHT_NEXT_TRIGGER_KEY = "oatz_holy_light_next_trigger";
    private static final String SHIELD_AMOUNT_KEY = "oatz_shield_amount";
    private static final String SHIELD_REMAINING_KEY = "oatz_shield_remaining";
    private static final String SHIELD_EFFECT_KEY = "oatz_shield_effect";
    private static final String SHIELD_VERSION_KEY = "oatz_shield_version";
    private static final String DEFLECTED_KEY = "wsee_deflected";
    private static final String FLYING_HOOK_LEVEL_KEY = "wsee_flying_hook_level";
    private static final Identifier SLOW_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shield_bash_slow");
    private static final Identifier SHIELD_ABSORPTION_LIMIT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shield_absorption_limit");
    private static final Identifier BATTLE_MOMENTUM_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "battle_momentum_attack_speed");
    private static final Identifier FORCED_MARCH_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "forced_march_movement_speed");
    private static final Identifier FOUNDATION_MODIFIER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "foundation_knockback_resistance");
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
    private final CrossbowEnchantmentHandler crossbowHandler = new CrossbowEnchantmentHandler();
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
    private final TridentEnchantmentHandler tridentHandler = new TridentEnchantmentHandler();
    private final Map<UUID, BattleMomentumState> battleMomentumStates = new HashMap<>();
    private final Map<UUID, PendingMeleeAttack> pendingMeleeAttacks = new HashMap<>();
    private final FrostbiteHandler frostbiteHandler = new FrostbiteHandler();
    private final Map<UUID, PendingComboAttack> pendingComboAttacks = new HashMap<>();
    private final Map<UUID, ComboState> comboStates = new HashMap<>();
    private final Map<UUID, Integer> comboOverlayStates = new HashMap<>();
    private final Map<UUID, ForcedMarchState> forcedMarchStates = new HashMap<>();
    private final Set<UUID> clarityActive = new HashSet<>();
    private final ExplosiveArrowHandler explosiveArrowHandler = new ExplosiveArrowHandler();
    private final ArrowRainHandler arrowRainHandler = new ArrowRainHandler();
    private final TrackingHandler trackingHandler = new TrackingHandler();
    private final Map<ResourceKey<Level>, Long> explosiveStateTicks = new HashMap<>();
    private static final FlyingHookController FLYING_HOOKS =
            new FlyingHookController(WseeMod::hasFlyingHook, WseeMod::sendFlyingHookAnchor);
    private static final FlyingHookClientState CLIENT_HOOKS = new FlyingHookClientState();
    private static final Map<UUID, InteractionHand> PENDING_HOOK_CASTS = new HashMap<>();
    private final DoubleJumpController doubleJumpController = new DoubleJumpController();
    private final Map<UUID, FallingStarHit> fallingStarHits = new HashMap<>();
    private final Map<UUID, ShockRingHit> shockRingHits = new HashMap<>();
    private final List<ShockRingInstance> shockRings = new ArrayList<>();
    private record OverchargeCharge(double damageMultiplier, double velocityMultiplier) {}
    private record PendingMeleeAttack(UUID target, long tick) {}
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
            this.maxRadius = shockRingRadius(level);
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
        ModRegistries.register(modBus);
        cn.wusheng.oatz.registry.MovementAttachments.register(modBus);
        modBus.addListener((RegisterPayloadHandlersEvent event) -> ModNetworking.register(event, this::shieldBash,
                this::triggerSwordAura, this::tryDoubleJump, this::handleFlyingHookAnchor, this::useFlyingHook));
        NeoForge.EVENT_BUS.addListener(this::onPlayerTick);
        NeoForge.EVENT_BUS.addListener(this::onPlayerLoggedOut);
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event) -> {
            FLYING_HOOKS.clearAll();
            CLIENT_HOOKS.clearAll();
            PENDING_HOOK_CASTS.clear();
            doubleJumpController.clearAll();
            cn.wusheng.oatz.movement.ServerMotion.clearAll();
        });
        NeoForge.EVENT_BUS.addListener(this::onRegisterCommands);
        NeoForge.EVENT_BUS.addListener(EnchantmentApplicabilityHandler::onAnvilUpdate);
        NeoForge.EVENT_BUS.addListener(EnchantmentApplicabilityHandler::onPlayerEnchantItem);
        NeoForge.EVENT_BUS.addListener(this::onAttackEntity);
        NeoForge.EVENT_BUS.addListener(this::onLeftClickEmpty);
        NeoForge.EVENT_BUS.addListener(crossbowHandler::onUseItemFinish);
        NeoForge.EVENT_BUS.addListener(this::onUseItemStop);
        NeoForge.EVENT_BUS.addListener(this::onProjectileJoin);
        NeoForge.EVENT_BUS.addListener(this::onFishingHookJoin);
        NeoForge.EVENT_BUS.addListener(this::onEntityLeave);
        NeoForge.EVENT_BUS.addListener(this::onProjectileImpact);
        NeoForge.EVENT_BUS.addListener(RefiningHandler::onBlockDrops);
        NeoForge.EVENT_BUS.addListener(this::onIncomingDamage);
        NeoForge.EVENT_BUS.addListener(this::onLivingDamagePost);
        NeoForge.EVENT_BUS.addListener(this::onEntityTick);
        NeoForge.EVENT_BUS.addListener(this::onLivingFall);
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
            frostbiteHandler.recordAttack(attacker, target, weapon);
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

    private void tickFlyingHook(Player player) {
        FLYING_HOOKS.tickPlayer(player);
    }



    private void onPlayerTick(PlayerTickEvent.Pre event) {
        Player player = event.getEntity();
        tickLongEdgeRange(player);
        if (!player.level().isClientSide()) {
            cooldowns.computeIfPresent(player.getUUID(), (id, ticks) -> ticks <= 1 ? null : ticks - 1);
            tickSlowModifier(player.level());
            crossbowHandler.onPlayerTick(player);
            tickChestEnchantments(player);
            tickOverchargeParticles(player);
            tickSwordWave(player);
            if (player.level() instanceof ServerLevel serverLevel) {
                long gameTime = serverLevel.getGameTime();
                if (!gameTimeEquals(explosiveStateTicks.get(serverLevel.dimension()), gameTime)) {
                    explosiveStateTicks.put(serverLevel.dimension(), gameTime);
                    explosiveArrowHandler.tickDetachedStates(serverLevel);
                    arrowRainHandler.tickStates(serverLevel);
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
        if (player.isDeadOrDying() || player.isRemoved()) {
            doubleJumpController.clearPlayer(player.getUUID());
            return;
        }
        boolean genuineGroundContact = player.onGround() && !player.isInWater() && !player.isInLava()
                && player.level().getBlockCollisions(player,
                        player.getBoundingBox().move(0.0D, -0.01D, 0.0D)).iterator().hasNext();
        if (doubleJumpController.tick(player.getUUID(), player, player.level().dimension(),
                isDoubleJumpEligible(player), genuineGroundContact, player.isJumping())
                && player instanceof ServerPlayer serverPlayer) {
            tryDoubleJump(serverPlayer);
        }
    }

    private static boolean isDoubleJumpEligible(Player player) {
        return enchantmentLevel(player, player.getItemBySlot(EquipmentSlot.FEET), DOUBLE_JUMP_ID) > 0
                && !player.isDeadOrDying() && !player.isRemoved() && !player.isSpectator()
                && !player.isSwimming() && !player.isPassenger() && !player.isFallFlying()
                && !player.getAbilities().flying && !player.isInWater() && !player.isInLava();
    }

    private void tryDoubleJump(ServerPlayer player) {
        if (!doubleJumpController.consumeRequest(player.getUUID(), player, player.level().dimension(),
                isDoubleJumpEligible(player), player.onGround())) {
            return;
        }
        // The server authorizes one increment; the owner adds it to its actual
        // current inertia when received, instead of accepting stale server X/Z.
        cn.wusheng.oatz.movement.ServerMotion.applyDoubleJump(player);
        player.fallDistance = 0.0F;
        noteFlyingHookDoubleJump(player);
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
        event.setDistance(Math.max(0.0F, event.getDistance() - doubleJumpSafeFallBonus()));
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
        double ratio = overchargeProgress(level, heldTicks);
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
            int velocityPercent = (int) Math.round(overchargeVelocityBonus(level) * ratio * 100.0D);
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
        double percent = shieldAbsorptionPercent(shieldLevel);
        float shield = (float) (player.getMaxHealth() * percent);
        float previousShield = (float) player.getPersistentData().getDoubleOr(
                SHIELD_REMAINING_KEY,
                player.getPersistentData().getDoubleOr(SHIELD_AMOUNT_KEY, 0.0D));
        float preservedAbsorption = Math.max(0.0F, player.getAbsorptionAmount() - previousShield);
        ensureShieldAbsorptionLimit(player, shield + preservedAbsorption);
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, shieldRefreshTicks(), shieldLevel - 1,
                false, true, true));
        player.setAbsorptionAmount(preservedAbsorption + shield);
        player.getPersistentData().putBoolean(SHIELD_EFFECT_KEY, true);
        player.getPersistentData().putDouble(SHIELD_AMOUNT_KEY, shield);
        player.getPersistentData().putDouble(SHIELD_REMAINING_KEY, shield);
        player.getPersistentData().putLong(SHIELD_NEXT_REFRESH_KEY, now + shieldRefreshTicks());
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
        double ratio = overchargeProgress(level, heldTicks);
        double maxDamageBonus = overchargeDamageBonus(level);
        double maxVelocityBonus = overchargeVelocityBonus(level);
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
            tridentHandler.capture(trident, serverLevel);
        }
        if (arrow.getPersistentData().getBoolean(ARROW_RAIN_PROJECTILE_KEY).orElse(false)) {
            return;
        }
        boolean generatedExpansionArrow = crossbowHandler.removeGeneratedExpansion(arrow);
        if (crossbowHandler.removeGeneratedCluster(arrow)) {
            return;
        }
        ItemStack weapon = arrow.getWeaponItem();
        if (!(arrow.getOwner() instanceof LivingEntity shooter)) {
            return;
        }
        boolean bow = weapon.getItem() instanceof BowItem;
        boolean crossbow = weapon.getItem() instanceof CrossbowItem;
        if (!bow && !crossbow) {
            return;
        }
        int explosiveLevel = enchantmentLevel(shooter, weapon, EXPLOSIVE_ARROW_ID);
        if (explosiveLevel > 0) {
            explosiveArrowHandler.recordShot(arrow, shooter, explosiveLevel,
                    shooter.getAttributeValue(Attributes.ATTACK_DAMAGE)
                            + vanillaPowerDamageBonus(shooter, weapon));
        }
        Double recordedDrawRatio = bow ? bowDrawRatios.remove(shooter.getUUID()) : null;
        // The release event normally runs before projectile creation. Keep a
        // vanilla-physics fallback for bows; a crossbow projectile is already
        // released from a fully charged shot.
        double drawRatio = crossbow ? 1.0D
                : recordedDrawRatio != null
                        ? recordedDrawRatio
                        : Mth.clamp(arrow.getDeltaMovement().length() / 3.0D, 0.0D, 1.0D);
        int trackingLevel = enchantmentLevel(shooter, weapon, TRACKING_ID);
        if (trackingLevel > 0 && drawRatio >= 0.80D) {
            trackingHandler.acquireTarget(arrow, shooter, trackingLevel, serverLevel);
        }
        if (bow) {
            OverchargeCharge charge = overchargeCharges.remove(shooter.getUUID());
            if (charge != null) {
                applyOvercharge(arrow, charge, serverLevel);
            }
        }
        if (bow && !generatedExpansionArrow) {
            int arrowRainLevel = enchantmentLevel(shooter, weapon, ARROW_RAIN_ID);
            if (arrowRainLevel > 0 && drawRatio >= 0.90D) {
                arrowRainHandler.recordShot(arrow, shooter, arrowRainLevel,
                        readArrowBaseDamage(arrow), weapon, serverLevel);
            }
        }
        crossbowHandler.spawnFollowups(arrow, shooter, weapon, serverLevel, crossbow,
                generatedExpansionArrow, event.loadedFromDisk());
    }

    private void onFishingHookJoin(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide() || !(event.getEntity() instanceof FishingHook hook)
                || !(hook.getOwner() instanceof Player owner)) return;
        boolean airForceActive = enchantmentLevel(owner, owner.getMainHandItem(), AIR_FORCE_ID) > 0
                || enchantmentLevel(owner, owner.getOffhandItem(), AIR_FORCE_ID) > 0;
        hook.getPersistentData().putBoolean(AirForceFishing.ACTIVE_KEY, airForceActive);
        InteractionHand hand = PENDING_HOOK_CASTS.remove(owner.getUUID());
        if (hand != null && hasFlyingHook(owner, owner.getItemInHand(hand))) {
            hook.getPersistentData().putInt(FLYING_HOOK_LEVEL_KEY, 1);
            FLYING_HOOKS.start(hook, owner, hand);
        }
    }

    private void onEntityLeave(EntityLeaveLevelEvent event) {
        if (event.getEntity() instanceof FishingHook hook) removeFlyingHook(hook);
    }

    private void onProjectileImpact(ProjectileImpactEvent event) {
        if (deflectArrow(event)) {
            return;
        }
        if (event.getProjectile() instanceof AbstractArrow arrow
                && arrow.level() instanceof ServerLevel arrowLevel) {
            explosiveArrowHandler.arm(arrow, event, arrowLevel);
            arrowRainHandler.arm(arrow, event, arrowLevel);
        }
        if (!(event.getProjectile() instanceof ThrownTrident trident)
                || !(trident.level() instanceof ServerLevel level)
                || trident.noPhysics) {
            return;
        }
        tridentHandler.onImpact(trident, level, event);
    }





    private void handleFlyingHookAnchor(FlyingHookAnchorPayload payload, Player receiver) {
        CLIENT_HOOKS.update(payload, receiver);
    }

    private static void sendFlyingHookAnchor(FishingHook hook, Vec3 anchor) {
        FlyingHookController.Anchor state = FLYING_HOOKS.snapshot(hook);
        if (state == null) return;
        Vec3 offset = state.offset();
        PacketDistributor.sendToPlayersTrackingEntity(hook,
                new FlyingHookAnchorPayload(hook.getId(), hook.getUUID(), state.phase(), state.entityId(),
                        anchor.x, anchor.y, anchor.z, offset.x, offset.y, offset.z));
    }

    public static boolean hasFlyingHook(Player player, ItemStack rod) {
        return rod.is(Items.FISHING_ROD) && enchantmentLevel(player, rod, FLYING_HOOK_ID) > 0;
    }

    public static void prepareFlyingHookCast(Player player, InteractionHand hand) {
        if (hasFlyingHook(player, player.getItemInHand(hand))) PENDING_HOOK_CASTS.put(player.getUUID(), hand);
        else PENDING_HOOK_CASTS.remove(player.getUUID());
    }

    public static boolean tickFlyingHookEntity(FishingHook hook) {
        return hook.level().isClientSide() ? CLIENT_HOOKS.tickHook(hook) : FLYING_HOOKS.tickHook(hook);
    }

    public static int retrieveFlyingHook(FishingHook hook, ItemStack rod) {
        return hook.level().isClientSide() ? 0 : FLYING_HOOKS.retrieve(hook, rod);
    }

    public static void removeFlyingHook(FishingHook hook) {
        if (hook.level().isClientSide()) CLIENT_HOOKS.remove(hook);
        else FLYING_HOOKS.remove(hook);
    }

    public static void clearFlyingHookClientState() { CLIENT_HOOKS.clearAll(); }

    public static void noteFlyingHookDoubleJump(Player player) { FLYING_HOOKS.noteDoubleJump(player); }

    public static boolean isFlyingHookPulling(Player player) {
        return player.fishing != null && player.fishing.isAlive()
                && ((cn.wusheng.oatz.access.FlyingHookAccess) player.fishing).wsee$getPhase() == FlyingHookController.REELING;
    }

    private void useFlyingHook(net.minecraft.server.level.ServerPlayer player, FlyingHookUsePayload payload) {
        FLYING_HOOKS.use(player, payload.hookUuid(), payload.phase(),
                payload.offhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND);
    }





    private boolean deflectArrow(ProjectileImpactEvent event) {
        if (!(event.getProjectile() instanceof AbstractArrow arrow)
                || !(arrow instanceof Arrow || arrow instanceof SpectralArrow)
                || !(arrow.level() instanceof ServerLevel level)
                || explosiveArrowHandler.isRegistered(arrow)
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

    /**
     * Power is a damage-effect enchantment in 26.3. It is applied only when
     * an arrow's normal damage event is built, so custom delayed damage must
     * carry the same additive contribution explicitly.
     */
    private static double vanillaPowerDamageBonus(LivingEntity shooter, ItemStack weapon) {
        if (weapon.isEmpty()) {
            return 0.0D;
        }
        Holder<Enchantment> power = shooter.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .getOrThrow(Enchantments.POWER);
        int level = EnchantmentHelper.getItemEnchantmentLevel(power, weapon);
        return level <= 0 ? 0.0D : 1.0D + 0.5D * (level - 1);
    }

    private static boolean gameTimeEquals(Long previous, long current) {
        return previous != null && previous == current;
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



    private void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerId = player.getUUID();
        FLYING_HOOKS.clear(player);
        PENDING_HOOK_CASTS.remove(playerId);
        crossbowHandler.onPlayerLoggedOut(playerId);
        doubleJumpController.clearPlayer(playerId);
        cn.wusheng.oatz.movement.ServerMotion.clear(player);
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
        tridentHandler.modifyIncomingDamage(event);
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
                || !(event.getEntity().level() instanceof ServerLevel)) {
            return;
        }
        Entity owner = arrow.getOwner();
        if (!(owner instanceof LivingEntity shooter)) {
            return;
        }
        ItemStack weapon = arrow.getWeaponItem();
        if (arrow.getPersistentData().getBoolean(DEFLECTED_KEY).orElse(false)) {
            // Power is applied after the arrow base damage in 26.3. Scale the
            // completed reflected hit so the reflection multiplier covers it.
            event.setAmount(event.getAmount() * 0.80F);
        }
        crossbowHandler.modifyArrowDamage(event, shooter, weapon);
    }

    private void onLivingDamagePost(LivingDamageEvent.Post event) {
        triggerFallingStar(event);
        triggerShockRing(event);
        tridentHandler.afterDamage(event);
        if (event.getEntity() instanceof ServerPlayer player && player.getHealth() > 0.0F) {
            tryTriggerHolyLight(player);
        }
        if (event.getSource().is(EXPLOSIVE_ARROW_DAMAGE_TYPE)
                || event.getSource().is(SHOCK_RING_DAMAGE_TYPE)) {
            return;
        }
        frostbiteHandler.apply(event);
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
                || hit.fallDistance() < fallingStarFallThreshold()) {
            return;
        }
        LivingEntity impactTarget = event.getEntity();
        double radius = fallingStarRadius(hit.level());
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

    private void clearForcedMarch(Player player) {
        forcedMarchStates.remove(player.getUUID());
        removeForcedMarchModifier(player);
    }

    private void onEntityTick(EntityTickEvent.Pre event) {
        if (event.getEntity() instanceof ThrownTrident trident
                && trident.level() instanceof ServerLevel level) {
            tridentHandler.tick(trident, level);
        }
        if (event.getEntity() instanceof AbstractArrow arrow
                && arrow.level() instanceof ServerLevel level) {
            trackingHandler.tick(arrow, level);
            explosiveArrowHandler.tickArrow(arrow, level);
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











    private void tickHolyField(ServerPlayer player, ServerLevel level) {
        HolyField field = holyFields.get(player.getUUID());
        if (field == null) return;
        long now = level.getGameTime();
        if (now >= field.expiresAt()) {
            holyFields.remove(player.getUUID());
            return;
        }
        if (now < field.nextHealAt()) return;
        holyFields.put(player.getUUID(), new HolyField(field.center(), field.expiresAt(), now + holyLightHealIntervalTicks(), field.level()));
        double percent = holyLightHealPercent(field.level());
        player.heal((float) (player.getMaxHealth() * percent));
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0D, player.getZ(),
                3, 0.35D, 0.55D, 0.35D, 0.0D);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 0.8D, player.getZ(),
                2, 0.25D, 0.4D, 0.25D, 0.01D);
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
        if (level <= 0 || player.getHealth() > player.getMaxHealth() * holyLightTriggerThreshold()) return;
        long now = player.level().getGameTime();
        if (now < player.getPersistentData().getLongOr(HOLY_LIGHT_NEXT_TRIGGER_KEY, 0L)) return;
        player.getPersistentData().putLong(HOLY_LIGHT_NEXT_TRIGGER_KEY, now + holyLightCooldownTicks());
        holyFields.put(player.getUUID(), new HolyField(player.position(), now + holyLightDurationTicks(), now + holyLightHealIntervalTicks(), level));
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
        double waveRange = swordAuraRange(levelValue);
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
                        frostbiteHandler.recordAttack(attacker, target, weapon);
                        boolean damaged = target.hurtServer(level, level.damageSources().playerAttack(attacker),
                                waveDamage);
                        if (damaged) {
                            wave.hitTargets.add(target.getUUID());
                        } else {
                            frostbiteHandler.clearPending(attacker);
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
