package cn.wusheng.oatz.mixin;

import cn.wusheng.oatz.AirForceFishing;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Redirect(
            method = "retrieve",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
            )
    )
    private ObjectArrayList<ItemStack> wsee$replaceAirForceLoot(LootTable lootTable, LootParams params, ItemStack rod) {
        ObjectArrayList<ItemStack> vanillaLoot = lootTable.getRandomItems(params);
        return new ObjectArrayList<>(AirForceFishing.replaceLoot((FishingHook) (Object) this, rod, vanillaLoot));
    }

    @Inject(method = "getDefaultGravity", at = @At("HEAD"), cancellable = true)
    private void wsee$honorNoGravity(CallbackInfoReturnable<Double> callback) {
        if (((FishingHook) (Object) this).isNoGravity()) {
            callback.setReturnValue(0.0D);
        }
    }

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
