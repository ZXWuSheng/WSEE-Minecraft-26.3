package cn.wusheng.oatz.projectile;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static cn.wusheng.oatz.enchantment.EnchantmentValues.arrowRainCount;
import static cn.wusheng.oatz.enchantment.EnchantmentValues.arrowRainDuration;
import static cn.wusheng.oatz.enchantment.EnchantmentValues.arrowRainRadius;

/** Owns warning, fixed-area spawning, and recursion marking for Arrow Rain. */
public final class ArrowRainHandler {
    public static final String ARROW_RAIN_PROJECTILE_KEY = "wsee_arrow_rain_projectile";
    private final Map<UUID, ArrowRainState> arrowRainStates = new HashMap<>();

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

    public void recordShot(AbstractArrow arrow, LivingEntity shooter, int level, double damageBase,
            ItemStack weapon, ServerLevel world) {
        arrowRainStates.put(arrow.getUUID(), new ArrowRainState(shooter.getUUID(), level,
                damageBase, weapon, world.dimension()));
    }

    public void arm(AbstractArrow arrow, ProjectileImpactEvent event, ServerLevel level) {
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

    public void tickStates(ServerLevel level) {
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
}
