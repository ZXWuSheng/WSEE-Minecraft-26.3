package cn.wusheng.oatz.mixin;

import net.minecraft.world.entity.projectile.FishingHook;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @ModifyConstant(
            method = "shouldStopFishing",
            constant = @Constant(doubleValue = 1024.0D),
            require = 1,
            allow = 1
    )
    private double wsee$extendFlyingHookRange(double originalLimit) {
        int level = ((FishingHook) (Object) this).getPersistentData().getIntOr("wsee_flying_hook_level", 0);
        return switch (level) {
            case 1 -> 24.0D * 24.0D;
            case 2 -> 30.0D * 30.0D;
            case 3 -> 36.0D * 36.0D;
            case 4 -> 42.0D * 42.0D;
            case 5 -> 48.0D * 48.0D;
            default -> originalLimit;
        };
    }
}
