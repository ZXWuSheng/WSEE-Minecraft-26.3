package cn.wusheng.oatz.qa;

import cn.wusheng.oatz.MovementImpulsePayload;
import cn.wusheng.oatz.FlyingHookAnchorPayload;
import cn.wusheng.oatz.movement.MovementImpulseState;
import net.minecraft.network.Connection;
import net.minecraft.network.ConnectionProtocol;
import net.minecraft.network.DisconnectionDetails;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.common.*;
import net.minecraft.network.protocol.configuration.*;
import net.minecraft.network.protocol.cookie.*;
import net.minecraft.network.protocol.game.*;
import net.minecraft.network.protocol.login.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.EventLoopGroupHolder;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/** 隔离测试使用的 TCP 协议客户端：执行真实登录、配置、编解码及收发，不创建游戏窗口。 */
public final class ProtocolBot implements InvocationHandler {
    public final MinecraftServer server;
    public final String name;
    public Connection connection;
    public volatile int entityId = -1;
    public volatile boolean joined;
    public volatile int motionPackets, anchorPackets, teleportPackets;
    public volatile long lastAppliedImpulseSequence;
    public volatile Vec3 velocity = Vec3.ZERO;
    public volatile Vec3 position = Vec3.ZERO;
    public volatile String failure;
    public final Map<Integer, Vec3> entityPositions = new ConcurrentHashMap<>();
    public final Map<Integer, Vec3> entityVelocities = new ConcurrentHashMap<>();
    public final Map<UUID, FlyingHookAnchorPayload> anchors = new ConcurrentHashMap<>();
    public final ConcurrentLinkedQueue<Packet<?>> received = new ConcurrentLinkedQueue<>();
    private volatile ConnectionProtocol stage = ConnectionProtocol.LOGIN;
    private Object listener;
    private volatile Identifier dimension;
    private Object levelToken = new Object();
    private final MovementImpulseState impulses = new MovementImpulseState();

    private ProtocolBot(MinecraftServer server, String name) {
        this.server = server;
        this.name = name;
    }

    public static ProtocolBot start(MinecraftServer server, int port, String name) {
        ProtocolBot bot = new ProtocolBot(server, name);
        bot.connection = new Connection(PacketFlow.CLIENTBOUND) {
            @Override
            public void exceptionCaught(io.netty.channel.ChannelHandlerContext context, Throwable error) {
                error.printStackTrace();
                super.exceptionCaught(context, error);
            }
        };
        Connection.connect(new InetSocketAddress("127.0.0.1", port),
                EventLoopGroupHolder.remote(false), bot.connection).syncUninterruptibly();
        bot.listener = bot.listener(ClientLoginPacketListener.class);
        bot.connection.initiateServerboundPlayConnection("127.0.0.1", port,
                (ClientLoginPacketListener) bot.listener);
        bot.send(new ServerboundHelloPacket(name,
                UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8))));
        return bot;
    }

    private <T> T listener(Class<T> type) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, this));
    }

    public void send(Packet<?> packet) { connection.send(packet); }
    public void tick() {
        connection.tick();
    }
    public void close() { connection.disconnect(Component.literal("WSEE 隔离验证结束")); }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("toString")) return "WSEE TCP QA " + name + " " + stage;
        if (methodName.equals("hashCode")) return System.identityHashCode(proxy);
        if (methodName.equals("equals")) return proxy == args[0];
        if (methodName.equals("protocol")) return stage;
        if (methodName.equals("flow")) return PacketFlow.CLIENTBOUND;
        if (methodName.equals("getConnection")) return connection;
        if (methodName.equals("isAcceptingMessages") || methodName.equals("shouldHandleMessage")) return true;
        if (methodName.equals("getConnectionType")) return neo("connectionType", connection);
        if (methodName.equals("send")) {
            if (args[0] instanceof Packet<?> packet) send(packet);
            else send(new ServerboundCustomPayloadPacket((net.minecraft.network.protocol.common.custom.CustomPacketPayload) args[0]));
            return null;
        }
        if (methodName.equals("disconnect")) { connection.disconnect((Component) args[0]); return null; }
        if (methodName.equals("createDisconnectionInfo")) return new DisconnectionDetails((Component) args[0]);
        if (methodName.equals("onDisconnect")) {
            if (!server.isStopped()) failure = String.valueOf(args[0]);
            return null;
        }
        if (methodName.equals("onPacketError")) {
            failure = args[0].getClass().getSimpleName() + ": " + args[1];
            ((Exception) args[1]).printStackTrace();
            return null;
        }
        if (args != null && args.length == 1 && args[0] instanceof Packet<?> packet) {
            receive(packet);
            return null;
        }
        if (method.isDefault()) return InvocationHandler.invokeDefault(proxy, method, args);
        if (method.getReturnType() == boolean.class) return false;
        if (method.getReturnType() == int.class) return 0;
        return null;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private void receive(Packet<?> packet) {
        if (!(packet instanceof ClientboundLevelChunkWithLightPacket)
                && !(packet instanceof ClientboundLightUpdatePacket)) received.add(packet);
        if (received.size() > 4000) received.poll();
        if (packet instanceof ClientboundLoginCompressionPacket compression) {
            connection.setupCompression(compression.getCompressionThreshold(), false);
        } else if (packet instanceof ClientboundLoginFinishedPacket) {
            stage = ConnectionProtocol.CONFIGURATION;
            listener = listener(ClientConfigurationPacketListener.class);
            connection.setupInboundProtocol(ConfigurationProtocols.CLIENTBOUND, (ClientConfigurationPacketListener) listener);
            send(ServerboundLoginAcknowledgedPacket.INSTANCE);
            connection.setupOutboundProtocol(ConfigurationProtocols.SERVERBOUND);
            neo("configure", connection);
        } else if (packet instanceof ClientboundHelloPacket) {
            failure = "隔离测试服务器意外要求在线身份验证";
        } else if (packet instanceof ClientboundCustomQueryPacket query) {
            send(new ServerboundCustomQueryAnswerPacket(query.transactionId(), null));
        } else if (packet instanceof ClientboundCookieRequestPacket cookie) {
            send(new ServerboundCookieResponsePacket(cookie.key(), null));
        } else if (packet instanceof ClientboundSelectKnownPacks packs) {
            send(new ServerboundSelectKnownPacks(packs.knownPacks()));
        } else if (packet instanceof ClientboundCodeOfConductPacket) {
            send(ServerboundAcceptCodeOfConductPacket.INSTANCE);
        } else if (packet instanceof ClientboundFinishConfigurationPacket) {
            stage = ConnectionProtocol.PLAY;
            listener = listener(ClientGamePacketListener.class);
            connection.setupInboundProtocol(GameProtocols.CLIENTBOUND_TEMPLATE.bind(
                    RegistryFriendlyByteBuf.decorator(server.registryAccess())), (ClientGamePacketListener) listener);
            send(ServerboundFinishConfigurationPacket.INSTANCE);
            GameProtocols.Context context = (GameProtocols.Context) Proxy.newProxyInstance(
                    GameProtocols.Context.class.getClassLoader(), new Class<?>[]{GameProtocols.Context.class},
                    (p, m, a) -> false);
            connection.setupOutboundProtocol(GameProtocols.SERVERBOUND_TEMPLATE.bind(
                    RegistryFriendlyByteBuf.decorator(server.registryAccess()), context));
        } else if (packet instanceof ClientboundKeepAlivePacket keepAlive) {
            send(new ServerboundKeepAlivePacket(keepAlive.getId()));
        } else if (packet instanceof ClientboundPingPacket ping) {
            send(new ServerboundPongPacket(ping.getId()));
        } else if (packet instanceof ClientboundCustomPayloadPacket custom) {
            if (custom.payload() instanceof FlyingHookAnchorPayload anchor) {
                anchorPackets++;
                anchors.put(anchor.hookUuid(), anchor);
            }
            if (custom.payload() instanceof MovementImpulsePayload impulse) {
                // 单一模拟客户端 tick 线程接收运动，防止与物理步的读改写发生竞争。
                server.execute(() -> {
                    if (impulse.playerId() == entityId && impulse.dimension().equals(dimension)
                            && impulses.accept(this, levelToken, dimension, impulse.sessionUuid(), impulse.sequence())) {
                        Vec3 before = velocity;
                        Vec3 approved = MovementImpulseState.approvedDelta(before, impulse);
                        RuntimeSuite.observeApprovedImpulse(before, approved, impulse);
                        velocity = before.add(approved);
                        motionPackets++;
                        lastAppliedImpulseSequence = impulse.sequence();
                    }
                });
            }
            neo("onPayload", connection, custom.payload());
        } else if (packet instanceof ClientboundLoginPacket login) {
            entityId = login.playerId();
            dimension = login.commonPlayerSpawnInfo().dimension().identifier();
            joined = true;
            send(new ServerboundPlayerLoadedPacket());
        } else if (packet instanceof ClientboundRespawnPacket respawn) {
            dimension = respawn.commonPlayerSpawnInfo().dimension().identifier();
            levelToken = new Object();
            velocity = Vec3.ZERO;
            impulses.clear();
            send(new ServerboundPlayerLoadedPacket());
        } else if (packet instanceof ClientboundPlayerPositionPacket teleport) {
            teleportPackets++;
            PositionMoveRotation absolute = PositionMoveRotation.calculateAbsolute(
                    new PositionMoveRotation(position, velocity, 0, 0), teleport.change(), teleport.relatives());
            position = absolute.position();
            velocity = absolute.deltaMovement();
            send(new ServerboundAcceptTeleportationPacket(teleport.id(), position.x, position.y, position.z,
                    absolute.yRot(), absolute.xRot()));
        } else if (packet instanceof ClientboundSetEntityMotionPacket motion) {
            entityVelocities.put(motion.id(), motion.movement());
            if (motion.id() == entityId) { velocity = motion.movement(); motionPackets++; }
        } else if (packet instanceof ClientboundAddEntityPacket entity) {
            entityPositions.put(entity.getId(), new Vec3(entity.getX(), entity.getY(), entity.getZ()));
        } else if (packet instanceof ClientboundChunkBatchFinishedPacket) {
            send(new ServerboundChunkBatchReceivedPacket(64.0F));
        } else if (packet instanceof ClientboundBundlePacket bundle) {
            for (Packet<?> bundled : bundle.subPackets()) receive(bundled);
        } else if (packet instanceof ClientboundDisconnectPacket disconnect) {
            failure = disconnect.reason().getString();
        } else if (packet instanceof ClientboundLoginDisconnectPacket disconnect) {
            failure = disconnect.reason().getString();
        }
    }

    private static Object neo(String method, Object... args) {
        for (String className : List.of("NeoBotNetworking", "FabricBotNetworking")) {
            try {
                Class<?> helper = Class.forName("cn.wusheng.oatz.qa." + className);
                for (Method candidate : helper.getMethods()) {
                    if (candidate.getName().equals(method) && candidate.getParameterCount() == args.length)
                        return candidate.invoke(null, args);
                }
            } catch (ClassNotFoundException ignored) {
                // 两个加载器分别提供自己的协议协商辅助类。
            } catch (ReflectiveOperationException error) {
                throw new IllegalStateException("QA 网络协商失败", error);
            }
        }
        return null;
    }
}
