package com.roadtrippin.shared

import com.roadtrippin.shared.ui.HighwayBounds
import com.roadtrippin.shared.ui.HighwayClass
import com.roadtrippin.shared.ui.MaxMapScale
import com.roadtrippin.shared.ui.approximateMapMilesAcross
import com.roadtrippin.shared.ui.decodeHighwayVectors
import com.roadtrippin.shared.ui.nextMapScale
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HighwayVectorsTest {
    @Test
    fun binaryDecoderRestoresLevelsLabelsTilesAndDeltaCoordinates() {
        val bytes = buildList<Int> {
            addAll("RTHW".encodeToByteArray().map { it.toInt() and 0xFF })
            unsigned(2)
            unsigned(10_000)
            string("test source")
            unsigned(3)
            string("")
            string("I-90")
            string("WA 20")
            unsigned(2)

            unsigned(1)
            unsigned(5)
            unsigned(1)
            signed(12)
            signed(27)
            unsigned(1)
            unsigned(HighwayClass.INTERSTATE.ordinal)
            unsigned(1)
            unsigned(2)
            signed(-1_200_000)
            signed(470_000)
            signed(100)
            signed(-100)

            unsigned(20)
            unsigned(1)
            unsigned(1)
            signed(60)
            signed(137)
            unsigned(1)
            unsigned(HighwayClass.STATE_ROUTE.ordinal)
            unsigned(2)
            unsigned(2)
            signed(-1_199_000)
            signed(469_000)
            signed(200)
            signed(300)
        }.map(Int::toByte).toByteArray()

        val decoded = decodeHighwayVectors(bytes)

        assertEquals("test source", decoded.source)
        assertEquals(10_000, decoded.quantization)
        assertEquals(listOf("", "I-90", "WA 20"), decoded.labels)
        assertEquals(2, decoded.levels.size)
        val overview = decoded.visibleTiles(
            scale = 1f,
            bounds = HighwayBounds(-121.0, -119.0, 46.0, 48.0),
        ).single().roads.single()
        assertEquals(HighwayClass.INTERSTATE, overview.highwayClass)
        assertContentEquals(intArrayOf(-1_200_000, 470_000, -1_199_900, 469_900), overview.coordinates)
        val close = decoded.visibleTiles(
            scale = 32f,
            bounds = HighwayBounds(-121.0, -119.0, 46.0, 48.0),
        ).single().roads.single()
        assertEquals(HighwayClass.STATE_ROUTE, close.highwayClass)
        assertEquals("WA 20", decoded.labels[close.labelIndex])
    }

    @Test
    fun zoomStopsReachApproximatelyOneHundredMilesAcross() {
        assertEquals(2f, nextMapScale(1f, zoomIn = true))
        assertEquals(48f, nextMapScale(MaxMapScale, zoomIn = false))
        assertEquals(MaxMapScale, nextMapScale(48f, zoomIn = true))
        assertTrue(approximateMapMilesAcross(MaxMapScale) in 95..105)
    }

    private fun MutableList<Int>.string(value: String) {
        val bytes = value.encodeToByteArray()
        unsigned(bytes.size)
        addAll(bytes.map { it.toInt() and 0xFF })
    }

    private fun MutableList<Int>.signed(value: Int) {
        unsigned((value shl 1) xor (value shr 31))
    }

    private fun MutableList<Int>.unsigned(initial: Int) {
        var value = initial
        while (value and 0x7F.inv() != 0) {
            add((value and 0x7F) or 0x80)
            value = value ushr 7
        }
        add(value)
    }
}
