package pl.garage.bmwassistant

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import pl.garage.bmwassistant.data.VehicleStorage
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.ui.screens.AddVehicleWizard
import pl.garage.bmwassistant.ui.screens.DeleteVehicleDialog
import pl.garage.bmwassistant.ui.screens.GarageDashboard
import pl.garage.bmwassistant.ui.screens.VehicleOverviewScreen

@Composable
fun GarageApp() {
    val context = LocalContext.current
    val vehicleStorage = remember { VehicleStorage(context.applicationContext) }
    val vehicles = remember {
        mutableStateListOf<Vehicle>().apply {
            addAll(vehicleStorage.loadVehicles())
        }
    }
    var isAddingVehicle by rememberSaveable { mutableStateOf(false) }
    var selectedVehicle by remember { mutableStateOf<Vehicle?>(null) }
    var vehiclePendingDeletion by remember { mutableStateOf<Vehicle?>(null) }

    BackHandler(enabled = selectedVehicle != null || isAddingVehicle) {
        when {
            selectedVehicle != null -> selectedVehicle = null
            isAddingVehicle -> isAddingVehicle = false
        }
    }

    vehiclePendingDeletion?.let { vehicle ->
        DeleteVehicleDialog(
            vehicle = vehicle,
            onConfirm = {
                vehicles.remove(vehicle)
                vehicleStorage.saveVehicles(vehicles)
                vehiclePendingDeletion = null
            },
            onDismiss = { vehiclePendingDeletion = null }
        )
    }

    when {
        selectedVehicle != null -> VehicleOverviewScreen(
            vehicle = selectedVehicle,
            onBack = { selectedVehicle = null },
            onVehicleUpdated = { updatedVehicle ->
                val index = vehicles.indexOfFirst { it.stableId() == updatedVehicle.stableId() }
                if (index >= 0) {
                    vehicles[index] = updatedVehicle
                }
                selectedVehicle = updatedVehicle
                vehicleStorage.saveVehicles(vehicles)
            }
        )

        isAddingVehicle -> AddVehicleWizard(
            onVehicleCreated = { vehicle ->
                vehicles.add(vehicle)
                vehicleStorage.saveVehicles(vehicles)
                isAddingVehicle = false
            },
            onCancel = { isAddingVehicle = false }
        )

        else -> GarageDashboard(
            vehicles = vehicles,
            onAddVehicle = { isAddingVehicle = true },
            onOpenVehicle = { selectedVehicle = it },
            onCopyVehicle = { vehicle ->
                vehicles.add(vehicle.copyAsDuplicate())
                vehicleStorage.saveVehicles(vehicles)
            },
            onDeleteVehicle = { vehiclePendingDeletion = it }
        )
    }
}

private fun Vehicle.copyAsDuplicate(): Vehicle = copy(
    id = "vehicle-${System.currentTimeMillis()}",
    model = "${model.ifBlank { "Auto" }} kopia",
    note = note.ifBlank { "Skopiowany profil auta." }
)

private fun Vehicle.stableId(): String =
    id.ifBlank { vin.ifBlank { displayName.ifBlank { "unknown_vehicle" } } }
