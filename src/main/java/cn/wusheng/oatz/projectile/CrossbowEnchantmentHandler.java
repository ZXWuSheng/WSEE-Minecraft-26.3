package cn.wusheng.oatz.projectile;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ArrowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ChargedProjectiles;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static cn.wusheng.oatz.enchantment.EnchantmentUtil.enchantmentLevel;
import static cn.wusheng.oatz.projectile.ProjectileRules.readArrowBaseDamage;
import static cn.wusheng.oatz.registry.ModEnchantments.ARMOR_PIERCING_ID;
import static cn.wusheng.oatz.registry.ModEnchantments.CLUSTER_ID;
import static cn.wusheng.oatz.registry.ModEnchantments.EXPANSION_ID;

/** Owns Expansion and Cluster follow-up arrows plus crossbow armor handling. */
public final class CrossbowEnchantmentHandler {
    private final Map<UUID, ArrayDeque<QueuedProjectile>> delayedCrossbowShots = new HashMap<>();
    private final Map<UUID, Long> expansionShotTicks = new HashMap<>();
    private final Set<UUID> generatedClusterArrows = new HashSet<>();
    private final Set<UUID> generatedExpansionArrows = new HashSet<>();

    private record QueuedProjectile(Projectile projectile, long dueTick, boolean crossbow) {}

    public void onUseItemFinish(LivingEntityUseItemEvent.Finish event) {
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

    public void onPlayerTick(Player player) {
        expansionShotTicks.remove(player.getUUID());
        releaseQueuedShot(player);
    }

    public boolean removeGeneratedExpansion(AbstractArrow arrow) {
        return generatedExpansionArrows.remove(arrow.getUUID());
    }

    public boolean removeGeneratedCluster(AbstractArrow arrow) {
        return generatedClusterArrows.remove(arrow.getUUID());
    }

    public void spawnFollowups(AbstractArrow arrow, LivingEntity shooter, ItemStack weapon,
            ServerLevel serverLevel, boolean crossbow, boolean generatedExpansionArrow, boolean loadedFromDisk) {
        // EntityJoinLevelEvent also fires when an old arrow is restored from a
        // world save. Only a genuinely new bow or crossbow shot may create follow-ups.
        if (loadedFromDisk) {
            return;
        }
        int expansion = enchantmentLevel(shooter, weapon, EXPANSION_ID);
        if (!generatedExpansionArrow && expansion > 0) {
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
                    extra.setBaseDamage(readArrowBaseDamage(arrow));
                    extra.setCritArrow(arrow.isCritArrow());
                    generatedExpansionArrows.add(extra.getUUID());
                    queue.add(new QueuedProjectile(extra, tick + i * 5L, crossbow));
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
        double baseDamage = readArrowBaseDamage(arrow);
        arrow.setBaseDamage(baseDamage * damageMultiplier);
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
            extra.setBaseDamage(baseDamage * damageMultiplier);
            extra.setCritArrow(arrow.isCritArrow());
            generatedClusterArrows.add(extra.getUUID());
            serverLevel.addFreshEntity(extra);
        }
    }

    private static Vec3 rotateAroundY(Vec3 vector, double angle) {
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);
        return new Vec3(vector.x * cos - vector.z * sin, vector.y, vector.x * sin + vector.z * cos).normalize();
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
            player.level().playSound(null, player.blockPosition(), next.crossbow()
                    ? SoundEvents.CROSSBOW_SHOOT : SoundEvents.ARROW_SHOOT,
                    SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (queue.isEmpty()) {
            delayedCrossbowShots.remove(player.getUUID());
        }
    }

    public void onPlayerLoggedOut(UUID playerId) {
        clearQueuedCrossbowShots(playerId);
        expansionShotTicks.remove(playerId);
    }

    private void clearQueuedCrossbowShots(UUID playerId) {
        ArrayDeque<QueuedProjectile> removed = delayedCrossbowShots.remove(playerId);
        if (removed != null) {
            removed.forEach(shot -> generatedExpansionArrows.remove(shot.projectile().getUUID()));
        }
    }

    public void modifyArrowDamage(LivingIncomingDamageEvent event, LivingEntity shooter, ItemStack weapon) {
        if (weapon.getItem() instanceof CrossbowItem) {
            int level = enchantmentLevel(shooter, weapon, ARMOR_PIERCING_ID);
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
        }
        int cluster = enchantmentLevel(shooter, weapon, CLUSTER_ID);
        int expansion = enchantmentLevel(shooter, weapon, EXPANSION_ID);
        if (cluster > 0 || expansion > 0) {
            // Extra arrows from cluster and expansion shots can arrive before
            // vanilla's ten-tick hurt cooldown expires.
            event.getEntity().setInvulnerableTime(0);
            event.getEntity().hurtTime = 0;
        }
    }
}
