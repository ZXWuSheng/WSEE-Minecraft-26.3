package cn.wusheng.oatz.movement;

import cn.wusheng.oatz.access.FlyingHookAccess;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;

/** One server-owned cast per player. Both loaders use this exact implementation. */
public final class FlyingHookController {
    public static final byte INACTIVE = FlyingHookCycle.INACTIVE, FLYING = FlyingHookCycle.FLYING,
            ANCHORED = FlyingHookCycle.ANCHORED, REELING = FlyingHookCycle.REELING;
    private final Map<UUID, Cast> casts = new HashMap<>();
    private final BiPredicate<Player, ItemStack> eligible;
    private final BiConsumer<FishingHook, Vec3> syncAnchor;

    private static final class Cast {
        final FishingHook hook;
        final Player owner;
        final ResourceKey<Level> dimension;
        final InteractionHand hand;
        final ItemStack rod;
        final FlyingHookCycle cycle;
        Vec3 anchor, offset, lastPullPosition, lastPullMovement;
        LivingEntity target;
        BlockPos block;
        Direction face;
        double ropeLength, savedFallDistance;
        double lastGap = Double.NaN;
        int ticks, stuckTicks, durability;

        Cast(FishingHook hook, Player owner, InteractionHand hand) {
            this.hook = hook;
            this.owner = owner;
            this.dimension = hook.level().dimension();
            this.hand = hand;
            this.rod = owner.getItemInHand(hand);
            this.cycle = new FlyingHookCycle(hook.getUUID(), owner);
        }
    }

    public record Anchor(byte phase, Vec3 position, int entityId, Vec3 offset) {}

    public FlyingHookController(BiPredicate<Player, ItemStack> eligible,
            BiConsumer<FishingHook, Vec3> syncAnchor) {
        this.eligible = eligible;
        this.syncAnchor = syncAnchor;
    }

    public void start(FishingHook hook, Player owner, InteractionHand hand) {
        if (!(hook.level() instanceof ServerLevel) || !eligible.test(owner, owner.getItemInHand(hand))) return;
        Cast replaced = casts.get(owner.getUUID());
        if (replaced != null && replaced.owner == owner && replaced.hook == hook) return;
        if (replaced != null) finish(replaced, true);
        owner.fishing = hook; // Discarding an older cast must not erase this new vanilla cast.
        Cast cast = new Cast(hook, owner, hand);
        casts.put(owner.getUUID(), cast);
        hook.setDeltaMovement(hook.getDeltaMovement().scale(MovementMath.PROJECTILE_SPEED_MULTIPLIER));
        ((FlyingHookAccess) hook).wsee$setOffhand(hand == InteractionHand.OFF_HAND);
        syncPhase(cast);
    }

    private static void syncPhase(Cast cast) {
        ((FlyingHookAccess) cast.hook).wsee$setPhase(cast.cycle.phase());
    }

    private Cast current(FishingHook hook) {
        Player owner = hook.getPlayerOwner();
        Cast cast = owner == null ? null : casts.get(owner.getUUID());
        return cast != null && cast.owner == owner && cast.hook == hook ? cast : null;
    }

    private boolean valid(Cast cast) {
        Player player = cast.owner;
        FishingHook hook = cast.hook;
        return hook.isAlive() && hook.getPlayerOwner() == player && player.isAlive() && player.canInteractWithLevel()
                && !player.isSpectator() && !player.isPassenger() && !player.isFallFlying()
                && !player.getAbilities().flying && player.level() == hook.level()
                && cast.dimension.equals(player.level().dimension()) && player.fishing == hook
                && player.getItemInHand(cast.hand) == cast.rod && eligible.test(player, cast.rod)
                && player.distanceToSqr(hook) <= MovementMath.MAX_DISTANCE * MovementMath.MAX_DISTANCE
                && hook.level().hasChunkAt(hook.blockPosition());
    }

    /** Called once at the real entity's tick; cancels vanilla fishing buoyancy/loot states. */
    public boolean tickHook(FishingHook hook) {
        Cast cast = current(hook);
        if (cast == null) {
            if (hook.level() instanceof ServerLevel && ((FlyingHookAccess) hook).wsee$getPhase() != INACTIVE) {
                hook.discard();
                return true;
            }
            return false;
        }
        if (!valid(cast)) { finish(cast, true); return true; }
        hook.baseTick();
        if (cast.cycle.phase() == FLYING) {
            Vec3 movement = MovementMath.flyingVelocity(hook.getDeltaMovement(), hook.getGravity());
            hook.setDeltaMovement(movement);
            AABB sweptBounds = hook.getBoundingBox().expandTowards(movement).inflate(1.0D);
            if (!chunksAvailable(hook.level(), sweptBounds.getMinPosition(), sweptBounds.getMaxPosition())) {
                finish(cast, true); return true;
            }
            HitResult hit = ProjectileUtil.getHitResultOnMoveVector(hook,
                    target -> target instanceof LivingEntity && target != cast.owner
                            && target.isAlive() && !target.isSpectator()
                            && !cast.owner.isPassengerOfSameVehicle(target), ClipContext.Block.COLLIDER);
            hit = firstBlockContact(hook, movement, sweptBounds, hit);
            if (anchor(cast, hit)) return true;
            hook.move(MoverType.SELF, movement);
            hook.setDeltaMovement(movement.scale(0.92D));
        } else {
            Vec3 anchor = resolve(cast);
            if (anchor == null) { finish(cast, true); return true; }
            cast.anchor = anchor;
            hook.getInterpolation().cancel();
            hook.setPos(anchor);
            hook.setDeltaMovement(Vec3.ZERO);
            hook.setOldPosAndRot();
            hook.syncPosition = true;
            hook.syncVelocity = true;
            // Re-send for clients beginning to track an already fixed hook.
            if (cast.target != null || hook.level().getGameTime() % 5 == 0) syncAnchor.accept(hook, anchor);
        }
        return true;
    }

    private static HitResult firstBlockContact(FishingHook hook, Vec3 movement, AABB area, HitResult rayHit) {
        double length = movement.length();
        if (length < 1.0E-7D) return rayHit;
        double first = rayHit.getType() == HitResult.Type.MISS ? Double.POSITIVE_INFINITY
                : hook.position().distanceTo(rayHit.getLocation()) / length;
        HitResult result = rayHit;
        CollisionContext context = CollisionContext.of(hook);
        for (BlockPos pos : BlockPos.betweenClosed(BlockPos.containing(area.minX, area.minY, area.minZ),
                BlockPos.containing(area.maxX, area.maxY, area.maxZ))) {
            VoxelShape shape = hook.level().getBlockState(pos).getCollisionShape(hook.level(), pos, context);
            for (AABB box : shape.toAabbs()) {
                FlyingHookCollision.Contact contact = FlyingHookCollision.sweep(hook.getBoundingBox(), movement,
                        box.move(pos), pos);
                if (contact != null && contact.fraction() < first) {
                    first = contact.fraction();
                    result = contact.hit();
                }
            }
        }
        return result;
    }

    private static boolean chunksAvailable(Level level, Vec3 from, Vec3 to) {
        int minX = BlockPos.containing(Math.min(from.x, to.x), 0, 0).getX() >> 4;
        int maxX = BlockPos.containing(Math.max(from.x, to.x), 0, 0).getX() >> 4;
        int minZ = BlockPos.containing(0, 0, Math.min(from.z, to.z)).getZ() >> 4;
        int maxZ = BlockPos.containing(0, 0, Math.max(from.z, to.z)).getZ() >> 4;
        for (int x = minX; x <= maxX; x++) for (int z = minZ; z <= maxZ; z++) {
            if (!level.hasChunk(x, z)) return false;
        }
        return true;
    }

    private boolean anchor(Cast cast, HitResult hit) {
        if (hit.getType() != HitResult.Type.MISS && hit.getLocation().distanceToSqr(cast.owner.position())
                > MovementMath.MAX_DISTANCE * MovementMath.MAX_DISTANCE) {
            finish(cast, true);
            return true;
        }
        if (hit instanceof BlockHitResult blockHit && hit.getType() == HitResult.Type.BLOCK) {
            cast.block = blockHit.getBlockPos().immutable();
            cast.face = blockHit.getDirection();
            cast.anchor = blockHit.getLocation();
            if (!blockSupported(cast)) { cast.block = null; cast.anchor = null; return false; }
            cast.durability = 2;
        } else if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target) {
            cast.target = target;
            cast.anchor = hit.getLocation();
            cast.offset = cast.anchor.subtract(target.position());
            cast.durability = 5;
        } else return false;
        if (!cast.cycle.anchor()) return false;
        syncPhase(cast);
        cast.hook.getInterpolation().cancel();
        cast.hook.setPos(cast.anchor);
        cast.hook.setDeltaMovement(Vec3.ZERO);
        cast.hook.setOldPosAndRot();
        cast.hook.syncPosition = true;
        cast.hook.syncVelocity = true;
        syncAnchor.accept(cast.hook, cast.anchor);
        ServerLevel level = (ServerLevel) cast.hook.level();
        level.playSound(null, BlockPos.containing(cast.anchor), SoundEvents.FISHING_BOBBER_RETRIEVE,
                SoundSource.PLAYERS, 0.75F, 1.2F);
        level.sendParticles(ParticleTypes.CLOUD, cast.anchor.x, cast.anchor.y, cast.anchor.z,
                5, 0.18D, 0.18D, 0.18D, 0.02D);
        return true;
    }

    private static boolean blockSupported(Cast cast) {
        Level level = cast.hook.level();
        if (!level.hasChunkAt(cast.block)) return false;
        VoxelShape shape = level.getBlockState(cast.block).getCollisionShape(level, cast.block);
        return MovementMath.supportsAnchor(shape, cast.block, cast.face, cast.anchor);
    }

    private static Vec3 resolve(Cast cast) {
        if (cast.target != null) {
            LivingEntity target = cast.target;
            if (!target.isAlive() || target.isRemoved() || target.level() != cast.hook.level()
                    || !target.level().hasChunkAt(target.blockPosition())) return null;
            Vec3 anchor = target.position().add(cast.offset);
            return anchor.distanceToSqr(cast.owner.position()) <= MovementMath.MAX_DISTANCE * MovementMath.MAX_DISTANCE
                    && target.level().hasChunkAt(BlockPos.containing(anchor)) ? anchor : null;
        }
        return cast.block != null && blockSupported(cast) ? cast.anchor : null;
    }

    /** Client actions include hook identity and expected phase, so old/duplicate requests are inert. */
    public boolean use(Player player, UUID expectedHook, byte expectedPhase, InteractionHand hand) {
        Cast cast = casts.get(player.getUUID());
        if (cast == null || !cast.cycle.matchesRequest(player, expectedHook, expectedPhase)
                || cast.hand != hand || !valid(cast)) return false;
        if (cast.cycle.phase() == ANCHORED) {
            beginReeling(cast);
        } else {
            finish(cast, true);
        }
        return true;
    }

    /** Fallback for vanilla use packets; no active Flying Hook ever enters fishing retrieve. */
    public int retrieve(FishingHook hook, ItemStack rod) {
        Cast cast = current(hook);
        if (cast == null) { hook.discard(); return 0; }
        if (!valid(cast) || rod != cast.rod) { finish(cast, true); return 0; }
        if (cast.cycle.phase() == ANCHORED) { beginReeling(cast); return 0; }
        int cost = cast.durability;
        finish(cast, false);
        return cost;
    }

    private void beginReeling(Cast cast) {
        Vec3 anchor = resolve(cast);
        if (anchor == null) { finish(cast, true); return; }
        if (!cast.cycle.beginReeling()) return;
        cast.anchor = anchor;
        cast.ropeLength = anchor.distanceTo(cast.owner.getBoundingBox().getCenter()) + MovementMath.REEL_SPEED;
        cast.savedFallDistance = cast.owner.fallDistance;
        syncPhase(cast);
        syncAnchor.accept(cast.hook, anchor);
    }

    public void tickPlayer(Player player) {
        Cast cast = casts.get(player.getUUID());
        if (cast == null) return;
        if (cast.owner != player) { finish(cast, true); return; }
        if (!valid(cast)) { finish(cast, true); return; }
        long tick = player.level().getGameTime();
        if (!cast.cycle.takeForceTick(tick)) return;
        Vec3 anchor = resolve(cast);
        if (anchor == null) { finish(cast, true); return; }
        double gap = Math.sqrt(player.getBoundingBox().distanceToSqr(anchor));
        if (gap <= MovementMath.ARRIVAL_DISTANCE || ++cast.ticks > MovementMath.REEL_TIMEOUT) {
            finish(cast, true); return;
        }
        Vec3 toAnchor = anchor.subtract(player.getBoundingBox().getCenter());
        boolean taut = toAnchor.length() > cast.ropeLength;
        Vec3 probe = toAnchor.normalize().scale(0.05D);
        // As in ServerMotion, a new accepted movement sample is required:
        // network latency alone must not count as an obstructed attempt.
        Vec3 observation = player.getKnownMovement();
        Vec3 actualMovement = cast.lastPullPosition == null || observation == cast.lastPullMovement
                ? null : player.position().subtract(cast.lastPullPosition);
        boolean blockedAlongRope = FlyingHookCollision.blocksPull(player.getBoundingBox(), probe, actualMovement,
                player.onGround(), player.level().getBlockCollisions(player,
                        player.getBoundingBox().expandTowards(probe).inflate(1.0E-7D)));
        // Observe a real movement attempt before cancelling; a supporting floor
        // after a downward cast and successful vanilla stepping are not walls.
        if (taut && blockedAlongRope) { finish(cast, true); return; }
        if (taut && actualMovement != null && (player.horizontalCollision || player.verticalCollision) && !player.onGround()
                && !Double.isNaN(cast.lastGap) && cast.lastGap - gap < 0.01D) cast.stuckTicks++;
        else cast.stuckTicks = 0;
        cast.lastGap = gap;
        if (cast.stuckTicks >= MovementMath.STUCK_TICKS) { finish(cast, true); return; }
        Vec3 current = ServerMotion.basis(player);
        MovementMath.Pull pull = MovementMath.reel(current, toAnchor, gap, cast.ropeLength);
        cast.ropeLength = pull.ropeLength();
        if (!pull.velocity().equals(current)) {
            Vec3 delta = pull.velocity().subtract(current);
            double limit = delta.dot(toAnchor) > 0.0D ? MovementMath.radialSpeedLimit(gap) : -1.0D;
            ServerMotion.applyImpulse(player, delta, limit);
            cast.lastPullPosition = player.position();
            cast.lastPullMovement = observation;
        } else if (cast.lastPullPosition != null) {
            cast.lastPullPosition = player.position();
            cast.lastPullMovement = observation;
        }
        player.fallDistance = 0.0D; // Existing hook rule; restored on cancellation, not permanent immunity.
        if (cast.ticks % 4 == 0) ((ServerLevel) player.level()).sendParticles(ParticleTypes.CLOUD,
                player.getX(), player.getY() + 0.5D, player.getZ(), 1, 0.12D, 0.12D, 0.12D, 0.01D);
    }

    private void finish(Cast cast, boolean charge) {
        finish(cast, charge, true);
    }

    private void finish(Cast cast, boolean charge, boolean discard) {
        if (!casts.remove(cast.owner.getUUID(), cast)) return;
        boolean wasReeling = cast.cycle.phase() == REELING;
        if (!cast.cycle.finish()) return;
        if (wasReeling && !cast.owner.onGround())
            cast.owner.fallDistance = Math.max(cast.savedFallDistance, cast.owner.fallDistance);
        syncPhase(cast);
        if (charge && cast.durability > 0 && !cast.owner.isCreative())
            cast.rod.hurtAndBreak(cast.durability, cast.owner, cast.hand.asEquipmentSlot());
        cast.durability = 0;
        cast.target = null;
        cast.block = null;
        cast.anchor = null;
        cast.offset = null;
        FishingHook newerHook = cast.owner.fishing;
        if (discard) cast.hook.discard();
        // Vanilla remove clears the owner's fishing field even when this is an
        // older hook. Its cleanup must not detach a more recent valid cast.
        if (newerHook != null && newerHook != cast.hook && newerHook.isAlive()
                && newerHook.getPlayerOwner() == cast.owner) cast.owner.fishing = newerHook;
    }

    public Anchor snapshot(FishingHook hook) {
        Cast cast = current(hook);
        return cast == null || cast.anchor == null ? null
                : new Anchor(cast.cycle.phase(), cast.anchor, cast.target == null ? 0 : cast.target.getId(),
                        cast.offset == null ? Vec3.ZERO : cast.offset);
    }

    public void noteDoubleJump(Player player) {
        Cast cast = casts.get(player.getUUID());
        if (cast != null && cast.owner == player && cast.cycle.phase() == REELING) {
            cast.savedFallDistance = 0.0D;
        }
    }

    public void remove(FishingHook hook) {
        Cast cast = current(hook);
        // During death, disconnect or dimension removal the vanilla owner
        // lookup can already be unavailable. The stored cast must still end.
        if (cast == null) {
            for (Cast candidate : casts.values()) {
                if (candidate.hook == hook) { cast = candidate; break; }
            }
        }
        if (cast != null) finish(cast, true, false);
    }
    public void clear(Player player) {
        Cast cast = casts.get(player.getUUID());
        if (cast != null && cast.owner == player) finish(cast, true);
    }
    public void clearAll() { for (Cast cast : casts.values().toArray(Cast[]::new)) finish(cast, true); }
}
