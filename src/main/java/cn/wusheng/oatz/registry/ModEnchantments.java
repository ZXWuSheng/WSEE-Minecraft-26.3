package cn.wusheng.oatz.registry;

import cn.wusheng.oatz.WseeMod;
import net.minecraft.resources.Identifier;

import java.util.List;

public final class ModEnchantments {
    private static final String MOD_ID = WseeMod.MOD_ID;
    public static final Identifier ENCHANTMENT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shield_bash");
    public static final Identifier DEFLECT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "deflect");
    public static final Identifier FROSTBITE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "frostbite");
    public static final Identifier SWORD_AURA_ID = Identifier.fromNamespaceAndPath(MOD_ID, "sword_aura");
    public static final Identifier EXPANSION_ID = Identifier.fromNamespaceAndPath(MOD_ID, "expansion");
    public static final Identifier CLUSTER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "cluster");
    public static final Identifier ARMOR_PIERCING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "armor_piercing");
    public static final Identifier BLEEDING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "bleeding");
    public static final Identifier LONG_EDGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "long_edge");
    public static final Identifier SURGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "surge");
    public static final Identifier TETHER_ID = Identifier.fromNamespaceAndPath(MOD_ID, "tether");
    public static final Identifier REBOUND_ID = Identifier.fromNamespaceAndPath(MOD_ID, "rebound");
    public static final Identifier HYDROSHOCK_ID = Identifier.fromNamespaceAndPath(MOD_ID, "hydroshock");
    public static final Identifier OVERCHARGE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "overcharge");
    public static final Identifier COMBO_STRIKE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "combo_strike");
    public static final Identifier EXPLOSIVE_ARROW_ID = Identifier.fromNamespaceAndPath(MOD_ID, "explosive_arrow");
    public static final Identifier ARROW_RAIN_ID = Identifier.fromNamespaceAndPath(MOD_ID, "arrow_rain");
    public static final Identifier TRACKING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "tracking");
    public static final Identifier BATTLE_MOMENTUM_ID = Identifier.fromNamespaceAndPath(MOD_ID, "battle_momentum");
    public static final Identifier FORCED_MARCH_ID = Identifier.fromNamespaceAndPath(MOD_ID, "forced_march");
    public static final Identifier CLARITY_ID = Identifier.fromNamespaceAndPath(MOD_ID, "clarity");
    public static final Identifier EXECUTION_ID = Identifier.fromNamespaceAndPath(MOD_ID, "execution");
    public static final Identifier FALLING_STAR_ID = Identifier.fromNamespaceAndPath(MOD_ID, "falling_star");
    public static final Identifier SHOCK_RING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shock_ring");
    public static final Identifier SHIELD_ID = Identifier.fromNamespaceAndPath(MOD_ID, "shield");
    public static final Identifier HOLY_LIGHT_ID = Identifier.fromNamespaceAndPath(MOD_ID, "holy_light");
    public static final Identifier REFINING_ID = Identifier.fromNamespaceAndPath(MOD_ID, "refining");
    public static final Identifier FOUNDATION_ID = Identifier.fromNamespaceAndPath(MOD_ID, "foundation");
    public static final Identifier AIR_FORCE_ID = Identifier.fromNamespaceAndPath(MOD_ID, "air_force");
    public static final Identifier FLYING_HOOK_ID = Identifier.fromNamespaceAndPath(MOD_ID, "flying_hook");
    public static final Identifier DOUBLE_JUMP_ID = Identifier.fromNamespaceAndPath(MOD_ID, "double_jump");
    public static final List<Identifier> OATZ_ENCHANTMENT_IDS = List.of(
            SWORD_AURA_ID, ENCHANTMENT_ID, DEFLECT_ID, FROSTBITE_ID, EXPANSION_ID, CLUSTER_ID,
            ARMOR_PIERCING_ID, BLEEDING_ID, LONG_EDGE_ID, SURGE_ID, TETHER_ID, REBOUND_ID, HYDROSHOCK_ID,
            OVERCHARGE_ID, COMBO_STRIKE_ID, EXPLOSIVE_ARROW_ID, ARROW_RAIN_ID, TRACKING_ID, BATTLE_MOMENTUM_ID, FORCED_MARCH_ID, CLARITY_ID, EXECUTION_ID,
            FALLING_STAR_ID, SHOCK_RING_ID, SHIELD_ID, HOLY_LIGHT_ID, REFINING_ID, FOUNDATION_ID, AIR_FORCE_ID, FLYING_HOOK_ID, DOUBLE_JUMP_ID);

    private ModEnchantments() {
    }
}
