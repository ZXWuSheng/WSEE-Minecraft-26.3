# Repository Guidelines

## Project Structure & Module Organization

This repository is a distributable Minecraft client instance.

- `OA测试.jar` is the packaged Minecraft client artifact.
- `OA测试.json` is the launcher profile: version metadata, JVM/game arguments, NeoForge settings, and library references.
- `mods/` is the drop-in location for optional NeoForge mod JARs (currently empty).
- `resourcepacks/` is the drop-in location for resource-pack ZIPs (currently empty).
- `PCL/Setup.ini` contains Plain Craft Launcher profile metadata.

Keep launcher metadata and binary artifacts at the root. Do not add generated game directories, saves, logs, or caches.

## Build, Test, and Development Commands

There is no build system or automated test suite. Use Plain Craft Launcher (PCL) to launch the `OA测试` profile. It targets Minecraft `26.3`, NeoForge `26.3.0.6-beta`, and Java 25.

Validate profile syntax and record artifact checksums before submitting changes:

```powershell
Get-Content -Raw .\OA测试.json | ConvertFrom-Json
Get-FileHash .\OA测试.jar -Algorithm SHA256
```

For mod or resource-pack changes, place files in the respective directory and perform a clean client launch. Do not modify `OA测试.jar` unless replacing the upstream client is required.

## Coding Style & Naming Conventions

Use two-space JSON indentation and preserve launcher-generated key ordering. Keep the profile ID and filenames consistent (`OA测试.json`, `OA测试.jar`). Use upstream `modid-version.jar` names for mods and descriptive lowercase names for resource-pack ZIPs. No formatter or linter is configured.

## Testing Guidelines

Testing is manual smoke testing: launch through PCL, confirm the title screen loads, verify NeoForge, and check each added mod or resource pack in-game. Record compatibility notes and OS/Java versions in the pull request.

## Commit & Pull Request Guidelines

No Git history is present, so no established convention can be inferred. Use short imperative subjects such as `Add performance mod` or `Update NeoForge profile`. Pull requests should list changed paths and versions, explain binary provenance, include validation results, attach screenshots for visible changes, and call out checksum changes when replacing JARs.

## Security & Configuration Tips

Treat JARs and ZIPs as executable content; verify source and checksum before adding them. Never commit account tokens, private endpoints, worlds, or launcher caches. Change profile values through PCL and review the JSON diff for unintended JVM or game-argument changes.

## Enchantment Specification Document Rules

The document `无声无息-拓展附魔（Without a sound Expanded Enchantments）-详细数值.docx` is maintained with every enchantment update.

- New entries must be placed at the end of the document. Do not append maintenance statistics, history logs, or unrelated tracking sections.
- Every enchantment entry must use this exact order: `附魔介绍` → `等级数值` → `规则` → `冲突` → `指令`.
- `附魔介绍` states the Chinese and English names, applicable equipment, maximum level, and core effect.
- `等级数值` must use a readable table whenever levels or numeric values exist. Command information must also use a two-column table (`命令` / `作用`).
- Section titles use the existing Heading 1 style. Subsection titles use the existing blue Heading 2 style. Rule details use the existing `List Bullet` style.
- Table headers use a dark blue fill with bold white text. Body text and table text use the same document font, `微软雅黑`.
- Keep all entries under the correct version section. The current release is `WSEE 0.1.1`; all current enchantment entries belong to its `WSEE 0.1.1` section unless a later version is explicitly created.
- The Chinese name of the shield enchantment must be written as `“闪”`; its English name is `Deflect`. There is no custom mace enchantment.
- After editing, validate that the DOCX opens successfully and that its internal XML is well-formed before handing it off.

## Enchantment Implementation Rules

All WSEE enchantments are custom, equipment-specific enchantments. Their data definitions, item tags, runtime checks, anvil checks, enchanting-table checks, creative-book entries, language keys, and documentation must agree. Never rely only on a data tag: keep the server-side `isValidEnchantmentItem` backstop in sync.

### Equipment ownership

- 剑气 (`sword_aura`): swords only; conflicts with vanilla Sweeping Edge. Its range by level I-V is 6/7/8/9/10 blocks and its wave width is 2.5 blocks. It fires toward the player's horizontal facing direction, works on both a hit and an air swing, is disabled while blocking or while an inventory/chat screen is open, and uses one sweeping-edge particle per forward row rather than a thin line or a stack of three waves.
- 盾击 (`shield_bash`): shields only; it has a 50-tick cooldown and an air swing also starts the cooldown. It must not trigger while an inventory/chat screen is open.
- “闪” (`deflect`): shields only; English name `Deflect`. It reflects only normal arrows, tipped arrows, and spectral arrows. Reflection uses the blocking player's look direction, speed ×0.90, damage ×0.80, transfers attack ownership, and marks each projectile so it can be reflected at most once. Do not reflect explosive arrows or unrelated projectiles.
- 扩容 (`expansion`) supports bows and crossbows; 聚束 (`cluster`) and 穿甲 (`armor_piercing`) remain crossbow-only. Expansion and Cluster conflict with each other and with vanilla Multishot; Expansion may coexist with Explosive Arrow and Tracking. Cluster must allow every generated arrow to resolve its damage rather than only the first arrow.
- 流血 (`bleeding`), 长锋 (`long_edge`): spear tag only. Bleeding conflicts with Lunge and is a physical damage-over-time effect that respects armor, toughness, Protection, and does not recursively trigger hit enchantments. Long Edge changes spear entity attack range only.
- 涌流 (`surge`), 牵引 (`tether`), 回旋 (`rebound`), 震水 (`hydroshock`): thrown-trident mechanics only; never apply them to melee trident attacks. Surge conflicts with Riptide. Tether conflicts with Riptide, Surge, and Rebound. Rebound requires Loyalty and conflicts with Riptide and Tether. Hydroshock conflicts with Riptide and may coexist with Surge, Tether, and Rebound.
- 蓄势 (`overcharge`) remains bow-only; 爆矢 (`explosive_arrow`) supports bows and crossbows. These have different equipment ownership from Combo Strike. Overcharge's bonus starts only after the vanilla full-draw threshold; I/II/III add up to +15%/+25%/+35% damage and +5%/+8%/+12% velocity after 10/15/20 extra ticks, with linear progress and visible but restrained feedback. Explosive Arrow conflicts with Arrow Rain, may coexist with Expansion and Tracking, and its delayed explosion is based on the shooter's attack power and must trigger on both entity and block impacts.
- 连斩 (`combo_strike`), 剑气 (`sword_aura`): swords only. Combo Strike conflicts with Sword Aura, uses a two-cell charge display (empty, then yellow, then red for the final-hit-ready state), and must not conflict with Battle Momentum.
- 越战越勇 (`battle_momentum`): melee weapons only (swords, axes, spears, tridents, and maces). It counts only successful direct melee damage by the player using the current main-hand weapon; projectile, bleed, fire, thorns, explosion, potion, and other independent damage must not add stacks.
- 处决 (`execution`): axes only. It modifies the current attack before defense calculation, uses current health plus absorption for the 30% threshold, and never creates a second damage event.
- 急行军 (`forced_march`): boots only; conflicts with Frost Walker. It uses a movement-speed attribute modifier, requires ground sprinting, and has a 15-tick (0.75-second) grace period after sprinting stops.
- 明 (`clarity`): helmets only; it is level I and removes/blocks Darkness, Blindness, and Nausea while equipped.
- 护盾 (`shield`), 圣光 (`holy_light`): chest armor only and mutually exclusive. Shield periodically grants vanilla Absorption based on maximum health (I-V: 8%, 12%, 16%, 20%, 25%), refreshes every 4800 ticks (240 seconds), does not stack, preserves its cooldown across re-equipping/relog, immediately removes its absorption when the chest armor is removed, and supports the targeted cooldown reset command. Holy Light is self-only, has a 7200-tick (360-second) cooldown at every level, and must not create a 5x5 group field.

### Frostbite implementation note

- Frostbite (`frostbite`) is restricted to swords and spears. Successful direct melee damage by the player, including Sword Aura damage, adds vanilla Frozen Ticks (I/II/III: +30/+45/+60, capped by the entity freeze requirement) and applies Slowness I/I/II for 40/60/60 ticks. It does not add direct damage or create a custom freeze effect; vanilla freezing damage and thawing remain authoritative. Frostbite conflicts with Fire Aspect and Bleeding. Projectile, bleeding, fire, thorns, explosion, potion, and other independent damage must not trigger it, and it must not recursively trigger itself.

### Arrow Rain implementation note

- Arrow Rain (`arrow_rain`) is restricted to bows and conflicts with Explosive Arrow. A shot drawn to at least 90% that first hits an entity or block creates a 10-tick warning, then spawns real non-pickup arrows over the impact area for 30/36/40 ticks (I/II/III: 6/9/12 arrows, radius 2.25/2.75/3.25 blocks, base damage multipliers 0.18/0.20/0.22). Rain arrows use normal collision, armor, protection, shields, and hurt immunity; they are marked to prevent Arrow Rain, Explosive Arrow, and Overcharge recursion. Entity centers may be followed during the warning only; the rain area is fixed afterward, and indoor ceiling clearance must be respected.
- Tracking (`tracking`) supports bows and crossbows. For bow or crossbow shots reaching at least 80% firing strength, select one valid LivingEntity target once at spawn within 24/48/72 blocks for levels I/II/III and ±25°/±35°/±50° of the initial direction. During up to 40 ticks, gently turn toward the locked target by at most 3.0°/5.0°/7.0° per tick while preserving speed, gravity, air drag, collision, armor, shields, and damage. Stop permanently on obstruction, invalid/dead target, >70° initial-direction deviation, or timeout; never switch targets, pierce blocks, increase damage/speed, or alter Owner. Tracking has no additional conflict and may coexist with Expansion, Explosive Arrow, and Arrow Rain.
- Tracking lock feedback is shooter-local: send only the locked entity ID to that player's client, show a temporary vanilla glowing outline while the arrow remains tracked, and clear it when tracking ends. Never set the server entity's global glowing flag or expose the indicator to other players.

### Shared behavior safeguards

- Every custom enchantment must be rejected when applied to an unsupported item through commands, an anvil, or the enchanting table. Creative mode may list only valid enchanted books.
- Direct damage modifiers must modify the existing damage event. Do not create a second damage event for Sword Aura, Combo Strike, Execution, Overcharge, Surge, or similar effects. Persistent damage effects must use their own damage type and explicitly prevent recursion.
- Cooldown reset commands must reset only the named enchantment state. Keep command suggestions and localized error text synchronized with the actual command handlers; do not expose unrelated internal names.
- The supported reset names are `shield`, `holy_light`, and `shield_bash`; each reset is independent and must not clear the other enchantments' state.
- Any effect that is difficult to see needs restrained positive feedback: particles and/or a one-shot sound on activation or a state change. Do not generate large continuous particle volumes, obscure the crosshair, or repeat full-charge bursts every tick.
- UI state displays must match the intended semantics: Combo Strike shows two cells while held; the second cell turns red when the next hit is the final hit. Overcharge shows its charge stages only while drawing and must not show empty cells merely from holding the item.
- Shield absorption must be the vanilla Absorption effect so the yellow hearts and effect icon are visible in the inventory. Removing the enchanted chest armor must immediately remove the WSEE-granted absorption without deleting unrelated absorption effects.
- Do not trigger Sword Aura or Shield Bash while the player has an inventory, chat, or other blocking screen open. Do not use the player's aim target for Sword Aura direction; use the player's facing direction.

### Version, build, and launch safeguards

- The current mod identity is `WSEE 0.1.1`, display name `无声无息：拓展附魔（Without a sound: Expanded Enchantments）`, author `ZX_Wu_Sheng`, and mod id `wsee`. Credits are `KsqsnK，Badnng，Wangmeili，Niorris`. Do not reintroduce OATZ as the public display name.
- All current enchantment entries are filed under the WSEE 0.1.1 section. Keep the document's version sections and the README enchantment count synchronized.
- After source or resource changes, run a clean build with Java 25, validate every JSON resource, inspect the JAR entries, calculate its SHA-256, and copy the resulting JAR to the test instance mods directory.
- Do not start the Minecraft client automatically. The user launches the client manually; report build and validation results without opening PCL or a test world.

### Foundation enchantment

- Foundation (`foundation`) is leggings-only, maximum level III, and uses the vanilla `minecraft:knockback_resistance` attribute. Apply a transient modifier of +0.15/+0.25/+0.35 for levels I/II/III while the enchanted leggings are equipped.
- Remove the modifier immediately when leggings are removed or replaced, and replace it when the enchantment level changes. Never allow duplicate Foundation modifiers. Keep the data tag, runtime item validation, enchanting/anvil restrictions, creative book entry, language keys, and DOCX entry synchronized.

### Air Force enchantment

- Air Force (`air_force`) is fishing-rod-only, maximum level I. On a genuine successful fishing loot event, replace the normal fish/junk/treasure drop with exactly one base ItemStack selected from the current registered items.
- Candidates must exclude `#wsee:air_force_fish`, `#wsee:air_force_unobtainable`, `#wsee:air_force_admin_blacklist`, and `#wsee:air_force_blacklist`, plus the built-in technical-item safety list. Fish probability is always 0%; Luck of the Sea only selects Common/Uncommon/Rare/Epic pools using the documented weights.
- Preserve vanilla fishing experience, rod durability, entity-hook behavior, armor/item state, and projectile/drop presentation. Keep fishing-rod runtime validation, anvil/enchanting restrictions, creative book entry, language keys, tags, README count, and the DOCX entry synchronized.

### 飞钩附魔

- 飞钩（`flying_hook`）仅适用于钓鱼竿，最高等级为 I。保留现有鱼钩发射速度 ×1.15 和无重力飞行；最大距离沿用原版鱼竿的 32 格，不增加等级对应的距离奖励。
- 方块锚点必须使用真实碰撞形状，保存精确命中坐标、维度、方块位置及命中面；`LivingEntity` 锚点保存相对命中偏移并跟随实体。命中后进入固定等待阶段，不自动牵引玩家。
- 沿用钓鱼竿使用键：首次使用发射，锚定后再次使用开始收绳，收绳期间再次使用取消并回收，未命中时再次使用回收，到达锚点附近自动回收。服务端校验原始持竿手、当前钩头、玩家、锚点及操作阶段。
- 收绳每 tick 将目标绳长缩短 0.35 格。只有绳索绷紧时才向当前 `DeltaMovement` 叠加朝锚点的加速度，最大为 0.24 格/tick²；飞钩提供的径向速度上限为 0.55 格/tick，保留切向运动和独立冲量。在 1.5 格到达边界之外的 3 格范围内逐步减力，系数由 1 降至最低 0.5（`MIN_TENSION_FACTOR`），保留有限张力；锚点至玩家碰撞箱 `AABB` 的最近距离不超过 1.5 格时判定到达。
- 收绳持续 120 tick、连续 10 tick 无进展、锚点无效或缺失、区块不可用、超出距离、钩头移除、失去装备资格、切换维度、死亡、退出、骑乘、滑翔或创造飞行时结束。清理可重复调用，并保留自然速度；不得强制加载远处区块或刷新二段跳次数。
- 与原版饵钓、海之眷顾和空军冲突。强制加入非法组合时，飞钩模式优先并抑制普通钓鱼战利品；不带飞钩的鱼竿继续普通钓鱼和空军流程。耐久、经验修补及消失诅咒保持原版兼容。
- 保留既有耐久和摔落距离规则：保存收绳前的摔落距离，收绳期间清零，结束时恢复 `max(saved, current)`。服务端使用已验证的 `getKnownMovement()` 估算径向速度，只授权本次牵引增量并切换阶段。通过 `MovementImpulsePayload` 同步增量，客户端校验当前自身、维度、会话 UUID 和递增序号后，只向接收时的自身实际速度叠加一次；不得用全量速度包覆盖玩家惯性，也不得清除原版或其他模组的运动同步标志。服务端同时给正向牵引增量附带径向速度上限；客户端只按接收时的实际速度缩小这一次授权增量，已有速度超过上限时保留原速度、不再追加，切向速度不变。接近锚点时只减少新增拉力，完整保留已有速度；由原版重力、阻力、碰撞和到达回收自然结束运动。二段跳不使用该正向限速，这只是执行服务端授权的保守幅度，不是客户端自行判定资格或持续预测牵引。客户端同时显示同步后的钩头和绳索。

### 二段跳附魔

- 二段跳（`double_jump`）仅适用于靴子，最高等级为 I。离地后，一次经过服务端验证的第二跳读取当前完整速度，并且只叠加一次独立的向上冲量 `(0, 0.65, 0)`。X/Z 不变，Y 在原值上增加，保留原有上升、下落及外部推力。不得调用原版地面跳跃加速、改变疾跑状态、设置隐藏的最低 Y 速度或在客户端预测叠加。
- 通过绑定跳跃键的客户端按键事件、tick 上升沿以及服务端 `isJumping()` 上升沿识别一次新按键；同一空中机会的重复请求必须拒绝，成功后由服务端通过 `MovementImpulsePayload` 授权一次向上增量。客户端收到授权后向自身实际当前速度叠加，通过会话和序号去重；不得在输入时预测施加冲量，也不得自行决定资格。激活反馈只由服务端产生一次。
- 只有真实的 `onGround` 接触可以刷新次数。墙面或实体接触、蜘蛛网、梯子、游泳、骑乘、鞘翅、旁观模式、创造飞行、物品切换及飞钩清理都不能刷新。失去靴子资格时禁止触发，但不得授予新的空中机会。
- 第二跳保留既有摔落距离重置规则，之后按原版继续下落。原版摔落伤害起算阈值额外增加 2 格，不乘算或取消最终伤害，并保持与摔落保护兼容。
- 飞钩收绳期间可以叠加二段跳的一次向上冲量；两个系统不得清除或重置对方的状态，飞钩后续更新必须保留切向速度及独立运动。

### Current synchronization requirements

- Documentation values must match the current game implementation: Overcharge uses the implemented I/II/III bonuses of +7%/+17%/+27% damage and +5%/+8%/+12% velocity after 10/15/20 extra ticks; Refining directly converts iron, gold, and copper ore drops to the corresponding ingots while preserving Fortune counts; Holy Light uses the implemented self-only healing field (20% health trigger, 15 seconds, 20-tick healing, level values 2%/2.5%/3%) and a 7200-tick cooldown.
- Falling Star (`falling_star`) is mace-only, maximum level III, and triggers only after a valid vanilla `MACE_SMASH` direct hit with final health damage greater than zero and the pre-hit fall distance at least 4 blocks. It must capture the fall distance before vanilla mace processing resets or consumes it, use the directly hit entity position as the impact center, apply no extra damage, and apply Slowness I/I/II for 40/60/60 ticks within radius 3/4/5 blocks while respecting existing stronger Slowness. Search LivingEntity targets only, exclude the attacker and allied/friendly targets according to normal attack rules, and never scan item/projectile/experience entities. Spawn restrained falling-star particles above the impact center and one extra impact sound; do not create FallingBlock or Projectile entities or per-particle sounds. Keep the data tag, runtime validation, enchanting/anvil restrictions, creative book entry, language keys, README count, and DOCX entry synchronized.
- Shock Ring (`shock_ring`) is mace-only, maximum level III, and triggers only after a valid vanilla `MACE_SMASH` direct hit on a LivingEntity with final health damage greater than zero. It fixes the direct-hit entity position as the center, records the pre-defense smash damage as the base, and expands once per tick through a 0.65-block ring band at 0.60/0.75/0.90 blocks per tick to maximum radius 3/4/5. Each eligible LivingEntity is hit at most once for base damage x0.12/x0.16/x0.20 using the dedicated `wsee:shock_ring` DamageType, normal armor/shield/Protection defenses, and owner attribution; apply horizontal knockback 0.30/0.45/0.60 scaled by knockback resistance plus Y 0.08. Exclude the owner, direct target, allied/friendly or PvP-invalid targets, entities outside 1.75 blocks vertically, and targets behind solid blocks. Shock Ring damage is indirect and must not recursively trigger Falling Star, Battle Momentum, Combo Strike, Bleeding, Frostbite, Execution, or other direct-hit enchantments. Spawn a fixed-center ground-level particle ring and one impact sound, allow multiple independent instances, and remove each instance after it reaches its maximum radius.
- Surge synchronization: Surge is evaluated only from the server-side `ThrownTrident` projectile path. During outbound flight, each consecutive water-flight tick is counted using both the entity fluid state and the occupied fluid blocks (I/II/III require 6/5/4 ticks); the impact tick is included so a water-boundary hit cannot miss activation. Active Surge modifies that same thrown-trident damage event (x1.15/x1.25/x1.35) and adds the documented knockback (x1.10/x1.20/x1.30). Melee trident attacks never enter this path, and return/no-physics tridents are excluded.
- The previously planned custom mace enchantment has been removed completely. It must not appear in source, resources, language files, creative entries, network payloads, HUD code, README, or DOCX documentation; the enchantment count is reduced accordingly.
- Enchantment book descriptions in `zh_cn.json` and `en_us.json` must describe the current implementation, not legacy behavior. Keep each description as one concise translation string without literal newline control characters; the WSEE client tooltip hook splits deliberate `；` / `; ` separators into separate rows because Enchantment Descriptions may display `LF` markers for embedded newlines. Include important trigger conditions and current numeric values without creating an excessively long tooltip.
- WSEE description rows use `DARK_GRAY` exactly (RGB 85/85/85, `#555555`); do not inherit arbitrary external tooltip colors.
- Wave Treading is removed completely and must not appear in source, resources, language files, creative entries, network payloads, HUD code, README, or DOCX documentation.
- The test instance must keep Minecraft's advanced item tooltips disabled (`advancedItemTooltips:false`); `F3+H` debug fields such as item IDs and component counts are not enchantment descriptions.

### Platform-specific artifact names

- NeoForge builds must produce `build/libs/WSEE-0.1.1-NeoForge.jar`.
- Fabric builds must produce `build/libs/WSEE-0.1.1-Fabric.jar`.
- Keep the platform suffix when copying, uploading, or documenting artifacts; never describe either platform's current output as the unsuffixed `WSEE-0.1.1.jar`.

### Dual-loader specification document

- The formal current specification is `WSEE-0.1.1-Minecraft-26.3-Fabric-NeoForge-详细规格.docx` at the repository root.
- The document must cover both NeoForge `26.3.0.6-beta` and Fabric Loader `0.19.5` with Fabric API `0.158.0+26.3`. Both loaders share the `wsee` IDs, language keys, data tags, equipment restrictions, conflicts, numeric values, and behavior contract; only loader integration details may differ.
- Keep the document's 31-enchantment inventory synchronized with source, resources, README, and both loader metadata files. Do not present the old single-loader document as the current specification.
