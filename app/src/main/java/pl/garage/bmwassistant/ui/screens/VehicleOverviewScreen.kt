package pl.garage.bmwassistant.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.R
import pl.garage.bmwassistant.data.PartInventoryStorage
import pl.garage.bmwassistant.data.RepairProjectStorage
import pl.garage.bmwassistant.data.sampleConsumablesFor
import pl.garage.bmwassistant.data.sampleInventoryPartsFor
import pl.garage.bmwassistant.data.sampleRepairDocumentationFor
import pl.garage.bmwassistant.data.sampleRepairsFor
import pl.garage.bmwassistant.data.sampleShoppingListFor
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.SectionTitle
import pl.garage.bmwassistant.ui.theme.GarageTheme

@Composable
fun VehicleOverviewScreen(
    vehicle: Vehicle?,
    onBack: () -> Unit,
) {
    val currentVehicle = vehicle ?: return
    val context = LocalContext.current
    val repairStorage = remember { RepairProjectStorage(context.applicationContext) }
    val partStorage = remember { PartInventoryStorage(context.applicationContext) }
    var selectedModule by remember { mutableStateOf<VehicleModule?>(null) }
    var initialDocumentationRepairTitle by remember { mutableStateOf<String?>(null) }
    var initialRepairListRepairTitle by remember { mutableStateOf<String?>(null) }
    var shouldReturnFromDocumentationToRepairs by remember { mutableStateOf(false) }
    var repairProjects by remember(currentVehicle) {
        mutableStateOf(repairStorage.loadRepairs(currentVehicle).ifEmpty { sampleRepairsFor(currentVehicle) })
    }
    var repairDocumentation by remember(currentVehicle) {
        mutableStateOf(
            repairStorage.loadDocumentation(currentVehicle)
                .ifEmpty { sampleRepairDocumentationFor(currentVehicle) }
        )
    }

    fun updateRepairs(repairs: List<RepairProject>) {
        repairProjects = repairs
        repairStorage.saveRepairs(currentVehicle, repairs)
    }

    fun updateRepairDocumentation(documentation: List<RepairDocumentation>) {
        repairDocumentation = documentation
        repairStorage.saveDocumentation(currentVehicle, documentation)
    }

    BackHandler(enabled = selectedModule != null) {
        selectedModule = null
    }

    if (selectedModule?.type == VehicleModuleType.Status) {
        VehicleStatusScreen(
            vehicle = currentVehicle,
            activeRepairAreas = repairProjects.map { it.area }.toSet(),
            onBack = { selectedModule = null }
        )
        return
    }

    if (selectedModule?.type == VehicleModuleType.Repairs) {
        val inventoryParts = partStorage.loadParts(currentVehicle).ifEmpty {
            sampleInventoryPartsFor(currentVehicle)
        }
        VehicleRepairListScreen(
            vehicle = currentVehicle,
            repairs = repairProjects,
            repairDocumentation = repairDocumentation,
            inventoryParts = inventoryParts,
            initialRepairTitle = initialRepairListRepairTitle,
            onRepairAdded = { repair, documentation ->
                updateRepairs(repairProjects + repair)
                updateRepairDocumentation(repairDocumentation + documentation)
            },
            onOpenDocumentation = { documentation ->
                initialDocumentationRepairTitle = documentation.repairTitle
                initialRepairListRepairTitle = documentation.repairTitle
                shouldReturnFromDocumentationToRepairs = true
                selectedModule = vehicleModules.first { it.type == VehicleModuleType.Documentation }
            },
            onInitialRepairClosed = {
                initialRepairListRepairTitle = null
            },
            onBack = {
                initialRepairListRepairTitle = null
                selectedModule = null
            }
        )
        return
    }

    if (selectedModule?.type == VehicleModuleType.Documentation) {
        VehicleDocumentationScreen(
            vehicle = currentVehicle,
            repairDocumentation = repairDocumentation,
            initialRepairTitle = initialDocumentationRepairTitle,
            returnToPreviousModuleOnBack = shouldReturnFromDocumentationToRepairs,
            onDocumentationUpdated = { updatedDocumentation ->
                updateRepairDocumentation(
                    repairDocumentation.map { documentation ->
                        if (documentation.repairTitle == updatedDocumentation.repairTitle &&
                            documentation.area == updatedDocumentation.area
                        ) {
                            updatedDocumentation
                        } else {
                            documentation
                        }
                    }
                )
            },
            onBack = {
                initialDocumentationRepairTitle = null
                if (shouldReturnFromDocumentationToRepairs) {
                    shouldReturnFromDocumentationToRepairs = false
                    selectedModule = vehicleModules.first { it.type == VehicleModuleType.Repairs }
                } else {
                    initialRepairListRepairTitle = null
                    selectedModule = null
                }
            }
        )
        return
    }

    if (selectedModule?.type == VehicleModuleType.PartsStorage) {
        VehiclePartsStorageScreen(
            vehicle = currentVehicle,
            inventoryParts = sampleInventoryPartsFor(currentVehicle),
            shoppingList = sampleShoppingListFor(currentVehicle),
            consumables = sampleConsumablesFor(),
            onBack = { selectedModule = null }
        )
        return
    }

    selectedModule?.let { module ->
        ModulePlaceholderDialog(
            module = module,
            onDismiss = { selectedModule = null }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text("Wroc do garazu")
                }
            }

            item {
                Header(
                    title = currentVehicle.displayName.ifBlank { "BMW" },
                    subtitle = currentVehicle.technicalSummary.ifBlank { "Profil auta" }
                )
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Image(
                            painter = painterResource(R.drawable.car_bmw_e61),
                            contentDescription = currentVehicle.displayName.ifBlank { "BMW" },
                            modifier = Modifier
                                .weight(1f)
                                .height(92.dp),
                            contentScale = ContentScale.Fit
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Profil auta",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = currentVehicle.note.ifBlank { "Brak notatki startowej." },
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                                maxLines = 3
                            )
                        }
                    }
                }
            }

            item {
                SectionTitle("Moduly auta")
            }

            item {
                VehicleModuleGrid(
                    modules = vehicleModules,
                    onOpenModule = { selectedModule = it }
                )
            }
        }
    }
}

private data class VehicleModule(
    val type: VehicleModuleType,
    val title: String,
    val subtitle: String,
    val accentColor: Color,
)

private enum class VehicleModuleType {
    Status,
    Repairs,
    Documentation,
    PartsStorage
}

private val vehicleModules = listOf(
    VehicleModule(
        type = VehicleModuleType.Status,
        title = "Stan auta",
        subtitle = "Status techniczny, przebieg, aktywne problemy",
        accentColor = Color(0xFF7EC8E3)
    ),
    VehicleModule(
        type = VehicleModuleType.Repairs,
        title = "Lista napraw",
        subtitle = "Projekty napraw, checklisty i historia prac",
        accentColor = Color(0xFFE2C16B)
    ),
    VehicleModule(
        type = VehicleModuleType.Documentation,
        title = "Notatki / Dokumentacja",
        subtitle = "Zdjecia, PDF-y, linki, RealOEM i uwagi",
        accentColor = Color(0xFFB8A7FF)
    ),
    VehicleModule(
        type = VehicleModuleType.PartsStorage,
        title = "Magazyn czesci",
        subtitle = "Czesci na polce, zamowienia i zapasy",
        accentColor = Color(0xFF8FD6A8)
    )
)

@Composable
private fun VehicleModuleGrid(
    modules: List<VehicleModule>,
    onOpenModule: (VehicleModule) -> Unit,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxWidth()
            .height(316.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        userScrollEnabled = false
    ) {
        items(modules) { module ->
            VehicleModuleTile(
                module = module,
                onClick = { onOpenModule(module) }
            )
        }
    }
}

@Composable
private fun VehicleModuleTile(
    module: VehicleModule,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1.05f)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = module.accentColor,
                    shape = RoundedCornerShape(50)
                ) {}
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = module.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = module.subtitle,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
private fun ModulePlaceholderDialog(
    module: VehicleModule,
    onDismiss: () -> Unit,
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(module.title) },
        text = { Text("Ten widok przygotujemy w kolejnym kroku.") },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK")
            }
        }
    )
}

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun VehicleOverviewScreenPreview() {
    GarageTheme {
        VehicleOverviewScreen(
            vehicle = Vehicle(
                brand = "BMW",
                model = "E61 520d",
                generation = "E61",
                engine = "M47N2 2.0d",
                year = "2006",
                vin = "WBAXXXXXXXXXXXXXX",
                mileage = "285000",
                note = "Tylna zwrotnica lewa, zardzewiala sruba"
            ),
            onBack = {}
        )
    }
}
