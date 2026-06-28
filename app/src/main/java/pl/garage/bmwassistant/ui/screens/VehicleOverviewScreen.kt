package pl.garage.bmwassistant.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import pl.garage.bmwassistant.appContainer
import pl.garage.bmwassistant.data.ImportedRepairArchive
import pl.garage.bmwassistant.data.sampleConsumablesFor
import pl.garage.bmwassistant.database.repository.VehicleDataSnapshot
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.isFinishedRepairStatus
import pl.garage.bmwassistant.model.normalizedRepairStatusLabel
import pl.garage.bmwassistant.feature.inventory.components.PartsStorageSection
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.AccentBlue
import pl.garage.bmwassistant.ui.components.AccentGreen
import pl.garage.bmwassistant.ui.components.AccentPurple
import pl.garage.bmwassistant.ui.components.AccentRed
import pl.garage.bmwassistant.ui.components.AccentYellow
import pl.garage.bmwassistant.ui.components.BottomNavBar
import pl.garage.bmwassistant.ui.components.GarageTextField
import pl.garage.bmwassistant.ui.components.GaragePanel
import pl.garage.bmwassistant.ui.components.MetricBlock
import pl.garage.bmwassistant.ui.components.SectionTitle
import pl.garage.bmwassistant.ui.components.StatusBadge
import pl.garage.bmwassistant.ui.components.detailImageResource
import pl.garage.bmwassistant.ui.components.garageBottomContentPadding
import pl.garage.bmwassistant.ui.theme.GarageTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VehicleOverviewScreen(
    vehicle: Vehicle?,
    onBack: () -> Unit,
    onVehicleUpdated: (Vehicle) -> Unit = {},
) {
    val currentVehicle = vehicle ?: return
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val garageRepository = remember(context) { context.appContainer.garageRepository }
    var selectedModuleType by rememberSaveable(currentVehicle.id) { mutableStateOf<VehicleModuleType?>(null) }
    var isEditingVehicle by rememberSaveable(currentVehicle.id) { mutableStateOf(false) }
    var initialDocumentationRepairTitle by rememberSaveable(currentVehicle.id) { mutableStateOf<String?>(null) }
    var initialRepairListRepairId by rememberSaveable(currentVehicle.id) { mutableStateOf<String?>(null) }
    var initialRepairListRepairTitle by rememberSaveable(currentVehicle.id) { mutableStateOf<String?>(null) }
    var startAddRepairFlow by rememberSaveable(currentVehicle.id) { mutableStateOf(false) }
    var isQuickRepairActionOpen by rememberSaveable(currentVehicle.id) { mutableStateOf(false) }
    var initialShoppingRepairTitle by rememberSaveable(currentVehicle.id) { mutableStateOf<String?>(null) }
    var initialShoppingAreaName by rememberSaveable(currentVehicle.id) { mutableStateOf<String?>(null) }
    var initialShoppingItemId by rememberSaveable(currentVehicle.id) { mutableStateOf<String?>(null) }
    var isDocumentationDetailsOpen by rememberSaveable(currentVehicle.id) { mutableStateOf(false) }
    var shouldReturnFromDocumentationToRepairs by rememberSaveable(currentVehicle.id) { mutableStateOf(false) }
    var shouldReturnFromShoppingToRepairs by rememberSaveable(currentVehicle.id) { mutableStateOf(false) }
    var repairPendingExport by remember { mutableStateOf<RepairProject?>(null) }
    var pendingExportArchive by remember { mutableStateOf<ByteArray?>(null) }
    var pendingImportArchive by remember { mutableStateOf<ByteArray?>(null) }
    var pendingImportTitle by remember { mutableStateOf<String?>(null) }
    var pendingImportNewTitle by remember { mutableStateOf("") }
    var archiveTransferMessage by remember { mutableStateOf<String?>(null) }
    val selectedModule = remember(selectedModuleType) {
        selectedModuleType?.let { selectedType -> vehicleModules.firstOrNull { it.type == selectedType } }
    }
    val initialShoppingArea = remember(initialShoppingAreaName) {
        initialShoppingAreaName?.let(VehicleArea::valueOf)
    }
    var repairProjects by remember(currentVehicle.id) { mutableStateOf(emptyList<RepairProject>()) }
    var repairDocumentation by remember(currentVehicle.id) { mutableStateOf(emptyList<RepairDocumentation>()) }
    var shoppingListItems by remember(currentVehicle.id) { mutableStateOf(emptyList<ShoppingListItem>()) }
    var inventoryPartItems by remember(currentVehicle.id) { mutableStateOf(emptyList<PartInventoryItem>()) }

    LaunchedEffect(currentVehicle.id) {
        val snapshot = withContext(Dispatchers.IO) {
            garageRepository.loadVehicleSnapshot(currentVehicle)
        }
        repairProjects = snapshot.repairs
        repairDocumentation = snapshot.documentation
        shoppingListItems = snapshot.shoppingList
        inventoryPartItems = snapshot.inventoryParts
    }

    fun persistVehicleSnapshot(
        repairs: List<RepairProject> = repairProjects,
        documentation: List<RepairDocumentation> = repairDocumentation,
        shoppingItems: List<ShoppingListItem> = shoppingListItems,
        inventoryParts: List<PartInventoryItem> = inventoryPartItems,
    ) {
        coroutineScope.launch(Dispatchers.IO) {
            garageRepository.saveVehicleSnapshot(
                vehicleId = currentVehicle.id,
                snapshot = VehicleDataSnapshot(
                    repairs = repairs,
                    documentation = documentation,
                    shoppingList = shoppingItems,
                    inventoryParts = inventoryParts
                )
            )
        }
    }

    fun updateRepairs(repairs: List<RepairProject>) {
        repairProjects = repairs
        persistVehicleSnapshot(repairs = repairs)
    }

    fun updateRepairDocumentation(documentation: List<RepairDocumentation>) {
        repairDocumentation = documentation
        persistVehicleSnapshot(documentation = documentation)
    }

    fun upsertRepairDocumentation(updatedDocumentation: RepairDocumentation) {
        var wasUpdated = false
        val updatedList = repairDocumentation.map { documentation ->
            if (documentation.belongsToRepair(updatedDocumentation)) {
                wasUpdated = true
                updatedDocumentation
            } else {
                documentation
            }
        }
        updateRepairDocumentation(
            if (wasUpdated) updatedList else updatedList + updatedDocumentation
        )
    }

    fun appendShoppingItems(items: List<ShoppingListItem>) {
        if (items.isEmpty()) return
        val updatedItems = shoppingListItems + items
        shoppingListItems = updatedItems
        persistVehicleSnapshot(shoppingItems = updatedItems)
    }

    fun updateShoppingItems(items: List<ShoppingListItem>) {
        shoppingListItems = items
        persistVehicleSnapshot(shoppingItems = items)
    }

    fun appendInventoryPart(part: PartInventoryItem) {
        val updatedParts = inventoryPartItems + part
        inventoryPartItems = updatedParts
        persistVehicleSnapshot(inventoryParts = updatedParts)
    }

    fun appendInventoryPartAndUpdateShopping(
        part: PartInventoryItem,
        items: List<ShoppingListItem>,
    ) {
        val updatedParts = inventoryPartItems + part
        inventoryPartItems = updatedParts
        shoppingListItems = items
        persistVehicleSnapshot(
            inventoryParts = updatedParts,
            shoppingItems = items
        )
    }

    fun updateInventoryParts(parts: List<PartInventoryItem>) {
        inventoryPartItems = parts
        persistVehicleSnapshot(inventoryParts = parts)
    }

    fun updateInventoryAndShopping(
        parts: List<PartInventoryItem>,
        items: List<ShoppingListItem>,
    ) {
        inventoryPartItems = parts
        shoppingListItems = items
        persistVehicleSnapshot(
            inventoryParts = parts,
            shoppingItems = items
        )
    }

    fun startRepairExport(repair: RepairProject) {
        val documentation = repairDocumentation.firstOrNull { it.belongsToRepair(repair) }
            ?: RepairDocumentation(
                title = "Dokumentacja: ${repair.title}",
                area = repair.area,
                repairTitle = repair.title,
                repairId = repair.id,
                summary = "Dokumentacja powiazana z naprawa: ${repair.title}."
            )
        val exportShoppingItems = documentation.archivedShoppingList.ifEmpty {
            (
                shoppingListItems.filter { it.belongsToRepair(repair) } +
                    inventoryPartItems
                        .filter { it.belongsToRepair(repair) }
                        .map { it.toArchivedShoppingListItem(repair) }
                ).mergeArchivedShoppingItems(repair)
        }
        pendingExportArchive = garageRepository.createRepairArchiveExport(
            vehicle = currentVehicle,
            repair = repair,
            documentation = documentation,
            shoppingItems = exportShoppingItems
        )
        repairPendingExport = repair
    }

    fun importRepairFromArchive(
        rawArchive: ByteArray,
        importAsArchived: Boolean,
        titleOverride: String? = null,
    ) {
        val imported = garageRepository.importRepairArchive(
            vehicle = currentVehicle,
            rawArchive = rawArchive,
            importAsArchived = importAsArchived
        )
        if (imported == null) {
            archiveTransferMessage = "Nie udalo sie odczytac pliku naprawy."
            return
        }
        val renamedImport = titleOverride
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let { imported.withRepairTitle(it) }
            ?: imported
        if (repairProjects.any { it.title.hasSameRepairTitleAs(renamedImport.repair.title) }) {
            archiveTransferMessage = "Naprawa o tej nazwie juz istnieje. Zmien nazwe importowanej naprawy."
            return
        }
        updateRepairs(repairProjects + renamedImport.repair)
        updateRepairDocumentation(repairDocumentation + renamedImport.documentation)
        if (renamedImport.shoppingList.isNotEmpty()) {
            appendShoppingItems(renamedImport.shoppingList)
        }
        archiveTransferMessage = if (importAsArchived) {
            "Zaimportowano naprawe do archiwum."
        } else {
            "Zaimportowano naprawe jako aktualna."
        }
    }

    val repairExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        val exportArchive = pendingExportArchive
        if (uri != null && exportArchive != null) {
            coroutineScope.launch {
                val saved = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openOutputStream(uri)?.use { output ->
                            output.write(exportArchive)
                        }
                    }.isSuccess
                }
                archiveTransferMessage = if (saved) {
                    "Eksport naprawy zapisany do pliku."
                } else {
                    "Nie udalo sie zapisac pliku eksportu."
                }
                pendingExportArchive = null
                repairPendingExport = null
            }
        } else {
            pendingExportArchive = null
            repairPendingExport = null
        }
    }

    val repairImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                val rawArchive = withContext(Dispatchers.IO) {
                    runCatching {
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            input.readBytes()
                        }
                    }.getOrNull()
                }
                if (rawArchive == null) {
                    archiveTransferMessage = "Nie udalo sie otworzyc pliku naprawy."
                } else {
                    pendingImportTitle = garageRepository.peekRepairArchiveTitle(rawArchive)
                    pendingImportNewTitle = pendingImportTitle
                        ?.takeIf { title -> repairProjects.any { it.title.hasSameRepairTitleAs(title) } }
                        ?.let { title -> repairProjects.nextAvailableRepairTitle(title) }
                        .orEmpty()
                    pendingImportArchive = rawArchive
                }
            }
        }
    }

    repairPendingExport?.let { repair ->
        AlertDialog(
            onDismissRequest = {
                repairPendingExport = null
                pendingExportArchive = null
            },
            title = { Text("Eksport naprawy") },
            text = {
                Text("Zapisac naprawe \"${repair.title}\" do jednego pliku do udostepnienia?")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val safeTitle = repair.title
                            .lowercase()
                            .replace(Regex("[^a-z0-9]+"), "-")
                            .trim('-')
                            .ifBlank { "naprawa" }
                        repairExportLauncher.launch("$safeTitle.bmwrepair")
                    }
                ) {
                    Text("Zapisz plik")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        repairPendingExport = null
                        pendingExportArchive = null
                    }
                ) {
                    Text("Anuluj")
                }
            }
        )
    }

    pendingImportArchive?.let { rawArchive ->
        val importedTitle = pendingImportTitle ?: "z pliku"
        val hasTitleConflict = repairProjects.any { it.title.hasSameRepairTitleAs(importedTitle) }
        val importTitle = if (hasTitleConflict) pendingImportNewTitle.trim() else importedTitle
        val canImport = !hasTitleConflict || (
            importTitle.isNotBlank() &&
                repairProjects.none { it.title.hasSameRepairTitleAs(importTitle) }
            )
        AlertDialog(
            onDismissRequest = {
                pendingImportArchive = null
                pendingImportTitle = null
                pendingImportNewTitle = ""
            },
            title = { Text("Import naprawy") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Gdzie dodac naprawe \"$importedTitle\"?"
                    )
                    if (hasTitleConflict) {
                        Text(
                            text = "Naprawa o tej nazwie juz istnieje. Podaj nowa nazwe dla importu.",
                            color = MaterialTheme.colorScheme.error
                        )
                        GarageTextField(
                            value = pendingImportNewTitle,
                            onValueChange = { pendingImportNewTitle = it },
                            label = "Nowa nazwa naprawy",
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = repairProjects.nextAvailableRepairTitle(importedTitle)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = canImport,
                    onClick = {
                        importRepairFromArchive(
                            rawArchive = rawArchive,
                            importAsArchived = false,
                            titleOverride = importTitle.takeIf { hasTitleConflict }
                        )
                        pendingImportArchive = null
                        pendingImportTitle = null
                        pendingImportNewTitle = ""
                    }
                ) {
                    Text("Jako aktualna")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        enabled = canImport,
                        onClick = {
                            importRepairFromArchive(
                                rawArchive = rawArchive,
                                importAsArchived = true,
                                titleOverride = importTitle.takeIf { hasTitleConflict }
                            )
                            pendingImportArchive = null
                            pendingImportTitle = null
                            pendingImportNewTitle = ""
                        }
                    ) {
                        Text("Do archiwum")
                    }
                    TextButton(
                        onClick = {
                            pendingImportArchive = null
                            pendingImportTitle = null
                            pendingImportNewTitle = ""
                        }
                    ) {
                        Text("Anuluj")
                    }
                }
            }
        )
    }

    archiveTransferMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { archiveTransferMessage = null },
            title = { Text("Dokumentacja naprawy") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { archiveTransferMessage = null }) {
                    Text("OK")
                }
            }
        )
    }

    if (isQuickRepairActionOpen) {
        QuickRepairActionDialog(
            onImportRepair = {
                isQuickRepairActionOpen = false
                repairImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            onAddRepair = {
                isQuickRepairActionOpen = false
                startAddRepairFlow = true
                selectedModuleType = VehicleModuleType.Repairs
            },
            onDismiss = { isQuickRepairActionOpen = false }
        )
    }

    BackHandler(enabled = selectedModule != null) {
        startAddRepairFlow = false
        selectedModuleType = null
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

    val bottomItems = listOf("Przeglad", "Naprawy", "Czesci", "Dokumenty", "Wiecej")

    fun selectBottomItem(item: String) {
        selectedModuleType = when (item) {
            "Naprawy" -> VehicleModuleType.Repairs
            "Czesci" -> VehicleModuleType.PartsStorage
            "Dokumenty" -> VehicleModuleType.Documentation
            "Wiecej" -> VehicleModuleType.Status
            else -> null
        }
    }

    if (selectedModule?.type == VehicleModuleType.Status) {
        VehicleStatusScreen(
            vehicle = currentVehicle,
            activeRepairAreas = repairProjects.filterNot { it.status.isFinishedRepairStatus() }.map { it.area }.toSet(),
            bottomBar = {
                BottomNavBar(
                    items = bottomItems,
                    selectedItem = "Wiecej",
                    onSelect = ::selectBottomItem,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            },
            onBack = { selectedModuleType = null }
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
            initialRepairId = initialRepairListRepairId,
            initialRepairTitle = initialRepairListRepairTitle,
            startAddRepairFlow = startAddRepairFlow,
            onStartAddRepairFlowConsumed = { startAddRepairFlow = false },
            onRepairAdded = { repair, documentation ->
                updateRepairs(repairProjects + repair)
                updateRepairDocumentation(repairDocumentation + documentation)
            },
            onRepairUpdated = { updatedRepair ->
                val previousRepair = repairProjects.firstOrNull { it.id == updatedRepair.id }
                val movedToArchive = previousRepair?.status?.isFinishedRepairStatus() != true &&
                    updatedRepair.status.isFinishedRepairStatus()
                updateRepairs(
                    repairProjects.map { repair ->
                        if (repair.id == updatedRepair.id) updatedRepair else repair
                    }
                )
                if (movedToArchive) {
                    val archivedShoppingList = (
                        shoppingListItems.filter { it.belongsToRepair(updatedRepair) } +
                        inventoryPartItems
                            .filter { it.belongsToRepair(updatedRepair) }
                            .map { it.toArchivedShoppingListItem(updatedRepair) }
                        ).mergeArchivedShoppingItems(updatedRepair)
                    updateRepairDocumentation(
                        repairDocumentation.withArchivedShoppingList(updatedRepair, archivedShoppingList)
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
                initialRepairListRepairId = documentation.repairId
                initialRepairListRepairTitle = documentation.repairTitle
                isDocumentationDetailsOpen = true
                shouldReturnFromDocumentationToRepairs = true
                selectedModuleType = VehicleModuleType.Documentation
            },
            onDocumentationUpdated = { updatedDocumentation ->
                upsertRepairDocumentation(updatedDocumentation)
            },
            onOpenShoppingList = { repair ->
                initialShoppingRepairTitle = repair.title
                initialShoppingAreaName = repair.area.name
                initialRepairListRepairId = repair.id
                initialRepairListRepairTitle = repair.title
                shouldReturnFromShoppingToRepairs = true
                selectedModuleType = VehicleModuleType.PartsStorage
            },
            onOpenShoppingListItem = { repair, item ->
                initialShoppingRepairTitle = repair.title
                initialShoppingAreaName = repair.area.name
                initialShoppingItemId = item.stableId()
                initialRepairListRepairId = repair.id
                initialRepairListRepairTitle = repair.title
                shouldReturnFromShoppingToRepairs = true
                selectedModuleType = VehicleModuleType.PartsStorage
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
            onInventoryPartAddedAndShoppingListUpdated = { part, items ->
                appendInventoryPartAndUpdateShopping(part, items)
            },
            onExportRepair = { repair ->
                startRepairExport(repair)
            },
            onImportRepair = {
                repairImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
            },
            onInitialRepairClosed = {
                initialRepairListRepairId = null
                initialRepairListRepairTitle = null
                startAddRepairFlow = false
            },
            onBack = {
                initialRepairListRepairId = null
                initialRepairListRepairTitle = null
                startAddRepairFlow = false
                selectedModuleType = null
            },
            onBottomSelect = ::selectBottomItem
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
                    selectedModuleType = VehicleModuleType.Documentation
                },
                onDocumentationUpdated = { updatedDocumentation ->
                    upsertRepairDocumentation(updatedDocumentation)
                },
                onOpenShoppingList = { repair ->
                    initialShoppingRepairTitle = repair.title
                    initialShoppingAreaName = repair.area.name
                    selectedModuleType = VehicleModuleType.PartsStorage
                },
                onOpenShoppingListItem = { repair, item ->
                    initialShoppingRepairTitle = repair.title
                    initialShoppingAreaName = repair.area.name
                    initialShoppingItemId = item.stableId()
                    selectedModuleType = VehicleModuleType.PartsStorage
                },
                onAddShoppingItems = {},
                onShoppingListUpdated = {},
                onInventoryPartAdded = {},
                onExportRepair = { repair ->
                    startRepairExport(repair)
                },
                onImportRepair = {
                    repairImportLauncher.launch(arrayOf("application/json", "text/*", "*/*"))
                },
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
                    selectedModuleType = null
                },
                onBottomSelect = ::selectBottomItem
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
                upsertRepairDocumentation(updatedDocumentation)
            },
            onBack = {
                initialDocumentationRepairTitle = null
                isDocumentationDetailsOpen = false
                if (shouldReturnFromDocumentationToRepairs) {
                    shouldReturnFromDocumentationToRepairs = false
                    selectedModuleType = VehicleModuleType.Repairs
                } else {
                    initialRepairListRepairId = null
                    initialRepairListRepairTitle = null
                    selectedModuleType = VehicleModuleType.Documentation
                }
            }
        )
        return
    }

    if (selectedModule?.type == VehicleModuleType.PartsStorage) {
        VehiclePartsStorageScreen(
            vehicle = currentVehicle,
            availableRepairs = repairProjects.filterNot { it.status.isFinishedRepairStatus() },
            inventoryParts = inventoryPartItems,
            shoppingList = shoppingListItems,
            consumables = sampleConsumablesFor(),
            initialSection = if (initialShoppingRepairTitle != null) {
                PartsStorageSection.Shopping
            } else {
                null
            },
            initialShoppingRepairTitle = initialShoppingRepairTitle,
            initialShoppingArea = initialShoppingArea,
            initialShoppingItemId = initialShoppingItemId,
            onInitialShoppingClosed = {
                initialShoppingRepairTitle = null
                initialShoppingAreaName = null
                initialShoppingItemId = null
            },
            onInventoryUpdated = { parts ->
                updateInventoryParts(parts)
            },
            onShoppingListUpdated = { items ->
                updateShoppingItems(items)
            },
            onInventoryAndShoppingUpdated = { parts, items ->
                updateInventoryAndShopping(parts, items)
            },
            onBack = {
                if (shouldReturnFromShoppingToRepairs) {
                    shouldReturnFromShoppingToRepairs = false
                    initialShoppingRepairTitle = null
                    initialShoppingAreaName = null
                    initialShoppingItemId = null
                    selectedModuleType = VehicleModuleType.Repairs
                } else {
                    initialShoppingRepairTitle = null
                    initialShoppingAreaName = null
                    initialShoppingItemId = null
                    selectedModuleType = null
                }
            },
            bottomBar = {
                BottomNavBar(
                    items = bottomItems,
                    selectedItem = "Czesci",
                    onSelect = ::selectBottomItem,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        )
        return
    }

    selectedModule?.let { module ->
        ModulePlaceholderDialog(
            module = module,
            onDismiss = { selectedModuleType = null }
        )
    }

    val activeRepairs = repairProjects.filterNot { it.status.isFinishedRepairStatus() }
    val partsToBuy = shoppingListItems.take(3)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    top = 18.dp,
                    end = 18.dp,
                    bottom = garageBottomContentPadding(hasBottomBar = true)
                ),
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
                                        initialRepairListRepairId = repair.id
                                        initialRepairListRepairTitle = repair.title
                                        selectedModuleType = VehicleModuleType.Repairs
                                    }
                                )
                            }
                            TextButton(
                                onClick = { selectedModuleType = VehicleModuleType.Repairs }
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
                    QuickAddRepairAction(onClick = { isQuickRepairActionOpen = true })
                }
            }
            BottomNavBar(
                items = bottomItems,
                selectedItem = "Przeglad",
                onSelect = ::selectBottomItem,
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
            painter = painterResource(vehicle.detailImageResource()),
            contentDescription = vehicle.displayName.ifBlank { "BMW" },
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
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
        StatusBadge(repair.status.normalizedRepairStatusLabel())
        Text("›", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f), fontSize = 22.sp)
    }
}

@Composable
private fun QuickAddRepairAction(
    onClick: () -> Unit,
) {
    GaragePanel(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(76.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(54.dp),
                color = AccentYellow.copy(alpha = 0.18f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        color = AccentYellow,
                        fontSize = 30.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    text = "Dodaj naprawe",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Importuj dokumentacje albo utworz nowy projekt naprawy.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    fontSize = 12.sp,
                    lineHeight = 15.sp
                )
            }
        }
    }
}

@Composable
private fun QuickRepairActionDialog(
    onImportRepair: () -> Unit,
    onAddRepair: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj naprawe") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GaragePanel(onClick = onImportRepair) {
                    Text("Importuj naprawe", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Wczytaj paczke naprawy z dokumentacja i lista czesci.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 12.sp
                    )
                }
                GaragePanel(onClick = onAddRepair) {
                    Text("Dodaj naprawe", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Utworz nowy projekt i wybierz obszar auta.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

private fun RepairProject.areaColor(): Color = when (area) {
    VehicleArea.Engine -> AccentYellow
    VehicleArea.Suspension -> AccentBlue
    VehicleArea.Electronics -> AccentPurple
    VehicleArea.Body -> AccentGreen
    VehicleArea.Service -> AccentGreen
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
