package cn.wusheng.oatz.mixin;

import cn.wusheng.oatz.access.FlyingHookInputAccess;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Handles fast mouse or keyboard releases which occur between client ticks. */
@Mixin(KeyMapping.class)
public abstract class FlyingHookUseKeyMixin {
    @Inject(method = "setDown", at = @At("HEAD"))
    private void wsee$releaseUse(boolean down, CallbackInfo ci) {
        if (!down) {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft != null && minecraft.options != null
                    && (Object) this == minecraft.options.keyUse) {
                ((FlyingHookInputAccess) minecraft).wsee$releaseFlyingHookUse();
            }
        }
    }
}
