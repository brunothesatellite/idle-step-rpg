package com.brunothesatellite.idlesteprpg

import com.brunothesatellite.idlesteprpg.ui.splitFusedName
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentTextTest {

    @Test
    fun splitsRepeatedFusedSuffixes() {
        assertEquals("Hache gnoll" to 3, splitFusedName("Hache gnoll fusionné fusionné fusionné"))
        assertEquals("Coffret-reliquaire" to 1, splitFusedName("Coffret-reliquaire Fusionné"))
        assertEquals("Lame runique" to 0, splitFusedName("Lame runique"))
    }
}
