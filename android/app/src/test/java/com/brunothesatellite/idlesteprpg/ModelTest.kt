package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.engine.BIOME_POOLS
import com.brunothesatellite.idlesteprpg.engine.CONSUMABLES
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.LOOT_TEMPLATES
import com.brunothesatellite.idlesteprpg.engine.biomeKeyFor
import com.brunothesatellite.idlesteprpg.engine.lootTemplatesFor
import com.brunothesatellite.idlesteprpg.engine.bossFor
import com.brunothesatellite.idlesteprpg.engine.allBossTemplates
import com.brunothesatellite.idlesteprpg.engine.allMobTemplates
import com.brunothesatellite.idlesteprpg.engine.mobPoolFor
import com.brunothesatellite.idlesteprpg.engine.statsOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ModelTest {

    @Test
    fun biomesSwitchEveryTenFloors() {
        assertEquals("crypts", biomeKeyFor(1))
        assertEquals("crypts", biomeKeyFor(10))
        assertEquals("catacombs", biomeKeyFor(11))
        assertEquals("catacombs", biomeKeyFor(20))
        assertEquals("ash", biomeKeyFor(21))
        assertEquals("forge", biomeKeyFor(31))
        assertEquals("peaks", biomeKeyFor(41))
        assertEquals("woods", biomeKeyFor(51))
        assertEquals("abyss", biomeKeyFor(61))
        assertEquals("throne", biomeKeyFor(71))
        assertEquals("throne", biomeKeyFor(200))
    }

    @Test
    fun eachBiomeHasACompleteMobPool() {
        BIOME_POOLS.forEach { (key, pool) ->
            assertTrue("$key easy", pool.easy.isNotEmpty())
            assertTrue("$key mid", pool.mid.isNotEmpty())
            assertTrue("$key hard", pool.hard.isNotEmpty())
            assertTrue("$key bosses", pool.bosses.isNotEmpty())
        }
    }

    @Test
    fun mobPoolsAreBiomeSpecific() {
        val crypt = mobPoolFor(1).easy.first().icon
        val catacomb = mobPoolFor(11).easy.first().icon
        val woods = mobPoolFor(51).easy.first().icon
        val abyss = mobPoolFor(61).easy.first().icon
        assertNotEquals(crypt, catacomb)
        assertNotEquals(catacomb, woods)
        assertNotEquals(woods, abyss)
    }

    @Test
    fun bossesFollowBiomeAndMilestones() {
        assertEquals("Roi des Cryptes", bossFor(10).name)
        assertEquals("Hydre de Lerne", bossFor(20).name)
        assertEquals("Balrug", bossFor(30).name)
        assertEquals("Seigneur de la Forge", bossFor(40).name)
        assertEquals("Dragon de givre", bossFor(50).name)
        assertEquals("Tréant", bossFor(60).name)
        assertEquals("Vase mortelle", bossFor(70).name)
        assertEquals("Dispater", bossFor(80).name)
        assertEquals("L'Âme Première", bossFor(100).name)
    }

    @Test
    fun eachBiomeHasItsOwnLootTable() {
        assertEquals(true, lootTemplatesFor(1).any { it.id == "crypt_blade" })
        assertEquals(true, lootTemplatesFor(11).any { it.id == "cat_axe" })
        assertEquals(true, lootTemplatesFor(21).any { it.id == "ash_blade" })
        assertEquals(true, lootTemplatesFor(31).any { it.id == "forge_hammer" })
        assertEquals(true, lootTemplatesFor(41).any { it.id == "peak_blade" })
        assertEquals(true, lootTemplatesFor(51).any { it.id == "wood_staff" })
        assertEquals(true, lootTemplatesFor(61).any { it.id == "abyss_whip" })
        assertEquals(true, lootTemplatesFor(71).any { it.id == "throne_blade" })
        assertTrue(lootTemplatesFor(1).size >= 12)
        assertTrue(LOOT_TEMPLATES.size >= 40)
    }

    @Test
    fun newSetsApplyTheirBonuses() {
        val base = statsOf(GameState())
        val osState = GameState().apply {
            equipment = equipment.map { it.copy(setName = "os") }
        }
        val os = statsOf(osState)
        assertTrue(os.atk > base.atk)
        assertTrue(os.maxHp > base.maxHp)

        val forgeState = GameState().apply {
            equipment = equipment.map { it.copy(setName = "forge") }
        }
        assertTrue(statsOf(forgeState).damageReduction > base.damageReduction)

        val oathState = GameState().apply {
            equipment = equipment.map { it.copy(setName = "serment") }
        }
        assertTrue(statsOf(oathState).bossDamage > base.bossDamage)
    }

    @Test
    fun newEffectsApplyTheirBonuses() {
        val base = statsOf(GameState())
        val vitality = GameState().apply {
            equipment = equipment + Item(
                "vitality_test", "Test vitalité", "talisman", "epic",
                "item/i-regeneration.png", 1, effect = "vitalite"
            )
        }
        assertTrue(statsOf(vitality).regen > base.regen)

        val precision = GameState().apply {
            equipment = equipment + Item(
                "precision_test", "Test précision", "talisman", "epic",
                "item/i-slaying.png", 1, effect = "precision"
            )
        }
        assertTrue(statsOf(precision).crit > base.crit)
    }

    @Test
    fun everyBiomeHasARule() {
        listOf(1, 11, 21, 31, 41, 51, 61, 71).forEach { floor ->
            val rule = com.brunothesatellite.idlesteprpg.engine.biomeRuleFor(floor)
            assertTrue("floor $floor label", rule.label.isNotBlank())
            assertTrue("floor $floor description", rule.description.isNotBlank())
            assertTrue("floor $floor hp", rule.enemyHpMult > 0.0)
            assertTrue("floor $floor atk", rule.enemyAtkMult > 0.0)
            assertTrue("floor $floor interval", rule.enemyIntervalMult > 0.0)
        }
    }

    @Test
    fun biomeRulesModifyHeroStats() {
        val forge = statsOf(GameState().apply { floor = 40 })
        val peaks = statsOf(GameState().apply { floor = 41 })
        val crypts = statsOf(GameState().apply { floor = 1 })
        val abyss = statsOf(GameState().apply { floor = 61 })
        assertTrue("peaks max hp", peaks.maxHp < forge.maxHp)
        assertTrue("abyss attack", abyss.atk > crypts.atk)
    }

    @Test
    fun bossesHaveDistinctAbilities() {
        assertEquals(
            "drain",
            com.brunothesatellite.idlesteprpg.engine.bossAbilityFor("Roi des Cryptes", 10).id
        )
        assertEquals(
            "regen",
            com.brunothesatellite.idlesteprpg.engine.bossAbilityFor("Hydre de Lerne", 20).id
        )
        assertEquals(
            "slam",
            com.brunothesatellite.idlesteprpg.engine.bossAbilityFor("Balrug", 30).id
        )
        assertEquals(
            "shield",
            com.brunothesatellite.idlesteprpg.engine.bossAbilityFor("Seigneur de la Forge", 40).id
        )
        assertEquals(
            "breath",
            com.brunothesatellite.idlesteprpg.engine.bossAbilityFor("Dragon de givre", 50).id
        )
        assertEquals(
            "roots",
            com.brunothesatellite.idlesteprpg.engine.bossAbilityFor("Tréant", 60).id
        )
        assertEquals(
            "acid",
            com.brunothesatellite.idlesteprpg.engine.bossAbilityFor("Vase mortelle", 70).id
        )
        assertEquals(
            "pressure",
            com.brunothesatellite.idlesteprpg.engine.bossAbilityFor("Asmodée", 80).id
        )
    }

    @Test
    fun codexHelpersListUniqueContent() {
        val mobs = allMobTemplates()
        val bosses = allBossTemplates()
        assertTrue(mobs.size >= 60)
        assertTrue(bosses.size >= 10)
        assertEquals(mobs.size, mobs.distinctBy { it.name }.size)
        assertEquals(bosses.size, bosses.distinctBy { it.name }.size)
        assertTrue(bosses.any { it.name == "Mère des Abysses" })
        assertTrue(bosses.any { it.name == "L'Âme Première" })
    }

    @Test
    fun setCollectionUsesEquipmentInventoryAndStash() {
        val state = GameState().apply {
            equipment = equipment + Item(
                "set_weapon", "Lame d'os", "weapon", "rare", "item/short_sword1.png", 1,
                setName = "os"
            )
            inventory = inventory + Item(
                "set_head", "Heaume d'os", "head", "rare", "item/helmet2.png", 1,
                setName = "os"
            )
            stash = listOf(
                Item("set_body", "Plate d'os", "body", "rare", "item/plate1.png", 1, setName = "os")
            )
        }
        val slots = com.brunothesatellite.idlesteprpg.engine.ownedSetSlots(state, "os")
        assertEquals(setOf("weapon", "head", "body"), slots)
    }

    @Test
    fun everyGameplayAssetExists() {
        val root = sequenceOf(File("src/main/assets"), File("app/src/main/assets"))
            .firstOrNull { it.isDirectory }
        assertTrue("assets directory not found", root != null)
        val paths = mutableSetOf<String>()
        BIOME_POOLS.values.forEach { pool ->
            pool.easy.forEach { paths += it.icon }
            pool.mid.forEach { paths += it.icon }
            pool.hard.forEach { paths += it.icon }
            pool.bosses.forEach { paths += it.icon }
        }
        LOOT_TEMPLATES.forEach { paths += it.icon }
        CONSUMABLES.forEach { paths += it.icon }
        paths += listOf(
            "hero/human_m.png", "hero/pants_brown.png", "hero/buckler_round.png",
            "hero/coat_red.png", "hero/bplate_metal1.png", "hero/aragorn.png",
            "hero/helm_green.png", "hero/helm_plume.png", "hero/horned.png",
            "hero/middle_brown2.png", "hero/middle_green.png",
            "hero/broadsword.png", "hero/long_sword_slant.png",
            "hero/weapon_axe.png", "hero/weapon_mace.png", "hero/weapon_staff.png",
            "hero/weapon_dagger.png", "hero/weapon_spear.png", "hero/weapon_whip.png",
            "hero/weapon_greatsword.png",
            "hero/body_leather.png", "hero/body_mail.png", "hero/body_mail_gray.png",
            "hero/body_plate.png", "hero/body_coat.png", "hero/body_robe.png",
            "hero/body_mesh_black.png", "hero/body_metal_blue.png",
            "hero/head_hood.png", "hero/head_hood_green.png", "hero/head_helm_gray.png",
            "hero/head_crown.png",
            "hero/legs_plate.png", "hero/legs_mail.png", "hero/legs_cloth.png",
            "hero/boots_metal.png", "hero/boots_leather.png"
        )
        listOf("crypts", "catacombs", "ash", "forge", "peaks", "woods", "abyss", "throne")
            .forEach { key ->
                val visuals = com.brunothesatellite.idlesteprpg.ui.biomeVisuals(key)
                paths += visuals.walls
                paths += visuals.floors
                paths += visuals.statues
                paths += visuals.props
            }
        listOf("closed", "runed", "sealed").forEach { style ->
            paths += listOf(
                "env/gate_${style}_left.png",
                "env/gate_${style}_middle.png",
                "env/gate_${style}_right.png"
            )
        }
        paths += listOf("fx/cloud_fire0.png")
        listOf("crypts", "catacombs", "ash", "forge", "peaks", "woods", "abyss", "throne")
            .forEach { biome ->
                paths += "music/${biome}_1.ogg"
                paths += "music/${biome}_2.ogg"
            }
        val missing = paths.filter { !File(root, it).isFile }
        assertTrue("missing assets: $missing", missing.isEmpty())
    }
}
