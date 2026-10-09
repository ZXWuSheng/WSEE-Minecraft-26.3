package cn.wusheng.oatz.mixin;

import cn.wusheng.oatz.access.FlyingHookAccess;
import cn.wusheng.oatz.movement.FlyingHookController;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Collections;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.FishingHookRenderer;
import net.minecraft.client.renderer.entity.state.FishingHookRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keeps the existing hook model and rope, with the line starting at the exact anchor. */
@Mixin(FishingHookRenderer.class)
public abstract class FishingHookRendererMixin {
    @Unique
    private final Set<FishingHookRenderState> wsee$flyingHookStates =
            Collections.newSetFromMap(new WeakHashMap<>());

    @Shadow
    private Vec3 getPlayerHandPos(Player player, float swing, float partialTick) {
        throw new AssertionError();
    }

    @Inject(method = "getHoldingArm", at = @At("HEAD"), cancellable = true)
    private static void wsee$useCastingHand(Player player, CallbackInfoReturnable<HumanoidArm> cir) {
        FishingHook hook = player.fishing;
        if (hook != null && ((FlyingHookAccess) hook).wsee$getPhase() != FlyingHookController.INACTIVE) {
            cir.setReturnValue(((FlyingHookAccess) hook).wsee$isOffhand()
                    ? player.getMainArm().getOpposite() : player.getMainArm());
        }
    }

    @Inject(method = "extractRenderState(Lnet/minecraft/world/entity/projectile/FishingHook;Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;F)V",
            at = @At("TAIL"))
    private void wsee$exactLineOrigin(FishingHook hook, FishingHookRenderState state, float partialTick, CallbackInfo ci) {
        byte phase = ((FlyingHookAccess) hook).wsee$getPhase();
        if (phase == FlyingHookController.INACTIVE || hook.getPlayerOwner() == null) {
            this.wsee$flyingHookStates.remove(state);
            return;
        }
        this.wsee$flyingHookStates.add(state);
        if (phase >= FlyingHookController.ANCHORED) {
            state.x = hook.getX();
            state.y = hook.getY();
            state.z = hook.getZ();
        }
        Player owner = hook.getPlayerOwner();
        float swing = Mth.sin(Mth.sqrt(owner.getSwingAnimation(partialTick)) * Math.PI);
        Vec3 hand = this.getPlayerHandPos(owner, swing, partialTick);
        // Vanilla stringVertex adds +0.25 at both endpoints; the line pose below
        // removes that bobber offset without moving its endpoint away from the hand.
        state.lineOriginOffset = hand.subtract(new Vec3(state.x, state.y, state.z));
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("HEAD"))
    private void wsee$alignRope(FishingHookRenderState state, PoseStack pose, SubmitNodeCollector collector,
            CameraRenderState camera, CallbackInfo ci) {
        if (this.wsee$flyingHookStates.contains(state)) {
            pose.pushPose();
            pose.translate(0, -0.25D, 0);
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;pushPose()V", ordinal = 1, shift = At.Shift.AFTER))
    private void wsee$preserveHookModel(FishingHookRenderState state, PoseStack pose, SubmitNodeCollector collector,
            CameraRenderState camera, CallbackInfo ci) {
        if (this.wsee$flyingHookStates.contains(state)) {
            pose.translate(0, 0.25D, 0);
        }
    }

    @Inject(method = "submit(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",
            at = @At("RETURN"))
    private void wsee$restoreRopePose(FishingHookRenderState state, PoseStack pose, SubmitNodeCollector collector,
            CameraRenderState camera, CallbackInfo ci) {
        if (this.wsee$flyingHookStates.contains(state)) {
            pose.popPose();
        }
    }
}
