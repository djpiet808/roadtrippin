package com.roadtrippin.shared.domain

object JurisdictionCatalog {
    val all: List<Jurisdiction> = listOf(
        us("AL", "Alabama", Region.DEEP_SOUTH),
        us("AK", "Alaska", Region.WEST),
        us("AZ", "Arizona", Region.WEST),
        us("AR", "Arkansas", Region.SOUTH),
        us("CA", "California", Region.WEST),
        us("CO", "Colorado", Region.WEST),
        us("CT", "Connecticut", Region.NORTHEAST),
        us("DE", "Delaware", Region.NORTHEAST),
        us("DC", "District of Columbia", Region.NORTHEAST),
        us("FL", "Florida", Region.DEEP_SOUTH),
        us("GA", "Georgia", Region.DEEP_SOUTH),
        us("HI", "Hawaii", Region.WEST),
        us("ID", "Idaho", Region.WEST),
        us("IL", "Illinois", Region.MIDWEST),
        us("IN", "Indiana", Region.MIDWEST),
        us("IA", "Iowa", Region.MIDWEST),
        us("KS", "Kansas", Region.MIDWEST),
        us("KY", "Kentucky", Region.SOUTH),
        us("LA", "Louisiana", Region.DEEP_SOUTH),
        us("ME", "Maine", Region.NORTHEAST),
        us("MD", "Maryland", Region.NORTHEAST),
        us("MA", "Massachusetts", Region.NORTHEAST),
        us("MI", "Michigan", Region.MIDWEST),
        us("MN", "Minnesota", Region.MIDWEST),
        us("MS", "Mississippi", Region.DEEP_SOUTH),
        us("MO", "Missouri", Region.MIDWEST),
        us("MT", "Montana", Region.WEST),
        us("NE", "Nebraska", Region.MIDWEST),
        us("NV", "Nevada", Region.WEST),
        us("NH", "New Hampshire", Region.NORTHEAST),
        us("NJ", "New Jersey", Region.NORTHEAST),
        us("NM", "New Mexico", Region.WEST),
        us("NY", "New York", Region.NORTHEAST),
        us("NC", "North Carolina", Region.SOUTH),
        us("ND", "North Dakota", Region.MIDWEST),
        us("OH", "Ohio", Region.MIDWEST),
        us("OK", "Oklahoma", Region.SOUTH),
        us("OR", "Oregon", Region.WEST),
        us("PA", "Pennsylvania", Region.NORTHEAST),
        us("RI", "Rhode Island", Region.NORTHEAST),
        us("SC", "South Carolina", Region.DEEP_SOUTH),
        us("SD", "South Dakota", Region.MIDWEST),
        us("TN", "Tennessee", Region.SOUTH),
        us("TX", "Texas", Region.SOUTH),
        us("UT", "Utah", Region.WEST),
        us("VT", "Vermont", Region.NORTHEAST),
        us("VA", "Virginia", Region.SOUTH),
        us("WA", "Washington", Region.WEST),
        us("WV", "West Virginia", Region.SOUTH),
        us("WI", "Wisconsin", Region.MIDWEST),
        us("WY", "Wyoming", Region.WEST),
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
