package pl.garage.bmwassistant.ui.components

import androidx.annotation.DrawableRes
import pl.garage.bmwassistant.R
import pl.garage.bmwassistant.model.Vehicle

data class VehicleImageSet(
    @param:DrawableRes val selectionImage: Int,
    @param:DrawableRes val detailImage: Int,
)

fun Vehicle.selectionImageResource(): Int =
    imageSetForVehicle().selectionImage

fun Vehicle.detailImageResource(): Int =
    imageSetForVehicle().detailImage

private fun Vehicle.imageSetForVehicle(): VehicleImageSet {
    val key = listOf(brand, model, generation)
        .joinToString(" ")
        .uppercase()

    return when {
        "E61" in key -> VehicleImageSet(
            selectionImage = R.drawable.car_bmw_e61_selection,
            detailImage = R.drawable.car_bmw_e61_detail
        )

        "E60" in key -> VehicleImageSet(
            selectionImage = R.drawable.e60_bok,
            detailImage = R.drawable.e60_front
        )


        else -> VehicleImageSet(
            selectionImage = R.drawable.car_bmw_e61,
            detailImage = R.drawable.car_bmw_e61
        )
    }
}