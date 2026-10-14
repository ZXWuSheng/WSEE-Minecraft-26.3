package cn.wusheng.oatz.mixin;

import cn.wusheng.oatz.FlyingHookUsePayload;
import cn.wusheng.oatz.WseeMod;
import cn.wusheng.oatz.access.FlyingHookAccess;
import cn.wusheng.oatz.access.FlyingHookInputAccess;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** A held use key must not cycle cast / reel / cancel through vanilla auto-repeat. */
@Mixin(Minecraft.class)
public abstract class FlyingHookInputMixin implements FlyingHookInputAccess {
    @Unique
    private boolean wsee$flyingHookUseConsumed;

    @Override
    public void wsee$releaseFlyingHookUse() {
        this.wsee$flyingHookUseConsumed = false;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    private void wsee$releaseUseLatch(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.player == null || minecraft.level == null || !minecraft.options.keyUse.isDown()) {
            this.wsee$flyingHookUseConsumed = false;
        } else if (minecraft.gui.screen() != null || minecraft.gui.overlay() != null) {
            this.wsee$flyingHookUseConsumed = true;
        }
    }

    @Inject(method = "startUseItem", at = @At("HEAD"), cancellable = true)
    private void wsee$useFlyingHookOnce(CallbackInfo ci) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (minecraft.player == null || minecraft.level == null
                || minecraft.gui.screen() != null || minecraft.gui.overlay() != null) {
            return;
        }
        InteractionHand hand = WseeMod.hasFlyingHook(minecraft.player, minecraft.player.getMainHandItem())
                ? InteractionHand.MAIN_HAND
                : WseeMod.hasFlyingHook(minecraft.player, minecraft.player.getOffhandItem())
                    ? InteractionHand.OFF_HAND : null;
        FishingHook hook = minecraft.player.fishing;
        byte phase = hook == null ? 0 : ((FlyingHookAccess) hook).wsee$getPhase();
        if (phase != 0) {
            hand = ((FlyingHookAccess) hook).wsee$isOffhand() ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        }
        if (hand == null && phase == 0) {
            return;
        }
        if (this.wsee$flyingHookUseConsumed) {
            ci.cancel();
            return;
        }
        this.wsee$flyingHookUseConsumed = true;
        if (phase != 0) {
            // Bind the operation to this exact hook and phase. No predicted pull
            // and no accompanying vanilla use packet can affect a newer cast.
            if (hand != null) {
                ClientPacketDistributor.sendToServer(new FlyingHookUsePayload(hook.getUUID(), phase, hand == InteractionHand.OFF_HAND));
            }
            ci.cancel();
        }
    }
}
