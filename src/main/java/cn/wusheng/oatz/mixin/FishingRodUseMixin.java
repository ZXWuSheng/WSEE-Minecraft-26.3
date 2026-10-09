package cn.wusheng.oatz.mixin;

import cn.wusheng.oatz.WseeMod;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Captures vanilla's actual casting hand before the real hook is spawned. */
@Mixin(FishingRodItem.class)
public abstract class FishingRodUseMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void wsee$rememberCastingHand(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<InteractionResult> cir) {
        if (!level.isClientSide() && player.fishing == null) {
            WseeMod.prepareFlyingHookCast(player, hand);
        }
    }
}
