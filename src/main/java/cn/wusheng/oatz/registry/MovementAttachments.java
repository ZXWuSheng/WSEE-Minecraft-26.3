package cn.wusheng.oatz.registry;

import cn.wusheng.oatz.WseeMod;
import net.minecraft.network.codec.ByteBufCodecs;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.function.Supplier;

/** 使用 NeoForge 官方同步附件，避免修改原版实体的同步数据 ID。 */
public final class MovementAttachments {
    private static final DeferredRegister<AttachmentType<?>> TYPES =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, WseeMod.MOD_ID);
    public static final Supplier<AttachmentType<Byte>> HOOK_PHASE = TYPES.register("flying_hook_phase",
            () -> AttachmentType.builder(() -> (byte) 0).sync(ByteBufCodecs.BYTE).build());
    public static final Supplier<AttachmentType<Boolean>> HOOK_OFFHAND = TYPES.register("flying_hook_offhand",
            () -> AttachmentType.builder(() -> false).sync(ByteBufCodecs.BOOL).build());

    private MovementAttachments() {}
    public static void register(IEventBus bus) { TYPES.register(bus); }
}
