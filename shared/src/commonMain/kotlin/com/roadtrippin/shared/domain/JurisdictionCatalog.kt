package com.roadtrippin.shared.domain

object JurisdictionCatalog {
    val all: List<Jurisdiction> = listOf(
        us("AL", "Alabama", Region.SOUTHEAST),
        us("AK", "Alaska", Region.PACIFIC),
        us("AZ", "Arizona", Region.SOUTHWEST),
        us("AR", "Arkansas", Region.SOUTHEAST),
        us("CA", "California", Region.PACIFIC),
        us("CO", "Colorado", Region.MOUNTAIN_WEST),
        us("CT", "Connecticut", Region.NEW_ENGLAND),
        us("DE", "Delaware", Region.MID_ATLANTIC),
        us("DC", "District of Columbia", Region.MID_ATLANTIC),
        us("FL", "Florida", Region.SOUTHEAST),
        us("GA", "Georgia", Region.SOUTHEAST),
        us("HI", "Hawaii", Region.PACIFIC),
        us("ID", "Idaho", Region.PACIFIC_NORTHWEST),
        us("IL", "Illinois", Region.MIDWEST),
        us("IN", "Indiana", Region.MIDWEST),
        us("IA", "Iowa", Region.MIDWEST),
        us("KS", "Kansas", Region.GREAT_PLAINS),
        us("KY", "Kentucky", Region.SOUTHEAST),
        us("LA", "Louisiana", Region.SOUTHEAST),
        us("ME", "Maine", Region.NEW_ENGLAND),
        us("MD", "Maryland", Region.MID_ATLANTIC),
        us("MA", "Massachusetts", Region.NEW_ENGLAND),
        us("MI", "Michigan", Region.MIDWEST),
        us("MN", "Minnesota", Region.MIDWEST),
        us("MS", "Mississippi", Region.SOUTHEAST),
        us("MO", "Missouri", Region.MIDWEST),
        us("MT", "Montana", Region.MOUNTAIN_WEST),
        us("NE", "Nebraska", Region.GREAT_PLAINS),
        us("NV", "Nevada", Region.SOUTHWEST),
        us("NH", "New Hampshire", Region.NEW_ENGLAND),
        us("NJ", "New Jersey", Region.MID_ATLANTIC),
        us("NM", "New Mexico", Region.SOUTHWEST),
        us("NY", "New York", Region.MID_ATLANTIC),
        us("NC", "North Carolina", Region.SOUTHEAST),
        us("ND", "North Dakota", Region.GREAT_PLAINS),
        us("OH", "Ohio", Region.MIDWEST),
        us("OK", "Oklahoma", Region.SOUTHWEST),
        us("OR", "Oregon", Region.PACIFIC_NORTHWEST),
        us("PA", "Pennsylvania", Region.MID_ATLANTIC),
        us("RI", "Rhode Island", Region.NEW_ENGLAND),
        us("SC", "South Carolina", Region.SOUTHEAST),
        us("SD", "South Dakota", Region.GREAT_PLAINS),
        us("TN", "Tennessee", Region.SOUTHEAST),
        us("TX", "Texas", Region.SOUTHWEST),
        us("UT", "Utah", Region.MOUNTAIN_WEST),
        us("VT", "Vermont", Region.NEW_ENGLAND),
        us("VA", "Virginia", Region.MID_ATLANTIC),
        us("WA", "Washington", Region.PACIFIC_NORTHWEST),
        us("WV", "West Virginia", Region.MID_ATLANTIC),
        us("WI", "Wisconsin", Region.MIDWEST),
        us("WY", "Wyoming", Region.MOUNTAIN_WEST),
        Jurisdiction("BC", "British Columbia", Country.CANADA, Region.NON_US),
        Jurisdiction("MX", "Mexico", Country.MEXICO, Region.NON_US),
    )

    val byCode: Map<String, Jurisdiction> = all.associateBy(Jurisdiction::code)
    val usStates: List<Jurisdiction> = all.filter { it.country == Country.UNITED_STATES && it.code != "DC" }
    val countrySections: List<Pair<Country, List<Jurisdiction>>> = Country.entries.map { country ->
        country to all.filter { it.country == country }.sortedBy { it.name }
    }

    private fun us(code: String, name: String, region: Region) =
        Jurisdiction(code, name, Country.UNITED_STATES, region)
}

