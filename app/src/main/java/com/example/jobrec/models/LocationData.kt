package com.example.jobrec.models

object LocationData {
    
    val provinces = listOf(
        "Eastern Cape",
        "Free State", 
        "Gauteng",
        "KwaZulu-Natal",
        "Limpopo",
        "Mpumalanga",
        "Northern Cape",
        "North West",
        "Western Cape"
    )
    
    val provinceCityMap = mapOf(
        "Eastern Cape" to listOf(
            "Port Elizabeth",
            "East London",
            "Uitenhage",
            "Queenstown",
            "King William's Town",
            "Mdantsane",
            "Bhisho",
            "Grahamstown",
            "Alice",
            "Fort Beaufort",
            "Cradock",
            "Somerset East",
            "Graaff-Reinet",
            "Jeffreys Bay",
            "Humansdorp",
            "Stutterheim"
        ),
        
        "Free State" to listOf(
            "Bloemfontein",
            "Welkom",
            "Kroonstad",
            "Bethlehem",
            "Sasolburg",
            "Phuthaditjhaba",
            "Virginia",
            "Odendaalsrus",
            "Bothaville",
            "Harrismith",
            "Heilbron",
            "Parys",
            "Vredefort",
            "Winburg",
            "Senekal"
        ),
        
        "Gauteng" to listOf(
            "Johannesburg",
            "Pretoria",
            "Soweto",
            "Benoni",
            "Tembisa",
            "Germiston",
            "Boksburg",
            "Roodepoort",
            "Randburg",
            "Sandton",
            "Vanderbijlpark",
            "Kempton Park",
            "Alberton",
            "Centurion",
            "Springs",
            "Brakpan",
            "Krugersdorp",
            "Midrand",
            "Vereeniging",
            "Carletonville",
            "Westonaria",
            "Randfontein",
            "Heidelberg",
            "Edenvale",
            "Nigel"
        ),
        
        "KwaZulu-Natal" to listOf(
            "Durban",
            "Pietermaritzburg",
            "Pinetown",
            "Chatsworth",
            "Umlazi",
            "Port Shepstone",
            "Newcastle",
            "Dundee",
            "Ladysmith",
            "Kokstad",
            "Estcourt",
            "Margate",
            "Richards Bay",
            "Empangeni",
            "Stanger",
            "Eshowe",
            "Vryheid",
            "Ulundi",
            "Greytown",
            "Howick"
        ),
        
        "Limpopo" to listOf(
            "Polokwane",
            "Seshego",
            "Lebowakgomo",
            "Phalaborwa",
            "Tzaneen",
            "Giyani",
            "Thohoyandou",
            "Louis Trichardt",
            "Musina",
            "Mokopane",
            "Bela-Bela",
            "Modimolle",
            "Thabazimbi",
            "Lephalale",
            "Hoedspruit"
        ),
        
        "Mpumalanga" to listOf(
            "Nelspruit",
            "Witbank",
            "Middelburg",
            "Secunda",
            "Standerton",
            "Ermelo",
            "Bethal",
            "Barberton",
            "White River",
            "Sabie",
            "Hazyview",
            "Lydenburg",
            "Piet Retief",
            "Carolina",
            "Volksrust"
        ),
        
        "Northern Cape" to listOf(
            "Kimberley",
            "Upington",
            "Kuruman",
            "Port Nolloth",
            "De Aar",
            "Springbok",
            "Alexander Bay",
            "Postmasburg",
            "Calvinia",
            "Prieska",
            "Carnarvon",
            "Britstown",
            "Colesberg",
            "Hanover",
            "Richmond"
        ),
        
        "North West" to listOf(
            "Rustenburg",
            "Klerksdorp",
            "Potchefstroom",
            "Mmabatho",
            "Brits",
            "Stilfontein",
            "Orkney",
            "Lichtenburg",
            "Zeerust",
            "Koster",
            "Vryburg",
            "Schweizer-Reneke",
            "Wolmaransstad",
            "Christiana",
            "Sannieshof"
        ),
        
        "Western Cape" to listOf(
            "Cape Town",
            "Bellville",
            "Mitchells Plain",
            "Khayelitsha",
            "Somerset West",
            "George",
            "Wynberg",
            "Stellenbosch",
            "Paarl",
            "Mossel Bay",
            "Worcester",
            "Hermanus",
            "Strand",
            "Goodwood",
            "Kraaifontein",
            "Kuils River",
            "Malmesbury",
            "Swellendam",
            "Oudtshoorn",
            "Wellington",
            "Caledon",
            "Grabouw",
            "Robertson",
            "Montagu",
            "Bredasdorp"
        )
    )
    
    fun getCitiesForProvince(province: String): List<String> {
        return provinceCityMap[province] ?: emptyList()
    }
    
    fun getProvinceForCity(city: String): String? {
        return provinceCityMap.entries.find { (_, cities) ->
            cities.any { it.equals(city, ignoreCase = true) }
        }?.key
    }
    
    fun isValidProvinceCity(province: String, city: String): Boolean {
        val cities = getCitiesForProvince(province)
        return cities.any { it.equals(city, ignoreCase = true) }
    }
    
    fun getAllCities(): List<String> {
        return provinceCityMap.values.flatten().sorted()
    }
}
