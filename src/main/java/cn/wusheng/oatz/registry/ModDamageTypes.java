package cn.wusheng.oatz.registry;

import cn.wusheng.oatz.WseeMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {
    private static final String MOD_ID = WseeMod.MOD_ID;
    public static final ResourceKey<DamageType> BLEEDING_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "bleeding"));
    public static final ResourceKey<DamageType> EXPLOSIVE_ARROW_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "explosive_arrow"));
    public static final ResourceKey<DamageType> SHOCK_RING_DAMAGE_TYPE = ResourceKey.create(
            Registries.DAMAGE_TYPE, Identifier.fromNamespaceAndPath(MOD_ID, "shock_ring"));

    private ModDamageTypes() {
    }
}
