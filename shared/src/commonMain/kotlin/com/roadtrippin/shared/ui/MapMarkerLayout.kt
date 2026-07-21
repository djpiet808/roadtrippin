package com.roadtrippin.shared.ui

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

internal enum class MapMarkerKind { JOURNAL, PLATE }

internal data class MapMarkerLayoutInput(
    val id: String,
    val kind: MapMarkerKind,
    val anchor: Offset,
    val preferredCenter: Offset,
    val collisionSize: Size,
)

internal data class MapMarkerPlacement(
    val marker: MapMarkerLayoutInput,
    val center: Offset,
    val bounds: Rect,
) {
    val isDisplaced: Boolean
        get() = (center - marker.preferredCenter).getDistance() > 1f
}

/**
 * Places map markers in stable rings around their preferred position. The original anchor is
 * retained so a displaced marker can draw a leader line to its exact map location.
 */
internal fun placeMapMarkers(
    markers: List<MapMarkerLayoutInput>,
    canvasSize: Size,
    gapPx: Float,
    ringStepPx: Float,
): List<MapMarkerPlacement> {
    if (canvasSize.width <= 0f || canvasSize.height <= 0f) return emptyList()
    val placements = mutableListOf<MapMarkerPlacement>()
    markers.forEach { marker ->
        val halfWidth = marker.collisionSize.width / 2f
        val halfHeight = marker.collisionSize.height / 2f
        var chosenCenter: Offset? = null
        var chosenBounds: Rect? = null

        for (ring in 0..24) {
            val slots = if (ring == 0) 1 else ring * 8
            val radius = ring * ringStepPx
            val phase = if (ring > 0 && ring % 2 == 0) PI / slots else 0.0
            for (slot in 0 until slots) {
                val angle = if (ring == 0) 0.0 else -PI / 2.0 + phase + 2.0 * PI * slot / slots
                val candidate = marker.preferredCenter + Offset(
                    x = (cos(angle) * radius).toFloat(),
                    y = (sin(angle) * radius).toFloat(),
                )
                val bounds = Rect(
                    left = candidate.x - halfWidth,
                    top = candidate.y - halfHeight,
                    right = candidate.x + halfWidth,
                    bottom = candidate.y + halfHeight,
                )
                val insideCanvas = bounds.left >= 1f && bounds.top >= 1f &&
                    bounds.right <= canvasSize.width - 1f && bounds.bottom <= canvasSize.height - 1f
                val collisionBounds = bounds.expandedBy(gapPx / 2f)
                if (insideCanvas && placements.none { it.bounds.expandedBy(gapPx / 2f).overlaps(collisionBounds) }) {
                    chosenCenter = candidate
                    chosenBounds = bounds
                    break
                }
            }
            if (chosenCenter != null) break
        }

        // Real trip marker counts fit comfortably in the ring search. Keep a bounded fallback for
        // malformed/imported datasets so rendering never fails even if the canvas is over capacity.
        val center = chosenCenter ?: Offset(
            marker.preferredCenter.x.coerceIn(halfWidth + 1f, canvasSize.width - halfWidth - 1f),
            marker.preferredCenter.y.coerceIn(halfHeight + 1f, canvasSize.height - halfHeight - 1f),
        )
        placements += MapMarkerPlacement(
            marker = marker,
            center = center,
            bounds = chosenBounds ?: Rect(
                center.x - halfWidth,
                center.y - halfHeight,
                center.x + halfWidth,
                center.y + halfHeight,
            ),
        )
    }
    return placements
}

private fun Rect.expandedBy(amount: Float): Rect = Rect(
    left = left - amount,
    top = top - amount,
    right = right + amount,
    bottom = bottom + amount,
)
