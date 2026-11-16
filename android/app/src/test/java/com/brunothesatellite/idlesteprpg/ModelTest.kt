package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.engine.BIOME_POOLS
import com.brunothesatellite.idlesteprpg.engine.CONSUMABLES
import com.brunothesatellite.idlesteprpg.engine.GameState
import com.brunothesatellite.idlesteprpg.engine.Item
import com.brunothesatellite.idlesteprpg.engine.LOOT_TEMPLATES
import com.brunothesatellite.idlesteprpg.engine.biomeKeyFor
import com.brunothesatellite.idlesteprpg.engine.lootTemplatesFor
import com.brunothesatellite.idlesteprpg.engine.bossFor
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
            "hero/broadsword.png", "hero/long_sword_slant.png"
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
        val missing = paths.filter { !File(root, it).isFile }
        assertTrue("missing assets: $missing", missing.isEmpty())
    }
}
