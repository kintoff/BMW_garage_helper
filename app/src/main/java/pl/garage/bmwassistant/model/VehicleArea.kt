package pl.garage.bmwassistant.model

enum class VehicleArea(
    val label: String,
    val description: String,
) {
    Engine(
        label = "Silnik",
        description = "Uklad napedowy, osprzet, dolot, paliwo i chlodzenie"
    ),
    Body(
        label = "Nadwozie",
        description = "Karoseria, korozja, szyby, drzwi i elementy zewnetrzne"
    ),
    Suspension(
        label = "Zawieszenie",
        description = "Wahacze, zwrotnice, amortyzatory, kola i geometria"
    ),
    Electronics(
        label = "Elektronika",
        description = "Moduly, bledy, czujniki, instalacja i diagnostyka"
    ),
    Service(
        label = "Serwis standardowy",
        description = "Oleje, filtry, plyny, przeglady i czynnosci okresowe"
    )
}
