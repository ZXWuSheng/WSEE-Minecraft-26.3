"""Read-only cross-loader movement wiring checks, never game/runtime tests.

Use the bundled Python runtime. The sibling Fabric checkout is detected by
default; --fabric-root selects another checkout. Repeated --minecraft-jar
arguments optionally verify API and mixin targets against existing MC JARs.
No Java process, client, server, build, or generated repository file is used.
"""

import argparse
import json
import re
import struct
import sys
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = Path("cn/wusheng/oatz")
SPEC = "WSEE-0.1.1-Minecraft-26.3-Fabric-NeoForge-详细规格.docx"
NS = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
SHARED = (
    "movement/MovementMath.java", "movement/DoubleJumpController.java",
    "movement/FlyingHookController.java", "movement/FlyingHookCycle.java",
    "movement/FlyingHookClientState.java", "access/FlyingHookAccess.java",
    "MovementImpulsePayload.java", "movement/ServerMotion.java",
    "movement/MovementImpulseState.java",
)


def read(path):
    return path.read_text(encoding="utf-8-sig")


def java(root, name):
    candidates = [root / f"src/{side}/java" / PACKAGE / name for side in ("main", "client")]
    return read(next(path for path in candidates if path.is_file()))


def clean(source):
    # Preserve string literals when removing comments (including annotation descriptors).
    pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*.*?\*/'
    return re.sub(pattern, lambda m: "" if m.group().startswith(("//", "/*")) else m.group(), source, flags=re.S)


def method(source, name):
    """Find a declared method and its balanced body; insensitive to formatting."""
    source = clean(source)
    pattern = r"\b(?:public|private|protected)\s+(?:(?:static|final|synchronized)\s+)*[\w.$<>, ?\[\]]+\s+" + re.escape(name) + r"\s*\([^;{}]*\)\s*\{"
    matches = list(re.finditer(pattern, source))
    if not matches:
        raise AssertionError(f"Method missing: {name}")
    bodies = []
    for match in matches:
        start, depth = match.end() - 1, 0
        for position in range(start, len(source)):
            if source[position] == "{":
                depth += 1
            elif source[position] == "}":
                depth -= 1
                if depth == 0:
                    bodies.append(source[start + 1:position])
                    break
        else:
            raise AssertionError(f"Unbalanced method: {name}")
    # A thin overload may delegate to the actual implementation. Inspect the
    # full body instead of tying the contract to overload order/parameter count.
    return max(bodies, key=len)


def require(condition, message):
    if not condition:
        raise AssertionError(message)


class Checks:
    def __init__(self):
        self.passed = 0
        self.failures = []

    def run(self, name, callback):
        try:
            callback()
            self.passed += 1
        except (AssertionError, OSError, StopIteration, ValueError, KeyError, IndexError, ET.ParseError) as error:
            self.failures.append(f"{name}: {error}")


def check_loader(root, loader, checks):
    main = java(root, "WseeMod.java")
    controller = java(root, "movement/FlyingHookController.java")
    cycle = java(root, "movement/FlyingHookCycle.java")
    state = java(root, "mixin/FlyingHookStateMixin.java")
    input_source = java(root, "mixin/FlyingHookInputMixin.java")
    client = java(root, "ClientHooks.java") + java(root, "movement/FlyingHookClientState.java") + input_source
    resources = root / "src/main/resources"
    config = json.loads(read(resources / "wsee.mixins.json"))
    common_mixins, client_mixins = set(config["mixins"]), set(config["client"])

    def mixins():
        require({"FlyingHookStateMixin", "FishingRodUseMixin"} <= common_mixins, "Common hook mixins not enabled")
        require({"FlyingHookInputMixin", "FishingHookRendererMixin"} <= client_mixins, "Client input/render mixins not enabled")
        require(not common_mixins & {"FlyingHookInputMixin", "FishingHookRendererMixin", "DoubleJumpInputMixin"}, "Client mixin on common side")
        require(config["required"] and config["injectors"]["defaultRequire"] == 1, "Mixin target failures must be visible")
        for name in common_mixins | client_mixins:
            java(root, f"mixin/{name}.java")
        if loader == "Fabric":
            require("DoubleJumpInputMixin" in client_mixins, "Fast keyboard tap hook not enabled")
    checks.run(f"{loader} mixin configuration", mixins)

    def fishing_boundary():
        retrieve = method(state, "wsee$retrieveFlyingHook")
        require("wsee$getPhase()" in retrieve and "!= 0" in retrieve and "setReturnValue" in retrieve
                and "retrieveFlyingHook" in retrieve, "Active hook must cancel vanilla retrieve at HEAD")
        require(re.search(r'@Inject\([^)]*method\s*=\s*"retrieve"[^\n]*HEAD[^\n]*cancellable\s*=\s*true', state), "Retrieve suppression must be cancellable at HEAD")
        loot_name = "FishingHookLootMixin" if loader == "Fabric" else "FishingHookMixin"
        require(loot_name in common_mixins, "Ordinary fishing Air Force redirect missing")
        loot = java(root, f"mixin/{loot_name}.java")
        require('@Redirect' in loot and 'method = "retrieve"' in loot and "getRandomItems" in loot
                and "AirForceFishing.replaceLoot" in loot, "Existing real-loot Air Force route missing")
        require("AirForceFishing" not in retrieve, "Hook reeling must not call Air Force")
    checks.run(f"{loader} flying-hook/ordinary-fishing boundary", fishing_boundary)

    def synchronized_phase():
        if loader == "NeoForge":
            attachments = java(root, "registry/MovementAttachments.java")
            require("MovementAttachments.register(modBus)" in main
                    and "NeoForgeRegistries.ATTACHMENT_TYPES" in attachments,
                    "Official phase/hand attachment registration missing")
            require("HOOK_PHASE" in attachments and ".sync(ByteBufCodecs.BYTE)" in attachments
                    and "HOOK_OFFHAND" in attachments and ".sync(ByteBufCodecs.BOOL)" in attachments,
                    "Phase/casting-hand attachments lack network codecs")
            require("SynchedEntityData.defineId" not in state and "defineSynchedData" not in state,
                    "NeoForge modifies vanilla entity data IDs instead of using official attachments")
            for name, field, operation in (("wsee$getPhase", "HOOK_PHASE", "getData("),
                    ("wsee$setPhase", "HOOK_PHASE", "setData("),
                    ("wsee$isOffhand", "HOOK_OFFHAND", "getData("),
                    ("wsee$setOffhand", "HOOK_OFFHAND", "setData(")):
                body = method(state, name)
                require(field in body and operation in body, f"{name} bypasses synchronized attachment")
        else:
            require("SynchedEntityData.defineId(FishingHook.class, EntityDataSerializers.BYTE)" in state
                    and "EntityDataSerializers.BOOLEAN" in state, "Phase/casting-hand entity data missing")
            for name in ("wsee$getPhase", "wsee$setPhase", "wsee$isOffhand", "wsee$setOffhand"):
                require("getEntityData()" in method(state, name), f"{name} bypasses synchronized entity data")
        for name in ("INACTIVE", "FLYING", "ANCHORED", "REELING"):
            require(name in cycle, f"Missing explicit phase: {name}")
        require("setPhase" in controller and "wsee$setPhase" in controller, "Server transitions not replicated")
    checks.run(f"{loader} phase and hand synchronization", synchronized_phase)

    def request_identity():
        payload = java(root, "FlyingHookUsePayload.java")
        require(re.search(r"record\s+FlyingHookUsePayload\s*\(\s*UUID\s+hookUuid,\s*byte\s+phase,\s*boolean\s+offhand", payload), "Action lacks hook UUID/phase/hand")
        require(all(token in payload for token in ("readUUID", "writeUUID", "readByte", "writeByte", "readBoolean", "writeBoolean")), "Action codec omits identity fields")
        use = method(controller, "use")
        require("expectedHook" in use and "expectedPhase" in use and "cast.hand != hand" in use
                and "valid(cast)" in use and "matchesRequest" in use, "Server action not checked against current cast/phase/hand")
        match = method(cycle, "matchesRequest")
        require("player == owner" in match and "hook.equals(expectedHook)" in match
                and "expectedPhase == phase" in match and "INACTIVE" in match, "Stale action can match another cast")
        require("payload.hookUuid()" in main and "payload.phase()" in main and "payload.offhand()" in main, "Payload identity not forwarded to controller")
    checks.run(f"{loader} current player/hook/phase/hand requests", request_identity)

    def input_latch():
        require("keyUse.isDown()" in input_source and "wsee$flyingHookUseConsumed" in input_source
                and "false" in method(input_source, "wsee$releaseUseLatch"), "Held use key does not wait for release")
        action = method(input_source, "wsee$useFlyingHookOnce")
        require("hook.getUUID()" in action and "phase" in action and "InteractionHand.OFF_HAND" in action
                and "new FlyingHookUsePayload" in action and "ci.cancel()" in action, "Active hook sends competing vanilla use or loses identity")
        require("gui.screen()" in action and "gui.overlay()" in action, "Blocking screens do not gate input")
    checks.run(f"{loader} fresh rod-use latch", input_latch)

    def server_authority():
        dispatch = method(main, "tickFlyingHookEntity")
        require("isClientSide()" in dispatch and dispatch.count("tickHook(hook)") == 2, "Hook tick not split by logical side")
        require("ServerLevel" in method(controller, "start"), "Cast can be started by a client")
        tick = method(main, "onPlayerTick")
        gate = tick.find("!player.level().isClientSide()")
        require(gate >= 0 and tick.find("tickFlyingHook(player)") > gate and tick.find("tickDoubleJump(player)") > gate, "Player forces lack a server-side tick gate")
        require("takeForceTick" in method(controller, "tickPlayer"), "Continuous force is not gated once per world tick")
        forbid = r"(?:minecraft\.player|player|receiver)\s*\.\s*(?:setDeltaMovement|jumpFromGround|push|setPos|teleportTo)\s*\("
        require(not re.search(forbid, clean(client)), "Client input predicts player movement outside authorized receipt")
    checks.run(f"{loader} server movement authority", server_authority)

    def network_wiring():
        network = java(root, "registry/ModNetworking.java") if loader == "NeoForge" else main
        require("FlyingHookUsePayload.TYPE" in network and "DoubleJumpPayload.TYPE" in network, "Serverbound movement registration missing")
        require("FlyingHookAnchorPayload.TYPE" in network, "Anchor presentation registration missing")
        require("MovementImpulsePayload.TYPE" in network, "Authorized increment registration missing")
        require("handleMovementImpulse(payload)" in client if loader == "Fabric" else "ClientHooks.handleMovementImpulse(payload)" in network,
                "Authorized increment is not handled on the client")
        require("enqueueWork" in network if loader == "NeoForge" else "context.server().execute" in network, "Server packet work not enqueued")
        require("FlyingHookAnchorPayload" in client if loader == "Fabric" else "flyingHookAnchor.accept" in network
                and "handleFlyingHookAnchor" in main, "Client anchor handler missing")
        require("DoubleJumpVelocityPayload" not in network + main + client, "Client velocity report reintroduced")
    checks.run(f"{loader} input/anchor payload registration", network_wiring)

    def impulse_authorization():
        network = java(root, "registry/ModNetworking.java") if loader == "NeoForge" else main
        payload = java(root, "MovementImpulsePayload.java")
        motion = java(root, "movement/ServerMotion.java")
        receipt = java(root, "movement/MovementImpulseState.java")
        require(all(token in payload for token in ("playerId", "dimension", "sessionUuid", "sequence", "Vec3 delta", "double radialLimit",
                "readIdentifier", "writeIdentifier", "readUUID", "writeUUID", "readVarLong", "writeVarLong")),
                "Authorized delta codec omits identity or server-approved radial limit")
        require("buf.writeDouble(value.radialLimit)" in payload and "delta, -1.0D" in payload,
                "Radial limit codec or unlimited independent-impulse default missing")
        require(not re.search(r"serverboundPlay\(\)\.register\(MovementImpulsePayload\.TYPE", network) if loader == "Fabric" else
                not re.search(r"playToServer\(MovementImpulsePayload\.TYPE", network), "Clients can submit an authoritative motion increment")
        apply = method(motion, "applyImpulse")
        require("instanceof ServerPlayer" in apply and "finite(delta)" in apply and "sender.accept" in apply,
                "Increment lacks a validated server producer")
        require("getDeltaMovement().add(delta)" in apply and "new MovementImpulsePayload" in apply,
                "Server mirror or authorized delta send replaces inertia")
        require("ClientboundSetEntityMotionPacket" not in motion and not re.search(r"\.syncVelocity\s*=", clean(motion)),
                "WSEE increments trigger an additional full owner velocity packet or clear external flags")
        require("getKnownMovement()" in motion and "pending" in method(motion, "basis"),
                "Radial estimate ignores accepted client movement or same-tick authorized additions")
        handler = method(java(root, "ClientHooks.java"), "handleMovementImpulse")
        require("MOVEMENT_IMPULSES.apply" in handler and "Minecraft.getInstance().player" in handler,
                "Increment is not restricted to the current local player")
        client_apply = method(receipt, "apply")
        require(all(token in client_apply for token in ("payload.playerId()", "payload.dimension()", "payload.sessionUuid()",
                "payload.sequence()", "finite(payload.delta())", "accept(", "current.add(approvedDelta(current, payload))")),
                "Client receipt misses identity validation, deduplication or additive motion")
        require(client_apply.count("setDeltaMovement(") == 1, "Receipt applies an increment more than once")
        approved = method(receipt, "approvedDelta")
        require("payload.radialLimit() < 0.0D" in approved and "return delta" in approved
                and "payload.radialLimit() - current.dot(axis)" in approved
                and "delta.scale(Math.min(1.0D, allowed / length))" in approved,
                "Receipt does not conservatively restrict only this server-approved delta")
        require(not re.search(r"current\.(?:scale|multiply|with)\(", approved), "Receipt clips unrelated current motion")
        require("applyImpulse(player, delta, -1.0D)" in motion, "Independent impulses unexpectedly use a hook radial cap")
        accept = method(receipt, "accept")
        require("retired.contains" in accept and "retired.add" in accept and "packetSequence <= sequence" in accept
                and "playerIdentity != player" in accept and "levelIdentity != level" in accept,
                "Receipt does not retire sessions/deduplicate or reset changed player/world identities")
        require(all(token not in receipt for token in ("enchantmentLevel", "consumeRequest", "onGround()", "MovementMath.reel")),
                "Client receipt calculates entitlement or predicts a force")
        require("MOVEMENT_IMPULSES.clear()" in client and "ServerMotion.clearAll()" in main and "ServerMotion.clear(" in main,
                "Motion session cleanup missing from client world/server lifecycle")
    checks.run(f"{loader} server-approved additive motion receipt", impulse_authorization)

    def double_jump():
        trigger = method(main, "tryDoubleJump")
        consume = re.search(r"\bconsume(?:Request)?\(", trigger)
        require(consume is not None and consume.start() < trigger.index("ServerMotion.applyImpulse"), "Impulse precedes opportunity consumption")
        require(re.search(r"new Vec3\(0\.0D,\s*DoubleJumpController\.VERTICAL_IMPULSE,\s*0\.0D\)", trigger),
                "Jump does not authorize precisely one independent world-up increment")
        require(trigger.count("ServerMotion.applyImpulse(") == 1 and "setDeltaMovement(" not in trigger
                and "jumpFromGround" not in trigger, "Jump applies multiple launches or a direct absolute velocity")
        require("ClientboundSetEntityMotionPacket" not in trigger and "syncVelocity" not in trigger
                and "fallDistance = 0.0" in trigger, "Jump has an extra full owner motion packet or loses existing fall reset")
        require("getBlockCollisions" in method(main, "tickDoubleJump") and "onGround()" in method(main, "tickDoubleJump"), "Ground refresh lacks real block support")
        require("keyJump" in client and "new DoubleJumpPayload()" in client, "Bound jump key does not request server validation")
        eligibility = method(main, "isDoubleJumpEligible")
        require(all(token in eligibility for token in ("EquipmentSlot.FEET", "isSpectator", "isSwimming", "isPassenger", "isFallFlying", "getAbilities().flying", "isInWater")), "Existing jump eligibility restriction missing")
        if loader == "NeoForge":
            require("onClimbable" not in eligibility, "Vines must not consume an available air jump")
            require("ClimbingMovementMixin" in common_mixins, "Climbing momentum fix missing")
    checks.run(f"{loader} one additive authoritative jump", double_jump)

    def anchors():
        require("ClipContext.Block.COLLIDER" in controller and "getCollisionShape" in controller
                and ("shape.clip(" in controller or "supportsAnchor(" in controller), "Block anchor bypasses collision shape")
        anchor = method(controller, "anchor")
        require(all(token in anchor for token in ("getBlockPos()", "getDirection()", "getLocation()", "LivingEntity", "offset")), "Exact block or following entity anchor missing")
        require("cast.cycle.anchor()" in anchor and "phase = ANCHORED" in method(cycle, "anchor")
                and "beginReeling(" not in anchor, "Impact pulls automatically instead of waiting")
        require("target.position().add(cast.offset)" in method(controller, "resolve"), "Entity anchor fails to follow target")
        require("hasChunkAt" in controller and "hasChunk(" in controller and "dimension" in controller, "Anchor validation omits dimension/unavailable chunks")
        require(not re.search(r"\.(?:getChunk|setChunkForced|addRegionTicket)\(", controller), "Anchor forces remote chunk loading")
    checks.run(f"{loader} collision anchors and unloaded-chunk boundary", anchors)

    if loader == "NeoForge":
        def migrated_fixes():
            require("setNoGravity(true)" not in controller, "Hook gravity was disabled")
            for source in (controller, java(root, "movement/FlyingHookClientState.java")):
                require("MovementMath.flyingVelocity(hook.getDeltaMovement(), hook.getGravity())" in source,
                        "Flying hook gravity differs between logical sides")
                require("getInterpolation().cancel()" in source and "setOldPosAndRot()" in source,
                        "Anchored hook retains stale interpolated poses")
            require("FlyingHookCollision.sweep" in controller and "FlyingHookCollision.blocksPull" in controller,
                    "Real hitbox edge or observed obstruction fix missing")
            sources = "\n".join(read(p) for p in (root / "src/main/java").rglob("*.java"))
            require(not any(token in sources for token in ("DoubleJumpHud", "DoubleJumpStatePayload")),
                    "Removed double jump indicator reintroduced")
        checks.run("NeoForge migrated gravity/collision/anchor/HUD fixes", migrated_fixes)

    def additive_rope():
        math_source = java(root, "movement/MovementMath.java")
        tick = method(controller, "tickPlayer")
        require("ServerMotion.basis(player)" in tick and "MovementMath.reel(" in tick and "getBoundingBox()" in tick,
                "Pull bypasses accepted client motion or AABB arrival")
        require("pull.velocity().subtract(current)" in tick and "ServerMotion.applyImpulse(player, delta, limit)" in tick
                and "delta.dot(toAnchor) > 0.0D" in tick and "MovementMath.radialSpeedLimit(gap)" in tick
                and ": -1.0D" in tick and "player.setDeltaMovement" not in tick
                and "ClientboundSetEntityMotionPacket" not in tick and "player.syncVelocity" not in tick,
                "Pull sends a full velocity instead of only its own increment")
        require("REEL_TIMEOUT" in tick and "STUCK_TICKS" in tick, "Pull termination limits missing")
        require("generatedSpeed" not in controller and "generatedSpeed" not in math_source
                and "brake" not in method(math_source, "reel"),
                "Pull subtracts an unverified estimate of its realized client contribution")
        finish = method(controller, "finish")
        require("setDeltaMovement" not in finish and "setPos" not in finish and "teleport" not in finish, "Cleanup modifies player motion/position")
        require("remove(" in finish and "savedFallDistance" in finish and "Math.max" in finish, "Cleanup is not idempotent or loses fall history")
        require("noteFlyingHookDoubleJump" in method(main, "tryDoubleJump"), "Jump is not recorded for safe hook fall interaction")
        note = method(controller, "noteDoubleJump")
        require("ropeLength" not in note and "finish(" not in note and "setPhase(" not in note, "Jump clears rope/cast instead of only recording the independent impulse")
    checks.run(f"{loader} additive rope and isolated cleanup", additive_rope)

    def lifecycle():
        valid = method(controller, "valid")
        require(all(token in valid for token in ("isAlive", "isSpectator", "isPassenger", "isFallFlying", "getAbilities().flying", "dimension", "player.fishing", "getItemInHand", "eligible.test")), "Hook qualification/lifecycle validation missing")
        require("removeFlyingHook" in method(state, "wsee$clearFlyingHook"), "Removed entity leaves a cast")
        require("removeFlyingHook" in method(state, "wsee$clearClientFlyingHook")
                and 'method = "onClientRemoval"' in state, "Client removal leaves anchor presentation")
        removal = method(controller, "remove")
        require("candidate.hook == hook" in removal and "casts.values()" in removal,
                "Removal depends only on a vanilla owner lookup that may already be gone")
        require("finish(cast, true, false)" in removal, "External removal recursively discards the same entity")
        require("wsee$endRemoval()" in method(state, "wsee$preserveNewCast")
                and "wsee$endRemoval()" in method(state, "wsee$preserveNewClientCast"),
                "Vanilla removal can detach a newer cast")
        require("clearAll()" in main and "doubleJumpController.clearPlayer" in main
                and ("FLYING_HOOKS.clear(" in main or "flyingHookController.clear(" in main), "Logout/server shutdown cleanup not wired")
        require("clearFlyingHookClientState" in client, "Client world/logout cannot clear anchor presentation")
    checks.run(f"{loader} lifecycle cleanup wiring", lifecycle)

    def resources_and_books():
        definitions = resources / "data/wsee/enchantment"
        require(len(list(definitions.glob("*.json"))) == 31, "Current enchantment inventory differs from 31")
        for name, tag in (("flying_hook", "flying_hook"), ("double_jump", "double_jump")):
            data = json.loads(read(definitions / f"{name}.json"))
            require(data["max_level"] == 1 and data["primary_items"] == data["supported_items"] == f"#wsee:enchantable/{tag}", f"{name} level/equipment data mismatch")
        tags = resources / "data/wsee/tags/item/enchantable"
        require(json.loads(read(tags / "flying_hook.json"))["values"] == ["minecraft:fishing_rod"], "Hook tag accepts unsupported items")
        require(json.loads(read(tags / "double_jump.json"))["values"] == ["#minecraft:enchantable/foot_armor"], "Jump tag accepts unsupported items")
        conflicts = resources / "data/wsee/tags/enchantment/exclusive_set"
        require(set(json.loads(read(conflicts / "flying_hook.json"))["values"]) == {"minecraft:lure", "minecraft:luck_of_the_sea", "wsee:air_force"}, "Hook fishing conflicts changed")
        require("wsee:flying_hook" in json.loads(read(conflicts / "air_force.json"))["values"], "Air Force conflict not reciprocal")
        creative = java(root, "registry/ModRegistries.java") if loader == "NeoForge" else main
        require(re.findall(r"new\s+EnchantmentInstance\(flyingHook,\s*([^)]*)\)", creative) == ["1"], "Creative tab exposes hook levels beyond I")
        backstop = java(root, "enchantment/EquipmentRules.java") if loader == "NeoForge" else main
        require(re.search(r"id\.equals\(FLYING_HOOK_ID\)\)\s*\{\s*return[^;]*FISHING_ROD", clean(backstop)), "Runtime hook equipment restriction missing")
        require(re.search(r"id\.equals\(DOUBLE_JUMP_ID\)\)\s*\{\s*return[^;]*(?:FOOT_ARMOR|isFootEquipment)", clean(backstop)), "Runtime jump equipment restriction missing")
    checks.run(f"{loader} level I/equipment/conflicts/creative books", resources_and_books)

    def localized_descriptions():
        for locale in ("zh_cn", "en_us"):
            language = json.loads(read(resources / f"assets/wsee/lang/{locale}.json"))
            for name in ("double_jump", "flying_hook"):
                text = language[f"enchantment.wsee.{name}.desc"]
                require("\n" not in text and "\r" not in text, f"{locale} {name} has embedded newline")
                require("0.65" in text if name == "double_jump" else "32" in text, f"{locale} {name} omits current numeric contract")
    checks.run(f"{loader} concise current movement descriptions", localized_descriptions)


def check_document(root):
    with ZipFile(root / SPEC) as archive:
        for name in archive.namelist():
            if name.endswith((".xml", ".rels")):
                ET.fromstring(archive.read(name))
        xml = ET.fromstring(archive.read("word/document.xml"))
    body = xml.find("w:body", NS)
    entries = {}
    active = None
    for block in body:
        style = block.find("w:pPr/w:pStyle", NS)
        text = "".join(t.text or "" for t in block.findall(".//w:t", NS))
        if style is not None and style.get(f"{{{NS['w']}}}val") == "Heading1":
            active = next((name for name in ("Double Jump", "Flying Hook") if name in text), None)
        if active:
            entries.setdefault(active, []).append(block)
    for name in ("Double Jump", "Flying Hook"):
        blocks = entries[name]
        labels = ["".join(t.text or "" for t in b.findall(".//w:t", NS)) for b in blocks
                  if b.find("w:pPr/w:pStyle", NS) is not None
                  and b.find("w:pPr/w:pStyle", NS).get(f"{{{NS['w']}}}val") == "Heading2"]
        require(labels[:5] == ["附魔介绍", "等级数值", "规则", "冲突", "指令"], f"{name} section order changed")
        text = " ".join("".join(t.text or "" for t in b.findall(".//w:t", NS)) for b in blocks)
        require("最高等级：I" in text, f"{name} max level not I")
        numbers = ("0.65", "+2") if name == "Double Jump" else ("32", "1.15", "0.35", "0.24", "0.55", "1.5", "3", "120", "10")
        require(all(number in text for number in numbers), f"{name} default parameter documentation missing")
        if name == "Flying Hook":
            rows = {}
            for block in blocks:
                for row in block.findall("w:tr", NS):
                    cells = ["".join(t.text or "" for t in cell.findall(".//w:t", NS)) for cell in row.findall("w:tc", NS)]
                    if len(cells) == 2:
                        rows[cells[0]] = cells[1]
            require(rows.get("最大牵引加速度", "").startswith("0.24 ")
                    and rows.get("最低张力系数", "").startswith("0.5")
                    and rows.get("收绳最长时间 / 连续无进展", "").startswith("120 tick / 10 tick"),
                    "Hook numeric table does not match acceleration/tension/timeout defaults")
        require("Y 直接设为" not in text and "/enchant @s wsee:flying_hook 5" not in text, "Legacy movement contract remains")


class ClassFile:
    """Small read-only class-file member reader; no loading/execution of JAR code."""
    def __init__(self, data):
        self.data, self.offset = data, 0
        require(self.take(4) == b"\xca\xfe\xba\xbe", "Invalid class-file header")
        self.take(4)
        self.constants = [None] * self.number(2)
        index = 1
        while index < len(self.constants):
            tag = self.number(1)
            if tag == 1:
                value = self.take(self.number(2)).decode("utf-8", errors="replace")
            elif tag in (3, 4, 5, 6):
                value = struct.unpack({3: ">i", 4: ">f", 5: ">q", 6: ">d"}[tag], self.take(8 if tag in (5, 6) else 4))[0]
            else:
                size = {7: 2, 8: 2, 9: 4, 10: 4, 11: 4, 12: 4, 15: 3, 16: 2, 17: 4, 18: 4, 19: 2, 20: 2}[tag]
                value = self.take(size)
            self.constants[index] = (tag, value)
            index += 2 if tag in (5, 6) else 1
        self.take(6)
        self.take(2 * self.number(2))
        self.members(False)
        self.methods = self.members(True)

    def take(self, count):
        data = self.data[self.offset:self.offset + count]
        self.offset += count
        return data

    def number(self, size):
        return int.from_bytes(self.take(size), "big")

    def members(self, collect):
        result = {}
        for _ in range(self.number(2)):
            flags = self.number(2)
            name = self.constants[self.number(2)][1]
            descriptor = self.constants[self.number(2)][1]
            code = b""
            for _ in range(self.number(2)):
                attribute = self.constants[self.number(2)][1]
                value = self.take(self.number(4))
                if attribute == "Code":
                    code = value[8:8 + int.from_bytes(value[4:8], "big")]
            if collect:
                result[(name, descriptor)] = (flags, code)
        return result

    def method_references(self):
        result = {}
        for index, entry in enumerate(self.constants):
            if entry is None or entry[0] not in (10, 11):
                continue
            owner, signature = struct.unpack(">HH", entry[1])
            owner_name = self.constants[int.from_bytes(self.constants[owner][1], "big")][1]
            name, descriptor = struct.unpack(">HH", self.constants[signature][1])
            result[index] = (owner_name, self.constants[name][1], self.constants[descriptor][1])
        return result


def check_minecraft_jars(paths, roots, checks):
    archives = [ZipFile(path) for path in paths]
    cache = {}
    def get(class_name):
        if class_name not in cache:
            member = class_name + ".class"
            for archive in archives:
                if member in archive.namelist():
                    cache[class_name] = ClassFile(archive.read(member))
                    break
            else:
                raise AssertionError(f"Class not found in supplied MC JARs: {class_name}")
        return cache[class_name]
    def has(class_name, name, descriptor):
        require((name, descriptor) in get(class_name).methods, f"Missing MC method: {class_name}.{name}{descriptor}")
    hook = "net/minecraft/world/entity/projectile/FishingHook"
    apis = (
        (hook, "retrieve", "(Lnet/minecraft/world/item/ItemStack;)I"),
        (hook, "tick", "()V"),
        (hook, "remove", "(Lnet/minecraft/world/entity/Entity$RemovalReason;)V"),
        (hook, "defineSynchedData", "(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;)V"),
        ("net/minecraft/world/phys/AABB", "distanceToSqr", "(Lnet/minecraft/world/phys/Vec3;)D"),
        ("net/minecraft/world/phys/shapes/VoxelShape", "clip", "(Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/world/phys/Vec3;Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/phys/BlockHitResult;"),
        ("net/minecraft/world/entity/projectile/ProjectileUtil", "getHitResultOnMoveVector", "(Lnet/minecraft/world/entity/Entity;Ljava/util/function/Predicate;Lnet/minecraft/world/level/ClipContext$Block;)Lnet/minecraft/world/phys/HitResult;"),
        ("net/minecraft/world/level/LevelReader", "hasChunk", "(II)Z"),
        ("net/minecraft/world/level/LevelReader", "hasChunkAt", "(Lnet/minecraft/core/BlockPos;)Z"),
        ("net/minecraft/client/Minecraft", "startUseItem", "()V"),
        ("net/minecraft/client/Minecraft", "tick", "()V"),
        ("net/minecraft/client/KeyboardHandler", "keyPress", "(JILnet/minecraft/client/input/KeyEvent;)V"),
        ("net/minecraft/client/renderer/entity/FishingHookRenderer", "getPlayerHandPos", "(Lnet/minecraft/world/entity/player/Player;FF)Lnet/minecraft/world/phys/Vec3;"),
        ("net/minecraft/client/renderer/entity/FishingHookRenderer", "getHoldingArm", "(Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/entity/HumanoidArm;"),
        ("net/minecraft/client/renderer/entity/FishingHookRenderer", "lambda$submit$0", "(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;Lcom/mojang/blaze3d/vertex/PoseStack$Pose;Lcom/mojang/blaze3d/vertex/VertexConsumer;)V"),
    )
    try:
        for class_name, name, descriptor in apis:
            checks.run(f"Minecraft API {name}{descriptor}", lambda c=class_name, n=name, d=descriptor: has(c, n, d))
        def vanilla_range():
            klass = get(hook)
            entries = [(index, value) for index, value in enumerate(klass.constants) if value is not None and value == (6, 1024.0)]
            method_codes = [code for (name, _), (_, code) in klass.methods.items() if name == "shouldStopFishing"]
            require(entries and any(b"\x14" + index.to_bytes(2, "big") in code for index, _ in entries for code in method_codes), "Vanilla shouldStopFishing squared-range constant is not 1024 (32 blocks)")
        checks.run("Minecraft vanilla rod 32-block distance", vanilla_range)
        def renderer_push_sites():
            klass = get("net/minecraft/client/renderer/entity/FishingHookRenderer")
            references = [index for index, ref in klass.method_references().items()
                          if ref == ("com/mojang/blaze3d/vertex/PoseStack", "pushPose", "()V")]
            codes = [code for (name, descriptor), (_, code) in klass.methods.items()
                     if name == "submit" and descriptor.startswith("(Lnet/minecraft/client/renderer/entity/state/FishingHookRenderState;")]
            require(sum(code.count(b"\xb6" + index.to_bytes(2, "big")) for index in references for code in codes) >= 2,
                    "Renderer submit no longer has the second pushPose invocation required by ordinal 1")
        checks.run("Minecraft renderer ordinal-1 pushPose target", renderer_push_sites)
        for root in roots:
            def mixin_targets(root=root):
                bindings = [
                    ("FishingHookRendererMixin", "net/minecraft/client/renderer/entity/FishingHookRenderer"),
                    ("FlyingHookStateMixin", hook),
                    ("FlyingHookInputMixin", "net/minecraft/client/Minecraft"),
                    ("FishingRodUseMixin", "net/minecraft/world/item/FishingRodItem"),
                ]
                if root == ROOT:
                    bindings.append(("ClimbingMovementMixin", "net/minecraft/world/entity/LivingEntity"))
                if root != ROOT:
                    bindings.append(("DoubleJumpInputMixin", "net/minecraft/client/KeyboardHandler"))
                for mixin, class_name in bindings:
                    source, klass = java(root, f"mixin/{mixin}.java"), get(class_name)
                    targets = re.findall(r'@(?:Inject|ModifyConstant)\(\s*method\s*=\s*"([^"]+)"', source)
                    require(targets, f"{mixin} has no registered injection targets")
                    for target in targets:
                        name, separator, remainder = target.partition("(")
                        require(any(n == name and (not separator or d == "(" + remainder) for n, d in klass.methods),
                                f"Missing exact {mixin} injection target: {target}")
            checks.run(f"{root.name} exact movement mixin injection descriptors", mixin_targets)
    finally:
        for archive in archives:
            archive.close()


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--fabric-root", type=Path, default=ROOT.parent.parent / f"{ROOT.parent.name} - fa")
    parser.add_argument("--neoforge-only", action="store_true", help="Validate this checkout while Fabric synchronization is paused")
    parser.add_argument("--minecraft-jar", type=Path, action="append", default=[], help="Existing named MC common/client JAR; repeat for both")
    args = parser.parse_args()
    roots = [ROOT] if args.neoforge_only else [ROOT, args.fabric_root.resolve()]
    checks = Checks()
    for name in (() if args.neoforge_only else SHARED):
        checks.run(f"Shared source parity {name}", lambda n=name: require(java(roots[0], n) == java(roots[1], n), f"Loader implementations diverge: {n}"))
    def math_contract():
        math = java(ROOT, "movement/MovementMath.java")
        jump = java(ROOT, "movement/DoubleJumpController.java")
        require("current.add(" in method(math, "reel"), "Rope does not add to the incoming velocity")
        require("radialSpeedLimit(boxGap)" in method(math, "reel")
                and "MAX_RADIAL_SPEED * proximityFactor(boxGap)" in method(math, "radialSpeedLimit"),
                "Server force and authorized cap do not share the same proximity rule")
        require("currentY + VERTICAL_IMPULSE" in method(jump, "addVerticalImpulse"), "Jump sets an absolute Y velocity")
        expected = {"MAX_DISTANCE": 32, "PROJECTILE_SPEED_MULTIPLIER": 1.15, "REEL_SPEED": .35,
                    "PULL_ACCELERATION": .24, "MAX_RADIAL_SPEED": .55, "ARRIVAL_DISTANCE": 1.5,
                    "SLOWDOWN_DISTANCE": 3, "MIN_TENSION_FACTOR": .5, "REEL_TIMEOUT": 120, "STUCK_TICKS": 10}
        constants = dict(re.findall(r"public static final (?:double|int) (\w+)\s*=\s*([\d.]+)[Dd]?\s*;", math))
        require(all(float(constants[name]) == value for name, value in expected.items()), "Shared default parameters differ from documented contract")
        require(re.search(r"VERTICAL_IMPULSE\s*=\s*0\.65[Dd]?", jump), "Independent jump impulse differs from 0.65")
    checks.run("Shared additive math and documented defaults", math_contract)
    for root, loader in zip(roots, ("NeoForge", "Fabric")):
        check_loader(root, loader, checks)
        checks.run(f"{loader} current DOCX movement contract/XML", lambda r=root: check_document(r))
    if not args.neoforge_only:
        checks.run("Both current formal specifications match", lambda: require((roots[0] / SPEC).read_bytes() == (roots[1] / SPEC).read_bytes(), "Formal specification copies differ"))
    if args.minecraft_jar:
        check_minecraft_jars(args.minecraft_jar, roots, checks)
    for failure in checks.failures:
        print("FAIL " + failure, file=sys.stderr)
    print(f"Static movement contract: {checks.passed} passed, {len(checks.failures)} failed; source/resources/DOCX" + ("/cached Minecraft API" if args.minecraft_jar else " (Minecraft API check not requested)") + ".")
    print("These checks do not run Minecraft or verify collision behavior, networking at runtime, or game feel.")
    return bool(checks.failures)


if __name__ == "__main__":
    sys.exit(main())
