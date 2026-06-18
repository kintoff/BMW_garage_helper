package pl.garage.bmwassistant

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.ui.screens.AddVehicleWizard
import pl.garage.bmwassistant.ui.screens.DeleteVehicleDialog
import pl.garage.bmwassistant.ui.screens.GarageDashboard
import pl.garage.bmwassistant.ui.screens.VehicleOverviewScreen
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun GarageApp() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val appContainer = remember(context) { context.appContainer }
    val garageRepository = appContainer.garageRepository
    val roomMigrator = appContainer.legacyStorageRoomMigrator
    val vehicles = remember { mutableStateListOf<Vehicle>() }
    var isAddingVehicle by rememberSaveable { mutableStateOf(false) }
    var selectedVehicleId by rememberSaveable { mutableStateOf<String?>(null) }
    var vehiclePendingDeletion by remember { mutableStateOf<Vehicle?>(null) }
    val selectedVehicle = selectedVehicleId?.let { vehicleId ->
        vehicles.firstOrNull { it.id == vehicleId }
    }

    LaunchedEffect(roomMigrator, garageRepository) {
        val loadedVehicles = withContext(Dispatchers.IO) {
            roomMigrator.migrateIfNeeded()
            garageRepository.loadVehicles()
        }
        vehicles.clear()
        vehicles.addAll(loadedVehicles)
    }

    BackHandler(enabled = selectedVehicle != null || isAddingVehicle) {
        when {
            selectedVehicle != null -> selectedVehicleId = null
            isAddingVehicle -> isAddingVehicle = false
        }
    }

    vehiclePendingDeletion?.let { vehicle ->
        DeleteVehicleDialog(
            vehicle = vehicle,
            onConfirm = {
                vehicles.remove(vehicle)
                vehiclePendingDeletion = null
                if (selectedVehicle?.id == vehicle.id) {
                    selectedVehicleId = null
                }
                coroutineScope.launch(Dispatchers.IO) {
                    garageRepository.deleteVehicle(vehicle.id)
                }
            },
            onDismiss = { vehiclePendingDeletion = null }
        )
    }

    when {
        selectedVehicle != null -> VehicleOverviewScreen(
            vehicle = selectedVehicle,
            onBack = { selectedVehicleId = null },
            onVehicleUpdated = { updatedVehicle ->
                coroutineScope.launch(Dispatchers.IO) {
                    val savedVehicle = garageRepository.saveVehicle(updatedVehicle)
                    withContext(Dispatchers.Main) {
                        val index = vehicles.indexOfFirst { it.id == savedVehicle.id }
                        if (index >= 0) {
                            vehicles[index] = savedVehicle
                        }
                        selectedVehicleId = savedVehicle.id
                    }
                }
            }
        )

        isAddingVehicle -> AddVehicleWizard(
            onVehicleCreated = { vehicle ->
                isAddingVehicle = false
                coroutineScope.launch(Dispatchers.IO) {
                    val savedVehicle = garageRepository.saveVehicle(vehicle)
                    withContext(Dispatchers.Main) {
                        vehicles.add(savedVehicle)
                    }
                }
            },
            onCancel = { isAddingVehicle = false }
        )

        else -> GarageDashboard(
            vehicles = vehicles,
            onAddVehicle = { isAddingVehicle = true },
            onOpenVehicle = { selectedVehicleId = it.id },
            onCopyVehicle = { vehicle ->
                coroutineScope.launch(Dispatchers.IO) {
                    val duplicatedVehicle = garageRepository.saveVehicle(vehicle.copyAsDuplicate())
                    withContext(Dispatchers.Main) {
                        vehicles.add(duplicatedVehicle)
                    }
                }
            },
            onDeleteVehicle = { vehiclePendingDeletion = it }
        )
    }
}

private fun Vehicle.copyAsDuplicate(): Vehicle = copy(
    id = "",
    model = "${model.ifBlank { "Auto" }} kopia",
    note = note.ifBlank { "Skopiowany profil auta." }
)
