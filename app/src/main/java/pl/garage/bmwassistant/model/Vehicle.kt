package pl.garage.bmwassistant.model

data class Vehicle(
    val brand: String,
    val model: String,
    val generation: String,
    val engine: String,
    val year: String,
    val vin: String,
    val mileage: String,
    val note: String,
    val id: String = "",
    val partsCatalogUrl: String = "",
) {
    val displayName: String
        get() = listOf(brand, model, generation)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    val technicalSummary: String
        get() = listOf(engine, yearLabel, mileageLabel)
            .filter { it.isNotBlank() }
            .joinToString(" / ")

    private val yearLabel: String
        get() = if (year.isBlank()) "" else "Rok $year"

    private val mileageLabel: String
        get() = if (mileage.isBlank()) "" else "$mileage km"
}
