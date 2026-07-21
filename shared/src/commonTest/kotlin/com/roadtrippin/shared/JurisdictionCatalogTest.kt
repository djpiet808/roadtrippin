package com.roadtrippin.shared

import com.roadtrippin.shared.domain.Country
import com.roadtrippin.shared.domain.JurisdictionCatalog
import com.roadtrippin.shared.domain.Region
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class JurisdictionCatalogTest {
    @Test
    fun catalogHasExactEntriesAndCountryOrder() {
        assertEquals(53, JurisdictionCatalog.all.size)
        assertEquals(51, JurisdictionCatalog.all.count { it.country == Country.UNITED_STATES })
        assertEquals(listOf(Country.UNITED_STATES, Country.CANADA, Country.MEXICO), JurisdictionCatalog.countrySections.map { it.first })
        assertEquals("British Columbia", JurisdictionCatalog.byCode.getValue("BC").name)
        assertEquals("Mexico", JurisdictionCatalog.byCode.getValue("MX").name)
        assertTrue("DC" in JurisdictionCatalog.byCode)
    }

    @Test
    fun sectionsAreAlphabeticalAndRegionsCoverAllUsEntriesOnce() {
        JurisdictionCatalog.countrySections.forEach { (_, entries) ->
            assertEquals(entries.map { it.name }.sorted(), entries.map { it.name })
        }
        val regionalUs = Region.entries.filterNot { it == Region.NON_US }.sumOf { region ->
            JurisdictionCatalog.all.count { it.region == region }
        }
        assertEquals(51, regionalUs)
        assertEquals(50, JurisdictionCatalog.usStates.size)
    }
}
