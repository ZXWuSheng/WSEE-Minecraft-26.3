package cn.wusheng.oatz.projectile;

import net.minecraft.world.entity.projectile.arrow.AbstractArrow;

import java.lang.reflect.Field;

/** Shared access to the current arrow base damage before custom shot effects. */
public final class ProjectileRules {
    private static final Field ARROW_BASE_DAMAGE_FIELD = findArrowBaseDamageField();

    private ProjectileRules() {
    }

    private static Field findArrowBaseDamageField() {
        try {
            Field field = AbstractArrow.class.getDeclaredField("baseDamage");
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    public static double readArrowBaseDamage(AbstractArrow arrow) {
        if (ARROW_BASE_DAMAGE_FIELD != null) {
            try {
                return ARROW_BASE_DAMAGE_FIELD.getDouble(arrow);
            } catch (IllegalAccessException ignored) {
                // Fall through to vanilla's default when access is unavailable.
            }
        }
        return 2.0D;
    }
}
