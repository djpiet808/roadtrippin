package com.roadtrippin.shared

import com.roadtrippin.shared.data.RoadtrippinStore
import com.roadtrippin.shared.cloud.winningSide
import com.roadtrippin.shared.domain.AppSettings
import com.roadtrippin.shared.domain.CheerStyle
import com.roadtrippin.shared.domain.JournalEntryKind
import com.roadtrippin.shared.domain.JournalPhoto
import com.roadtrippin.shared.domain.LocationStamp
import com.roadtrippin.shared.domain.TagQuantity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RoadtrippinStoreTest {
    @Test
    fun cheerAuditionOffersTenStylesWithAnEnthusiasticDefault() {
        assertEquals(10, CheerStyle.entries.size)
        assertEquals(CheerStyle.ROADTRIP_RALLY, AppSettings().cheerStyle)
    }

    @Test
    fun quickStartCreatesOneActiveUnnamedTripWithoutLocation() {
        val store = RoadtrippinStore(repository = null)
        store.startTrip(null)
        assertEquals(1, store.trips.size)
        assertEquals("Road trip", store.activeTrip?.displayName)
        assertFalse(store.activeTrip!!.startLocation.hasCoordinates)
    }

    @Test
    fun firstSightingIsIdempotentAndCanBeCorrectedOrRemoved() {
        val store = RoadtrippinStore(repository = null)
        store.startTrip(null)
        assertTrue(store.recordSighting("OR", null))
        assertFalse(store.recordSighting("OR", LocationStamp(1.0, 2.0, "Wrong")))
        assertEquals(1, store.activeTrip!!.sightings.size)
        store.updateSighting("OR", 1234L, LocationStamp(45.5, -122.6, "Portland, OR"))
        assertEquals("Portland, OR", store.activeTrip!!.sightings.single().location.placeName)
        store.removeSighting("OR")
        assertTrue(store.activeTrip!!.sightings.isEmpty())
    }

    @Test
    fun journalEnforcesPositiveTagsAndFivePhotoMaximum() {
        val store = RoadtrippinStore(repository = null)
        store.startTrip(null)
        store.addJournalEntry(
            JournalEntryKind.STOP,
            "Lunch",
            "Sandwiches",
            listOf(TagQuantity("hawk", 10), TagQuantity("invalid", 0)),
            null,
        )
        val entry = store.activeTrip!!.journal.single()
        assertEquals(listOf(TagQuantity("hawk", 10)), entry.tags)
        val photos = (1..7).map { JournalPhoto("photo-$it", "/tmp/$it.jpg") }
        assertEquals(5, store.addJournalPhotos(entry.id, photos))
        assertEquals(5, store.activeTrip!!.journal.single().photos.size)
        assertTrue(store.achievementsFor(store.activeTrip!!).first { it.id == "tag_10" }.earned)
    }

    @Test
    fun lifecycleKeepsHistoryAndOnlyOneActiveTrip() {
        val store = RoadtrippinStore(repository = null)
        store.startTrip(null)
        val first = store.activeTrip!!.id
        store.endTrip(null)
        assertNull(store.activeTrip)
        store.startTrip(null)
        store.reopenTrip(first)
        assertEquals(first, store.activeTrip!!.id)
        assertEquals(1, store.trips.count { it.isActive })
    }

    @Test
    fun achievementsAreRevokedWhenQualifyingDataIsRemoved() {
        val store = RoadtrippinStore(repository = null)
        store.startTrip(null)
        store.recordSighting("DC", null)
        assertTrue(store.achievementsFor(store.activeTrip!!).first { it.id == "dc" }.earned)
        store.removeSighting("DC")
        assertFalse(store.achievementsFor(store.activeTrip!!).first { it.id == "dc" }.earned)
    }

    @Test
    fun localMutationsQueueAndDeletionsRetainTombstones() {
        val store = RoadtrippinStore(repository = null)
        store.startTrip(null)
        store.recordSighting("OR", null)
        assertEquals(2, store.pendingSyncCount)
        store.removeSighting("OR")
        assertEquals(3, store.pendingSyncCount)
        assertEquals(1, store.retainedDeletionCount)
    }

    @Test
    fun conflictResolverUsesModifiedTimeThenMutationId() {
        assertTrue(winningSide(100L to "z", 101L to "a") > 0)
        assertTrue(winningSide(101L to "a", 100L to "z") < 0)
        assertTrue(winningSide(100L to "a", 100L to "b") > 0)
        assertEquals(0, winningSide(100L to "same", 100L to "same"))
    }
}
