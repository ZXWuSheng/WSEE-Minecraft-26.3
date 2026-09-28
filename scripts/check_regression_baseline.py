"""Read-only WSEE 0.1.1 data and registration regression checks.

Run with: python scripts/check_regression_baseline.py
These checks protect static contracts; they do not replace in-game tests.
"""

import json
import re
import unittest
import xml.etree.ElementTree as ET
from pathlib import Path
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
RESOURCES = ROOT / "src/main/resources"
ENCHANTMENTS = RESOURCES / "data/wsee/enchantment"
EXPECTED = {
    "air_force": (1, "#wsee:enchantable/air_force"),
    "armor_piercing": (4, "#minecraft:enchantable/crossbow"),
    "arrow_rain": (3, "#minecraft:enchantable/bow"),
    "battle_momentum": (3, "#wsee:enchantable/battle_momentum"),
    "bleeding": (3, "#minecraft:spears"),
    "clarity": (1, "#minecraft:enchantable/head_armor"),
    "cluster": (3, "#minecraft:enchantable/crossbow"),
    "combo_strike": (3, "#minecraft:swords"),
    "deflect": (1, "#wsee:enchantable/shield"),
    "double_jump": (1, "#wsee:enchantable/double_jump"),
    "execution": (3, "#minecraft:axes"),
    "expansion": (3, "#wsee:enchantable/bow_crossbow"),
    "explosive_arrow": (3, "#wsee:enchantable/bow_crossbow"),
    "falling_star": (3, "#minecraft:enchantable/mace"),
    "flying_hook": (5, "#wsee:enchantable/flying_hook"),
    "forced_march": (5, "#minecraft:enchantable/foot_armor"),
    "foundation": (3, "#wsee:enchantable/foundation"),
    "frostbite": (3, "#wsee:enchantable/frostbite"),
    "holy_light": (3, "#minecraft:enchantable/chest_armor"),
    "hydroshock": (3, "#minecraft:enchantable/trident"),
    "long_edge": (3, "#minecraft:spears"),
    "overcharge": (3, "#minecraft:enchantable/bow"),
    "rebound": (3, "#minecraft:enchantable/trident"),
    "refining": (1, "#wsee:enchantable/refining"),
    "shield": (5, "#minecraft:enchantable/chest_armor"),
    "shield_bash": (1, "#wsee:enchantable/shield"),
    "shock_ring": (3, "#minecraft:enchantable/mace"),
    "surge": (3, "#minecraft:enchantable/trident"),
    "sword_aura": (5, "#minecraft:swords"),
    "tether": (3, "#minecraft:enchantable/trident"),
    "tracking": (3, "#wsee:enchantable/bow_crossbow"),
}
CONFLICTS = {
    "air_force": {"wsee:flying_hook"},
    "arrow_rain": {"wsee:explosive_arrow"},
    "bleeding": {"minecraft:lunge"},
    "cluster": {"wsee:expansion", "minecraft:multishot"},
    "combo_strike": {"wsee:sword_aura"},
    "expansion": {"wsee:cluster", "minecraft:multishot"},
    "explosive_arrow": {"wsee:arrow_rain"},
    "flying_hook": {"minecraft:lure", "minecraft:luck_of_the_sea", "wsee:air_force"},
    "forced_march": {"minecraft:frost_walker"},
    "foundation": set(),
    "frostbite": {"minecraft:fire_aspect", "wsee:bleeding"},
    "holy_light": {"wsee:shield"},
    "hydroshock": {"minecraft:riptide"},
    "rebound": {"minecraft:riptide", "wsee:tether"},
    "refining": {"minecraft:silk_touch"},
    "shield": {"wsee:holy_light"},
    "shield_bash": set(),
    "surge": {"minecraft:riptide", "wsee:tether"},
    "sword_aura": {"minecraft:sweeping_edge", "wsee:combo_strike"},
    "tether": {"minecraft:riptide", "wsee:surge", "wsee:rebound"},
}
CUSTOM_EQUIPMENT_TAGS = {
    "air_force": {"minecraft:fishing_rod"},
    "battle_momentum": {"#minecraft:enchantable/melee_weapon", "#minecraft:axes", "#minecraft:enchantable/trident", "#minecraft:enchantable/mace"},
    "bow_crossbow": {"#minecraft:enchantable/bow", "#minecraft:enchantable/crossbow"},
    "deflect": {"minecraft:shield"},
    "double_jump": {"#minecraft:enchantable/foot_armor"},
    "flying_hook": {"minecraft:fishing_rod"},
    "foundation": {"#minecraft:enchantable/leg_armor"},
    "frostbite": {"#minecraft:swords", "#minecraft:spears"},
    "refining": {"#minecraft:pickaxes"},
}


def read_json(path):
    return json.loads(path.read_text(encoding="utf-8"))


class RegressionBaseline(unittest.TestCase):
    def test_all_json_parses(self):
        files = list(RESOURCES.rglob("*.json"))
        self.assertEqual(79, len(files))
        for path in files:
            with self.subTest(path=path.relative_to(ROOT)):
                read_json(path)

    def test_enchantment_ids_levels_and_equipment(self):
        files = {path.stem: path for path in ENCHANTMENTS.glob("*.json")}
        self.assertEqual(31, len(EXPECTED))
        self.assertEqual(set(EXPECTED), set(files))
        for name, (level, equipment) in EXPECTED.items():
            with self.subTest(enchantment=name):
                definition = read_json(files[name])
                self.assertEqual(level, definition["max_level"])
                self.assertEqual(equipment, definition["primary_items"])
                self.assertEqual(equipment, definition["supported_items"])

    def test_custom_equipment_tags(self):
        root = RESOURCES / "data/wsee/tags/item/enchantable"
        for name, values in CUSTOM_EQUIPMENT_TAGS.items():
            with self.subTest(tag=name):
                self.assertEqual(values, set(read_json(root / f"{name}.json")["values"]))
        self.assertEqual(read_json(root / "deflect.json"), read_json(root / "shield.json"))

    def test_conflicts(self):
        root = RESOURCES / "data/wsee/tags/enchantment/exclusive_set"
        for name, values in CONFLICTS.items():
            with self.subTest(enchantment=name):
                self.assertEqual(values, set(read_json(root / f"{name}.json")["values"]))
                definition = read_json(ENCHANTMENTS / f"{name}.json")
                if name == "foundation":
                    self.assertNotIn("exclusive_set", definition)
                else:
                    self.assertEqual(f"#wsee:exclusive_set/{name}", definition["exclusive_set"])
        for name in ("overcharge", "tracking", "battle_momentum", "shock_ring", "falling_star"):
            with self.subTest(no_extra_conflict=name):
                self.assertNotIn("exclusive_set", read_json(ENCHANTMENTS / f"{name}.json"))

    def test_localized_names_and_descriptions(self):
        for locale in ("en_us", "zh_cn"):
            language = read_json(RESOURCES / f"assets/wsee/lang/{locale}.json")
            for name in EXPECTED:
                with self.subTest(locale=locale, enchantment=name):
                    key = f"enchantment.wsee.{name}"
                    self.assertTrue(language[key])
                    description = language[key + ".desc"]
                    self.assertIsInstance(description, str)
                    self.assertTrue(description)
                    self.assertNotIn("\n", description)
                    self.assertNotIn("\r", description)

    def test_creative_book_inventory(self):
        sources = list((ROOT / "src/main/java").rglob("*.java"))
        creative_sources = [p.read_text(encoding="utf-8") for p in sources if "CREATIVE_MODE_TABS.register" in p.read_text(encoding="utf-8")]
        self.assertEqual(1, len(creative_sources))
        text = creative_sources[0]
        block = text[text.index("CREATIVE_MODE_TABS.register"):text.index(".build());", text.index("CREATIVE_MODE_TABS.register"))]
        ids = re.findall(r'Identifier\.fromNamespaceAndPath\(MOD_ID,\s*"([a-z_]+)"\)', block)
        self.assertEqual(set(EXPECTED), set(ids))
        self.assertEqual(31, len(ids))

    def test_runtime_equipment_backstop_covers_all_ids(self):
        sources = [p.read_text(encoding="utf-8") for p in (ROOT / "src/main/java").rglob("*.java")]
        declarations = {}
        for source in sources:
            declarations.update(re.findall(
                r'(\w+_ID)\s*=\s*Identifier\.fromNamespaceAndPath\(MOD_ID,\s*"([a-z_]+)"\)',
                source,
            ))
        backstops = [source for source in sources if "boolean isValidEnchantmentItem(ItemStack stack, Identifier id)" in source]
        self.assertEqual(1, len(backstops))
        method = backstops[0].split("boolean isValidEnchantmentItem(ItemStack stack, Identifier id)", 1)[1]
        method = method.split("private static boolean isRangedWeapon", 1)[0]
        checked = set(re.findall(r'id\.equals\((\w+_ID)\)', method))
        self.assertEqual(set(EXPECTED), {declarations[constant] for constant in checked})

    def test_equipment_event_backstops_remain_registered(self):
        main = (ROOT / "src/main/java/cn/wusheng/oatz/WseeMod.java").read_text(encoding="utf-8")
        handler = (ROOT / "src/main/java/cn/wusheng/oatz/enchantment/EnchantmentApplicabilityHandler.java").read_text(encoding="utf-8")
        for event in ("onAnvilUpdate", "onPlayerEnchantItem"):
            with self.subTest(event=event):
                self.assertIn(f"EnchantmentApplicabilityHandler::{event}", main)
                self.assertIn(f"void {event}(", handler)
                self.assertIn("isValidEnchantmentItem(", handler)

    def test_payload_registration_contract(self):
        main = (ROOT / "src/main/java/cn/wusheng/oatz/WseeMod.java").read_text(encoding="utf-8")
        network = (ROOT / "src/main/java/cn/wusheng/oatz/registry/ModNetworking.java").read_text(encoding="utf-8")
        self.assertIn("modBus.addListener((RegisterPayloadHandlersEvent event) -> ModNetworking.register(", main)
        self.assertIn('event.registrar("1")', network)
        self.assertEqual(["ShieldBashPayload", "SwordAuraPayload", "DoubleJumpPayload"],
                         re.findall(r"registrar\.playToServer\((\w+)\.TYPE", network))
        self.assertEqual(["TrackingTargetPayload", "FlyingHookAnchorPayload", "DoubleJumpVelocityPayload"],
                         re.findall(r"registrar\.playToClient\((\w+)\.TYPE", network))
        self.assertEqual(6, network.count("context.enqueueWork("))

    def test_refining_uses_ore_tags_and_smelting_recipes(self):
        ore_tag = read_json(RESOURCES / "data/wsee/tags/block/refinable_ores.json")
        values = {entry["id"] if isinstance(entry, dict) else entry for entry in ore_tag["values"]}
        self.assertTrue({"#c:ores", "#minecraft:ores", "#forge:ores"}.issubset(values))
        handler = (ROOT / "src/main/java/cn/wusheng/oatz/tool/RefiningHandler.java").read_text(encoding="utf-8")
        self.assertIn("event.getState().is(REFINABLE_ORES)", handler)
        self.assertIn("getRecipeFor(", handler)
        self.assertIn("RecipeType.SMELTING", handler)
        self.assertIn("RefiningRules.fortunePreservingCount(result.getCount(), input.getCount())", handler)

    def test_formal_document_inventory(self):
        path = ROOT / "WSEE-0.1.1-Minecraft-26.3-Fabric-NeoForge-详细规格.docx"
        ns = {"w": "http://schemas.openxmlformats.org/wordprocessingml/2006/main"}
        with ZipFile(path) as archive:
            xml = ET.fromstring(archive.read("word/document.xml"))
        for table in xml.findall(".//w:tbl", ns):
            rows = []
            for row in table.findall("w:tr", ns):
                rows.append(["".join(t.text or "" for t in cell.findall(".//w:t", ns))
                             for cell in row.findall("w:tc", ns)])
            if rows and "ID" in rows[0] and "最高等级" in rows[0]:
                break
        else:
            self.fail("formal enchantment inventory missing")
        self.assertEqual(31, len(rows) - 1)
        roman = {"I": 1, "II": 2, "III": 3, "IV": 4, "V": 5}
        self.assertEqual({name: level for name, (level, _) in EXPECTED.items()},
                         {row[2]: roman[row[4]] for row in rows[1:]})

    def test_readme_and_repository_rules_count(self):
        self.assertIn("31 custom enchantments", (ROOT / "README.md").read_text(encoding="utf-8"))
        self.assertIn("31-enchantment inventory", (ROOT / "AGENTS.md").read_text(encoding="utf-8"))


if __name__ == "__main__":
    unittest.main(verbosity=2)
