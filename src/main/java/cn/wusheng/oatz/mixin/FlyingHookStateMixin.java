package cn.wusheng.oatz.mixin;

import cn.wusheng.oatz.WseeMod;
import cn.wusheng.oatz.access.FlyingHookAccess;
import cn.wusheng.oatz.registry.MovementAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Suppresses fishing physics and loot only for server-designated Flying Hook entities. */
@Mixin(FishingHook.class)
public abstract class FlyingHookStateMixin implements FlyingHookAccess {
    @Unique private int wsee$removalDepth;
    @Unique private Player wsee$removalOwner;
    @Unique private FishingHook wsee$newerHook;

    @Override
    public byte wsee$getPhase() {
        return ((FishingHook) (Object) this).getData(MovementAttachments.HOOK_PHASE);
    }

    @Override
    public void wsee$setPhase(byte phase) {
        ((FishingHook) (Object) this).setData(MovementAttachments.HOOK_PHASE, phase);
    }

    @Override
    public boolean wsee$isOffhand() {
        return ((FishingHook) (Object) this).getData(MovementAttachments.HOOK_OFFHAND);
    }

    @Override
    public void wsee$setOffhand(boolean offhand) {
        ((FishingHook) (Object) this).setData(MovementAttachments.HOOK_OFFHAND, offhand);
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void wsee$tickFlyingHook(CallbackInfo ci) {
        FishingHook hook = (FishingHook) (Object) this;
        if (this.wsee$getPhase() != 0 && WseeMod.tickFlyingHookEntity(hook)) {
            ci.cancel();
        }
    }

    @Inject(method = "retrieve", at = @At("HEAD"), cancellable = true)
    private void wsee$retrieveFlyingHook(ItemStack rod, CallbackInfoReturnable<Integer> cir) {
        if (this.wsee$getPhase() != 0) {
            cir.setReturnValue(WseeMod.retrieveFlyingHook((FishingHook) (Object) this, rod));
        }
    }

    @Inject(method = "remove", at = @At("HEAD"))
    private void wsee$clearFlyingHook(Entity.RemovalReason reason, CallbackInfo ci) {
        this.wsee$beginRemoval();
        WseeMod.removeFlyingHook((FishingHook) (Object) this);
    }

    @Inject(method = "remove", at = @At("TAIL"))
    private void wsee$preserveNewCast(Entity.RemovalReason reason, CallbackInfo ci) {
        this.wsee$endRemoval();
    }

    @Inject(method = "onClientRemoval", at = @At("HEAD"))
    private void wsee$clearClientFlyingHook(CallbackInfo ci) {
        this.wsee$beginRemoval();
        WseeMod.removeFlyingHook((FishingHook) (Object) this);
    }

    @Inject(method = "onClientRemoval", at = @At("TAIL"))
    private void wsee$preserveNewClientCast(CallbackInfo ci) {
        this.wsee$endRemoval();
    }

    @Unique
    private void wsee$beginRemoval() {
        if (this.wsee$removalDepth++ != 0) {
            return;
        }
        FishingHook hook = (FishingHook) (Object) this;
        Player owner = hook.getPlayerOwner();
        FishingHook current = owner == null ? null : owner.fishing;
        if (current != null && current != hook && ((FlyingHookAccess) current).wsee$getPhase() != 0) {
            this.wsee$removalOwner = owner;
            this.wsee$newerHook = current;
        }
    }

    @Unique
    private void wsee$endRemoval() {
        if (--this.wsee$removalDepth != 0) {
            return;
        }
        Player owner = this.wsee$removalOwner;
        FishingHook newer = this.wsee$newerHook;
        this.wsee$removalOwner = null;
        this.wsee$newerHook = null;
        FishingHook removed = (FishingHook) (Object) this;
        // Vanilla FishingHook.remove/onClientRemoval clear owner.fishing
        // unconditionally. An old entity's removal cannot revoke a newer cast.
        if (owner != null && newer != null && newer.isAlive() && newer.getPlayerOwner() == owner
                && newer.level() == owner.level() && ((FlyingHookAccess) newer).wsee$getPhase() != 0
                && (owner.fishing == null || owner.fishing == removed)) {
            owner.fishing = newer;
        }
    }
}
