# 无声无息：拓展附魔（Without a sound: Expanded Enchantments）

Fabric / NeoForge Minecraft 26.3 mod (WSEE 0.1.0). Adds 31 custom enchantments for swords, axes, spears, tridents, bows, crossbows, pickaxes, fishing rods, shields, maces, and armor, with shared behavior across both loaders. 

## Build

Use Java 25 and run `./gradlew build` (Windows: `./gradlew.bat build`) in the selected loader project. NeoForge produces `build/libs/WSEE-0.1.0-NeoForge.jar`; Fabric produces `build/libs/WSEE-0.1.0-Fabric.jar`. Copy only the matching artifact into the instance `mods/` directory.

## Behavior

All custom enchantments are restricted to their declared equipment type in both data-driven enchanting and the server-side backstop. Sword Aura releases a travelling frontal wave, Shield Bash uses a dedicated 50-tick cooldown, and Falling Star and Shock Ring are limited to valid mace smash attacks. Trident enchantments are limited to thrown `minecraft:trident` entities, while Flying Hook follows an accelerated arc with vanilla gravity, anchors to entities or physical block surfaces on contact, and automatically pulls the player in. See `WSEE-0.1.0-Minecraft-26.3-Fabric-NeoForge-详细规格.docx` for the complete level tables, conflicts, dual-loader installation boundary, and test commands.

Double Jump preserves upward momentum while climbing vines, ladders, and other climbable blocks, so climbing no longer cuts an air jump short. A successful double jump releases an active Flying Hook pull, preserves horizontal momentum, and sets upward speed to 0.65. Gravity and collisions still apply; only landing on the ground restores the extra jump.

Flying Hook uses its real hitbox to catch block edges as well as direct hits. Both its center ray and swept hitbox use physical collision shapes, so grass plants, flowers, and vines are neither anchors nor pull obstacles; solid grass blocks remain valid. Anchors are confirmed by the server and remain on the contacted surface; the current and previous render positions are fixed together so an attached hook does not repeatedly bob toward its anchor. Pulling gets an initial movement attempt before checking obstruction. A contacted block face that prevented movement then ends the pull and retracts the hook. Supporting ground does not interrupt a downward-aimed pull with horizontal movement, and vanilla stepping can complete before deciding that a ledge blocks progress. Climbing speed limits no longer interrupt an active pull, and small advances accumulate before the ten-tick stall fallback. `gradlew check` covers hook contacts, actual player obstructions, downward casts across ground and uphill steps, and movement away from or alongside obstacles.

## Credits

KsqsnK，Badnng，Wangmeili，Niorris
