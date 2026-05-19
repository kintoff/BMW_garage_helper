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
import androidx.compose.foundation.layout.size
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
import pl.garage.bmwassistant.data.sampleRepairDocumentationFor
import pl.garage.bmwassistant.data.sampleRepairsFor
import pl.garage.bmwassistant.data.sampleShoppingListFor
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.AccentBlue
import pl.garage.bmwassistant.ui.components.AccentGreen
import pl.garage.bmwassistant.ui.components.AccentPurple
import pl.garage.bmwassistant.ui.components.AccentRed
import pl.garage.bmwassistant.ui.components.AccentYellow
import pl.garage.bmwassistant.ui.components.BottomNavBar
import pl.garage.bmwassistant.ui.components.GaragePanel
import pl.garage.bmwassistant.ui.components.MetricBlock
import pl.garage.bmwassistant.ui.components.SectionTitle
import pl.garage.bmwassistant.ui.components.StatusBadge
import pl.garage.bmwassistant.ui.theme.GarageTheme

@Composable
fun VehicleOverviewScreen(
    vehicle: Vehicle?,
    onBack: () -> Unit,
    onVehicleUpdated: (Vehicle) -> Unit = {},
) {
    val currentVehicle = vehicle ?: return
    val context = LocalContext.current
    val repairStorage = remember { RepairProjectStorage(context.applicationContext) }
    val partStorage = remember { PartInventoryStorage(context.applicationContext) }
    var selectedModule by remember { mutableStateOf<VehicleModule?>(null) }
    var isEditingVehicle by remember { mutableStateOf(false) }
    var initialDocumentationRepairTitle by remember { mutableStateOf<String?>(null) }
    var initialRepairListRepairTitle by remember { mutableStateOf<String?>(null) }
    var initialShoppingRepairTitle by remember { mutableStateOf<String?>(null) }
    var initialShoppingArea by remember { mutableStateOf<VehicleArea?>(null) }
    var isDocumentationDetailsOpen by remember { mutableStateOf(false) }
    var shouldReturnFromDocumentationToRepairs by remember { mutableStateOf(false) }
    var shouldReturnFromShoppingToRepairs by remember { mutableStateOf(false) }
    var repairProjects by remember(currentVehicle) {
        mutableStateOf(repairStorage.loadRepairs(currentVehicle).ifEmpty { sampleRepairsFor(currentVehicle) })
    }
    var repairDocumentation by remember(currentVehicle) {
        mutableStateOf(
            repairStorage.loadDocumentation(currentVehicle)
                .ifEmpty { sampleRepairDocumentationFor(currentVehicle) }
        )
    }
    var shoppingListItems by remember(currentVehicle) {
        mutableStateOf(
            if (partStorage.hasShoppingList(currentVehicle)) {
                partStorage.loadShoppingList(currentVehicle)
            } else {
                sampleShoppingListFor(currentVehicle)
            }
        )
    }
    var inventoryPartItems by remember(currentVehicle) {
        mutableStateOf(partStorage.loadParts(currentVehicle))
    }

    fun updateRepairs(repairs: List<RepairProject>) {
        repairProjects = repairs
        repairStorage.saveRepairs(currentVehicle, repairs)
    }

    fun updateRepairDocumentation(documentation: List<RepairDocumentation>) {
        repairDocumentation = documentation
        repairStorage.saveDocumentation(currentVehicle, documentation)
    }

    fun appendShoppingItems(items: List<ShoppingListItem>) {
        if (items.isEmpty()) return
        val updatedItems = shoppingListItems + items
        shoppingListItems = updatedItems
        partStorage.saveShoppingList(currentVehicle, updatedItems)
    }

    fun updateShoppingItems(items: List<ShoppingListItem>) {
        shoppingListItems = items
        partStorage.saveShoppingList(currentVehicle, items)
    }

    fun appendInventoryPart(part: PartInventoryItem) {
        val updatedParts = inventoryPartItems + part
        inventoryPartItems = updatedParts
        partStorage.saveParts(currentVehicle, updatedParts)
    }

    fun updateInventoryParts(parts: List<PartInventoryItem>) {
        inventoryPartItems = parts
        partStorage.saveParts(currentVehicle, parts)
    }

    fun refreshShoppingList() {
        shoppingListItems = if (partStorage.hasShoppingList(currentVehicle)) {
            partStorage.loadShoppingList(currentVehicle)
        } else {
            shoppingListItems
        }
        inventoryPartItems = partStorage.loadParts(currentVehicle)
    }

    BackHandler(enabled = selectedModule != null) {
        selectedModule = null
    }

    if (isEditingVehicle) {
        BackHandler {
            isEditingVehicle = false
        }
        AddVehicleWizard(
            initialVehicle = currentVehicle,
            title = "Edytuj profil auta",
            subtitle = "Uzupelnij VIN i dane auta. VIN wykorzystamy do pobierania schematow z czescidobmw.pl.",
            saveLabel = "Zapisz zmiany",
            onVehicleCreated = { updatedVehicle ->
                onVehicleUpdated(updatedVehicle)
                isEditingVehicle = false
            },
            onCancel = { isEditingVehicle = false }
        )
        return
    }

    if (selectedModule?.type == VehicleModuleType.Status) {
        VehicleStatusScreen(
            vehicle = currentVehicle,
            activeRepairAreas = repairProjects.filterNot { it.status.isFinishedStatus() }.map { it.area }.toSet(),
            onBack = { selectedModule = null }
        )
        return
    }

    if (selectedModule?.type == VehicleModuleType.Repairs) {
        VehicleRepairListScreen(
            vehicle = currentVehicle,
            repairs = repairProjects,
            repairDocumentation = repairDocumentation,
            inventoryParts = inventoryPartItems,
            shoppingList = shoppingListItems,
            initialRepairTitle = initialRepairListRepairTitle,
            onRepairAdded = { repair, documentation ->
                updateRepairs(repairProjects + repair)
                updateRepairDocumentation(repairDocumentation + documentation)
            },
            onRepairUpdated = { updatedRepair ->
                val previousRepair = repairProjects.firstOrNull { it.id == updatedRepair.id }
                val movedToArchive = previousRepair?.status?.isFinishedStatus() != true &&
                    updatedRepair.status.isFinishedStatus()
                updateRepairs(
                    repairProjects.map { repair ->
                        if (repair.id == updatedRepair.id) updatedRepair else repair
                    }
                )
                if (movedToArchive) {
                    val archivedShoppingList = shoppingListItems.filter { it.belongsToRepair(updatedRepair) } +
                        inventoryPartItems
                            .filter { it.belongsToRepair(updatedRepair) }
                            .map { it.toArchivedShoppingListItem(updatedRepair) }
                    updateRepairDocumentation(
                        repairDocumentation.map { documentation ->
                            if (documentation.belongsToRepair(updatedRepair)) {
                                documentation.copy(archivedShoppingList = archivedShoppingList)
                            } else {
                                documentation
                            }
                        }
                    )
                    updateShoppingItems(
                        shoppingListItems.filterNot { it.belongsToRepair(updatedRepair) }
                    )
                    updateInventoryParts(
                        inventoryPartItems.filterNot { it.belongsToRepair(updatedRepair) }
                    )
                }
            },
            onOpenDocumentation = { documentation ->
                initialDocumentationRepairTitle = documentation.repairTitle
                initialRepairListRepairTitle = documentation.repairTitle
                isDocumentationDetailsOpen = true
                shouldReturnFromDocumentationToRepairs = true
                selectedModule = vehicleModules.first { it.type == VehicleModuleType.Documentation }
            },
            onDocumentationUpdated = { updatedDocumentation ->
                updateRepairDocumentation(
                    repairDocumentation.map { documentation ->
                        if (documentation.belongsToRepair(updatedDocumentation)) updatedDocumentation else documentation
                    }
                )
            },
            onOpenShoppingList = { repair ->
                initialShoppingRepairTitle = repair.title
                initialShoppingArea = repair.area
                initialRepairListRepairTitle = repair.title
                shouldReturnFromShoppingToRepairs = true
                selectedModule = vehicleModules.first { it.type == VehicleModuleType.PartsStorage }
            },
            onAddShoppingItems = { items ->
                appendShoppingItems(items)
            },
            onShoppingListUpdated = { items ->
                updateShoppingItems(items)
            },
            onInventoryPartAdded = { part ->
                appendInventoryPart(part)
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
        if (!isDocumentationDetailsOpen) {
            val archivedShoppingItems = repairDocumentation.flatMap { it.archivedShoppingList }
            VehicleRepairListScreen(
                vehicle = currentVehicle,
                repairs = repairProjects,
                repairDocumentation = repairDocumentation,
                inventoryParts = emptyList(),
                shoppingList = archivedShoppingItems,
                initialRepairTitle = initialDocumentationRepairTitle,
                onRepairAdded = { _, _ -> },
                onRepairUpdated = { updatedRepair ->
                    updateRepairs(
                        repairProjects.map { repair ->
                            if (repair.id == updatedRepair.id) updatedRepair else repair
                        }
                    )
                },
                onOpenDocumentation = { documentation ->
                    initialDocumentationRepairTitle = documentation.repairTitle
                    isDocumentationDetailsOpen = true
                    selectedModule = vehicleModules.first { it.type == VehicleModuleType.Documentation }
                },
                onDocumentationUpdated = { updatedDocumentation ->
                    updateRepairDocumentation(
                        repairDocumentation.map { documentation ->
                            if (documentation.belongsToRepair(updatedDocumentation)) updatedDocumentation else documentation
                        }
                    )
                },
                onOpenShoppingList = { repair ->
                    initialShoppingRepairTitle = repair.title
                    initialShoppingArea = repair.area
                    selectedModule = vehicleModules.first { it.type == VehicleModuleType.PartsStorage }
                },
                onAddShoppingItems = {},
                onShoppingListUpdated = {},
                onInventoryPartAdded = {},
                onInitialRepairClosed = {
                    initialDocumentationRepairTitle = null
                },
                title = "Dokumenty",
                selectedBottomItem = "Dokumenty",
                showArchivedRepairs = true,
                showAddRepairButton = false,
                showGeneralDocumentationSection = true,
                emptyText = "Brak zakonczonych napraw w archiwum.",
                onBack = {
                    initialDocumentationRepairTitle = null
                    selectedModule = null
                }
            )
            return
        }
        VehicleDocumentationScreen(
            vehicle = currentVehicle,
            repairDocumentation = repairDocumentation,
            repairProjects = repairProjects,
            shoppingList = shoppingListItems,
            initialRepairTitle = initialDocumentationRepairTitle,
            returnToPreviousModuleOnBack = shouldReturnFromDocumentationToRepairs,
            onDocumentationUpdated = { updatedDocumentation ->
                updateRepairDocumentation(
                    repairDocumentation.map { documentation ->
                        if (documentation.repairId == updatedDocumentation.repairId ||
                            (
                                documentation.repairId.isBlank() &&
                                    documentation.repairTitle == updatedDocumentation.repairTitle &&
                                    documentation.area == updatedDocumentation.area
                                )
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
                isDocumentationDetailsOpen = false
                if (shouldReturnFromDocumentationToRepairs) {
                    shouldReturnFromDocumentationToRepairs = false
                    selectedModule = vehicleModules.first { it.type == VehicleModuleType.Repairs }
                } else {
                    initialRepairListRepairTitle = null
                    selectedModule = vehicleModules.first { it.type == VehicleModuleType.Documentation }
                }
            }
        )
        return
    }

    if (selectedModule?.type == VehicleModuleType.PartsStorage) {
        VehiclePartsStorageScreen(
            vehicle = currentVehicle,
            inventoryParts = inventoryPartItems,
            shoppingList = shoppingListItems,
            consumables = sampleConsumablesFor(),
            initialSection = if (initialShoppingRepairTitle == null) null else PartsStorageSection.Shopping,
            initialShoppingRepairTitle = initialShoppingRepairTitle,
            initialShoppingArea = initialShoppingArea,
            onInitialShoppingClosed = {
                initialShoppingRepairTitle = null
                initialShoppingArea = null
            },
            onInventoryUpdated = { parts ->
                inventoryPartItems = parts
            },
            onShoppingListUpdated = { items ->
                shoppingListItems = items
            },
            onBack = {
                refreshShoppingList()
                if (shouldReturnFromShoppingToRepairs) {
                    shouldReturnFromShoppingToRepairs = false
                    initialShoppingRepairTitle = null
                    initialShoppingArea = null
                    selectedModule = vehicleModules.first { it.type == VehicleModuleType.Repairs }
                } else {
                    initialShoppingRepairTitle = null
                    initialShoppingArea = null
                    selectedModule = null
                }
            }
        )
        return
    }

    selectedModule?.let { module ->
        ModulePlaceholderDialog(
            module = module,
            onDismiss = { selectedModule = null }
        )
    }

    val activeRepairs = repairProjects.filterNot { it.status.isFinishedStatus() }
    val partsToBuy = shoppingListItems.take(3)
    val bottomItems = listOf("Przeglad", "Naprawy", "Czesci", "Dokumenty", "Wiecej")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = onBack) {
                            Text("‹ Garaz")
                        }
                        TextButton(onClick = { isEditingVehicle = true }) {
                            Text("Edytuj")
                        }
                    }
                }

                item {
                    CarDashboardHeader(
                        vehicle = currentVehicle,
                        activeRepairs = activeRepairs.size,
                        partsToBuy = shoppingListItems.size
                    )
                }

                item { SectionTitle("Aktywne naprawy") }
                item {
                    GaragePanel {
                        if (activeRepairs.isEmpty()) {
                            Text(
                                text = "Brak aktywnych napraw.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                            )
                        } else {
                            activeRepairs.take(3).forEach { repair ->
                                DashboardRepairRow(
                                    repair = repair,
                                    onClick = {
                                        initialRepairListRepairTitle = repair.title
                                        selectedModule = vehicleModules.first { it.type == VehicleModuleType.Repairs }
                                    }
                                )
                            }
                            TextButton(
                                onClick = { selectedModule = vehicleModules.first { it.type == VehicleModuleType.Repairs } }
                            ) {
                                Text("Zobacz wszystkie")
                            }
                        }
                    }
                }

                item { SectionTitle("Do kupienia") }
                item {
                    GaragePanel {
                        if (partsToBuy.isEmpty()) {
                            Text(
                                text = "Lista zakupow jest pusta.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                            )
                        } else {
                            partsToBuy.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.name,
                                        modifier = Modifier.weight(1f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${item.quantity} szt.",
                                        color = AccentYellow,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                item { SectionTitle("Szybkie akcje") }
                item {
                    QuickActionsGrid(
                        onOpenRepairs = { selectedModule = vehicleModules.first { it.type == VehicleModuleType.Repairs } },
                        onOpenParts = { selectedModule = vehicleModules.first { it.type == VehicleModuleType.PartsStorage } },
                        onOpenDocs = { selectedModule = vehicleModules.first { it.type == VehicleModuleType.Documentation } }
                    )
                }
            }
            BottomNavBar(
                items = bottomItems,
                selectedItem = "Przeglad",
                onSelect = { item ->
                    selectedModule = when (item) {
                        "Naprawy" -> vehicleModules.first { it.type == VehicleModuleType.Repairs }
                        "Czesci" -> vehicleModules.first { it.type == VehicleModuleType.PartsStorage }
                        "Dokumenty" -> vehicleModules.first { it.type == VehicleModuleType.Documentation }
                        "Wiecej" -> vehicleModules.first { it.type == VehicleModuleType.Status }
                        else -> null
                    }
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun CarDashboardHeader(
    vehicle: Vehicle,
    activeRepairs: Int,
    partsToBuy: Int,
) {
    GaragePanel {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = vehicle.displayName.ifBlank { "BMW" },
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = vehicle.technicalSummary.ifBlank { "Profil auta" },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
        Image(
            painter = painterResource(R.drawable.car_bmw_e61),
            contentDescription = vehicle.displayName.ifBlank { "BMW" },
            modifier = Modifier
                .fillMaxWidth()
                .height(128.dp),
            contentScale = ContentScale.Fit
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MetricBlock("$activeRepairs", "Aktywne\nnaprawy", AccentYellow, Modifier.weight(1f))
            MetricBlock("$partsToBuy", "Czesci do\nkupienia", AccentBlue, Modifier.weight(1f))
            MetricBlock("1", "Problem do\nsprawdzenia", AccentRed, Modifier.weight(1f))
        }
    }
}

@Composable
private fun DashboardRepairRow(
    repair: RepairProject,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(28.dp),
            color = repair.areaColor().copy(alpha = 0.16f),
            shape = RoundedCornerShape(8.dp)
        ) {}
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(repair.title, fontWeight = FontWeight.SemiBold)
            Text(
                text = repair.area.label,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp
            )
        }
        StatusBadge(repair.status.normalizedStatusLabel())
        Text("›", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 22.sp)
    }
}

@Composable
private fun QuickActionsGrid(
    onOpenRepairs: () -> Unit,
    onOpenParts: () -> Unit,
    onOpenDocs: () -> Unit,
) {
    val actions = listOf(
        Triple("Dodaj\nnaprawe", AccentYellow, onOpenRepairs),
        Triple("Dodaj\nczesc", AccentBlue, onOpenParts),
        Triple("Dodaj\nnotatke", AccentGreen, onOpenDocs),
        Triple("Dodaj\nzdjecie", AccentPurple, onOpenDocs)
    )
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        userScrollEnabled = false
    ) {
        items(actions) { action ->
            GaragePanel(onClick = action.third) {
                Text(
                    text = "+",
                    color = action.second,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = action.first,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f),
                    fontSize = 11.sp,
                    lineHeight = 13.sp
                )
            }
        }
    }
}

private fun RepairProject.areaColor(): Color = when (area) {
    VehicleArea.Engine -> AccentYellow
    VehicleArea.Suspension -> AccentBlue
    VehicleArea.Electronics -> AccentPurple
    VehicleArea.Body -> AccentGreen
    VehicleArea.Service -> AccentGreen
}

private fun String.isFinishedStatus(): Boolean = lowercase().contains("zakon")

private fun String.normalizedStatusLabel(): String = when {
    lowercase().contains("zakon") -> "Zakonczona"
    lowercase().contains("plan") -> "Planowana"
    lowercase().contains("trak") -> "W trakcie"
    else -> this
}

private fun RepairDocumentation.belongsToRepair(repair: RepairProject): Boolean =
    repairId == repair.id || (repairId.isBlank() && repairTitle == repair.title && area == repair.area)

private fun RepairDocumentation.belongsToRepair(updatedDocumentation: RepairDocumentation): Boolean =
    repairId == updatedDocumentation.repairId ||
        (
            repairId.isBlank() &&
                repairTitle == updatedDocumentation.repairTitle &&
                area == updatedDocumentation.area
            )

private fun ShoppingListItem.belongsToRepair(repair: RepairProject): Boolean =
    repairId == repair.id || (repairId.isBlank() && repairTitle == repair.title && area == repair.area)

private fun PartInventoryItem.belongsToRepair(repair: RepairProject): Boolean =
    repairId == repair.id || (repairId.isNullOrBlank() && repairTitle == repair.title)

private fun PartInventoryItem.toArchivedShoppingListItem(repair: RepairProject): ShoppingListItem =
    ShoppingListItem(
        id = id.ifBlank { "archived_${repair.id}_${partNumber}_${name}" },
        partNumber = oemPartNumber,
        manufacturerPartNumber = manufacturerPartNumber,
        name = name,
        manufacturer = manufacturer,
        repairTitle = repair.title,
        repairId = repair.id,
        area = repair.area,
        quantity = quantity,
        source = "Magazyn",
        price = purchasePrice,
        imageUri = photoUri,
        realOemUrl = realOemUrl
    )

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
        subtitle = "Zdjecia, PDF-y, linki, schematy i uwagi",
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
