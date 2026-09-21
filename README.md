# 无声无息：拓展附魔（Without a sound: Expanded Enchantments）

Fabric / NeoForge Minecraft 26.3 mod (WSEE 0.1.0). Adds 31 custom enchantments for swords, axes, spears, tridents, bows, crossbows, pickaxes, fishing rods, shields, maces, and armor, with shared behavior across both loaders. 

## Build

Use Java 25 and run `./gradlew build` (Windows: `./gradlew.bat build`) in the selected loader project. NeoForge produces `build/libs/WSEE-0.1.0-NeoForge.jar`; Fabric produces `build/libs/WSEE-0.1.0-Fabric.jar`. Copy only the matching artifact into the instance `mods/` directory.

## Behavior

All custom enchantments are restricted to their declared equipment type in both data-driven enchanting and the server-side backstop. Sword Aura releases a travelling frontal wave, Shield Bash uses a dedicated 50-tick cooldown, and Falling Star and Shock Ring are limited to valid mace smash attacks. Trident enchantments are limited to thrown `minecraft:trident` entities, while Flying Hook flies without dropping, anchors to any entity or block face on contact, and automatically pulls the player in. See `WSEE-0.1.0-Minecraft-26.3-Fabric-NeoForge-详细规格.docx` for the complete level tables, conflicts, dual-loader installation boundary, and test commands.

## Credits

KsqsnK，Badnng，Wangmeili，Niorris
