package cn.wusheng.oatz.mixin;

import cn.wusheng.oatz.WseeMod;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Keep confirmed grappling and upward jump momentum through climbable blocks. */
@Mixin(LivingEntity.class)
public abstract class ClimbingMovementMixin {
    @Inject(method = "handleOnClimbable", at = @At("HEAD"), cancellable = true)
    private void wsee$preserveFlyingHookVelocity(Vec3 movement, CallbackInfoReturnable<Vec3> callback) {
        if ((Object) this instanceof Player player && player.onClimbable() && WseeMod.isFlyingHookPulling(player)) {
            player.resetFallDistance();
            callback.setReturnValue(movement);
        }
    }

    @ModifyConstant(
            method = "handleRelativeFrictionAndCalculateMovement",
            constant = @Constant(doubleValue = 0.2D),
            require = 1,
            allow = 1
    )
    private double wsee$preserveDoubleJumpAscent(double climbingSpeed) {
        if ((Object) this instanceof Player player && player.onClimbable()) {
            // Read after move(): a ceiling collision must still cancel upward motion.
            // This runs on both logical sides, before vanilla gravity and drag.
            if (WseeMod.isFlyingHookPulling(player)) {
                return player.getDeltaMovement().y;
            }
            if (WseeMod.hasDoubleJump(player)) {
                return Math.max(climbingSpeed, player.getDeltaMovement().y);
            }
        }
        return climbingSpeed;
    }
}
