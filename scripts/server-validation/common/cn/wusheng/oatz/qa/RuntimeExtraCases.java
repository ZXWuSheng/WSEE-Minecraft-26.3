package cn.wusheng.oatz.qa;

import cn.wusheng.oatz.FlyingHookUsePayload;
import cn.wusheng.oatz.access.FlyingHookAccess;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ServerboundClientTickEndPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.animal.sheep.Sheep;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;

/** 同一真实 TCP 玩家上的碰撞、手归属、实体跟随及原版成功渔获分支夹具。 */
public final class RuntimeExtraCases {
    private static final BlockPos SUPPORT = new BlockPos(6, 81, 12);
    private static final Vec3 ORIGIN = new Vec3(0, 80, 12.5);
    private static final AABB LOOT_AREA = new AABB(-8, 76, 4, 14, 90, 22);
    private static ServerLevel level;
    private static ServerPlayer player, observer;
    private static ProtocolBot client;
    private static BiConsumer<Boolean, String> assertion;
    private static int stage, ticks;
    private static FishingHook hook, otherHook;
    private static Sheep target;
    private static Vec3 offset;
    private static float health;
    private static ItemStack offhandRod, mainRod;
    private static Field nibble, openWater;
    private static boolean done;
    private static boolean clientGround;
    private static int lastForceCount;

    private RuntimeExtraCases() {}

    public static void start(MinecraftServer server, ServerPlayer first, ServerPlayer second,
                             ProtocolBot ownerClient, BiConsumer<Boolean, String> check) {
        level = server.overworld(); player = first; observer = second; client = ownerClient; assertion = check;
        stage = 0; ticks = 0; done = false;
        try {
            nibble = FishingHook.class.getDeclaredField("nibble"); nibble.setAccessible(true);
            openWater = FishingHook.class.getDeclaredField("openWater"); openWater.setAccessible(true);
        } catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
        for (int x = -2; x <= 9; x++) for (int z = 10; z <= 14; z++)
            level.setBlock(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState(), 3);
        for (int x = 4; x <= 7; x++) for (int y = 80; y <= 84; y++) for (int z = 10; z <= 14; z++)
            level.setBlock(new BlockPos(x, y, z), Blocks.AIR.defaultBlockState(), 3);
        if (player.fishing != null) player.fishing.discard();
        player.teleportTo(level, ORIGIN.x, ORIGIN.y, ORIGIN.z, Set.of(), -90, 0, false);
        System.out.println("[WSEE-QA] extras collision shapes, offhand ownership, entity following and vanilla fishing loot");
    }

    public static boolean tick() {
        if (done) return true;
        ticks++;
        switch (stage) {
            case 0 -> {
                if (ticks < 6) break;
                level.setBlock(SUPPORT, Blocks.STONE_SLAB.defaultBlockState()
                        .setValue(BlockStateProperties.SLAB_TYPE, SlabType.BOTTOM), 3);
                cast(InteractionHand.MAIN_HAND, new Vec3(6, 81.25, 12.5)); next(1);
            }
            case 1 -> {
                if (!anchoredOrFailed("bottom slab")) break;
                if (active()) {
                    check(Math.abs(hook.getX() - 6) < .00001 && hook.getY() >= 81 && hook.getY() <= 81.5,
                            "slab exact collision point is on lower half face, not block center: " + hook.position());
                    level.setBlock(SUPPORT, Blocks.STONE_SLAB.defaultBlockState()
                            .setValue(BlockStateProperties.SLAB_TYPE, SlabType.TOP), 3);
                }
                next(2);
            }
            case 2 -> {
                if (ticks < 3) break;
                check(hook == null || hook.isRemoved(), "changing slab support away from exact point clears hook");
                clearHook();
                level.setBlock(SUPPORT, Blocks.STONE_STAIRS.defaultBlockState(), 3);
                cast(InteractionHand.MAIN_HAND, new Vec3(6, 81.25, 12.5)); next(3);
            }
            case 3 -> {
                if (!anchoredOrFailed("stairs")) break;
                if (active()) check(Math.abs(hook.getX() - 6) < .00001 && hook.getY() >= 81 && hook.getY() <= 81.5,
                        "stairs actual lower face collision point: " + hook.position());
                clearHook(); level.setBlock(SUPPORT, Blocks.STONE.defaultBlockState(), 3);
                cast(InteractionHand.OFF_HAND, new Vec3(6, 81.5, 12.5));
                offhandRod = player.getOffhandItem();
                mainRod = rod("flying_hook"); player.setItemInHand(InteractionHand.MAIN_HAND, mainRod);
                next(4);
            }
            case 4 -> {
                if (!anchoredOrFailed("offhand")) break;
                if (!active()) { next(8); break; }
                check(((FlyingHookAccess) hook).wsee$isOffhand(), "synched cast hand remains offhand with rods in both hands");
                use((byte) 2, false); next(5);
            }
            case 5 -> {
                if (ticks < 4) break;
                check(active() && phase(hook) == 2, "wrong main hand cannot reel offhand cast");
                use((byte) 2, true); next(6);
            }
            case 6 -> {
                if (ticks < 4) break;
                check(active() && phase(hook) == 3, "original offhand starts reeling through real TCP request");
                use((byte) 3, true); next(7);
            }
            case 7 -> {
                if (ticks < 4) break;
                check(hook.isRemoved() && phase(hook) == 0 && player.fishing == null, "offhand TCP cancel cleans hook");
                check(offhandRod.getDamageValue() == 2 && mainRod.getDamageValue() == 0,
                        "only original offhand rod pays block-anchor durability");
                check(items().isEmpty(), "flying hook retrieval never emits fishing loot");
                next(8);
            }
            case 8 -> {
                clearHook(); level.setBlock(SUPPORT, Blocks.AIR.defaultBlockState(), 3);
                target = EntityTypes.SHEEP.create(level, EntitySpawnReason.COMMAND);
                if (target == null) throw new IllegalStateException("cannot create entity fixture");
                target.setNoAi(true); target.setNoGravity(true); target.setPos(5, 80, 12.5);
                level.addFreshEntity(target); health = target.getHealth();
                cast(InteractionHand.MAIN_HAND, new Vec3(5, 80.6, 12.5)); next(9);
            }
            case 9 -> {
                if (!anchoredOrFailed("living entity")) break;
                if (!active()) { target.discard(); next(11); break; }
                offset = hook.position().subtract(target.position());
                target.setPos(5.6, 80.2, 13.5); next(10);
            }
            case 10 -> {
                if (ticks < 3) break;
                check(active() && hook.position().distanceToSqr(target.position().add(offset)) < 1E-10,
                        "entity anchor follows moving living target with saved local offset");
                check(target.getHealth() == health, "entity anchor creates no damage");
                check(target.getDeltaMovement().lengthSqr() == 0, "waiting entity anchor does not drag target");
                clearHook(); target.discard(); next(11);
            }
            case 11 -> {
                fishingLoot(false);
                for (int i = 0; i < 16; i++) fishingLoot(true);
                player.setItemInHand(InteractionHand.MAIN_HAND, rod("flying_hook"));
                player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                player.teleportTo(level, ORIGIN.x, ORIGIN.y, ORIGIN.z, Set.of(), -90, 0, false);
                next(12);
            }
            case 12 -> {
                if (ticks < 6) break;
                level.setBlock(SUPPORT, Blocks.STONE.defaultBlockState(), 3);
                cast(InteractionHand.MAIN_HAND, new Vec3(6, 81.5, 12.5)); next(13);
            }
            case 13 -> {
                if (!anchoredOrFailed("obstruction fixture")) break;
                if (!active()) { next(16); break; }
                // 在命中以后才增加障碍，锚点仍真实有效，测试玩家碰撞而非遮挡发射。
                for (int y = 80; y <= 84; y++) for (int z = 10; z <= 14; z++)
                    level.setBlock(new BlockPos(3, y, z), Blocks.STONE.defaultBlockState(), 3);
                client.position = player.position(); client.velocity = Vec3.ZERO; clientGround = true;
                use((byte) 2, false); next(14);
            }
            case 14 -> {
                if (ticks < 4) break;
                if (active()) {
                    clientStep();
                    check(client.position.x + player.getBbWidth() / 2 <= 3.000001,
                            "real vanilla client physics and TCP movement cannot cross intervening wall");
                    if (ticks > 65) {
                        check(false, "blocked reel must end after bounded no-progress ticks"); clearHook(); next(15);
                    }
                } else {
                    check(phase(hook) == 0 && player.fishing == null, "actual wall obstruction terminates active reel");
                    check(client.position.x >= 2 && client.position.x < 3,
                            "blocked player actually reached obstacle before termination");
                    next(15);
                }
            }
            case 15 -> {
                if (ticks == 2) lastForceCount = RuntimeSuite.outboundImpulseCount(player);
                if (ticks < 7) break;
                check(RuntimeSuite.outboundImpulseCount(player) == lastForceCount, "blocked reel leaves no further authorized force packets");
                for (int y = 80; y <= 84; y++) for (int z = 10; z <= 14; z++)
                    level.setBlock(new BlockPos(3, y, z), Blocks.AIR.defaultBlockState(), 3);
                next(16);
            }
            case 16 -> {
                if (ticks == 1) {
                    player.teleportTo(level, ORIGIN.x, ORIGIN.y, ORIGIN.z, Set.of(), -90, 0, false);
                    observer.teleportTo(level, 0, 80, -12.5, Set.of(), -90, 0, false);
                    level.setBlock(new BlockPos(6, 81, -13), Blocks.STONE.defaultBlockState(), 3);
                }
                if (ticks < 6) break;
                cast(InteractionHand.MAIN_HAND, new Vec3(6, 81.5, 12.5));
                if (observer.fishing != null) observer.fishing.discard();
                observer.setItemInHand(InteractionHand.MAIN_HAND, rod("flying_hook"));
                observer.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
                observer.setYRot(-90); observer.setXRot(0);
                observer.getMainHandItem().use(level, observer, InteractionHand.MAIN_HAND);
                otherHook = observer.fishing;
                if (otherHook == null) throw new IllegalStateException("second TCP player's real rod did not cast");
                otherHook.setPos(observer.getEyePosition());
                otherHook.setDeltaMovement(new Vec3(6, 81.5, -12.5).subtract(otherHook.position()).normalize().scale(1.25));
                next(17);
            }
            case 17 -> {
                if (active() && !otherHook.isRemoved() && phase(hook) == 2 && phase(otherHook) == 2) {
                    check(hook != otherHook && hook.getPlayerOwner() == player && otherHook.getPlayerOwner() == observer,
                            "two real TCP players maintain independent simultaneous anchored hooks");
                    player.getMainHandItem().use(level, player, InteractionHand.MAIN_HAND);
                    observer.getMainHandItem().use(level, observer, InteractionHand.MAIN_HAND);
                    next(18);
                } else if (ticks > 50 || !active() || otherHook.isRemoved()) {
                    check(false, "two simultaneous real casts must both anchor"); clearHook(); otherHook.discard(); next(20);
                }
            }
            case 18 -> {
                if (ticks < 4) break;
                check(active() && phase(hook) == 3 && !otherHook.isRemoved() && phase(otherHook) == 3,
                        "both TCP players can reel concurrently");
                use((byte) 3, false); next(19);
            }
            case 19 -> {
                if (ticks < 4) break;
                check(hook.isRemoved() && player.fishing == null && !otherHook.isRemoved()
                                && phase(otherHook) == 3 && observer.fishing == otherHook,
                        "canceling one player's hook preserves the other active reel");
                observer.getMainHandItem().use(level, observer, InteractionHand.MAIN_HAND);
                next(20);
            }
            case 20 -> {
                if (ticks < 4) break;
                check(otherHook == null || otherHook.isRemoved() && observer.fishing == null,
                        "second player's independent cancel clears its own hook");
                player.teleportTo(level, 0, 80, 0, Set.of(), -90, 0, false);
                observer.teleportTo(level, -4, 80, 8, Set.of(), -90, 0, false);
                next(21);
            }
            case 21 -> {
                if (ticks < 6) break;
                done = true;
            }
            default -> throw new IllegalStateException("extra fixture stage=" + stage);
        }
        return done;
    }

    private static void cast(InteractionHand hand, Vec3 destination) {
        clearHook();
        player.setPos(ORIGIN); player.setDeltaMovement(Vec3.ZERO); player.setKnownMovement(Vec3.ZERO);
        player.setOnGround(true); player.setJumping(false); player.setYRot(-90); player.setXRot(0);
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        ItemStack equipped = rod("flying_hook"); player.setItemInHand(hand, equipped);
        equipped.use(level, player, hand); hook = player.fishing;
        if (hook == null) throw new IllegalStateException("extra fixture real rod cast failed");
        check(phase(hook) == 1, "real rod use enters flying phase for " + hand);
        // 控制发射方向以固定碰撞夹具；命中仍由生产 projectile COLLIDER 路径逐 tick 确认。
        Vec3 launch = player.getEyePosition(); hook.setPos(launch);
        hook.setDeltaMovement(destination.subtract(launch).normalize().scale(1.25));
    }

    private static boolean anchoredOrFailed(String name) {
        if (active() && phase(hook) == 2) { check(true, name + " anchored through real entity flight"); return true; }
        if (hook == null || hook.isRemoved() || ticks > 50) {
            check(false, name + " failed to anchor through actual collision"); return true;
        }
        return false;
    }

    private static void fishingLoot(boolean airForce) {
        clearHook();
        items().forEach(ItemEntity::discard);
        level.getEntitiesOfClass(ExperienceOrb.class, LOOT_AREA).forEach(ExperienceOrb::discard);
        ItemStack equipped = airForce ? rod("air_force") : new ItemStack(Items.FISHING_ROD);
        player.setItemInHand(InteractionHand.MAIN_HAND, equipped);
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        equipped.use(level, player, InteractionHand.MAIN_HAND);
        FishingHook fishing = player.fishing;
        if (fishing == null) throw new IllegalStateException("ordinary fishing fixture failed to cast");
        check(phase(fishing) == 0, "ordinary / Air Force rod remains vanilla fishing mode");
        fishing.setPos(5, 80.5, 12.5); fishing.setOnGround(false);
        try { nibble.setInt(fishing, 20); openWater.setBoolean(fishing, true); }
        catch (IllegalAccessException error) { throw new IllegalStateException(error); }
        // 将原版可收取状态作为夹具输入，随后走真实 FishingRodItem.use -> FishingHook.retrieve -> LootTable。
        equipped.use(level, player, InteractionHand.MAIN_HAND);
        List<ItemEntity> drops = items();
        check(fishing.isRemoved() && player.fishing == null, "successful fishing retrieve removes vanilla hook");
        check(equipped.getDamageValue() == 1, "successful fishing preserves vanilla one durability cost");
        check(!level.getEntitiesOfClass(ExperienceOrb.class, LOOT_AREA).isEmpty(), "successful fishing retains vanilla experience orb");
        check(!drops.isEmpty(), "successful retrieve creates genuine ItemEntity drop");
        if (airForce) {
            check(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1, "Air Force emits exactly one base item");
            for (ItemEntity drop : drops) {
                ItemStack stack = drop.getItem();
                check(!stack.is(ItemTags.FISHES), "Air Force genuine loot excludes fish");
                for (String path : List.of("air_force_fish", "air_force_unobtainable", "air_force_admin_blacklist", "air_force_blacklist"))
                    check(!stack.is(TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("wsee", path))),
                            "Air Force genuine loot excludes #wsee:" + path);
                check(stack.getComponents().equals(new ItemStack(stack.getItem()).getComponents()), "Air Force loot retains base components");
            }
        }
        drops.forEach(ItemEntity::discard);
        level.getEntitiesOfClass(ExperienceOrb.class, LOOT_AREA).forEach(ExperienceOrb::discard);
    }

    private static ItemStack rod(String enchantment) {
        ItemStack stack = new ItemStack(Items.FISHING_ROD);
        stack.enchant(level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(
                ResourceKey.create(Registries.ENCHANTMENT, Identifier.fromNamespaceAndPath("wsee", enchantment))), 1);
        return stack;
    }
    private static void clientStep() {
        Vec3 serverPosition = player.position(), serverVelocity = player.getDeltaMovement();
        boolean serverGround = player.onGround(), serverHorizontal = player.horizontalCollision, serverVertical = player.verticalCollision;
        double serverFall = player.fallDistance;
        player.setPos(client.position); player.setDeltaMovement(client.velocity); player.setOnGround(clientGround);
        player.travel(Vec3.ZERO);
        Vec3 candidatePosition = player.position(), candidateVelocity = player.getDeltaMovement();
        clientGround = player.onGround(); boolean horizontal = player.horizontalCollision;
        player.setPos(serverPosition); player.setDeltaMovement(serverVelocity); player.setOnGround(serverGround);
        player.horizontalCollision = serverHorizontal; player.verticalCollision = serverVertical; player.fallDistance = serverFall;
        client.position = candidatePosition; client.velocity = candidateVelocity;
        client.send(new ServerboundMovePlayerPacket.PosRot(candidatePosition.x, candidatePosition.y, candidatePosition.z,
                player.getYRot(), player.getXRot(), clientGround, horizontal));
        client.send(ServerboundClientTickEndPacket.INSTANCE);
    }
    private static void use(byte expectedPhase, boolean offhand) {
        if (hook != null) client.send(new ServerboundCustomPayloadPacket(new FlyingHookUsePayload(hook.getUUID(), expectedPhase, offhand)));
    }
    private static List<ItemEntity> items() { return level.getEntitiesOfClass(ItemEntity.class, LOOT_AREA); }
    private static boolean active() { return hook != null && !hook.isRemoved(); }
    private static byte phase(FishingHook value) { return ((FlyingHookAccess) value).wsee$getPhase(); }
    private static void clearHook() { if (player != null && player.fishing != null) player.fishing.discard(); }
    private static void next(int value) { stage = value; ticks = 0; }
    private static void check(boolean valid, String description) { assertion.accept(valid, description); }
}
