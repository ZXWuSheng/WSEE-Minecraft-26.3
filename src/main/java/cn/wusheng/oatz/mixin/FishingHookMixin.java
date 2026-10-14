package cn.wusheng.oatz.mixin;

import cn.wusheng.oatz.AirForceFishing;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FishingHook.class)
public abstract class FishingHookMixin {
    @Redirect(
            method = "retrieve",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/storage/loot/LootTable;getRandomItems(Lnet/minecraft/world/level/storage/loot/LootParams;)Lit/unimi/dsi/fastutil/objects/ObjectArrayList;"
            )
    )
    private ObjectArrayList<ItemStack> wsee$replaceAirForceLoot(LootTable lootTable, LootParams params) {
        ObjectArrayList<ItemStack> vanillaLoot = lootTable.getRandomItems(params);
        // The fishing rod is the TOOL parameter used to build this exact loot
        // context.  A redirect handler can only receive the invocation's
        // receiver and arguments; the enclosing retrieve(ItemStack) argument
        // is not on the invocation stack.  Reading it from the context keeps
        // Air Force reliable when it is combined with other enchantments.
        ItemStack rod = params.contextMap().get(LootContextParams.TOOL) instanceof ItemStack stack
                ? stack
                : ItemStack.EMPTY;
        return new ObjectArrayList<>(AirForceFishing.replaceLoot((FishingHook) (Object) this, rod, vanillaLoot));
    }

}
