package cn.wusheng.oatz.combat;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static cn.wusheng.oatz.enchantment.EnchantmentUtil.enchantmentLevel;
import static cn.wusheng.oatz.registry.ModEnchantments.FROSTBITE_ID;

/** Tracks only direct player hits, including synchronous Sword Aura hits. */
public final class FrostbiteHandler {
    private final Map<UUID, PendingFrostbiteAttack> pendingFrostbiteAttacks = new HashMap<>();

    private record PendingFrostbiteAttack(UUID target, long tick, ItemStack weapon) {}

    public void recordAttack(Player attacker, LivingEntity target, ItemStack weapon) {
        if (enchantmentLevel(attacker, weapon, FROSTBITE_ID) > 0
                && (weapon.is(ItemTags.SWORDS) || weapon.is(ItemTags.SPEARS))) {
            pendingFrostbiteAttacks.put(attacker.getUUID(),
                    new PendingFrostbiteAttack(target.getUUID(), attacker.level().getGameTime(), weapon));
        }
    }

    public void clearPending(Player attacker) {
        pendingFrostbiteAttacks.remove(attacker.getUUID());
    }

    public void apply(LivingDamageEvent.Post event) {
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
}
