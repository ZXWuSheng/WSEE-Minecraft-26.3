package cn.wusheng.oatz.qa;

import cn.wusheng.oatz.DoubleJumpPayload;
import cn.wusheng.oatz.MovementImpulsePayload;
import cn.wusheng.oatz.FlyingHookAnchorPayload;
import cn.wusheng.oatz.FlyingHookUsePayload;
import cn.wusheng.oatz.ShieldBashPayload;
import cn.wusheng.oatz.SwordAuraPayload;
import cn.wusheng.oatz.TrackingTargetPayload;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.MinecraftRegisterPayload;
import net.neoforged.neoforge.network.payload.MinecraftUnregisterPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkPayload;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryComponent;
import net.neoforged.neoforge.network.payload.ModdedNetworkQueryPayload;
import net.neoforged.neoforge.network.payload.ExtensibleEnumDataPayload;
import net.neoforged.neoforge.network.payload.ExtensibleEnumAcknowledgePayload;
import net.neoforged.neoforge.network.payload.ConfigFilePayload;
import net.neoforged.neoforge.network.configuration.CheckExtensibleEnums;
import net.neoforged.neoforge.network.registration.ChannelAttributes;
import net.neoforged.neoforge.network.registration.NetworkRegistry;
import net.neoforged.neoforge.network.registration.PayloadRegistration;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;

/** 测试客户端执行真实 NeoForge 通道协商，只声明当前 WSEE 的实际网络通道。 */
public final class NeoBotNetworking {
    public static ConnectionType connectionType(Connection connection) {
        ConnectionType type = ChannelAttributes.getConnectionType(connection);
        return type == null ? ConnectionType.NEOFORGE : type;
    }

    public static void onPayload(Connection connection, CustomPacketPayload payload) {
        if (payload instanceof ModdedNetworkQueryPayload) {
            ChannelAttributes.setConnectionType(connection, ConnectionType.NEOFORGE);
            Set<ModdedNetworkQueryComponent> channels = new HashSet<>(Set.of(
                    channel(DoubleJumpPayload.TYPE, PacketFlow.SERVERBOUND),
                    channel(FlyingHookUsePayload.TYPE, PacketFlow.SERVERBOUND),
                    channel(ShieldBashPayload.TYPE, PacketFlow.SERVERBOUND),
                    channel(SwordAuraPayload.TYPE, PacketFlow.SERVERBOUND),
                    channel(FlyingHookAnchorPayload.TYPE, PacketFlow.CLIENTBOUND),
                    channel(TrackingTargetPayload.TYPE, PacketFlow.CLIENTBOUND),
                    channel(MovementImpulsePayload.TYPE, PacketFlow.CLIENTBOUND)));
            channels.addAll(playChannels());
            connection.send(new ServerboundCustomPayloadPacket(new ModdedNetworkQueryPayload(
                    Map.of(ConnectionProtocol.PLAY, channels, ConnectionProtocol.CONFIGURATION, Set.of(
                            new ModdedNetworkQueryComponent(ExtensibleEnumDataPayload.TYPE.id(), "1",
                                    Optional.of(PacketFlow.CLIENTBOUND), true),
                            new ModdedNetworkQueryComponent(ExtensibleEnumAcknowledgePayload.TYPE.id(), "1",
                                    Optional.of(PacketFlow.SERVERBOUND), true),
                            new ModdedNetworkQueryComponent(ConfigFilePayload.TYPE.id(), "1",
                                    Optional.of(PacketFlow.CLIENTBOUND), true))))));
        } else if (payload instanceof ModdedNetworkPayload setup) {
            ChannelAttributes.setPayloadSetup(connection, setup.setup());
        } else if (payload instanceof MinecraftRegisterPayload register) {
            ChannelAttributes.getOrCreateAdHocChannels(connection).addAll(register.newChannels());
        } else if (payload instanceof MinecraftUnregisterPayload unregister) {
            ChannelAttributes.getOrCreateAdHocChannels(connection).removeAll(unregister.forgottenChannels());
        } else if (payload instanceof ExtensibleEnumDataPayload enums) {
            try {
                var getter = CheckExtensibleEnums.class.getDeclaredMethod("getEnumEntries");
                getter.setAccessible(true);
                if (!getter.invoke(null).equals(enums.enumEntries()))
                    throw new IllegalStateException("QA 客户端与服务端枚举表不一致");
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException(error);
            }
            connection.send(new ServerboundCustomPayloadPacket(ExtensibleEnumAcknowledgePayload.INSTANCE));
        }
    }

    private static ModdedNetworkQueryComponent channel(CustomPacketPayload.Type<?> type, PacketFlow flow) {
        return new ModdedNetworkQueryComponent(type.id(), "4", Optional.of(flow), false);
    }

    @SuppressWarnings("unchecked")
    private static Set<ModdedNetworkQueryComponent> playChannels() {
        try {
            var field = NetworkRegistry.class.getDeclaredField("PAYLOAD_REGISTRATIONS");
            field.setAccessible(true);
            var registrations = (Map<ConnectionProtocol, Map<net.minecraft.resources.Identifier, PayloadRegistration<?>>>) field.get(null);
            Set<ModdedNetworkQueryComponent> result = new HashSet<>();
            for (var registration : registrations.get(ConnectionProtocol.PLAY).values())
                result.add(new ModdedNetworkQueryComponent(registration));
            return result;
        } catch (ReflectiveOperationException error) {
            throw new IllegalStateException(error);
        }
    }
}
