package com.roadtrippin.shared

import androidx.compose.ui.geometry.Offset
import com.roadtrippin.shared.ui.pointInStateRing
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapInteractionTest {
    private val square = listOf(
        listOf(-100.0, 30.0),
        listOf(-90.0, 30.0),
        listOf(-90.0, 40.0),
        listOf(-100.0, 40.0),
        listOf(-100.0, 30.0),
    )

    @Test
    fun projectedStateHitTestSeparatesInsideFromOutside() {
        assertTrue(pointInStateRing(Offset(750f, 400f), "TS", square, 1200f, 650f))
        assertFalse(pointInStateRing(Offset(1020f, 400f), "TS", square, 1200f, 650f))
    }
}
