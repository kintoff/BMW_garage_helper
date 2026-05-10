package pl.garage.bmwassistant.ui.components

import pl.garage.bmwassistant.R
import pl.garage.bmwassistant.model.VehicleArea

fun VehicleArea.iconResource(): Int = when (this) {
    VehicleArea.Engine -> R.drawable.ic_area_engine
    VehicleArea.Body -> R.drawable.ic_area_body
    VehicleArea.Suspension -> R.drawable.ic_area_suspension
    VehicleArea.Electronics -> R.drawable.ic_area_electronics
    VehicleArea.Service -> R.drawable.ic_area_service
}
