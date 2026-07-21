package com.roadtrippin.shared.ui

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.roundToInt

internal const val MaxMapScale = 64f

internal enum class HighwayClass {
    INTERSTATE,
    US_ROUTE,
    STATE_ROUTE,
}

internal data class HighwayRoad(
    val highwayClass: HighwayClass,
    val labelIndex: Int,
    /** Alternating absolute longitude/latitude values at [HighwayVectorData.quantization]. */
    val coordinates: IntArray,
)

internal data class HighwayTile(
    val x: Int,
    val y: Int,
    val roads: List<HighwayRoad>,
)

internal data class HighwayLevel(
    val minimumScale: Float,
    val tileDegrees: Int,
    val tiles: Map<Long, HighwayTile>,
)

internal data class HighwayVectorData(
    val source: String,
    val quantization: Int,
    val labels: List<String>,
    val levels: List<HighwayLevel>,
) {
    fun levelForScale(scale: Float): HighwayLevel =
        levels.lastOrNull { scale >= it.minimumScale } ?: levels.first()

    fun visibleTiles(scale: Float, bounds: HighwayBounds): List<HighwayTile> {
        val level = levelForScale(scale)
        val minimumX = floor((bounds.west + 180.0) / level.tileDegrees).toInt()
        val maximumX = floor((bounds.east + 180.0) / level.tileDegrees).toInt()
        val minimumY = floor((bounds.south + 90.0) / level.tileDegrees).toInt()
        val maximumY = floor((bounds.north + 90.0) / level.tileDegrees).toInt()
        return buildList {
            for (y in minimumY..maximumY) {
                for (x in minimumX..maximumX) {
                    level.tiles[highwayTileKey(x, y)]?.let(::add)
                }
            }
        }
    }
}

internal data class HighwayBounds(
    val west: Double,
    val east: Double,
    val south: Double,
    val north: Double,
)

internal fun decodeHighwayVectors(bytes: ByteArray): HighwayVectorData {
    val reader = HighwayByteReader(bytes)
    require(reader.readByte() == 'R'.code)
    require(reader.readByte() == 'T'.code)
    require(reader.readByte() == 'H'.code)
    require(reader.readByte() == 'W'.code) { "Invalid highway vector magic" }
    require(reader.readUnsigned() == 2) { "Unsupported highway vector version" }
    val quantization = reader.readUnsigned()
    require(quantization > 0)
    val source = reader.readString()
    val labels = List(reader.readUnsigned()) { reader.readString() }
    val levels = List(reader.readUnsigned()) {
        val minimumScale = reader.readUnsigned().toFloat()
        val tileDegrees = reader.readUnsigned()
        require(tileDegrees > 0)
        val tiles = buildMap {
            repeat(reader.readUnsigned()) {
                val tileX = reader.readSigned()
                val tileY = reader.readSigned()
                val roads = List(reader.readUnsigned()) {
                    val highwayClass = HighwayClass.entries[reader.readUnsigned()]
                    val labelIndex = reader.readUnsigned()
                    require(labelIndex in labels.indices) { "Invalid highway label index" }
                    val pointCount = reader.readUnsigned()
                    require(pointCount >= 2) { "Highway line needs at least two points" }
                    val coordinates = IntArray(pointCount * 2)
                    var longitude = 0
                    var latitude = 0
                    repeat(pointCount) { pointIndex ->
                        longitude += reader.readSigned()
                        latitude += reader.readSigned()
                        coordinates[pointIndex * 2] = longitude
                        coordinates[pointIndex * 2 + 1] = latitude
                    }
                    HighwayRoad(highwayClass, labelIndex, coordinates)
                }
                val tile = HighwayTile(tileX, tileY, roads)
                put(highwayTileKey(tileX, tileY), tile)
            }
        }
        HighwayLevel(minimumScale, tileDegrees, tiles)
    }
    require(reader.finished) { "Trailing highway vector data" }
    require(levels.isNotEmpty()) { "Highway vectors contain no levels" }
    return HighwayVectorData(source, quantization, labels, levels)
}

internal fun approximateMapMilesAcross(scale: Float, centerLatitude: Double = 39.5): Int {
    val milesPerLongitudeDegree = 69.172 * cos(centerLatitude * PI / 180.0)
    return (120.0 * milesPerLongitudeDegree / scale.coerceAtLeast(1f)).roundToInt()
}

internal fun nextMapScale(current: Float, zoomIn: Boolean): Float {
    val stops = floatArrayOf(1f, 2f, 4f, 8f, 12f, 20f, 32f, 48f, MaxMapScale)
    return if (zoomIn) {
        stops.firstOrNull { it > current + 0.01f } ?: MaxMapScale
    } else {
        stops.lastOrNull { it < current - 0.01f } ?: 1f
    }
}

private fun highwayTileKey(x: Int, y: Int): Long =
    (x.toLong() shl 32) xor (y.toLong() and 0xFFFF_FFFFL)

private class HighwayByteReader(private val bytes: ByteArray) {
    private var index = 0

    val finished: Boolean get() = index == bytes.size

    fun readByte(): Int {
        require(index < bytes.size) { "Unexpected end of highway vector data" }
        return bytes[index++].toInt() and 0xFF
    }

    fun readUnsigned(): Int {
        var result = 0
        var shift = 0
        while (shift < 35) {
            val byte = readByte()
            result = result or ((byte and 0x7F) shl shift)
            if (byte and 0x80 == 0) return result
            shift += 7
        }
        error("Highway varint is too long")
    }

    fun readSigned(): Int {
        val encoded = readUnsigned()
        return (encoded ushr 1) xor -(encoded and 1)
    }

    fun readString(): String {
        val length = readUnsigned()
        require(length >= 0 && index + length <= bytes.size) { "Invalid highway string length" }
        return bytes.decodeToString(index, index + length).also { index += length }
    }
}
