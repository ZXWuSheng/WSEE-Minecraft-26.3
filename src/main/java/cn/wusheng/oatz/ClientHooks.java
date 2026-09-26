package cn.wusheng.oatz;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RenderLivingEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import com.mojang.blaze3d.platform.InputConstants;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.api.distmarker.Dist;

@EventBusSubscriber(modid = WseeMod.MOD_ID, value = Dist.CLIENT)
public final class ClientHooks {
    private static final long DOUBLE_CLICK_WINDOW_NANOS = 500_000_000L;
    private static long lastShieldUseClick;
    private static boolean jumpKeyWasDown;
    private static boolean doubleJumpWasOnGround;
    private static final java.util.Map<Integer, Boolean> TRACKING_INDICATORS = new java.util.HashMap<>();

    private ClientHooks() {
    }

    public static void handleTrackingTarget(TrackingTargetPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }
        Entity target = minecraft.level.getEntity(payload.entityId());
        if (payload.glowing()) {
            if (target != null) {
                TRACKING_INDICATORS.putIfAbsent(payload.entityId(), target.hasGlowingTag());
                target.setGlowingTag(true);
            } else {
                TRACKING_INDICATORS.putIfAbsent(payload.entityId(), false);
            }
        } else {
            Boolean previous = TRACKING_INDICATORS.remove(payload.entityId());
            if (target != null && previous != null) {
                target.setGlowingTag(previous);
            }
        }
    }

    @SubscribeEvent
    public static void maintainTrackingIndicators(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            TRACKING_INDICATORS.clear();
            return;
        }
        for (Integer entityId : TRACKING_INDICATORS.keySet().toArray(Integer[]::new)) {
            Entity target = minecraft.level.getEntity(entityId);
            if (target != null) {
                target.setGlowingTag(true);
            }
        }
    }

    /** Poll the jump mapping as a fallback for clients that do not emit a key event for a quick second press. */
    @SubscribeEvent
    public static void pollDoubleJump(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            jumpKeyWasDown = false;
            doubleJumpWasOnGround = false;
            return;
        }
        boolean onGround = minecraft.player.onGround();
        if (minecraft.gui.screen() != null || minecraft.gui.overlay() != null) {
            // Keep the edge detector aligned while a screen is open. Otherwise
            // holding jump through a menu can create a fake second-press edge.
            jumpKeyWasDown = minecraft.options.keyJump.isDown();
            doubleJumpWasOnGround = onGround;
            return;
        }
        boolean down = minecraft.options.keyJump.isDown();
        if (down && !jumpKeyWasDown && !onGround && !doubleJumpWasOnGround
                && WseeMod.hasDoubleJump(minecraft.player)) {
            sendDoubleJumpPayload(minecraft.player);
        }
        jumpKeyWasDown = down;
        doubleJumpWasOnGround = onGround;
    }

    @SubscribeEvent
    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void renderTrackingIndicator(RenderLivingEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || TRACKING_INDICATORS.isEmpty()) {
            return;
        }
        var renderState = event.getRenderState();
        for (Integer entityId : TRACKING_INDICATORS.keySet()) {
            Entity target = minecraft.level.getEntity(entityId);
            if (target != null && target.getType() == renderState.entityType
                    && target.position().distanceToSqr(renderState.x, renderState.y, renderState.z) < 4.0D) {
                // Set the render-state outline directly so server metadata updates
                // cannot hide the shooter-local tracking indicator.
                renderState.outlineColor = 0x66CCFF;
                return;
            }
        }
    }

    @SubscribeEvent
    public static void onShieldUseClick(InputEvent.MouseButton.Pre event) {
        if (event.getAction() != InputConstants.PRESS) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gui.screen() != null || minecraft.gui.overlay() != null) {
            lastShieldUseClick = 0L;
            return;
        }
        LocalPlayer player = minecraft.player;

        // Use the platform constants: Minecraft 26.3's SDL input uses a different
        // numbering from the legacy GLFW assumptions (left is 1, right is 3).
        if (event.getButton() != InputConstants.MOUSE_BUTTON_RIGHT) {
            lastShieldUseClick = 0L;
            return;
        }
        if (player == null || !WseeMod.hasShieldBash(player)) {
            lastShieldUseClick = 0L;
            return;
        }

        long now = System.nanoTime();
        boolean doubleClick = now - lastShieldUseClick <= DOUBLE_CLICK_WINDOW_NANOS;
        lastShieldUseClick = now;
        if (!doubleClick) {
            return;
        }

        event.setCanceled(true);
        ClientPacketDistributor.sendToServer(new ShieldBashPayload());
    }

    @SubscribeEvent
    public static void onAttackClick(InputEvent.MouseButton.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        // Use the platform constant rather than a legacy raw button number.
        if (event.getButton() != InputConstants.MOUSE_BUTTON_LEFT
                || event.getAction() != InputConstants.PRESS
                || minecraft.gui.screen() != null
                || minecraft.gui.overlay() != null) {
            return;
        }
        LocalPlayer player = minecraft.player;
        if (player != null && !player.isUsingItem() && WseeMod.hasSwordAura(player)) {
            ClientPacketDistributor.sendToServer(new SwordAuraPayload());
        }
    }

    /** Apply the validated air-jump immediately to the locally predicted player. */
    public static void handleDoubleJumpVelocity(DoubleJumpVelocityPayload payload) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null) {
            return;
        }
        player.setDeltaMovement(payload.xVelocity(), payload.yVelocity(), payload.zVelocity());
        if (payload.sprinting()) {
            player.setSprinting(true);
        }
        player.fallDistance = 0.0F;
    }

    /**
     * Enchantment Descriptions supplies each WSEE description as one component. Split the
     * deliberate sentence separators into real tooltip rows without embedding a newline,
     * which this version of the descriptions mod renders as a visible LF marker.
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void splitWseeDescription(ItemTooltipEvent event) {
        var tooltip = event.getToolTip();
        for (int index = 0; index < tooltip.size(); index++) {
            Component component = tooltip.get(index);
            String text = component.getString();
            String[] rows = text.split("；|; ");
            if (rows.length <= 1) {
                continue;
            }
            tooltip.remove(index);
            for (int row = rows.length - 1; row >= 0; row--) {
                tooltip.add(index, Component.literal(rows[row].trim()).withStyle(ChatFormatting.DARK_GRAY));
            }
            index += rows.length - 1;
        }
    }

    @SubscribeEvent
    public static void onJumpKey(InputEvent.Key event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (event.getAction() != InputConstants.PRESS
                || event.getKey() != minecraft.options.keyJump.getKey().getValue()) {
            return;
        }
        if (minecraft.gui.screen() != null || minecraft.gui.overlay() != null || minecraft.player == null
                || !WseeMod.hasDoubleJump(minecraft.player)) {
            return;
        }
        jumpKeyWasDown = true;
        if (!minecraft.player.onGround()) {
            sendDoubleJumpPayload(minecraft.player);
        }
    }

    private static void sendDoubleJumpPayload(LocalPlayer player) {
        var motion = player.getDeltaMovement();
        ClientPacketDistributor.sendToServer(new DoubleJumpPayload(motion.x, motion.z, player.isSprinting()));
    }
}
