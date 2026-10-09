package cn.wusheng.oatz.qa;

import cn.wusheng.oatz.DoubleJumpPayload;
import cn.wusheng.oatz.FlyingHookUsePayload;
import cn.wusheng.oatz.MovementImpulsePayload;
import cn.wusheng.oatz.access.FlyingHookAccess;
import cn.wusheng.oatz.movement.MovementMath;
import cn.wusheng.oatz.movement.ServerMotion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.entity.player.Input;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Real dedicated-server players and TCP packets, with isolated deterministic fixtures.
 * The trajectory section explicitly invokes vanilla Player.travel on the server:
 * a headless protocol bot does not simulate client physics or render the rope.
 */
public final class RuntimeSuite {
    private static ProtocolBot a, b;
    private static ServerPlayer first, second;
    private static ServerLevel level;
    private static BufferedWriter trace;
    private static int tick, step, localTick, jumpCase, hookCase, cleanupCase, assertions, failures;
    private static final List<String> errors = new ArrayList<>();
    private static final Vec3[] MOTIONS = {
            Vec3.ZERO, new Vec3(.42, 0, .13), new Vec3(.15, .38, -.2),
            new Vec3(.2, -.9, .3), new Vec3(1.2, -1.4, -.8)};
    private static final String[] MOTION_NAMES = {"stationary", "sprint", "rising", "falling", "external"};
    private static final double[] DISTANCES = {8, 16, 31};
    private static FishingHook hook;
    private static Vec3 fixedAnchor, startPosition, jumpExpected;
    private static double minGap, maxRadial;
    private static boolean done, expectedDisconnect, clientGround;
    private static Vec3 cleanupVelocity;
    private static int duplicateMotionCount;
    private static int caseStartMotionCount;
    private static int terminationPacketCount, combinationRequestTick, combinationVerticalBefore;
    private static double combinationBeforeY;
    private static boolean combinationRequested, combinationChecked;
    private static RopeState combinationRope;
    private static BiConsumer<ServerPlayer, MovementImpulsePayload> originalTransport;
    private static final Map<UUID, Integer> outboundImpulses = new HashMap<>();
    private static int approvedForceSamples;
    private static int inwardForceSamples;
    private record ImpulseKey(UUID session, long sequence) {}
    private static final Map<ImpulseKey, Vec3> emittedRopeDirections = new HashMap<>();
    private record RopeState(Object cast, double length, int ticks) {}
    private static MinecraftServer activeServer;

    private RuntimeSuite() {}

    public static int outboundImpulseCount(ServerPlayer player) {
        return outboundImpulses.getOrDefault(player.getUUID(), 0);
    }

    /** Invoked only after the TCP payload has passed the real client sequence/session gate. */
    public static void observeApprovedImpulse(Vec3 before, Vec3 approved, MovementImpulsePayload payload) {
        if (done) return;
        approvedForceSamples++;
        Vec3 requested = payload.delta();
        check(ServerMotion.finite(before) && ServerMotion.finite(approved), "TCP approved motion remains finite");
        check(approved.lengthSqr() <= requested.lengthSqr() + 1E-12, "TCP client never adds more than server-approved increment");
        if (requested.lengthSqr() < 1E-16) return;
        Vec3 axis = requested.normalize();
        check(approved.cross(axis).lengthSqr() < 1E-16, "approved change follows only authorized radial axis");
        if (payload.radialLimit() < 0) {
            vector(approved, requested, "unlimited independent impulse is applied exactly once");
            return;
        }
        Vec3 emittedDirection = emittedRopeDirections.get(new ImpulseKey(payload.sessionUuid(), payload.sequence()));
        check(emittedDirection != null, "true TCP rope payload has an authoritative emission direction");
        if (emittedDirection != null) {
            inwardForceSamples++;
            check(requested.dot(emittedDirection) > 0, "true TCP hook force pulls inward and never brakes external motion");
            check(requested.cross(emittedDirection).lengthSqr() < 1E-16,
                    "true TCP hook force follows the anchor direction recorded at emission");
        }
        double oldRadial = before.dot(axis), newRadial = before.add(approved).dot(axis);
        check(newRadial <= Math.max(oldRadial, payload.radialLimit()) + 1E-9,
                "new rope increment obeys approved radial limit while preserving pre-existing overspeed");
        Vec3 oldTangent = before.subtract(axis.scale(oldRadial));
        Vec3 newTangent = before.add(approved).subtract(axis.scale(newRadial));
        vector(newTangent, oldTangent, "true TCP hook impulse preserves exact tangential inertia");
    }

    public static void start(MinecraftServer server) {
        try {
            activeServer = server;
            Field transport = ServerMotion.class.getDeclaredField("sender"); transport.setAccessible(true);
            @SuppressWarnings("unchecked")
            BiConsumer<ServerPlayer, MovementImpulsePayload> sender =
                    (BiConsumer<ServerPlayer, MovementImpulsePayload>) transport.get(null);
            if (sender == null) throw new IllegalStateException("production movement transport not registered");
            originalTransport = sender;
            ServerMotion.setSender((player, payload) -> {
                outboundImpulses.merge(player.getUUID(), 1, Integer::sum);
                if (payload.radialLimit() >= 0) {
                    FishingHook activeHook = player.fishing;
                    check(activeHook != null && ((FlyingHookAccess) activeHook).wsee$getPhase() == 3,
                            "only a currently reeling hook authorizes radial motion");
                    if (activeHook != null) emittedRopeDirections.put(
                            new ImpulseKey(payload.sessionUuid(), payload.sequence()),
                            activeHook.position().subtract(player.getBoundingBox().getCenter()).normalize());
                }
                sender.accept(player, payload);
            });
            Path directory = server.getServerDirectory();
            Files.createDirectories(directory);
            trace = Files.newBufferedWriter(directory.resolve("movement-qa-trace.jsonl"), StandardCharsets.UTF_8);
            log("start", "TCP login; real ServerPlayer; explicit vanilla travel physics; no client rendering");
            a = ProtocolBot.start(server, server.getPort(), "WseeQaA");
            b = ProtocolBot.start(server, server.getPort(), "WseeQaB");
        } catch (Throwable error) { fatal(server, error); }
    }

    public static void tick(MinecraftServer server) {
        if (done) return;
        try {
            tick++;
            if (a == null || b == null) return;
            if (!expectedDisconnect) a.tick();
            b.tick();
            if ((!expectedDisconnect && a.failure != null) || b.failure != null) throw new IllegalStateException("TCP bot: " + a.failure + " / " + b.failure);
            if (tick > 2600) throw new IllegalStateException("QA global timeout step=" + step);
            localTick++;
            switch (step) {
                case 0 -> {
                    first = server.getPlayerList().getPlayerByName("WseeQaA");
                    second = server.getPlayerList().getPlayerByName("WseeQaB");
                    if (first == null || second == null || !a.joined || !b.joined) {
                        if (localTick % 40 == 0) log("login-wait", "A=" + a.joined + "/" + a.connection.isConnected()
                                + " B=" + b.joined + "/" + b.connection.isConnected() + " packets="
                                + a.received.stream().skip(Math.max(0, a.received.size() - 3)).map(p -> p.getClass().getSimpleName()).toList()
                                + "/" + b.received.stream().skip(Math.max(0, b.received.size() - 3)).map(p -> p.getClass().getSimpleName()).toList());
                        return;
                    }
                    check(first != second && first.connection != second.connection, "two independent logged-in players");
                    level = server.overworld();
                    fixture();
                    equip(first); equip(second);
                    place(second, new Vec3(-4, 80, 8));
                    next(1);
                }
                case 1 -> {
                    if (localTick == 1) a.send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, false)));
                    if (localTick == 1) {
                        place(first, new Vec3(0, 80, 0)); place(second, new Vec3(-4, 80, 8));
                    }
                    first.setOnGround(true); first.setJumping(false); first.setSprinting(jumpCase == 1);
                    second.setOnGround(true); second.setJumping(false); second.setSprinting(jumpCase == 1);
                    first.setDeltaMovement(Vec3.ZERO); first.setKnownMovement(Vec3.ZERO);
                    if (localTick >= 6) next(2);
                }
                case 2 -> {
                    if (localTick == 1) {
                        place(first, new Vec3(0, 83, 0)); first.setOnGround(false);
                        place(second, new Vec3(-4, 83, 8)); second.setOnGround(false);
                        first.setDeltaMovement(MOTIONS[jumpCase]); first.setKnownMovement(MOTIONS[jumpCase]);
                    }
                    if (localTick == 4) {
                        // All relocation acknowledgements have completed before choosing
                        // the client's actual pre-request inertia for this independent case.
                        a.velocity = MOTIONS[jumpCase];
                        first.setDeltaMovement(MOTIONS[jumpCase]); first.setKnownMovement(MOTIONS[jumpCase]);
                        jumpExpected = MOTIONS[jumpCase].add(0, .65, 0);
                        caseStartMotionCount = a.motionPackets;
                        log("jump-request", "case=" + MOTION_NAMES[jumpCase] + " ground=" + first.onGround()
                                + " server=" + first.getDeltaMovement() + " client=" + a.velocity + " packets=" + caseStartMotionCount);
                        a.send(new ServerboundCustomPayloadPacket(new DoubleJumpPayload()));
                    }
                    if (localTick >= 8) {
                        Vec3 actual = first.getDeltaMovement();
                        check(Double.isFinite(actual.lengthSqr()), MOTION_NAMES[jumpCase] + " server motion remains finite after vanilla ticks");
                        vector(a.velocity, jumpExpected, MOTION_NAMES[jumpCase] + " TCP authorized delta preserves client motion");
                        check(a.motionPackets == caseStartMotionCount + 1, "one authorized impulse for fresh request " + MOTION_NAMES[jumpCase]);
                        check(first.isSprinting() == (jumpCase == 1), "sprint state unchanged");
                        a.send(new ServerboundCustomPayloadPacket(new DoubleJumpPayload()));
                        MovementImpulsePayload last = null;
                        for (Packet<?> packet : a.received) if (packet instanceof ClientboundCustomPayloadPacket custom
                                && custom.payload() instanceof MovementImpulsePayload impulse) last = impulse;
                        check(last != null, "actual impulse payload received over TCP");
                        if (last != null) {
                            vector(last.delta(), new Vec3(0, .65, 0), "server authorizes exactly one vertical increment");
                            check(last.playerId() == first.getId(), "impulse targets initiating player");
                        }
                        duplicateMotionCount = a.motionPackets;
                        if (last != null) {
                            first.connection.send(new ClientboundCustomPayloadPacket(last));
                            first.connection.send(new ClientboundCustomPayloadPacket(last));
                        }
                        check(b.motionPackets == jumpCase, "first player's TCP request cannot add motion to second player");
                        second.setDeltaMovement(MOTIONS[jumpCase]); second.setKnownMovement(MOTIONS[jumpCase]);
                        b.velocity = MOTIONS[jumpCase];
                        // The same registered server handler on its proper server thread,
                        // observed immediately before vanilla tick gravity can alter the result.
                        second.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(new DoubleJumpPayload()));
                        vector(second.getDeltaMovement(), jumpExpected,
                                "registered server handler preserves exact current X/Z and adds Y " + MOTION_NAMES[jumpCase]);
                        next(3);
                    }
                }
                case 3 -> {
                    if (localTick == 1) a.send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, true, false, false)));
                    if (localTick < 4) return;
                    vector(a.velocity, jumpExpected, "duplicate TCP impulse packet not reapplied " + MOTION_NAMES[jumpCase]);
                    check(a.motionPackets == duplicateMotionCount, "duplicate TCP impulse sequence rejected");
                    vector(b.velocity, jumpExpected, "second player's independently authorized impulse");
                    if (++jumpCase < MOTIONS.length) next(1);
                    else {
                        first.setItemSlot(EquipmentSlot.FEET, ItemStack.EMPTY);
                        a.send(new ServerboundCustomPayloadPacket(new DoubleJumpPayload()));
                        next(4);
                    }
                }
                case 4 -> {
                    if (localTick < 4) return;
                    check(a.motionPackets == duplicateMotionCount, "illegal unequipped request denied");
                    equip(first); next(10);
                }
                case 10 -> {
                    first.setYRot(-90); first.setXRot(hookCase % 3 == 2 ? -35 : 0);
                    if (localTick == 1) {
                        clearHook(); place(first, new Vec3(0, 80, 0));
                        a.send(new ServerboundPlayerInputPacket(new Input(false, false, false, false, false, false, false)));
                    }
                    first.setOnGround(true); first.setJumping(false);
                    first.setDeltaMovement(Vec3.ZERO); first.setKnownMovement(Vec3.ZERO);
                    if (localTick == 2) first.getMainHandItem().use(level, first, InteractionHand.MAIN_HAND);
                    hook = first.fishing;
                    if (hook != null) next(11);
                    else if (localTick > 10) throw new IllegalStateException("real FishingRodItem.use produced no hook");
                }
                case 11 -> {
                    if (hook.isRemoved()) throw new IllegalStateException("hook disappeared before real collision");
                    if (phase(hook) == 2) {
                        fixedAnchor = hook.position();
                        check(Math.abs(first.getDeltaMovement().x) < 1E-9 && Math.abs(first.getDeltaMovement().z) < 1E-9, "anchor hit does not pull horizontally");
                        next(12);
                    } else if (localTick > 80) throw new IllegalStateException("real projectile never hit fixture wall");
                }
                case 12 -> {
                    vector(hook.position(), fixedAnchor, "fixed anchor does not drift");
                    check(Math.abs(first.getDeltaMovement().x) < 1E-9 && Math.abs(first.getDeltaMovement().z) < 1E-9, "anchored wait gives no horizontal force");
                    if (localTick < 6) return;
                    var ownerAnchor = a.anchors.get(hook.getUUID());
                    var observerAnchor = b.anchors.get(hook.getUUID());
                    check(ownerAnchor != null && ownerAnchor.phase() == 2, "owner receives current anchored phase over TCP");
                    check(observerAnchor != null && observerAnchor.phase() == 2, "second player receives current anchored phase over TCP");
                    if (ownerAnchor != null) vector(new Vec3(ownerAnchor.x(), ownerAnchor.y(), ownerAnchor.z()), fixedAnchor, "owner precise anchor packet");
                    if (observerAnchor != null) vector(new Vec3(observerAnchor.x(), observerAnchor.y(), observerAnchor.z()), fixedAnchor, "observer precise anchor packet");
                    double distance = DISTANCES[hookCase / 3];
                    double z = hookCase % 3 == 1 ? -distance * .4 : 0;
                    double horizontal = Math.sqrt(distance * distance - z * z);
                    // Reposition the QA player after a genuine projectile collision to exercise
                    // every legal rope length, including farther than the rod's initial flight.
                    startPosition = new Vec3(fixedAnchor.x - horizontal, hookCase == 0 ? 83 : 80, fixedAnchor.z + z);
                    place(first, startPosition); first.setOnGround(hookCase != 0);
                    first.setDeltaMovement(hookCase % 3 == 1 ? new Vec3(0, 0, .18) : Vec3.ZERO);
                    first.setKnownMovement(first.getDeltaMovement());
                    minGap = Math.sqrt(first.getBoundingBox().distanceToSqr(fixedAnchor)); maxRadial = 0;
                    b.send(new ServerboundCustomPayloadPacket(new FlyingHookUsePayload(hook.getUUID(), (byte) 2, false)));
                    a.send(new ServerboundCustomPayloadPacket(new FlyingHookUsePayload(UUID.randomUUID(), (byte) 2, false)));
                    next(13);
                }
                case 13 -> {
                    if (localTick < 4) return;
                    check(phase(hook) == 2, "other player and stale UUID cannot reel current hook");
                    a.position = first.position();
                    a.velocity = hookCase % 3 == 1 ? new Vec3(0, 0, .18) : Vec3.ZERO;
                    clientGround = first.onGround();
                    combinationRequested = false; combinationChecked = false;
                    a.send(new ServerboundCustomPayloadPacket(new FlyingHookUsePayload(hook.getUUID(), (byte) 2, false)));
                    next(14);
                }
                case 14 -> {
                    if (localTick < 3) return;
                    check(phase(hook) == 3, "valid TCP request begins reel");
                    if (hookCase == 0 && localTick < 23) return;
                    if (hookCase == 0) {
                        Vec3 radial = fixedAnchor.subtract(first.getBoundingBox().getCenter()).normalize();
                        check(a.velocity.dot(radial) <= MovementMath.MAX_RADIAL_SPEED + 1E-8,
                                "twenty ticks without position feedback cannot accumulate unlimited rope velocity");
                    }
                    // Repeated phase-2 requests must be stale and must not cancel an active reel.
                    a.send(new ServerboundCustomPayloadPacket(new FlyingHookUsePayload(hook.getUUID(), (byte) 2, false)));
                    next(15);
                }
                case 15 -> {
                    if (!hook.isRemoved()) {
                        Vec3 serverPosition = first.position(), serverVelocity = first.getDeltaMovement();
                        boolean serverGround = first.onGround(), serverHorizontalCollision = first.horizontalCollision,
                                serverVerticalCollision = first.verticalCollision;
                        double serverFallDistance = first.fallDistance;
                        // One headless client physics step. The server has already run its
                        // own tick; restore it afterward and submit the client position over
                        // TCP, exactly as a real client does. Never double-integrate server motion.
                        first.setPos(a.position); first.setDeltaMovement(a.velocity); first.setOnGround(clientGround);
                        Vec3 toAnchor = fixedAnchor.subtract(first.getBoundingBox().getCenter());
                        Vec3 radial = toAnchor.normalize();
                        maxRadial = Math.max(maxRadial, a.velocity.dot(radial));
                        first.travel(Vec3.ZERO);
                        double gap = Math.sqrt(first.getBoundingBox().distanceToSqr(fixedAnchor));
                        minGap = Math.min(minGap, gap);
                        Vec3 clientPosition = first.position(), clientVelocity = first.getDeltaMovement();
                        boolean clientHorizontalCollision = first.horizontalCollision;
                        clientGround = first.onGround();
                        log("trajectory", "case=" + hookCase + " reelTick=" + localTick + " phase=" + phase(hook)
                                + " position=" + clientPosition + " velocity=" + clientVelocity
                                + " gap=" + gap + " collision=" + first.horizontalCollision + "/" + first.verticalCollision);
                        check(first.getX() + first.getBbWidth() / 2 <= 8.000001, "vanilla collision prevents crossing anchor wall");
                        check(Double.isFinite(first.getDeltaMovement().lengthSqr()), "finite reel motion");
                        first.setPos(serverPosition); first.setDeltaMovement(serverVelocity); first.setOnGround(serverGround);
                        first.horizontalCollision = serverHorizontalCollision; first.verticalCollision = serverVerticalCollision;
                        first.fallDistance = serverFallDistance;
                        a.position = clientPosition; a.velocity = clientVelocity;
                        a.send(new ServerboundMovePlayerPacket.PosRot(clientPosition.x, clientPosition.y, clientPosition.z,
                                first.getYRot(), first.getXRot(), clientGround, clientHorizontalCollision));
                        a.send(ServerboundClientTickEndPacket.INSTANCE);
                    } else {
                        log("hook-result", "distance=" + DISTANCES[hookCase / 3] + " axis=" + hookCase % 3
                                + " reelTicks=" + localTick + " minGap=" + minGap + " maxRadial=" + maxRadial
                                + " end=" + (minGap <= MovementMath.ARRIVAL_DISTANCE ? "arrival" : "timeout-or-stuck"));
                        check(first.position().distanceTo(startPosition) > 1, "reel advances toward real anchor");
                        check(minGap <= MovementMath.ARRIVAL_DISTANCE, "legal unobstructed reel reaches arrival distance case " + hookCase);
                        check(first.fishing == null, "termination removes current hook reference");
                        Vec3 natural = first.getDeltaMovement();
                        check(natural.lengthSqr() > 0, "termination retains natural velocity");
                        terminationPacketCount = outboundImpulses.getOrDefault(first.getUUID(), 0);
                        next(16);
                    }
                }
                case 16 -> {
                    if (localTick == 2) terminationPacketCount = outboundImpulses.getOrDefault(first.getUUID(), 0);
                    if (localTick < 7) return;
                    check(outboundImpulses.getOrDefault(first.getUUID(), 0) == terminationPacketCount, "arrival leaves no new force for five ticks");
                    if (++hookCase < 9) next(10); else next(20);
                }
                case 20 -> {
                    first.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FISHING_ROD));
                    place(first, new Vec3(0, 80, 0)); first.setYRot(-90); first.setXRot(0);
                    if (localTick == 2) first.getMainHandItem().use(level, first, InteractionHand.MAIN_HAND);
                    if (localTick == 4) {
                        check(first.fishing != null && phase(first.fishing) == 0, "unenchanted rod remains ordinary fishing");
                        clearHook(); equip(first);
                        RuntimeExtraCases.start(server, first, second, a, RuntimeSuite::check);
                        next(25);
                    }
                }
                case 21 -> {
                    clearHook(); place(first, new Vec3(0, 80, 0)); first.setOnGround(true); first.setJumping(false);
                    first.setYRot(-90); first.setXRot(0);
                    if (localTick == 3) first.getMainHandItem().use(level, first, InteractionHand.MAIN_HAND);
                    hook = first.fishing;
                    if (hook != null) next(22);
                    else if (localTick > 12) throw new IllegalStateException("cleanup fixture failed to cast");
                }
                case 22 -> {
                    if (hook.isRemoved()) throw new IllegalStateException("cleanup fixture lost hook before anchoring");
                    if (phase(hook) == 2) {
                        fixedAnchor = hook.position();
                        a.send(new ServerboundCustomPayloadPacket(new FlyingHookUsePayload(hook.getUUID(), (byte) 2, false)));
                        next(23);
                    } else if (localTick > 80) throw new IllegalStateException("cleanup fixture failed to anchor");
                }
                case 23 -> {
                    if (localTick < 5) return;
                    check(phase(hook) == 3, "cleanup setup has actual active reel");
                    cleanupVelocity = first.getDeltaMovement();
                    if (cleanupCase == 0) {
                        a.position = first.position(); a.velocity = new Vec3(a.velocity.x, .42, a.velocity.z);
                        clientGround = false; combinationRequested = false;
                        moveClientOnce(); next(26); return;
                    }
                    switch (cleanupCase) {
                        case 0 -> a.send(new ServerboundCustomPayloadPacket(new FlyingHookUsePayload(hook.getUUID(), (byte) 3, false)));
                        case 1 -> level.setBlock(BlockPos.containing(fixedAnchor.add(.01, 0, 0)), Blocks.AIR.defaultBlockState(), 3);
                        case 2 -> first.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                        case 3 -> { hook.discard(); hook.discard(); }
                        case 4 -> first.teleportTo(server.getLevel(Level.NETHER), 0, 80, 0, java.util.Set.of(), 0, 0, false);
                        case 5 -> first.setHealth(0);
                        case 6 -> { expectedDisconnect = true; a.close(); }
                        default -> throw new IllegalStateException("bad cleanup case");
                    }
                    next(24);
                }
                case 24 -> {
                    if (localTick == 2) { cleanupVelocity = first.getDeltaMovement(); terminationPacketCount = outboundImpulses.getOrDefault(first.getUUID(), 0); }
                    if (localTick < 7) return;
                    check(hook.isRemoved() && phase(hook) == 0, "cleanup case " + cleanupCase + " removes active hook and phase");
                    check(first.fishing == null, "cleanup case " + cleanupCase + " clears vanilla reference");
                    if (cleanupCase <= 3) check(first.getDeltaMovement().lengthSqr() > 0, "cleanup allows vanilla natural motion " + cleanupCase);
                    check(outboundImpulses.getOrDefault(first.getUUID(), 0) == terminationPacketCount, "cleanup has no further impulse for five ticks " + cleanupCase);
                    log("cleanup", "case=" + cleanupCase + " phase=" + phase(hook) + " velocity=" + first.getDeltaMovement());
                    if (cleanupCase == 1) level.setBlock(BlockPos.containing(fixedAnchor.add(.01, 0, 0)), Blocks.STONE.defaultBlockState(), 3);
                    if (cleanupCase == 4) first.teleportTo(level, 0, 80, 0, java.util.Set.of(), -90, 0, false);
                    if (cleanupCase == 5) {
                        // The dead owner remains dead; the independent second player becomes
                        // the logout fixture, exercising the actual disconnect callback.
                        first = second;
                        ProtocolBot retired = a; a = b; b = retired;
                    }
                    if (++cleanupCase <= 6) { equip(first); next(21); }
                    else finish(server);
                }
                case 25 -> {
                    if (RuntimeExtraCases.tick()) { equip(first); next(21); }
                }
                case 26 -> {
                    if (localTick == 3) {
                        check(!first.onGround() && !clientGround, "real accepted initial jump leaves ground before second jump");
                        combinationBeforeY = a.velocity.y; combinationVerticalBefore = verticalPackets(a);
                        combinationRope = ropeState();
                        a.send(new ServerboundCustomPayloadPacket(new DoubleJumpPayload()));
                        combinationRequested = true;
                    }
                    if (combinationRequested && verticalPackets(a) == combinationVerticalBefore + 1
                            && a.lastAppliedImpulseSequence >= latestVerticalSequence(a)) {
                        RopeState after = ropeState();
                        check(phase(hook) == 3 && first.fishing == hook, "double jump keeps same active reeling hook");
                        check(after.cast == combinationRope.cast && Math.abs(after.length - Math.max(MovementMath.ARRIVAL_DISTANCE,
                                combinationRope.length - (after.ticks - combinationRope.ticks) * MovementMath.REEL_SPEED)) < 1E-8,
                                "double jump keeps cast and rope length except normal per-tick reeling");
                        check(a.velocity.y > combinationBeforeY + .2, "next rope update preserves authorized jump and natural physics");
                        log("combination", "clientYBefore=" + combinationBeforeY + " clientYAfter=" + a.velocity.y
                                + " hook=" + hook.getUUID() + " phase=" + phase(hook));
                        a.send(new ServerboundCustomPayloadPacket(new FlyingHookUsePayload(hook.getUUID(), (byte) 3, false)));
                        next(27); return;
                    }
                    if (localTick > 12) throw new IllegalStateException("reeling double jump request not authorized");
                    moveClientOnce();
                }
                case 27 -> {
                    moveClientOnce();
                    if (localTick == 3) {
                        check(!clientGround && !first.onGround(), "cancel combination remains airborne without genuine ground refresh");
                        combinationVerticalBefore = verticalPackets(a);
                        a.send(new ServerboundCustomPayloadPacket(new DoubleJumpPayload()));
                    }
                    if (localTick < 7) return;
                    check(verticalPackets(a) == combinationVerticalBefore, "canceling hook cannot refresh spent airborne jump");
                    check(hook.isRemoved() && phase(hook) == 0, "combination cancel removes hook");
                    next(24);
                }
                default -> throw new IllegalStateException("unknown QA step " + step);
            }
        } catch (Throwable error) { fatal(server, error); }
    }

    private static void fixture() {
        for (int x = -40; x <= 12; x++) for (int z = -20; z <= 20; z++) {
            level.getChunk(x >> 4, z >> 4);
            level.setBlock(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState(), 3);
        }
        for (int y = 80; y <= 100; y++) for (int z = -6; z <= 6; z++)
            level.setBlock(new BlockPos(8, y, z), Blocks.STONE.defaultBlockState(), 3);
    }

    private static void equip(ServerPlayer player) {
        ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
        boots.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(
                ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("wsee", "double_jump"))), 1);
        player.setItemSlot(EquipmentSlot.FEET, boots);
        ItemStack rod = new ItemStack(Items.FISHING_ROD);
        rod.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(
                ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("wsee", "flying_hook"))), 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, rod);
    }

    private static void place(ServerPlayer player, Vec3 point) {
        player.connection.teleport(point.x, point.y, point.z, player.getYRot(), player.getXRot());
        player.resetFallDistance(); player.setDeltaMovement(Vec3.ZERO);
        player.setKnownMovement(Vec3.ZERO); player.setHealth(player.getMaxHealth());
    }
    private static byte phase(FishingHook value) { return ((FlyingHookAccess) value).wsee$getPhase(); }
    private static int authorizedPackets(ProtocolBot bot) {
        return (int) bot.received.stream().filter(p -> p instanceof ClientboundCustomPayloadPacket custom
                && custom.payload() instanceof MovementImpulsePayload impulse && impulse.playerId() == bot.entityId).count();
    }
    private static int verticalPackets(ProtocolBot bot) {
        return (int) bot.received.stream().filter(p -> p instanceof ClientboundCustomPayloadPacket custom
                && custom.payload() instanceof MovementImpulsePayload impulse && impulse.playerId() == bot.entityId
                && impulse.delta().equals(new Vec3(0, .65, 0))).count();
    }
    private static long latestVerticalSequence(ProtocolBot bot) {
        return bot.received.stream().filter(p -> p instanceof ClientboundCustomPayloadPacket custom
                && custom.payload() instanceof MovementImpulsePayload impulse && impulse.playerId() == bot.entityId
                && impulse.delta().equals(new Vec3(0, .65, 0)))
                .mapToLong(p -> ((MovementImpulsePayload) ((ClientboundCustomPayloadPacket) p).payload()).sequence()).max().orElse(0);
    }
    private static void clearHook() { if (first != null && first.fishing != null) first.fishing.discard(); }
    private static void moveClientOnce() {
        Vec3 serverPosition = first.position(), serverVelocity = first.getDeltaMovement();
        boolean serverGround = first.onGround(), serverHorizontal = first.horizontalCollision, serverVertical = first.verticalCollision;
        double serverFallDistance = first.fallDistance;
        first.setPos(a.position); first.setDeltaMovement(a.velocity); first.setOnGround(clientGround);
        first.travel(Vec3.ZERO);
        Vec3 clientPosition = first.position(), clientVelocity = first.getDeltaMovement();
        boolean clientHorizontal = first.horizontalCollision;
        clientGround = first.onGround();
        first.setPos(serverPosition); first.setDeltaMovement(serverVelocity); first.setOnGround(serverGround);
        first.horizontalCollision = serverHorizontal; first.verticalCollision = serverVertical; first.fallDistance = serverFallDistance;
        a.position = clientPosition; a.velocity = clientVelocity;
        a.send(new ServerboundMovePlayerPacket.PosRot(clientPosition.x, clientPosition.y, clientPosition.z,
                first.getYRot(), first.getXRot(), clientGround, clientHorizontal));
        a.send(ServerboundClientTickEndPacket.INSTANCE);
    }
    private static RopeState ropeState() {
        try {
            Class<?> mod = Class.forName("cn.wusheng.oatz.WseeMod");
            Object controller = null;
            for (Field field : mod.getDeclaredFields()) {
                if (!field.getType().getName().equals("cn.wusheng.oatz.movement.FlyingHookController")) continue;
                field.setAccessible(true);
                Object instance = null;
                if (!Modifier.isStatic(field.getModifiers())) {
                    Field singleton = mod.getDeclaredField("INSTANCE"); singleton.setAccessible(true);
                    instance = singleton.get(null);
                }
                controller = field.get(instance); break;
            }
            if (controller == null) throw new IllegalStateException("no real production hook controller");
            Field casts = controller.getClass().getDeclaredField("casts"); casts.setAccessible(true);
            Object cast = ((java.util.Map<?, ?>) casts.get(controller)).get(first.getUUID());
            if (cast == null) throw new IllegalStateException("no real active cast");
            Field length = cast.getClass().getDeclaredField("ropeLength"), count = cast.getClass().getDeclaredField("ticks");
            length.setAccessible(true); count.setAccessible(true);
            return new RopeState(cast, length.getDouble(cast), count.getInt(cast));
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
    }
    private static void next(int value) { step = value; localTick = 0; }
    private static void vector(Vec3 actual, Vec3 expected, String message) { check(actual.distanceToSqr(expected) < 1E-12, message + " actual=" + actual + " expected=" + expected); }
    private static void check(boolean condition, String message) {
        assertions++;
        if (!condition) { failures++; errors.add(message); log("FAIL", message); }
    }
    private static void log(String type, String message) {
        System.out.println("[WSEE-QA] " + type + " " + message);
        try {
            if (trace != null) { trace.write("{\"tick\":" + tick + ",\"type\":\"" + type + "\",\"detail\":\""
                    + message.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}\n"); trace.flush(); }
        } catch (Exception error) { throw new IllegalStateException(error); }
    }
    private static void fatal(MinecraftServer server, Throwable error) {
        failures++; errors.add(error.toString()); error.printStackTrace();
        log("FATAL", error.toString()); finish(server);
    }
    private static void finish(MinecraftServer server) {
        if (done) return;
        done = true;
        log("result", "assertions=" + assertions + " failures=" + failures + " A.motion=" + (a == null ? 0 : a.motionPackets)
                + " A.anchor=" + (a == null ? 0 : a.anchorPackets) + " B.anchor=" + (b == null ? 0 : b.anchorPackets)
                + " approvedSamples=" + approvedForceSamples + " inwardSamples=" + inwardForceSamples);
        try {
            Files.writeString(server.getServerDirectory().resolve("movement-qa-result.txt"),
                    "assertions=" + assertions + "\nfailures=" + failures + "\n" + String.join("\n", errors), StandardCharsets.UTF_8);
            if (trace != null) trace.close();
        } catch (Exception error) { error.printStackTrace(); }
        clearHook(); if (a != null) a.close(); if (b != null) b.close();
        if (originalTransport != null) ServerMotion.setSender(originalTransport);
        server.halt(false);
    }
}
