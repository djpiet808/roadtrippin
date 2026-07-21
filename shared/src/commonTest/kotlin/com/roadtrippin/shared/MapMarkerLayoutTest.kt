package com.roadtrippin.shared

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import com.roadtrippin.shared.ui.MapMarkerKind
import com.roadtrippin.shared.ui.MapMarkerLayoutInput
import com.roadtrippin.shared.ui.placeMapMarkers
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MapMarkerLayoutTest {
    @Test
    fun overlappingPlateAndJournalMarkersFanOutWithoutOverlap() {
        val anchor = Offset(300f, 220f)
        val markers = buildList {
            add(
                MapMarkerLayoutInput(
                    id = "journal",
                    kind = MapMarkerKind.JOURNAL,
                    anchor = anchor,
                    preferredCenter = anchor,
                    collisionSize = Size(30f, 30f),
                )
            )
            repeat(13) { index ->
                add(
                    MapMarkerLayoutInput(
                        id = "plate-$index",
                        kind = MapMarkerKind.PLATE,
                        anchor = anchor,
                        preferredCenter = anchor - Offset(0f, 14f),
                        collisionSize = Size(36f, 28f),
                    )
                )
            }
        }

        val placements = placeMapMarkers(
            markers = markers,
            canvasSize = Size(600f, 440f),
            gapPx = 6f,
            ringStepPx = 44f,
        )

        assertEquals(markers.size, placements.size)
        assertEquals(anchor, placements.first().center)
        assertTrue(placements.drop(1).any { it.isDisplaced })
        placements.forEach { assertEquals(anchor, it.marker.anchor) }
        placements.forEachIndexed { index, placement ->
            placements.drop(index + 1).forEach { other ->
                assertFalse(placement.bounds.overlaps(other.bounds))
            }
        }
    }
}
