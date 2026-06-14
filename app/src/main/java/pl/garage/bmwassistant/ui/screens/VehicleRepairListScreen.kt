package pl.garage.bmwassistant.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.OpenableColumns
import android.text.Html
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import pl.garage.bmwassistant.data.sampleRepairsFor
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairCheckpoint
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.TisDocumentationLink
import pl.garage.bmwassistant.model.TorqueDiagramAssignment
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.model.YoutubeVideo
import pl.garage.bmwassistant.model.isFinishedRepairStatus
import pl.garage.bmwassistant.model.normalizedRepairStatusLabel
import pl.garage.bmwassistant.ui.components.GarageTextField
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.AccentBlue
import pl.garage.bmwassistant.ui.components.AccentGreen
import pl.garage.bmwassistant.ui.components.AccentPurple
import pl.garage.bmwassistant.ui.components.AccentRed
import pl.garage.bmwassistant.ui.components.AccentYellow
import pl.garage.bmwassistant.ui.components.BottomNavBar
import pl.garage.bmwassistant.ui.components.GaragePanel
import pl.garage.bmwassistant.ui.components.SegmentTabs
import pl.garage.bmwassistant.ui.components.StatusBadge
import pl.garage.bmwassistant.ui.components.iconResource
import pl.garage.bmwassistant.ui.theme.GarageTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Composable
fun VehicleRepairListScreen(
    vehicle: Vehicle,
    repairs: List<RepairProject>,
    repairDocumentation: List<RepairDocumentation>,
    inventoryParts: List<PartInventoryItem>,
    shoppingList: List<ShoppingListItem>,
    initialRepairId: String? = null,
    initialRepairTitle: String? = null,
    existingRepairTitles: Set<String> = repairs.map { it.title }.toSet(),
    startAddRepairFlow: Boolean = false,
    onStartAddRepairFlowConsumed: () -> Unit = {},
    onRepairAdded: (RepairProject, RepairDocumentation) -> Unit,
    onRepairUpdated: (RepairProject) -> Unit,
    onOpenDocumentation: (RepairDocumentation) -> Unit,
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
    onOpenShoppingList: (RepairProject) -> Unit,
    onAddShoppingItems: (List<ShoppingListItem>) -> Unit,
    onShoppingListUpdated: (List<ShoppingListItem>) -> Unit,
    onInventoryPartAdded: (PartInventoryItem) -> Unit,
    onExportRepair: ((RepairProject) -> Unit)? = null,
    onImportRepair: (() -> Unit)? = null,
    onInitialRepairClosed: () -> Unit = {},
    title: String = "Naprawy",
    selectedBottomItem: String = "Naprawy",
    showArchivedRepairs: Boolean = false,
    showAddRepairButton: Boolean = true,
    showGeneralDocumentationSection: Boolean = false,
    emptyText: String = "Brak aktywnych napraw. Zakonczone naprawy znajdziesz w Dokumentach.",
    onBack: () -> Unit,
    onBottomSelect: (String) -> Unit = { item ->
        if (item != selectedBottomItem) onBack()
    },
) {
    val visibleRepairs = remember(repairs, showArchivedRepairs) {
        repairs.filter { repair ->
            val isFinished = repair.status.isFinishedRepairStatus()
            if (showArchivedRepairs) isFinished else !isFinished
        }
    }
    var expandedAreas by remember(visibleRepairs) {
        mutableStateOf(
            visibleRepairs.map { it.area }
                .toSet()
                .ifEmpty { setOf(VehicleArea.Engine) }
        )
    }
    var isChoosingRepairArea by rememberSaveable(vehicle.id, showArchivedRepairs) { mutableStateOf(false) }
    var selectedAreaForNewRepairName by rememberSaveable(vehicle.id, showArchivedRepairs) { mutableStateOf<String?>(null) }
    var selectedRepairId by rememberSaveable(vehicle.id, showArchivedRepairs, initialRepairId, initialRepairTitle) {
        mutableStateOf(
            initialRepairId
                ?: initialRepairTitle?.let { repairTitle ->
                    visibleRepairs.firstOrNull { it.title == repairTitle }?.id
                }
        )
    }
    val selectedRepair = remember(selectedRepairId, visibleRepairs) {
        selectedRepairId?.let { repairId -> visibleRepairs.firstOrNull { it.id == repairId } }
    }
    val selectedAreaForNewRepair = remember(selectedAreaForNewRepairName) {
        selectedAreaForNewRepairName?.let(VehicleArea::valueOf)
    }

    LaunchedEffect(initialRepairId, initialRepairTitle, visibleRepairs) {
        if (selectedRepairId == null) {
            selectedRepairId = initialRepairId
                ?: initialRepairTitle?.let { repairTitle ->
                    visibleRepairs.firstOrNull { it.title == repairTitle }?.id
                }
        }
    }

    LaunchedEffect(startAddRepairFlow) {
        if (startAddRepairFlow && showAddRepairButton) {
            isChoosingRepairArea = true
            onStartAddRepairFlowConsumed()
        }
    }

    BackHandler(enabled = selectedRepair != null) {
        selectedRepairId = null
        onInitialRepairClosed()
    }

    selectedRepair?.let { repair ->
        RepairDetailsScreen(
            vehicle = vehicle,
            repair = repair,
            documentation = repairDocumentation.firstOrNull { it.belongsToRepair(repair) },
            availableParts = inventoryParts.filter { it.belongsToRepair(repair) },
            shoppingItems = shoppingList.filter { it.belongsToRepair(repair) },
            allShoppingItems = shoppingList,
            isArchivedMode = showArchivedRepairs,
            onOpenDocumentation = onOpenDocumentation,
            onDocumentationUpdated = onDocumentationUpdated,
            onOpenShoppingList = onOpenShoppingList,
            onAddShoppingItems = onAddShoppingItems,
            onShoppingListUpdated = onShoppingListUpdated,
            onInventoryPartAdded = onInventoryPartAdded,
            onExportRepair = onExportRepair,
            onRepairUpdated = { updatedRepair ->
                selectedRepairId = updatedRepair.id
                onRepairUpdated(updatedRepair)
            },
            bottomBar = {
                RepairBottomNavBar(
                    selectedBottomItem = selectedBottomItem,
                    onSelect = onBottomSelect,
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            },
            onBack = {
                selectedRepairId = null
                onInitialRepairClosed()
            }
        )
        return
    }

    if (isChoosingRepairArea) {
        RepairAreaPickerDialog(
            onAreaSelected = { area ->
                selectedAreaForNewRepairName = area.name
                isChoosingRepairArea = false
            },
            onDismiss = { isChoosingRepairArea = false }
        )
    }

    selectedAreaForNewRepair?.let { area ->
        AddRepairDialog(
            vehicle = vehicle,
            area = area,
            existingRepairTitles = existingRepairTitles,
            onSave = { repair, documentation ->
                onRepairAdded(repair, documentation)
                expandedAreas = expandedAreas + area
                selectedAreaForNewRepairName = null
                selectedRepairId = repair.id
            },
            onDismiss = { selectedAreaForNewRepairName = null }
        )
    }

    val bottomItems = listOf("Przeglad", "Naprawy", "Czesci", "Dokumenty", "Wiecej")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Header(
                            title = title,
                            subtitle = vehicle.displayName.ifBlank { "Profil auta" }
                        )
                        if (showAddRepairButton) {
                            AddRepairButton(onClick = { isChoosingRepairArea = true })
                        }
                    }
                }

                if (showGeneralDocumentationSection) {
                    item {
                        GeneralDocumentationPanel(onImportRepair = onImportRepair)
                    }
                    item {
                        Text(
                            text = "Archiwum napraw",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (visibleRepairs.isEmpty()) {
                    item {
                        GaragePanel {
                            Text(
                                text = emptyText,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                            )
                        }
                    }
                } else if (showArchivedRepairs) {
                    VehicleArea.entries.forEach { area ->
                        val areaRepairs = visibleRepairs.filter { it.area == area }
                        if (areaRepairs.isNotEmpty()) {
                            item {
                                ArchiveRepairAreaHeader(
                                    area = area,
                                    repairsCount = areaRepairs.size,
                                    isExpanded = area in expandedAreas,
                                    onToggle = {
                                        expandedAreas = if (area in expandedAreas) {
                                            expandedAreas - area
                                        } else {
                                            expandedAreas + area
                                        }
                                    }
                                )
                            }
                            if (area in expandedAreas) {
                                items(areaRepairs) { repair ->
                                    RepairCard(
                                        repair = repair,
                                        documentation = repairDocumentation.firstOrNull { it.belongsToRepair(repair) },
                                        partsCount = archivedPartsCount(
                                            repair = repair,
                                            repairDocumentation = repairDocumentation,
                                            inventoryParts = inventoryParts
                                        ),
                                        showCompleteAction = false,
                                        onComplete = {},
                                        onExport = onExportRepair?.let { export -> { export(repair) } },
                                        onClick = { selectedRepairId = repair.id }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    items(visibleRepairs) { repair ->
                        RepairCard(
                            repair = repair,
                            documentation = repairDocumentation.firstOrNull { it.belongsToRepair(repair) },
                            partsCount = archivedPartsCount(
                                repair = repair,
                                repairDocumentation = repairDocumentation,
                                inventoryParts = inventoryParts
                            ),
                            showCompleteAction = !showArchivedRepairs,
                            onComplete = {
                                onRepairUpdated(repair.copy(status = "Zakonczona"))
                            },
                            onExport = null,
                            onClick = { selectedRepairId = repair.id }
                        )
                    }
                }
            }
            BottomNavBar(
                items = bottomItems,
                selectedItem = selectedBottomItem,
                onSelect = onBottomSelect,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun RepairBottomNavBar(
    selectedBottomItem: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BottomNavBar(
        items = listOf("Przeglad", "Naprawy", "Czesci", "Dokumenty", "Wiecej"),
        selectedItem = selectedBottomItem,
        onSelect = onSelect,
        modifier = modifier
    )
}

@Composable
private fun GeneralDocumentationPanel(
    onImportRepair: (() -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Dokumentacja ogolna",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        GaragePanel {
            Text(
                text = "Dokumenty stale auta",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Miejsce na VIN, instrukcje, stale linki, PDF-y, schematy i notatki niezalezne od konkretnej naprawy.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 13.sp,
                lineHeight = 18.sp
            )
            if (onImportRepair != null) {
                TextButton(onClick = onImportRepair) {
                    Text("Importuj naprawe z pliku")
                }
            }
        }
    }
}

@Composable
private fun ArchiveRepairAreaHeader(
    area: VehicleArea,
    repairsCount: Int,
    isExpanded: Boolean,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.72f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(34.dp),
                color = area.accentColor().copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(area.iconResource()),
                        contentDescription = area.label,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = area.label,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "$repairsCount zakonczonych napraw",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            }
            Text(
                text = if (isExpanded) "Zwin" else "Rozwin",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private fun archivedPartsCount(
    repair: RepairProject,
    repairDocumentation: List<RepairDocumentation>,
    inventoryParts: List<PartInventoryItem>,
): Int =
    inventoryParts.count { it.belongsToRepair(repair) }.takeIf { it > 0 }
        ?: repairDocumentation.firstOrNull { it.belongsToRepair(repair) }
            ?.archivedShoppingList
            ?.size
        ?: 0

@Composable
private fun RepairCard(
    repair: RepairProject,
    documentation: RepairDocumentation?,
    partsCount: Int,
    showCompleteAction: Boolean,
    onComplete: () -> Unit,
    onExport: (() -> Unit)?,
    onClick: () -> Unit,
) {
    GaragePanel(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                modifier = Modifier.size(38.dp),
                color = repair.area.accentColor().copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Image(
                        painter = painterResource(repair.area.iconResource()),
                        contentDescription = repair.area.label,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = repair.title,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = repair.area.label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    fontSize = 13.sp
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadge(repair.status.normalizedRepairStatusLabel())
                if (showCompleteAction) {
                    DoneRepairAction(onClick = onComplete)
                }
                if (onExport != null) {
                    TextButton(onClick = onExport) {
                        Text("Eksport")
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$partsCount czesci",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp
            )
            Text(
                text = "${documentation?.effectiveTorqueTables().orEmpty().sumOf { it.torqueSpecs.size }} momentow",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DoneRepairAction(onClick: () -> Unit) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        color = AccentGreen.copy(alpha = 0.16f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "✓",
                color = AccentGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Done",
                color = AccentGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AddRepairButton(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .size(54.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.primary
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "+",
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
private fun RepairAreaPickerDialog(
    onAreaSelected: (VehicleArea) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nowa naprawa") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Wybierz kategorie, do ktorej przypisac naprawe.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                VehicleArea.entries.forEach { area ->
                    RepairAreaChoiceRow(
                        area = area,
                        onClick = { onAreaSelected(area) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun RepairAreaChoiceRow(
    area: VehicleArea,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(area.iconResource()),
                contentDescription = area.label,
                modifier = Modifier.height(28.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = area.label,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = area.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    maxLines = 2
                )
            }
        }
    }
}

@Composable
private fun AddRepairDialog(
    vehicle: Vehicle,
    area: VehicleArea,
    existingRepairTitles: Set<String>,
    onSave: (RepairProject, RepairDocumentation) -> Unit,
    onDismiss: () -> Unit,
) {
    var repairTitle by remember { mutableStateOf("") }
    var repairNote by remember { mutableStateOf("") }
    val titleConflict = existingRepairTitles.any { it.hasSameRepairTitleAs(repairTitle) }
    val canSave = repairTitle.isNotBlank() && !titleConflict

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Naprawa: ${area.label}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Wpisz nazwe naprawy. Od razu powstanie powiazana dokumentacja i miejsce do przypisywania czesci w magazynie.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                GarageTextField(
                    value = repairTitle,
                    onValueChange = { repairTitle = it },
                    label = "Nazwa naprawy",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. Wymiana swiec"
                )
                if (titleConflict) {
                    Text(
                        text = "Naprawa o tej nazwie juz istnieje. Wpisz inna nazwe.",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 12.sp
                    )
                }
                GarageTextField(
                    value = repairNote,
                    onValueChange = { repairNote = it },
                    label = "Krotka notatka",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Opcjonalnie: objawy, cel, uwagi"
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val title = repairTitle.trim()
                    val description = repairNote.trim().ifBlank {
                        "Naprawa dodana recznie. Szczegoly uzupelnimy w dokumentacji, notatkach i liscie zakupow."
                    }
                    val repair = RepairProject(
                        title = title,
                        area = area,
                        vehicleName = vehicle.displayName.ifBlank { "BMW" },
                        status = "Planowane",
                        priority = "Do ustalenia",
                        problemDescription = description,
                        goal = "Przygotowac czesci, dokumentacje i plan wykonania naprawy.",
                        checklist = emptyList(),
                        partsToIdentify = emptyList(),
                        documentsToCollect = listOf("Dokumentacja: $title")
                    )
                    val documentation = RepairDocumentation(
                        title = "Dokumentacja: $title",
                        area = area,
                        repairTitle = title,
                        repairId = repair.id,
                        summary = "Dokumentacja powiazana z naprawa: $title. Ten wpis zostaje zapisany rowniez wtedy, gdy naprawa zniknie z aktywnej listy."
                    )
                    onSave(repair, documentation)
                }
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun RepairAreaSection(
    area: VehicleArea,
    repairs: List<RepairProject>,
    isExpanded: Boolean,
    onRepairClick: (RepairProject) -> Unit,
    onToggle: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(area.iconResource()),
                    contentDescription = area.label,
                    modifier = Modifier.height(32.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = area.label,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${repairs.size} napraw",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                    )
                }
                Text(
                    text = if (isExpanded) "Zwin" else "Rozwin",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isExpanded) {
                if (repairs.isEmpty()) {
                    EmptyRepairRow()
                } else {
                    repairs.forEach { repair ->
                        RepairRow(
                            repair = repair,
                            onClick = { onRepairClick(repair) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyRepairRow() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = "Brak napraw w tej kategorii.",
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
        )
    }
}

@Composable
private fun RepairRow(
    repair: RepairProject,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = repair.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = repair.problemDescription,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                maxLines = 3
            )
            Text(
                text = "${repair.status} / ${repair.priority}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Otworz naprawe",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun RepairDetailsScreen(
    vehicle: Vehicle,
    repair: RepairProject,
    documentation: RepairDocumentation?,
    availableParts: List<PartInventoryItem>,
    shoppingItems: List<ShoppingListItem>,
    allShoppingItems: List<ShoppingListItem>,
    isArchivedMode: Boolean = false,
    onOpenDocumentation: (RepairDocumentation) -> Unit,
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
    onOpenShoppingList: (RepairProject) -> Unit,
    onAddShoppingItems: (List<ShoppingListItem>) -> Unit,
    onShoppingListUpdated: (List<ShoppingListItem>) -> Unit,
    onInventoryPartAdded: (PartInventoryItem) -> Unit,
    onExportRepair: ((RepairProject) -> Unit)? = null,
    onRepairUpdated: (RepairProject) -> Unit,
    bottomBar: (@Composable BoxScope.() -> Unit)? = null,
    onBack: () -> Unit,
) {
    var isCatalogVisible by rememberSaveable(repair.id) { mutableStateOf(false) }
    var selectedTab by rememberSaveable(repair.id) { mutableStateOf("Opis") }

    if (isCatalogVisible) {
        RealOemSchematicsDialog(
            vehicle = vehicle,
            repair = repair,
            documentation = documentation,
            onAddShoppingItems = onAddShoppingItems,
            onDocumentationUpdated = onDocumentationUpdated,
            onDismiss = { isCatalogVisible = false }
        )
    }

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
                    bottom = if (bottomBar == null) 18.dp else 96.dp
                ),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    TextButton(onClick = onBack) {
                        Text("‹ Naprawy")
                    }
                }
                item {
                    RepairDetailsHeader(repair)
                }
                item {
                    SegmentTabs(
                        tabs = listOf("Opis", "Czesci", "Dokumentacja", "Momenty", "Notatki"),
                        selectedTab = selectedTab,
                        onSelect = { selectedTab = it }
                    )
                }
                when (selectedTab) {
                    "Opis" -> item {
                        RepairOverviewTab(
                            repair = repair,
                            onRepairUpdated = onRepairUpdated
                        )
                    }
                    "Czesci" -> item {
                        RepairPartsTab(
                            repair = repair,
                            availableParts = availableParts,
                            shoppingItems = shoppingItems,
                            allShoppingItems = allShoppingItems,
                            isArchivedMode = isArchivedMode,
                            onOpenShoppingList = { onOpenShoppingList(repair) },
                            onOpenCatalog = { isCatalogVisible = true },
                            onShoppingListUpdated = onShoppingListUpdated,
                            onInventoryPartAdded = onInventoryPartAdded
                        )
                    }
                    "Dokumentacja" -> item {
                        RepairDocumentsTab(
                            documentation = documentation,
                            onDocumentationUpdated = onDocumentationUpdated
                        )
                    }
                    "Momenty" -> item {
                        RepairTorqueTab(
                            vehicle = vehicle,
                            repair = repair,
                            documentation = documentation,
                            onDocumentationUpdated = onDocumentationUpdated
                        )
                    }
                    "Notatki" -> item {
                        RepairNotesTab(
                            repair = repair,
                            documentation = documentation,
                            onDocumentationUpdated = onDocumentationUpdated
                        )
                    }
                }
            }
            bottomBar?.invoke(this)
        }
    }
}

@Composable
private fun RepairDetailsHeader(repair: RepairProject) {
    GaragePanel {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = repair.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = repair.area.label,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            StatusBadge(repair.status.normalizedRepairStatusLabel())
        }
    }
}

@Composable
private fun RepairOverviewTab(
    repair: RepairProject,
    onRepairUpdated: (RepairProject) -> Unit,
) {
    var descriptionDraft by remember(repair.id, repair.problemDescription) {
        mutableStateOf(repair.problemDescription)
    }
    var newCheckpointText by remember(repair.id) { mutableStateOf("") }
    var checkpointBeingEdited by remember(repair.id) { mutableStateOf<RepairCheckpoint?>(null) }
    val checkpoints = repair.effectiveCheckpoints()

    checkpointBeingEdited?.let { checkpoint ->
        EditCheckpointDialog(
            checkpoint = checkpoint,
            onSave = { updatedText ->
                val updatedCheckpoints = checkpoints.map { item ->
                    if (item.id == checkpoint.id) item.copy(text = updatedText) else item
                }
                onRepairUpdated(repair.withCheckpoints(updatedCheckpoints))
                checkpointBeingEdited = null
            },
            onDismiss = { checkpointBeingEdited = null }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GaragePanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Opis problemu", fontWeight = FontWeight.SemiBold)
                TextButton(
                    enabled = descriptionDraft.trim() != repair.problemDescription,
                    onClick = {
                        onRepairUpdated(
                            repair.copy(problemDescription = descriptionDraft.trim())
                        )
                    }
                ) {
                    Text("Zapisz")
                }
            }
            GarageTextField(
                value = descriptionDraft,
                onValueChange = { descriptionDraft = it },
                label = "Opis",
                modifier = Modifier.fillMaxWidth(),
                placeholder = "Opisz objawy, kontekst i co trzeba sprawdzic",
                singleLine = false,
                minLines = 4
            )
        }
        GaragePanel {
            Text("Plan dzialania", fontWeight = FontWeight.SemiBold)
            if (checkpoints.isEmpty()) {
                Text(
                    text = "Dodaj pierwszy checkpoint naprawy.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                    fontSize = 13.sp
                )
            } else {
                checkpoints.forEach { checkpoint ->
                    RepairCheckpointRow(
                        checkpoint = checkpoint,
                        onCheckedChange = { isDone ->
                            val updatedCheckpoints = checkpoints.map { item ->
                                if (item.id == checkpoint.id) item.copy(isDone = isDone) else item
                            }
                            onRepairUpdated(repair.withCheckpoints(updatedCheckpoints))
                        },
                        onEdit = {
                            checkpointBeingEdited = checkpoint
                        },
                        onDelete = {
                            val updatedCheckpoints = checkpoints.filterNot { item ->
                                item.id == checkpoint.id
                            }
                            onRepairUpdated(repair.withCheckpoints(updatedCheckpoints))
                        }
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GarageTextField(
                    value = newCheckpointText,
                    onValueChange = { newCheckpointText = it },
                    label = "Checkpoint",
                    modifier = Modifier
                        .weight(1f)
                        .testTag("repair_checkpoint_input"),
                    placeholder = "Nowy checkpoint"
                )
                TextButton(
                    modifier = Modifier.testTag("repair_add_checkpoint_button"),
                    enabled = newCheckpointText.isNotBlank(),
                    onClick = {
                        val updatedCheckpoints = checkpoints + RepairCheckpoint(
                            id = "checkpoint-${System.currentTimeMillis()}",
                            text = newCheckpointText.trim()
                        )
                        onRepairUpdated(repair.withCheckpoints(updatedCheckpoints))
                        newCheckpointText = ""
                    }
                ) {
                    Text("Dodaj")
                }
            }
        }
    }
}

@Composable
private fun RepairCheckpointRow(
    checkpoint: RepairCheckpoint,
    onCheckedChange: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("repair_checkpoint_row_${checkpoint.id}"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = checkpoint.isDone,
            onCheckedChange = onCheckedChange
        )
        Text(
            text = checkpoint.text,
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.onSurface.copy(
                alpha = if (checkpoint.isDone) 0.52f else 0.82f
            )
        )
        TextButton(
            modifier = Modifier.testTag("repair_checkpoint_edit_${checkpoint.id}"),
            onClick = onEdit
        ) {
            Text("Edytuj")
        }
        TextButton(
            modifier = Modifier.testTag("repair_checkpoint_delete_${checkpoint.id}"),
            onClick = onDelete
        ) {
            Text("Usun")
        }
    }
}

@Composable
private fun EditCheckpointDialog(
    checkpoint: RepairCheckpoint,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var textDraft by remember(checkpoint.id, checkpoint.text) {
        mutableStateOf(checkpoint.text)
    }
    val trimmedText = textDraft.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj checkpoint") },
        text = {
            GarageTextField(
                value = textDraft,
                onValueChange = { textDraft = it },
                label = "Checkpoint",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("repair_edit_checkpoint_input"),
                placeholder = "Co trzeba zrobic?"
            )
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("repair_edit_checkpoint_save_button"),
                enabled = trimmedText.isNotBlank(),
                onClick = { onSave(trimmedText) }
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun RepairPartsTab(
    repair: RepairProject,
    availableParts: List<PartInventoryItem>,
    shoppingItems: List<ShoppingListItem>,
    allShoppingItems: List<ShoppingListItem>,
    isArchivedMode: Boolean,
    onOpenShoppingList: () -> Unit,
    onOpenCatalog: () -> Unit,
    onShoppingListUpdated: (List<ShoppingListItem>) -> Unit,
    onInventoryPartAdded: (PartInventoryItem) -> Unit,
) {
    var itemPendingReceive by remember { mutableStateOf<ShoppingListItem?>(null) }
    var isAddInventoryDialogVisible by remember { mutableStateOf(false) }
    val shoppingValue = remember(shoppingItems) { shoppingItems.sumOf { repairPartValue(it.price, it.quantity) } }
    val inventoryValue = remember(availableParts) { availableParts.sumOf { repairPartValue(it.purchasePrice, it.quantity) } }
    val totalValue = shoppingValue + inventoryValue

    if (!isArchivedMode) {
        itemPendingReceive?.let { item ->
            ReceiveRepairShoppingItemDialog(
                item = item,
                onConfirm = { receivedQuantity ->
                    onInventoryPartAdded(item.toInventoryPart(nextInventoryId(availableParts), receivedQuantity))
                    onShoppingListUpdated(allShoppingItems.afterReceiving(item, receivedQuantity))
                    itemPendingReceive = null
                },
                onDismiss = { itemPendingReceive = null }
            )
        }
    }

    if (!isArchivedMode && isAddInventoryDialogVisible) {
        ExternalPartLookupDialog(
            nextId = nextInventoryId(availableParts),
            availableRepairs = listOf(repair),
            initialRepairTitle = repair.title,
            initialRepairId = repair.id,
            onDismiss = { isAddInventoryDialogVisible = false },
            onSave = { part ->
                onInventoryPartAdded(part)
                isAddInventoryDialogVisible = false
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        GaragePanel {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Wartosc czesci",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    RepairPartsMetric(
                        label = "Lista zakupow",
                        value = repairCurrencyLabel(shoppingValue)
                    )
                    RepairPartsMetric(
                        label = "Na stanie",
                        value = repairCurrencyLabel(inventoryValue)
                    )
                    RepairPartsMetric(
                        label = "Razem",
                        value = repairCurrencyLabel(totalValue),
                        emphasize = true
                    )
                }
            }
        }

        Text(
            text = "Lista zakupow",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        GaragePanel(onClick = if (isArchivedMode) null else onOpenShoppingList) {
            if (shoppingItems.isEmpty()) {
                Text(
                    text = "Brak czesci do kupienia dla tej naprawy.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            } else {
                shoppingItems.forEach { item ->
                    ShoppingPartSummaryRow(
                        item = item,
                        isArchived = isArchivedMode,
                        onReceive = if (isArchivedMode) null else ({ itemPendingReceive = item })
                    )
                }
            }
        }

        Text(
            text = "Na stanie",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        GaragePanel {
            if (availableParts.isEmpty()) {
                Text(
                    text = "Brak czesci przypisanych do tej naprawy.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            } else {
                availableParts.forEach { part ->
                    InventoryPartSummaryRow(
                        part = part,
                        neededQuantity = shoppingItems
                            .filter { item -> part.matchesShoppingItem(item) }
                            .sumOf { item -> item.quantity }
                    )
                }
            }
        }

        if (!isArchivedMode) {
            AddPartToRepairButton(
                label = "Schematy czescidobmw.pl",
                onClick = onOpenCatalog
            )
        }
    }
}

@Composable
private fun ShoppingPartSummaryRow(
    item: ShoppingListItem,
    isArchived: Boolean = false,
    onReceive: (() -> Unit)?,
) {
    PartSummaryRow(
        title = item.name,
        subtitle = item.manufacturerPartNumber.ifBlank { item.partNumber.ifBlank { item.source } },
        quantity = "${item.quantity} szt.",
        value = item.price,
        badgeText = if (isArchived) "Historia" else "▣",
        badgeColor = AccentBlue,
        photoUri = item.imageUri,
        onBadgeClick = onReceive
    )
}

@Composable
private fun InventoryPartSummaryRow(
    part: PartInventoryItem,
    neededQuantity: Int,
) {
    PartSummaryRow(
        title = part.name,
        subtitle = part.manufacturerPartNumber.ifBlank { part.partNumber },
        quantity = "${part.quantity} szt.",
        value = part.purchasePrice,
        badgeText = if (neededQuantity > 0) "${part.quantity}/$neededQuantity" else "Na stanie",
        badgeColor = if (neededQuantity > 0 && part.quantity < neededQuantity) AccentYellow else AccentGreen,
        photoUri = part.photoUri
    )
}

@Composable
private fun PartSummaryRow(
    title: String,
    subtitle: String,
    quantity: String,
    value: String,
    badgeText: String,
    badgeColor: Color,
    photoUri: String? = null,
    onBadgeClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(64.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.62f),
            shape = RoundedCornerShape(10.dp)
        ) {
            PartPhotoContent(
                photoUri = photoUri,
                height = 64.dp,
                contentScale = ContentScale.Fit
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = partSummaryTitleSize(title),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = subtitle.ifBlank { "Bez numeru czesci" },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                fontSize = 13.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = quantity,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = value.ifBlank { "Brak ceny" },
                color = MaterialTheme.colorScheme.primary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold
            )
            Surface(
                modifier = Modifier
                    .then(if (onBadgeClick != null) Modifier.clickable(onClick = onBadgeClick) else Modifier),
                color = badgeColor.copy(alpha = 0.18f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = badgeText,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    color = badgeColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AddPartToRepairButton(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.primary,
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                color = MaterialTheme.colorScheme.onPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RepairPartsMetric(
    label: String,
    value: String,
    emphasize: Boolean = false,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
            fontSize = 12.sp
        )
        Text(
            text = value,
            color = if (emphasize) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            fontSize = if (emphasize) 16.sp else 15.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

private fun repairCurrencyLabel(value: Double): String =
    if (value > 0.0) "%.2f PLN".format(java.util.Locale.US, value).replace('.', ',') else "0,00 PLN"

private fun repairPartValue(price: String, quantity: Int): Double =
    parseRepairPartPrice(price)?.times(quantity) ?: 0.0

private fun parseRepairPartPrice(value: String): Double? {
    val normalized = value
        .replace("PLN", "", ignoreCase = true)
        .replace("zl", "", ignoreCase = true)
        .replace("zł", "", ignoreCase = true)
        .replace(" ", "")
        .replace(",", ".")
    val matched = Regex("""\d+(\.\d+)?""").find(normalized)?.value ?: return null
    return matched.toDoubleOrNull()
}

private fun partSummaryTitleSize(title: String) =
    when {
        title.length > 42 -> 13.sp
        title.length > 28 -> 14.sp
        else -> 15.sp
    }

@Composable
private fun AddRepairPartDestinationDialog(
    onShoppingList: () -> Unit,
    onInventory: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj czesc") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GaragePanel(onClick = onShoppingList) {
                    Text("Lista zakupow", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Wyszukaj czesc po OEM i dodaj ja do zakupow tej naprawy.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
                GaragePanel(onClick = onInventory) {
                    Text("Magazyn", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Dodaj czesc na stan, rowniez przez skan etykiety.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
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

@Composable
private fun ReceiveRepairShoppingItemDialog(
    item: ShoppingListItem,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var quantityText by remember(item.stableId()) { mutableStateOf(item.quantity.toString()) }
    var scanPreview by remember { mutableStateOf<Bitmap?>(null) }
    var scanStatus by remember {
        mutableStateOf("Mozesz dodac czesc recznie albo zeskanowac etykiete przed przeniesieniem do magazynu.")
    }
    var scannedLabel by remember { mutableStateOf<ParsedPartLabel?>(null) }
    val quantityValue = quantityText.toIntOrNull()
    val canAddQuantity = quantityValue != null && quantityValue in 1..item.quantity

    fun recognizeBitmap(bitmap: Bitmap) {
        scanPreview = bitmap
        scanStatus = "Odczytuje etykiete..."
        recognizePartLabelFromBitmap(
            bitmap = bitmap,
            onResult = { parsedLabel ->
                scannedLabel = parsedLabel
                val values = buildList {
                    parsedLabel.oemPartNumber?.let { add("OEM: $it") }
                    parsedLabel.manufacturerPartNumber?.let { add("producent: $it") }
                    parsedLabel.manufacturer?.let { add("marka: $it") }
                }
                scanStatus = if (values.isEmpty()) {
                    "Nie udalo sie pewnie odczytac etykiety. Nadal mozesz dodac czesc recznie."
                } else {
                    "Skan odczytany: ${values.joinToString(" / ")}."
                }
            },
            onError = { message -> scanStatus = message }
        )
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            recognizeBitmap(bitmap)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Do magazynu") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(item.name, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Na liscie zakupow: ${item.quantity} szt. / OEM: ${item.partNumber}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                GaragePanel(onClick = { onConfirm(item.quantity) }) {
                    Text("Dodaj calosc", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Dodaje wszystkie sztuki z pozycji do magazynu.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
                GaragePanel {
                    Text("Dodaj ilosc", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Podaj ile sztuk dodales do magazynu.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                    GarageTextField(
                        value = quantityText,
                        onValueChange = { quantityText = it.filter { character -> character.isDigit() } },
                        label = "Ilosc",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = item.quantity.toString(),
                        keyboardType = KeyboardType.Number
                    )
                    TextButton(
                        enabled = canAddQuantity,
                        onClick = { onConfirm(quantityValue ?: 1) }
                    ) {
                        Text("Dodaj ${quantityValue ?: 0} szt.")
                    }
                }
                GaragePanel {
                    Text("Skan etykiety", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = scanStatus,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
                    )
                    scanPreview?.let { bitmap ->
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Skan etykiety czesci",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentScale = ContentScale.Crop
                        )
                    }
                    scannedLabel?.let { label ->
                        Text(
                            text = "Odczyt: ${label.oemPartNumber ?: item.partNumber}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    TextButton(onClick = { cameraLauncher.launch(null) }) {
                        Text("Zeskanuj etykiete")
                    }
                }
            }
        },
        confirmButton = {
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun RepairDocumentsTab(
    documentation: RepairDocumentation?,
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
) {
    var isChoosingAddType by remember { mutableStateOf(false) }
    var isAddingTisLink by remember { mutableStateOf(false) }
    var isAddingYoutubeLink by remember { mutableStateOf(false) }
    var isAddingPersonalLink by remember { mutableStateOf(false) }
    var selectedMediaIndex by remember { mutableStateOf<Int?>(null) }
    var tisPendingAction by remember { mutableStateOf<RepairIndexedTisLink?>(null) }
    var youtubePendingAction by remember { mutableStateOf<RepairIndexedYoutubeVideo?>(null) }
    var filePendingAction by remember { mutableStateOf<PersonalDocumentationItem?>(null) }
    var tisPendingEdit by remember { mutableStateOf<RepairIndexedTisLink?>(null) }
    var youtubePendingEdit by remember { mutableStateOf<RepairIndexedYoutubeVideo?>(null) }
    var filePendingEdit by remember { mutableStateOf<PersonalDocumentationItem?>(null) }
    val effectiveTisLinks = documentation?.effectiveTisDocuments().orEmpty()
    val files = documentation?.personalNotes.orEmpty().filter {
        it.type == PersonalDocumentationItemType.Document || it.type == PersonalDocumentationItemType.File
    }
    val media = documentation?.personalNotes.orEmpty().filter {
        it.type == PersonalDocumentationItemType.Photo || it.type == PersonalDocumentationItemType.Video
    }
    val videos = documentation?.effectiveYoutubeVideos().orEmpty()
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current

    fun updateDocumentation(transform: (RepairDocumentation) -> RepairDocumentation) {
        documentation?.let { currentDocumentation ->
            onDocumentationUpdated(transform(currentDocumentation))
        }
    }

    fun addPersonalFile(
        type: PersonalDocumentationItemType,
        uri: Uri,
    ) {
        persistReadPermission(context, uri)
        val displayName = context.displayNameForUri(uri)
        updateDocumentation { currentDocumentation ->
            currentDocumentation.copy(
                personalNotes = currentDocumentation.personalNotes + PersonalDocumentationItem(
                    id = "personal-${System.currentTimeMillis()}",
                    type = type,
                    title = displayName
                        ?: uri.lastPathSegment?.substringAfterLast('/')?.substringAfterLast(':')?.ifBlank { null }
                        ?: type.defaultDocumentationTitle(),
                    text = context.sizeLabelForUri(uri) ?: type.defaultDocumentationTitle(),
                    uri = uri.toString()
                )
            )
        }
    }

    val documentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { addPersonalFile(PersonalDocumentationItemType.Document, it) }
    }
    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { addPersonalFile(PersonalDocumentationItemType.Photo, it) }
    }
    val videoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { addPersonalFile(PersonalDocumentationItemType.Video, it) }
    }

    if (isChoosingAddType) {
        AddDocumentationItemDialog(
            onAddTisLink = {
                isChoosingAddType = false
                isAddingTisLink = true
            },
            onAddDocument = {
                isChoosingAddType = false
                documentLauncher.launch(
                    arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/plain",
                        "application/octet-stream"
                    )
                )
            },
            onAddYoutube = {
                isChoosingAddType = false
                isAddingYoutubeLink = true
            },
            onAddPhoto = {
                isChoosingAddType = false
                photoLauncher.launch(arrayOf("image/*"))
            },
            onAddVideo = {
                isChoosingAddType = false
                videoLauncher.launch(arrayOf("video/*"))
            },
            onAddLink = {
                isChoosingAddType = false
                isAddingPersonalLink = true
            },
            onDismiss = { isChoosingAddType = false }
        )
    }

    if (isAddingTisLink) {
        AddRepairTisLinkDialog(
            onDismiss = { isAddingTisLink = false },
            onSave = { link ->
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(tisDocuments = currentDocumentation.effectiveTisDocuments() + link)
                }
                isAddingTisLink = false
            }
        )
    }

    if (isAddingYoutubeLink) {
        AddRepairYoutubeDialog(
            onDismiss = { isAddingYoutubeLink = false },
            onSave = { video ->
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(youtubeVideos = currentDocumentation.effectiveYoutubeVideos() + video)
                }
                isAddingYoutubeLink = false
            }
        )
    }

    if (isAddingPersonalLink) {
        AddRepairPersonalLinkDialog(
            onDismiss = { isAddingPersonalLink = false },
            onSave = { item ->
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(personalNotes = currentDocumentation.personalNotes + item)
                }
                isAddingPersonalLink = false
            }
        )
    }

    tisPendingAction?.let { indexedLink ->
        DocumentationItemActionsDialog(
            title = indexedLink.link.title,
            onEdit = {
                tisPendingEdit = indexedLink
                tisPendingAction = null
            },
            onDelete = {
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(
                        tisDocuments = currentDocumentation.effectiveTisDocuments()
                            .filterIndexed { index, _ -> index != indexedLink.index },
                        tisLinks = emptyList()
                    )
                }
                tisPendingAction = null
            },
            onDismiss = { tisPendingAction = null }
        )
    }

    tisPendingEdit?.let { indexedLink ->
        AddRepairTisLinkDialog(
            initialLink = indexedLink.link,
            onDismiss = { tisPendingEdit = null },
            onSave = { link ->
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(
                        tisDocuments = currentDocumentation.effectiveTisDocuments()
                            .mapIndexed { index, currentLink ->
                                if (index == indexedLink.index) link else currentLink
                            },
                        tisLinks = emptyList()
                    )
                }
                tisPendingEdit = null
            }
        )
    }

    youtubePendingAction?.let { indexedVideo ->
        DocumentationItemActionsDialog(
            title = indexedVideo.video.title,
            onEdit = {
                youtubePendingEdit = indexedVideo
                youtubePendingAction = null
            },
            onDelete = {
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(
                        youtubeVideos = currentDocumentation.effectiveYoutubeVideos()
                            .filterIndexed { index, _ -> index != indexedVideo.index },
                        youtubeLinks = emptyList()
                    )
                }
                youtubePendingAction = null
            },
            onDismiss = { youtubePendingAction = null }
        )
    }

    youtubePendingEdit?.let { indexedVideo ->
        AddRepairYoutubeDialog(
            initialVideo = indexedVideo.video,
            onDismiss = { youtubePendingEdit = null },
            onSave = { video ->
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(
                        youtubeVideos = currentDocumentation.effectiveYoutubeVideos()
                            .mapIndexed { index, currentVideo ->
                                if (index == indexedVideo.index) video else currentVideo
                            },
                        youtubeLinks = emptyList()
                    )
                }
                youtubePendingEdit = null
            }
        )
    }

    filePendingAction?.let { file ->
        DocumentationItemActionsDialog(
            title = file.title,
            onEdit = {
                filePendingEdit = file
                filePendingAction = null
            },
            onDelete = {
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(
                        personalNotes = currentDocumentation.personalNotes.filterNot { it.id == file.id }
                    )
                }
                filePendingAction = null
            },
            onDismiss = { filePendingAction = null }
        )
    }

    filePendingEdit?.let { file ->
        EditDocumentationFileDialog(
            item = file,
            onDismiss = { filePendingEdit = null },
            onSave = { updatedFile ->
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(
                        personalNotes = currentDocumentation.personalNotes.map { item ->
                            if (item.id == updatedFile.id) updatedFile else item
                        }
                    )
                }
                filePendingEdit = null
            }
        )
    }

    selectedMediaIndex?.let { initialIndex ->
        PersonalMediaGalleryDialog(
            media = media,
            initialIndex = initialIndex,
            onDismiss = { selectedMediaIndex = null },
            onOpen = { item ->
                item.uri?.let { uri -> openDocumentUri(context, uri) }
            },
            onEdit = { item ->
                filePendingEdit = item
                selectedMediaIndex = null
            },
            onDelete = { item ->
                updateDocumentation { currentDocumentation ->
                    currentDocumentation.copy(
                        personalNotes = currentDocumentation.personalNotes.filterNot { it.id == item.id }
                    )
                }
                selectedMediaIndex = null
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DocumentationSectionHeader(
            title = "Linki TIS",
            onAdd = if (documentation == null) null else ({ isAddingTisLink = true })
        )
        GaragePanel {
            if (effectiveTisLinks.isEmpty()) {
                EmptyDocumentationText("Brak linkow TIS.")
            } else {
                effectiveTisLinks.forEachIndexed { index, link ->
                    DocumentationLinkRow(
                        title = link.title,
                        subtitle = "BMW TIS",
                        marker = "↗",
                        accent = AccentBlue,
                        onClick = { uriHandler.openUri(link.url.withHttpsPrefix()) },
                        onLongClick = { tisPendingAction = RepairIndexedTisLink(index, link) }
                    )
                }
            }
        }

        DocumentationSectionHeader(
            title = "Pliki i dokumenty",
            onAdd = if (documentation == null) null else ({
                documentLauncher.launch(
                    arrayOf(
                        "application/pdf",
                        "application/msword",
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                        "text/plain",
                        "application/octet-stream"
                    )
                )
            })
        )
        GaragePanel {
            if (files.isEmpty()) {
                EmptyDocumentationText("Brak plikow i dokumentow.")
            } else {
                files.forEach { file ->
                    DocumentationFileRow(
                        file = file,
                        onClick = file.uri?.let { uri -> { openDocumentUri(context, uri) } },
                        onLongClick = { filePendingAction = file }
                    )
                }
            }
        }

        DocumentationSectionHeader(
            title = "Youtube",
            onAdd = if (documentation == null) null else ({ isAddingYoutubeLink = true })
        )
        GaragePanel {
            if (videos.isEmpty()) {
                EmptyDocumentationText("Brak filmow YouTube.")
            } else {
                videos.forEachIndexed { index, video ->
                    DocumentationYoutubeRow(
                        title = video.title,
                        subtitle = video.note.ifBlank { "YouTube" },
                        videoUrl = video.url,
                        onClick = { uriHandler.openUri(video.url) },
                        onLongClick = { youtubePendingAction = RepairIndexedYoutubeVideo(index, video) }
                    )
                }
            }
        }

        DocumentationSectionHeader(
            title = "Zdjecia i filmy",
            onAdd = if (documentation == null) null else ({ isChoosingAddType = true })
        )
        if (media.isEmpty()) {
            GaragePanel {
                EmptyDocumentationText("Brak zdjec i filmow.")
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(4) { index ->
                    val item = media.getOrNull(index)
                    if (item == null) {
                        DocumentationEmptyMediaTile(Modifier.weight(1f))
                    } else {
                        DocumentationMediaTile(
                            item = item,
                            extraCount = if (index == 3 && media.size > 4) media.size - 3 else 0,
                            modifier = Modifier.weight(1f),
                            onClick = { selectedMediaIndex = index },
                            onLongClick = { filePendingAction = item }
                        )
                    }
                }
            }
        }

        GaragePanel(onClick = if (documentation == null) null else ({ isChoosingAddType = true })) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+", color = AccentBlue, fontSize = 26.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = "Dodaj",
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AddDocumentationItemDialog(
    onAddTisLink: () -> Unit,
    onAddDocument: () -> Unit,
    onAddYoutube: () -> Unit,
    onAddPhoto: () -> Unit,
    onAddVideo: () -> Unit,
    onAddLink: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj dokumentacje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AddDocumentationChoice("Link TIS", "Procedura, schemat albo strona BMW TIS.", onAddTisLink)
                AddDocumentationChoice("Plik lub dokument", "PDF, DOC, TXT albo inny plik zwiazany z naprawa.", onAddDocument)
                AddDocumentationChoice("Film YouTube", "Film instruktazowy lub diagnostyczny.", onAddYoutube)
                AddDocumentationChoice("Zdjecie", "Zdjecie elementu, pomiaru albo przebiegu pracy.", onAddPhoto)
                AddDocumentationChoice("Film", "Nagranie z telefonu przypisane do naprawy.", onAddVideo)
                AddDocumentationChoice("Link", "Dowolny link z notatka.", onAddLink)
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

@Composable
private fun AddDocumentationChoice(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    GaragePanel(onClick = onClick) {
        Text(title, fontWeight = FontWeight.SemiBold)
        Text(
            text = subtitle,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun AddRepairTisLinkDialog(
    initialLink: TisDocumentationLink? = null,
    onDismiss: () -> Unit,
    onSave: (TisDocumentationLink) -> Unit,
) {
    var title by remember(initialLink) { mutableStateOf(initialLink?.title.orEmpty()) }
    var link by remember(initialLink) { mutableStateOf(initialLink?.url.orEmpty()) }
    val normalizedLink = link.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialLink == null) "Dodaj link TIS" else "Edytuj link TIS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GarageTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Nazwa",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. Wymiana zacisku przod"
                )
                GarageTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = "Link TIS",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "https://www.newtis.info/..."
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalizedLink.isNotBlank(),
                onClick = {
                    onSave(
                        TisDocumentationLink(
                            title = title.trim().ifBlank { "Link TIS" },
                            url = normalizedLink.withHttpsPrefix()
                        )
                    )
                }
            ) {
                Text(if (initialLink == null) "Dodaj" else "Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun AddRepairYoutubeDialog(
    initialVideo: YoutubeVideo? = null,
    onDismiss: () -> Unit,
    onSave: (YoutubeVideo) -> Unit,
) {
    var title by remember(initialVideo) { mutableStateOf(initialVideo?.title.orEmpty()) }
    var link by remember(initialVideo) { mutableStateOf(initialVideo?.url.orEmpty()) }
    var note by remember(initialVideo) { mutableStateOf(initialVideo?.note.orEmpty()) }
    val normalizedLink = link.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialVideo == null) "Dodaj film YouTube" else "Edytuj film YouTube") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GarageTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Tytul",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. Front brake pads and rotor replacement"
                )
                GarageTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = "Link YouTube",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "https://www.youtube.com/watch?v=..."
                )
                GarageTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Notatka",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Co jest wazne w tym filmie?",
                    singleLine = false,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalizedLink.isNotBlank(),
                onClick = {
                    val url = normalizedLink.withHttpsPrefix()
                    onSave(
                        YoutubeVideo(
                            title = title.trim().ifBlank { "Film YouTube" },
                            url = url,
                            note = note.trim()
                        )
                    )
                }
            ) {
                Text(if (initialVideo == null) "Dodaj" else "Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun AddRepairPersonalLinkDialog(
    onDismiss: () -> Unit,
    onSave: (PersonalDocumentationItem) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    val normalizedLink = link.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GarageTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Nazwa",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. Watek na forum"
                )
                GarageTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = "Link",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "https://..."
                )
                GarageTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Notatka",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Dlaczego ten link jest przydatny?",
                    singleLine = false,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalizedLink.isNotBlank(),
                onClick = {
                    onSave(
                        PersonalDocumentationItem(
                            id = "personal-${System.currentTimeMillis()}",
                            type = PersonalDocumentationItemType.Link,
                            title = title.trim().ifBlank { "Link" },
                            text = note.trim(),
                            url = normalizedLink.withHttpsPrefix()
                        )
                    )
                }
            ) {
                Text("Dodaj")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun DocumentationItemActionsDialog(
    title: String,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GaragePanel(onClick = onEdit) {
                    Text("Edytuj", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Zmien nazwe, link albo opis tej pozycji.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 13.sp
                    )
                }
                GaragePanel(onClick = onDelete) {
                    Text("Usun", fontWeight = FontWeight.SemiBold, color = AccentRed)
                    Text(
                        text = "Usuwa pozycje z dokumentacji tej naprawy.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 13.sp
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

@Composable
private fun EditDocumentationFileDialog(
    item: PersonalDocumentationItem,
    onDismiss: () -> Unit,
    onSave: (PersonalDocumentationItem) -> Unit,
) {
    var title by remember(item) { mutableStateOf(item.title) }
    var note by remember(item) { mutableStateOf(item.text) }
    val canSave = title.trim().isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edytuj pozycje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GarageTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Nazwa",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Nazwa dokumentu"
                )
                GarageTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Opis",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "Rozmiar, zrodlo albo notatka",
                    singleLine = false,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        item.copy(
                            title = title.trim(),
                            text = note.trim()
                        )
                    )
                }
            ) {
                Text("Zapisz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Anuluj")
            }
        }
    )
}

@Composable
private fun DocumentationSectionHeader(
    title: String,
    onAdd: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            color = MaterialTheme.colorScheme.onBackground,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        if (onAdd != null) {
            Surface(
                modifier = Modifier
                    .size(34.dp)
                    .clickable(onClick = onAdd),
                color = AccentBlue.copy(alpha = 0.16f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "+",
                        color = AccentBlue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyDocumentationText(text: String) {
    Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
        fontSize = 14.sp
    )
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DocumentationLinkRow(
    title: String,
    subtitle: String,
    marker: String,
    accent: androidx.compose.ui.graphics.Color,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(32.dp),
            color = accent.copy(alpha = 0.12f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("▱", color = accent, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Text(
            text = marker,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DocumentationYoutubeRow(
    title: String,
    subtitle: String,
    videoUrl: String = "",
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    val videoId = remember(videoUrl) { videoUrl.youtubeVideoId() }
    val thumbnailUrl = remember(videoId) {
        videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
    }
    val titleSize = when {
        title.length > 72 -> 14.sp
        title.length > 46 -> 15.sp
        else -> 16.sp
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(108.dp)
                .height(68.dp)
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF263746), Color(0xFF101922))
                    ),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            YoutubeThumbnail(thumbnailUrl = thumbnailUrl, modifier = Modifier.fillMaxSize())
            Surface(
                color = AccentRed.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = ">",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = titleSize,
                lineHeight = (titleSize.value + 4).sp,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                subtitle,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Text(
            text = "↗",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun YoutubeThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, thumbnailUrl) {
        value = thumbnailUrl?.let { loadBitmapFromUrl(it) }
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Miniatura YouTube",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Text(
                text = "YT",
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DocumentationFileRow(
    file: PersonalDocumentationItem,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null || onLongClick != null) {
                    Modifier.combinedClickable(
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick
                    )
                } else {
                    Modifier
                }
            ),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            color = AccentRed.copy(alpha = 0.18f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(file.documentBadge(), color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(file.title, fontWeight = FontWeight.SemiBold, maxLines = 2)
            Text(
                file.text.ifBlank { "Dokument" },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Text("↗", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 22.sp)
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun DocumentationMediaTile(
    item: PersonalDocumentationItem,
    extraCount: Int,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    onLongClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri, item.type) {
        value = when (item.type) {
            PersonalDocumentationItemType.Photo -> item.uri?.let {
                withContext(Dispatchers.IO) { loadBitmapFromUri(context, Uri.parse(it)) }
            }
            PersonalDocumentationItemType.Video -> item.uri?.let {
                loadVideoThumbnail(context, Uri.parse(it))
            }
            else -> null
        }
    }

    Box(
        modifier = modifier
            .height(82.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2B3C46), Color(0xFF0E1821))
                ),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = item.title,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        if (extraCount > 0) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$extraCount",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (item.type == PersonalDocumentationItemType.Video && bitmap != null) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
                shape = CircleShape
            ) {
                Text(
                    text = ">",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
        } else if (bitmap == null) {
            Text(
                text = if (item.type == PersonalDocumentationItemType.Video) "FILM" else "IMG",
                color = AccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Unit
        }
    }
}

@Composable
private fun DocumentationEmptyMediaTile(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(82.dp)
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.52f), RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "+",
            color = AccentBlue.copy(alpha = 0.72f),
            fontSize = 22.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun RepairTorqueTab(
    vehicle: Vehicle,
    repair: RepairProject,
    documentation: RepairDocumentation?,
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
) {
    val context = LocalContext.current
    var mode by rememberSaveable(repair.id) { mutableStateOf("Lista") }
    var selectedTorqueIndex by rememberSaveable(repair.id) { mutableStateOf(0) }
    val tables = documentation?.effectiveTorqueTables().orEmpty()
    var selectedTableId by rememberSaveable(documentation?.repairId, tables.map { it.id }) {
        mutableStateOf(tables.firstOrNull()?.id)
    }
    val activeTable = tables.firstOrNull { it.id == selectedTableId } ?: tables.firstOrNull()
    var isChoosingTorqueAddType by rememberSaveable(repair.id) { mutableStateOf(false) }
    var isAddingTorqueSpecManually by rememberSaveable(repair.id) { mutableStateOf(false) }
    var torqueImportStatus by rememberSaveable(repair.id) { mutableStateOf<String?>(null) }
    var tablePendingDeleteId by rememberSaveable(repair.id) { mutableStateOf<String?>(null) }
    val tablePendingDelete = remember(tablePendingDeleteId, tables) {
        tablePendingDeleteId?.let { tableId -> tables.firstOrNull { it.id == tableId } }
    }
    val specs = activeTable?.torqueSpecs.orEmpty()

    LaunchedEffect(activeTable?.id) {
        if (selectedTableId == null && activeTable != null) {
            selectedTableId = activeTable.id
        }
    }

    fun baseDocumentation(): RepairDocumentation =
        documentation ?: RepairDocumentation(
            title = "Dokumentacja: ${repair.title}",
            area = repair.area,
            repairTitle = repair.title,
            repairId = repair.id,
            summary = "Dokumentacja powiazana z naprawa: ${repair.title}."
        )

    fun ensureActiveTable(currentTables: List<TorqueSpecTable>): Pair<List<TorqueSpecTable>, TorqueSpecTable> {
        val selectedTable = currentTables.firstOrNull { it.id == selectedTableId } ?: currentTables.firstOrNull()
        if (selectedTable != null) return currentTables to selectedTable
        val newTable = TorqueSpecTable(
            id = "table-${System.currentTimeMillis()}",
            title = "Tabela momentow 1"
        )
        selectedTableId = newTable.id
        return listOf(newTable) to newTable
    }

    fun updateTorqueTables(updatedTables: List<TorqueSpecTable>) {
        val cleanedTables = updatedTables.map { it.withoutEmptyTorqueSpecs() }
        val updatedDocumentation = baseDocumentation().copy(
            torqueTables = cleanedTables,
            torqueSpecs = cleanedTables.firstOrNull()?.torqueSpecs.orEmpty(),
            torqueDiagramImageUri = cleanedTables.firstOrNull()?.diagramImageUri,
            torqueDiagramAssignments = cleanedTables.firstOrNull()?.diagramAssignments.orEmpty()
        )
        onDocumentationUpdated(updatedDocumentation)
    }

    fun updateActiveTorqueTable(transform: (TorqueSpecTable) -> TorqueSpecTable) {
        val (currentTables, currentTable) = ensureActiveTable(baseDocumentation().effectiveTorqueTables())
        updateTorqueTables(
            currentTables.map { table ->
                if (table.id == currentTable.id) transform(table) else table
            }
        )
    }

    fun importTorqueScreenshot(uri: Uri) {
        val bitmap = loadDocumentationBitmapFromUri(context, uri)
        if (bitmap == null) {
            torqueImportStatus = "Nie udalo sie wczytac screenshotu."
            return
        }
        torqueImportStatus = "Odczytuje screenshot TIS..."
        recognizeTorqueSpecsFromBitmap(
            bitmap = bitmap,
            onResult = { importedSpecs ->
                val cleanedImportedSpecs = importedSpecs.filter { it.isUsableTorqueSpec() }
                if (cleanedImportedSpecs.isEmpty()) {
                    torqueImportStatus = "Nie udalo sie rozpoznac wierszy tabeli. Sprobuj przyciac screenshot blizej tabeli."
                } else {
                    var addedCount = 0
                    updateActiveTorqueTable { table ->
                        val mergedSpecs = table.torqueSpecs.replaceOcrTorqueSpecs(cleanedImportedSpecs)
                        addedCount = mergedSpecs.size - table.torqueSpecs.size
                        table.copy(torqueSpecs = mergedSpecs)
                    }
                    torqueImportStatus = if (addedCount == 0) {
                        "OCR rozpoznal wpisy, ale wszystkie byly juz w tej tabeli."
                    } else {
                        "Dodano $addedCount wpisow do aktualnej tabeli."
                    }
                }
            },
            onError = { message -> torqueImportStatus = message }
        )
    }

    val torqueScreenshotLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            importTorqueScreenshot(uri)
        }
    }

    if (isChoosingTorqueAddType) {
        AddTorqueDataChoiceDialog(
            tableTitle = activeTable?.title ?: "Tabela momentow 1",
            onManual = {
                isChoosingTorqueAddType = false
                isAddingTorqueSpecManually = true
            },
            onScreenshot = {
                isChoosingTorqueAddType = false
                torqueScreenshotLauncher.launch("image/*")
            },
            onDismiss = { isChoosingTorqueAddType = false }
        )
    }

    if (isAddingTorqueSpecManually) {
        AddTorqueSpecDialog(
            onDismiss = { isAddingTorqueSpecManually = false },
            onSave = { spec ->
                updateActiveTorqueTable { table ->
                    table.copy(torqueSpecs = table.torqueSpecs + spec)
                }
                torqueImportStatus = "Dodano reczny wpis do aktualnej tabeli."
                isAddingTorqueSpecManually = false
            }
        )
    }

    tablePendingDelete?.let { table ->
        AlertDialog(
            onDismissRequest = { tablePendingDeleteId = null },
            title = { Text("Usun schemat momentow?") },
            text = {
                Text(
                    text = "Schemat \"${table.title}\" oraz jego tabela momentow zostana usuniete z tej naprawy."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedTables = tables.filterNot { it.id == table.id }
                        selectedTableId = updatedTables.firstOrNull()?.id
                        selectedTorqueIndex = 0
                        updateTorqueTables(updatedTables)
                        torqueImportStatus = "Usunieto schemat momentow."
                        tablePendingDeleteId = null
                    }
                ) {
                    Text("Usun")
                }
            },
            dismissButton = {
                TextButton(onClick = { tablePendingDeleteId = null }) {
                    Text("Anuluj")
                }
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentTabs(
            tabs = listOf("Lista", "Szczegoly"),
            selectedTab = mode,
            onSelect = { mode = it }
        )
        if (tables.isNotEmpty()) {
            TorqueTableSelector(
                tables = tables,
                selectedTableId = activeTable?.id,
                onSelect = {
                    selectedTableId = it.id
                    selectedTorqueIndex = 0
                },
                onDelete = { tablePendingDeleteId = it.id }
            )
        }
        if (mode == "Lista") {
            TorqueDiagramListView(
                vehicle = vehicle,
                table = activeTable,
                specs = specs,
                selectedTorqueIndex = selectedTorqueIndex.coerceIn(0, (specs.size - 1).coerceAtLeast(0)),
                onSelectTorque = { selectedTorqueIndex = it },
                onAssignmentAdded = { assignment ->
                    updateActiveTorqueTable { table ->
                        table.copy(diagramAssignments = table.diagramAssignments.upsertAssignment(assignment))
                    }
                },
                onAssignmentRemoved = { torqueSpecIndex ->
                    updateActiveTorqueTable { table ->
                        table.copy(
                            diagramAssignments = table.diagramAssignments
                                .filterNot { it.torqueSpecIndex == torqueSpecIndex }
                        )
                    }
                }
            )
        } else {
            TorqueDetailsTable(
                vehicle = vehicle,
                specs = specs,
                onSpecUpdated = { index, updatedSpec ->
                    updateActiveTorqueTable { table ->
                        table.copy(
                            torqueSpecs = table.torqueSpecs.mapIndexed { specIndex, spec ->
                                if (specIndex == index) updatedSpec else spec
                            }
                        )
                    }
                    torqueImportStatus = "Zapisano zmiany w rekordzie."
                },
                onSpecDeleted = { index ->
                    updateActiveTorqueTable { table ->
                        table.copy(
                            torqueSpecs = table.torqueSpecs.filterIndexed { specIndex, _ -> specIndex != index },
                            diagramAssignments = table.diagramAssignments.afterTorqueSpecRemoved(index)
                        )
                    }
                    selectedTorqueIndex = selectedTorqueIndex.coerceAtMost((specs.size - 2).coerceAtLeast(0))
                    torqueImportStatus = "Usunieto rekord z aktualnej tabeli."
                }
            )
        }
        torqueImportStatus?.let { status ->
            GaragePanel {
                Text(status, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            }
        }
        GaragePanel(onClick = if (mode == "Szczegoly") ({ isChoosingTorqueAddType = true }) else null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+", color = AccentBlue, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = if (mode == "Lista") {
                        "Dodaj schemat z zakladki Czesci przez dlugie przytrzymanie obrazu schematu."
                    } else {
                        "Dodaj dane do tabeli: recznie albo ze screenshota TIS"
                    },
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun AddTorqueDataChoiceDialog(
    tableTitle: String,
    onManual: () -> Unit,
    onScreenshot: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj dane do tabeli") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = tableTitle,
                    fontWeight = FontWeight.SemiBold
                )
                GaragePanel(onClick = onManual) {
                    Text("Dodaj recznie", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Wpisz element, moment, gwint, zrodlo i notatki dla aktualnego schematu.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
                GaragePanel(onClick = onScreenshot) {
                    Text("Import ze screenshota", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Wybierz screenshot tabeli TIS. Wpisy trafia tylko do tej tabeli momentow.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
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

@Composable
private fun TorqueTableSelector(
    tables: List<TorqueSpecTable>,
    selectedTableId: String?,
    onSelect: (TorqueSpecTable) -> Unit,
    onDelete: (TorqueSpecTable) -> Unit,
) {
    GaragePanel {
        Text("Schematy momentow", fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            tables.forEachIndexed { index, table ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(table) },
                    color = if (table.id == selectedTableId) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                    } else {
                        MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                    },
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(table.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text(
                                text = "${table.torqueSpecs.size} momentow",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                fontSize = 12.sp
                            )
                        }
                        TextButton(onClick = { onDelete(table) }) {
                            Text("Usun")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TorqueDiagramListView(
    vehicle: Vehicle,
    table: TorqueSpecTable?,
    specs: List<TorqueSpec>,
    selectedTorqueIndex: Int,
    onSelectTorque: (Int) -> Unit,
    onAssignmentAdded: (TorqueDiagramAssignment) -> Unit,
    onAssignmentRemoved: (Int) -> Unit,
) {
    var isDiagramActionVisible by remember { mutableStateOf(false) }
    var isDiagramEditVisible by remember { mutableStateOf(false) }
    val assignments = table?.diagramAssignments.orEmpty()
        .filter { it.torqueSpecIndex in specs.indices }
    val selectedSpec = specs.getOrNull(selectedTorqueIndex)
    val vehicleCodes = remember(vehicle) { vehicle.torqueModelCodes() }
    val allDetailRows = selectedSpec?.displayDetailRows().orEmpty()
    val vehicleRows = allDetailRows
        .filter { row -> vehicleCodes.any { code -> row.type.contains(code, ignoreCase = true) } }

    if (isDiagramActionVisible) {
        AlertDialog(
            onDismissRequest = { isDiagramActionVisible = false },
            title = { Text("Schemat momentow") },
            text = {
                GaragePanel(onClick = {
                    isDiagramActionVisible = false
                    isDiagramEditVisible = true
                }) {
                    Text("Nanies momenty", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Wybierz rekord i kliknij miejsce na schemacie.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { isDiagramActionVisible = false }) {
                    Text("Anuluj")
                }
            }
        )
    }

    if (isDiagramEditVisible) {
        TorqueDiagramEditDialog(
            imageUri = table?.diagramImageUri,
            specs = specs,
            assignments = assignments,
            selectedTorqueIndex = selectedTorqueIndex,
            onSelectTorque = onSelectTorque,
            onAssignmentAdded = onAssignmentAdded,
            onAssignmentRemoved = onAssignmentRemoved,
            onDismiss = { isDiagramEditVisible = false }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TorqueDiagramPreview(
            imageUri = table?.diagramImageUri,
            assignments = assignments,
            specs = specs,
            selectedTorqueIndex = selectedTorqueIndex,
            onSelectTorque = onSelectTorque,
            onLongClick = { isDiagramActionVisible = true }
        )

        if (specs.isEmpty()) {
            GaragePanel {
                EmptyDocumentationText("Brak zapisanych momentow dla tej naprawy.")
            }
        } else {
            specs.forEachIndexed { index, spec ->
                TorquePointRow(
                    index = index,
                    spec = spec,
                    selected = index == selectedTorqueIndex,
                    onClick = { onSelectTorque(index) }
                )
            }
        }

        GaragePanel {
            Text("Informacja", fontWeight = FontWeight.SemiBold)
            if (selectedSpec == null) {
                Text(
                    text = "Momenty dokrecania zgodnie z TIS. Zawsze sprawdzaj aktualne dane techniczne.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            } else if (vehicleRows.isNotEmpty()) {
                Text(
                    text = "Pasujace do auta: ${vehicleCodes.joinToString(", ")}",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
                TorqueSpecDetailsGrid(rows = vehicleRows, vehicleCodes = vehicleCodes)
            } else {
                Text(
                    text = "Brak wariantu dopasowanego do modelu auta. Ponizej pelne dane rekordu.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                TorqueSpecDetailsGrid(rows = allDetailRows, vehicleCodes = vehicleCodes)
            }
        }
    }
}

@Composable
private fun TorqueDiagramPreview(
    imageUri: String?,
    assignments: List<TorqueDiagramAssignment>,
    specs: List<TorqueSpec>,
    selectedTorqueIndex: Int,
    onSelectTorque: (Int) -> Unit,
    onLongClick: () -> Unit = {},
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUri) {
        value = imageUri?.let { loadBitmapFromDiagramSource(context, it) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(8.dp))
            .combinedClickable(
                onClick = {},
                onLongClick = onLongClick
            )
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        val diagramWidth = maxWidth
        val diagramHeight = maxHeight
        val diagramBitmap = bitmap
        if (diagramBitmap != null) {
            Image(
                bitmap = diagramBitmap.asImageBitmap(),
                contentDescription = "Schemat momentow dokrecania",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {
            TorquePlaceholderDiagram()
        }

        assignments.forEach { assignment ->
            val spec = specs.getOrNull(assignment.torqueSpecIndex)
            val selected = assignment.torqueSpecIndex == selectedTorqueIndex
            Surface(
                modifier = Modifier
                    .offset(
                        x = (diagramWidth * assignment.xRatio) - 14.dp,
                        y = (diagramHeight * assignment.yRatio) - 14.dp
                    )
                    .size(28.dp)
                    .clickable { onSelectTorque(assignment.torqueSpecIndex) },
                color = if (selected) AccentBlue else AccentBlue.copy(alpha = 0.82f),
                shape = RoundedCornerShape(50)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${assignment.torqueSpecIndex + 1}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            if (selected && spec != null) {
                Text(
                    text = spec.torque,
                    modifier = Modifier
                        .offset(
                            x = (diagramWidth * assignment.xRatio) + 14.dp,
                            y = (diagramHeight * assignment.yRatio) - 12.dp
                        )
                        .background(Color.Black.copy(alpha = 0.42f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    color = AccentBlue,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun TorqueDiagramEditDialog(
    imageUri: String?,
    specs: List<TorqueSpec>,
    assignments: List<TorqueDiagramAssignment>,
    selectedTorqueIndex: Int,
    onSelectTorque: (Int) -> Unit,
    onAssignmentAdded: (TorqueDiagramAssignment) -> Unit,
    onAssignmentRemoved: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUri) {
        value = imageUri?.let { loadBitmapFromDiagramSource(context, it) }
    }
    val selectedSpec = specs.getOrNull(selectedTorqueIndex)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Nanies momenty") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Wybierz rekord i kliknij miejsce na schemacie.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                TorqueSpecSelector(
                    specs = specs,
                    selectedTorqueIndex = selectedTorqueIndex,
                    onSelectTorque = onSelectTorque
                )
                val diagramBitmap = bitmap
                if (diagramBitmap == null) {
                    EmptyDocumentationText("Brak obrazu schematu do edycji.")
                } else {
                    EditableTorqueDiagramImage(
                        bitmap = diagramBitmap,
                        assignments = assignments,
                        specs = specs,
                        selectedTorqueIndex = selectedTorqueIndex,
                        onTap = { tap ->
                            if (selectedTorqueIndex in specs.indices) {
                                onAssignmentAdded(
                                    TorqueDiagramAssignment(
                                        torqueSpecIndex = selectedTorqueIndex,
                                        xRatio = tap.x.coerceIn(0f, 1f),
                                        yRatio = tap.y.coerceIn(0f, 1f)
                                    )
                                )
                            }
                        }
                    )
                }
                selectedSpec?.let { spec ->
                    Text(
                        text = "Aktywny rekord: ${spec.component} / ${spec.torque}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                assignments
                    .filter { it.torqueSpecIndex in specs.indices }
                    .sortedBy { it.torqueSpecIndex }
                    .let { savedAssignments ->
                        if (savedAssignments.isEmpty()) {
                            Text(
                                text = "Brak naniesionych punktow dla tego schematu.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                fontSize = 12.sp
                            )
                        } else {
                            savedAssignments.forEach { assignment ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${assignment.torqueSpecIndex + 1}. ${specs[assignment.torqueSpecIndex].component}",
                                        modifier = Modifier.weight(1f),
                                        fontSize = 12.sp,
                                        maxLines = 1
                                    )
                                    TextButton(onClick = { onAssignmentRemoved(assignment.torqueSpecIndex) }) {
                                        Text("Usun")
                                    }
                                }
                            }
                        }
                    }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zakoncz")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}

@Composable
private fun TorqueSpecSelector(
    specs: List<TorqueSpec>,
    selectedTorqueIndex: Int,
    onSelectTorque: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        specs.forEachIndexed { index, spec ->
            val selected = index == selectedTorqueIndex
            Surface(
                modifier = Modifier.clickable { onSelectTorque(index) },
                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(190.dp)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text("${index + 1}. ${spec.component}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, maxLines = 2)
                    Text(spec.torque, color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

@Composable
private fun EditableTorqueDiagramImage(
    bitmap: Bitmap,
    assignments: List<TorqueDiagramAssignment>,
    specs: List<TorqueSpec>,
    selectedTorqueIndex: Int,
    onTap: (Offset) -> Unit,
) {
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White)
            .pointerInput(selectedTorqueIndex, bitmap) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val height = size.height.toFloat().coerceAtLeast(1f)
                    onTap(Offset(offset.x / width, offset.y / height))
                }
            }
    ) {
        val diagramWidth = maxWidth
        val diagramHeight = diagramWidth / aspectRatio
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(diagramHeight)
        ) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Schemat momentow dokrecania",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )
            assignments
                .filter { it.torqueSpecIndex in specs.indices }
                .forEach { assignment ->
                    val selected = assignment.torqueSpecIndex == selectedTorqueIndex
                    Surface(
                        modifier = Modifier
                            .offset(
                                x = (diagramWidth * assignment.xRatio) - 14.dp,
                                y = (diagramHeight * assignment.yRatio) - 14.dp
                            )
                            .size(if (selected) 32.dp else 28.dp),
                        color = if (selected) AccentBlue else AccentBlue.copy(alpha = 0.82f),
                        shape = CircleShape
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "${assignment.torqueSpecIndex + 1}",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
        }
    }
}

@Composable
private fun TorquePlaceholderDiagram() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        val lineColor = Color.White.copy(alpha = 0.42f)
        val thin = 3f
        val path = Path().apply {
            moveTo(w * 0.12f, h * 0.62f)
            cubicTo(w * 0.18f, h * 0.28f, w * 0.42f, h * 0.20f, w * 0.68f, h * 0.28f)
            cubicTo(w * 0.86f, h * 0.34f, w * 0.90f, h * 0.58f, w * 0.76f, h * 0.72f)
            cubicTo(w * 0.55f, h * 0.88f, w * 0.24f, h * 0.82f, w * 0.12f, h * 0.62f)
        }
        drawPath(path, lineColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = thin))
        drawCircle(lineColor, radius = w * 0.13f, center = Offset(w * 0.48f, h * 0.50f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = thin))
        drawCircle(lineColor, radius = w * 0.08f, center = Offset(w * 0.64f, h * 0.57f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = thin))
        drawLine(lineColor, Offset(w * 0.22f, h * 0.42f), Offset(w * 0.84f, h * 0.42f), strokeWidth = thin)
        drawLine(lineColor, Offset(w * 0.20f, h * 0.70f), Offset(w * 0.72f, h * 0.28f), strokeWidth = thin)
        drawLine(lineColor, Offset(w * 0.35f, h * 0.82f), Offset(w * 0.82f, h * 0.54f), strokeWidth = thin)
    }
}

@Composable
private fun TorquePointRow(
    index: Int,
    spec: TorqueSpec,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 1.dp else 0.dp,
                color = if (selected) AccentBlue else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(26.dp),
                color = AccentBlue.copy(alpha = if (selected) 0.95f else 0.32f),
                shape = RoundedCornerShape(50)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${index + 1}",
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text(
                text = spec.component,
                modifier = Modifier.weight(1f),
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = spec.torque,
                color = AccentBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun TorqueDetailsTable(
    vehicle: Vehicle,
    specs: List<TorqueSpec>,
    onSpecUpdated: (Int, TorqueSpec) -> Unit,
    onSpecDeleted: (Int) -> Unit,
) {
    var specPendingDetails by remember { mutableStateOf<RepairTorqueSpecEditTarget?>(null) }
    var specPendingEdit by remember { mutableStateOf<RepairTorqueSpecEditTarget?>(null) }

    specPendingDetails?.let { target ->
        TorqueSpecDetailsDialog(
            vehicle = vehicle,
            spec = target.spec,
            onDismiss = { specPendingDetails = null },
            onEdit = {
                specPendingDetails = null
                specPendingEdit = target
            }
        )
    }

    specPendingEdit?.let { target ->
        AddTorqueSpecDialog(
            initialSpec = target.spec,
            onDismiss = { specPendingEdit = null },
            onSave = { updatedSpec ->
                onSpecUpdated(target.index, updatedSpec)
                specPendingEdit = null
            },
            onDelete = {
                onSpecDeleted(target.index)
                specPendingEdit = null
            }
        )
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GaragePanel {
            Text("Pelna tabela dokrecen", fontWeight = FontWeight.SemiBold)
            if (specs.isEmpty()) {
                EmptyDocumentationText("Brak zapisanych momentow.")
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "ID",
                        modifier = Modifier.width(34.dp),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Nazwa elementu",
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Moment",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                specs.forEachIndexed { index, spec ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { specPendingDetails = RepairTorqueSpecEditTarget(index, spec) }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${index + 1}",
                            modifier = Modifier.width(34.dp),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = spec.component,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2
                        )
                        Text(
                            text = spec.torque,
                            color = AccentBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TorqueSpecDetailsDialog(
    vehicle: Vehicle,
    spec: TorqueSpec,
    onDismiss: () -> Unit,
    onEdit: (() -> Unit)? = null,
) {
    val detailRows = remember(spec) { spec.displayDetailRows() }
    val vehicleCodes = remember(vehicle) { vehicle.torqueModelCodes() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(spec.component) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                TorqueSpecDetailLine("Moment", spec.torque)
                TorqueSpecDetailsGrid(
                    rows = detailRows,
                    vehicleCodes = vehicleCodes
                )
                TorqueSpecDetailLine("Zrodlo", spec.source)
                val plainNotes = spec.notes.takeUnless { it.startsWith("OCR_ROWS\n") }.orEmpty()
                TorqueSpecDetailLine("Uwagi", plainNotes)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        },
        dismissButton = {
            if (onEdit != null) {
                TextButton(onClick = onEdit) {
                    Text("Edytuj")
                }
            }
        }
    )
}

@Composable
private fun TorqueSpecDetailsGrid(
    rows: List<TorqueDetailDisplayRow>,
    vehicleCodes: Set<String>,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            TorqueGridHeader("Typ", Modifier.weight(1.1f))
            TorqueGridHeader("Gwint", Modifier.weight(0.72f))
            TorqueGridHeader("Specyfikacja", Modifier.weight(1.25f))
            TorqueGridHeader("Moment", Modifier.weight(0.72f))
        }
        rows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.background.copy(alpha = 0.34f),
                        RoundedCornerShape(8.dp)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                TorqueGridCell(
                    text = row.type,
                    modifier = Modifier.weight(1.1f),
                    highlightedCodes = vehicleCodes
                )
                TorqueGridCell(row.thread, Modifier.weight(0.72f))
                TorqueGridCell(row.tighteningSpecifications, Modifier.weight(1.25f))
                TorqueGridCell(row.torque, Modifier.weight(0.72f), emphasize = true)
            }
        }
    }
}

@Composable
private fun TorqueGridHeader(
    text: String,
    modifier: Modifier,
) {
    Text(
        text = text,
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun TorqueGridCell(
    text: String,
    modifier: Modifier,
    highlightedCodes: Set<String> = emptySet(),
    emphasize: Boolean = false,
) {
    val value = text.ifBlank { "-" }
    val highlightRanges = highlightedCodes
        .flatMap { code ->
            Regex("\\b${Regex.escape(code)}\\b", RegexOption.IGNORE_CASE)
                .findAll(value)
                .map { it.range }
                .toList()
        }
        .sortedBy { it.first }
    val styledText = buildAnnotatedString {
        var cursor = 0
        highlightRanges.forEach { range ->
            if (range.first > cursor) {
                append(value.substring(cursor, range.first))
            }
            withStyle(
                SpanStyle(
                    color = Color.White,
                    background = AccentBlue.copy(alpha = 0.42f),
                    fontWeight = FontWeight.Bold
                )
            ) {
                append(value.substring(range.first, range.last + 1))
            }
            cursor = range.last + 1
        }
        if (cursor < value.length) {
            append(value.substring(cursor))
        }
    }
    Text(
        text = styledText,
        modifier = modifier,
        color = if (emphasize) AccentBlue else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f),
        fontSize = 12.sp,
        fontWeight = if (emphasize) FontWeight.SemiBold else FontWeight.Normal,
        lineHeight = 15.sp
    )
}

@Composable
private fun TorqueSpecDetailLine(
    label: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.primary,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = value.ifBlank { "-" },
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.76f)
        )
    }
}

private data class TorqueDetailDisplayRow(
    val type: String,
    val thread: String,
    val tighteningSpecifications: String,
    val torque: String,
)

private data class RepairTorqueSpecEditTarget(
    val index: Int,
    val spec: TorqueSpec,
)

private fun TorqueSpec.displayDetailRows(): List<TorqueDetailDisplayRow> =
    ocrDetailRows().ifEmpty {
        listOf(
            TorqueDetailDisplayRow(
                type = type,
                thread = thread,
                tighteningSpecifications = tighteningSpecifications,
                torque = torque
            )
        )
    }

private fun TorqueSpec.ocrDetailRows(): List<TorqueDetailDisplayRow> {
    if (!notes.startsWith("OCR_ROWS\n")) return emptyList()
    return runCatching {
        val array = org.json.JSONArray(notes.removePrefix("OCR_ROWS\n"))
        buildList {
            for (index in 0 until array.length()) {
                val item = array.optJSONObject(index) ?: continue
                add(
                    TorqueDetailDisplayRow(
                        type = item.optString("type"),
                        thread = item.optString("thread"),
                        tighteningSpecifications = item.optString("tighteningSpecifications"),
                        torque = item.optString("torque")
                    )
                )
            }
        }
    }.getOrDefault(emptyList())
}

private fun Vehicle.torqueModelCodes(): Set<String> =
    listOf(displayName, model, generation)
        .flatMap { source ->
            Regex("\\b[EGF]\\d{2}\\b", RegexOption.IGNORE_CASE)
                .findAll(source)
                .map { it.value.uppercase() }
                .toList()
        }
        .toSet()

private fun List<TorqueDiagramAssignment>.upsertAssignment(
    assignment: TorqueDiagramAssignment,
): List<TorqueDiagramAssignment> =
    filterNot { it.torqueSpecIndex == assignment.torqueSpecIndex } + assignment

private fun List<TorqueDiagramAssignment>.afterTorqueSpecRemoved(
    removedIndex: Int,
): List<TorqueDiagramAssignment> =
    mapNotNull { assignment ->
        when {
            assignment.torqueSpecIndex == removedIndex -> null
            assignment.torqueSpecIndex > removedIndex -> assignment.copy(
                torqueSpecIndex = assignment.torqueSpecIndex - 1
            )
            else -> assignment
        }
    }

private fun TorqueSpecTable.withoutEmptyTorqueSpecs(): TorqueSpecTable {
    val indexMap = mutableMapOf<Int, Int>()
    val cleanedSpecs = torqueSpecs.mapIndexedNotNull { oldIndex, spec ->
        if (spec.isUsableTorqueSpec()) {
            indexMap[oldIndex] = indexMap.size
            spec
        } else {
            null
        }
    }
    val cleanedAssignments = diagramAssignments.mapNotNull { assignment ->
        val newIndex = indexMap[assignment.torqueSpecIndex] ?: return@mapNotNull null
        assignment.copy(torqueSpecIndex = newIndex)
    }
    return copy(
        torqueSpecs = cleanedSpecs,
        diagramAssignments = cleanedAssignments
    )
}

private fun TorqueSpec.isUsableTorqueSpec(): Boolean =
    component.trim().isNotBlank() &&
        torque.trim().isNotBlank() &&
        component.trim() != "-" &&
        torque.trim() != "-"

private fun defaultTorqueAssignments(count: Int): List<TorqueDiagramAssignment> {
    val positions = listOf(
        Offset(0.22f, 0.28f),
        Offset(0.76f, 0.54f),
        Offset(0.35f, 0.66f),
        Offset(0.58f, 0.31f),
        Offset(0.68f, 0.72f),
        Offset(0.46f, 0.46f)
    )
    return positions.take(count).mapIndexed { index, offset ->
        TorqueDiagramAssignment(
            torqueSpecIndex = index,
            xRatio = offset.x,
            yRatio = offset.y
        )
    }
}

private fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap? =
    runCatching {
        context.contentResolver.openInputStream(uri)?.use(BitmapFactory::decodeStream)
    }.getOrNull()

private suspend fun loadBitmapFromDiagramSource(context: Context, source: String): Bitmap? =
    if (source.startsWith("http://") || source.startsWith("https://")) {
        loadBitmapFromUrl(source)
    } else {
        withContext(Dispatchers.IO) {
            loadBitmapFromUri(context, Uri.parse(source))
        }
    }

@Composable
private fun PersonalMediaGalleryDialog(
    media: List<PersonalDocumentationItem>,
    initialIndex: Int,
    onDismiss: () -> Unit,
    onOpen: (PersonalDocumentationItem) -> Unit,
    onEdit: (PersonalDocumentationItem) -> Unit,
    onDelete: (PersonalDocumentationItem) -> Unit,
) {
    BackHandler(onBack = onDismiss)
    val galleryScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = initialIndex.coerceIn(0, (media.size - 1).coerceAtLeast(0)),
        pageCount = { media.size }
    )
    var currentScale by remember { mutableStateOf(1f) }
    val item = media.getOrNull(pagerState.currentPage) ?: return
    LaunchedEffect(pagerState.currentPage) {
        currentScale = 1f
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize(),
                    userScrollEnabled = currentScale <= 1.01f
                ) { page ->
                    media.getOrNull(page)?.let { pageItem ->
                        ZoomableGalleryMedia(
                            item = pageItem,
                            isActive = page == pagerState.currentPage,
                            onScaleChanged = { scale -> currentScale = scale }
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.54f))
                        .padding(horizontal = 12.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("<", color = Color.White, fontSize = 24.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1}/${media.size}",
                            color = Color.White.copy(alpha = 0.68f),
                            fontSize = 12.sp
                        )
                    }
                    TextButton(onClick = { onEdit(item) }) {
                        Text("Edytuj", color = Color.White)
                    }
                    TextButton(onClick = { onDelete(item) }) {
                        Text("Usun", color = AccentRed)
                    }
                }

                if (media.size > 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.Center)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(
                            enabled = pagerState.currentPage > 0,
                            onClick = {
                                galleryScope.launch {
                                    currentScale = 1f
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        ) {
                            Text("<", color = Color.White, fontSize = 34.sp)
                        }
                        TextButton(
                            enabled = pagerState.currentPage < media.lastIndex,
                            onClick = {
                                galleryScope.launch {
                                    currentScale = 1f
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        ) {
                            Text(">", color = Color.White, fontSize = 34.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.54f))
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (currentScale > 1f) "Przesun palcem. Podwojne tapniecie resetuje zoom." else "Przesun palcem, aby zmienic zdjecie. Uszczypniecie powieksza w miejscu palcow.",
                        modifier = Modifier.weight(1f),
                        color = Color.White.copy(alpha = 0.72f),
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                    TextButton(onClick = { onOpen(item) }) {
                        Text("Otworz", color = Color.White)
                    }
                }
                if (item.text.isNotBlank()) {
                    Text(
                        text = item.text,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(start = 16.dp, end = 110.dp, bottom = 58.dp),
                        color = Color.White.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableGalleryMedia(
    item: PersonalDocumentationItem,
    isActive: Boolean,
    onScaleChanged: (Float) -> Unit,
) {
    val context = LocalContext.current
    var scale by remember(item.id) { mutableStateOf(1f) }
    var offset by remember(item.id) { mutableStateOf(Offset.Zero) }
    var viewportSize by remember { mutableStateOf(androidx.compose.ui.unit.IntSize.Zero) }
    val bitmap by produceState<Bitmap?>(initialValue = null, item.uri, item.type) {
        value = when (item.type) {
            PersonalDocumentationItemType.Photo -> item.uri?.let {
                withContext(Dispatchers.IO) { loadBitmapFromUri(context, Uri.parse(it)) }
            }
            PersonalDocumentationItemType.Video -> item.uri?.let {
                loadVideoThumbnail(context, Uri.parse(it))
            }
            else -> null
        }
    }

    fun clampOffset(value: Offset, targetScale: Float): Offset {
        if (viewportSize.width <= 0 || viewportSize.height <= 0 || targetScale <= 1f) return Offset.Zero
        if (!value.x.isFinite() || !value.y.isFinite()) return Offset.Zero
        val maxX = (viewportSize.width * (targetScale - 1f) / 2f).coerceAtLeast(0f)
        val maxY = (viewportSize.height * (targetScale - 1f) / 2f).coerceAtLeast(0f)
        return Offset(
            x = value.x.coerceIn(-maxX, maxX),
            y = value.y.coerceIn(-maxY, maxY)
        )
    }

    fun updateTransform(nextScale: Float, nextOffset: Offset) {
        scale = if (nextScale.isFinite()) nextScale.coerceIn(1f, 5f) else 1f
        offset = clampOffset(nextOffset, scale)
        if (isActive) onScaleChanged(scale)
    }

    LaunchedEffect(isActive) {
        if (!isActive) {
            scale = 1f
            offset = Offset.Zero
        } else {
            onScaleChanged(scale)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { viewportSize = it }
            .pointerInput(item.id) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val pressedCount = event.changes.count { it.pressed }
                        val zoom = event.calculateZoom()
                        val pan = event.calculatePan()
                        val centroid = event.calculateCentroid(useCurrent = true)

                        if (pressedCount > 0 && (pressedCount > 1 || scale > 1.01f)) {
                            val oldScale = scale
                            val nextScale = (scale * zoom).coerceIn(1f, 5f)
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val focalPoint = centroid - center
                            val scaleFactor = nextScale / oldScale
                            val zoomOffset = if (nextScale > 1f) {
                                offset * scaleFactor + focalPoint * (1f - scaleFactor)
                            } else {
                                Offset.Zero
                            }
                            updateTransform(
                                nextScale = nextScale,
                                nextOffset = if (nextScale > 1f) zoomOffset + pan else Offset.Zero
                            )
                            event.changes.forEach { it.consume() }
                        }
                    } while (event.changes.any { it.pressed })
                }
            }
            .pointerInput(item.id) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1f) {
                            updateTransform(1f, Offset.Zero)
                        } else {
                            val targetScale = 2.6f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            updateTransform(
                                nextScale = targetScale,
                                nextOffset = (center - tapOffset) * (targetScale - 1f)
                            )
                        }
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = item.title,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = item.type.defaultDocumentationTitle(),
                color = AccentBlue,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

private suspend fun loadBitmapFromUrl(url: String): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            URL(url).openStream().use(BitmapFactory::decodeStream)
        }.getOrNull()
    }

private suspend fun saveTorqueDiagramImageLocally(
    context: Context,
    imageUrl: String,
    repairId: String,
    tableTitle: String,
): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val directory = File(context.filesDir, "torque_diagrams/$repairId")
            directory.mkdirs()
            val fileName = buildString {
                append(System.currentTimeMillis())
                append("-")
                append(tableTitle.catalogKey().replace(Regex("[^a-z0-9]+"), "-").trim('-'))
                append(".jpg")
            }
            val destination = File(directory, fileName)
            URL(imageUrl).openStream().use { input ->
                destination.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destination).toString()
        }.getOrNull()
    }

private suspend fun loadVideoThumbnail(context: Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.frameAtTime
            }
        }.getOrNull()
    }

private fun String.youtubeVideoId(): String? {
    val normalized = trim()
    val patterns = listOf(
        Regex("[?&]v=([A-Za-z0-9_-]{11})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
        Regex("youtube\\.com/embed/([A-Za-z0-9_-]{11})"),
        Regex("youtube\\.com/shorts/([A-Za-z0-9_-]{11})")
    )
    return patterns.firstNotNullOfOrNull { pattern ->
        pattern.find(normalized)?.groupValues?.getOrNull(1)
    }
}

private fun Context.displayNameForUri(uri: Uri): String? =
    runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
            ?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (cursor.moveToFirst() && nameIndex >= 0) {
                    cursor.getString(nameIndex)
                } else {
                    null
                }
            }
    }.getOrNull()

private fun Context.sizeLabelForUri(uri: Uri): String? =
    runCatching {
        contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)
            ?.use { cursor ->
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst() && sizeIndex >= 0) {
                    cursor.getLong(sizeIndex).takeIf { it > 0 }?.toFileSizeLabel()
                } else {
                    null
                }
            }
    }.getOrNull()

private fun Long.toFileSizeLabel(): String =
    if (this >= 1024L * 1024L) {
        "${this / (1024L * 1024L)} MB"
    } else {
        "${(this / 1024L).coerceAtLeast(1)} KB"
    }

private fun persistReadPermission(context: Context, uri: Uri) {
    runCatching {
        context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
}

private fun openDocumentUri(context: Context, rawUri: String) {
    val uri = Uri.parse(rawUri)
    runCatching {
        val intent = Intent(Intent.ACTION_VIEW)
            .setData(uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(intent)
    }
}

private fun PersonalDocumentationItem.documentBadge(): String =
    title.substringAfterLast('.', "")
        .take(3)
        .uppercase()
        .ifBlank {
            when (type) {
                PersonalDocumentationItemType.Document -> "DOC"
                PersonalDocumentationItemType.File -> "PLK"
                else -> "PDF"
            }
        }

@Composable
private fun RepairNotesTab(
    repair: RepairProject,
    documentation: RepairDocumentation?,
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
) {
    var noteText by remember(documentation?.repairId) {
        mutableStateOf(documentation?.userNotes.orEmpty())
    }
    GaragePanel {
        Text("Notatki", fontWeight = FontWeight.SemiBold)
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            GarageTextField(
                value = noteText,
                onValueChange = { noteText = it },
                label = "Twoje notatki",
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("repair_notes_input"),
                placeholder = "Wpisz uwagi z naprawy, objawy, numery czesci albo rzeczy do sprawdzenia...",
                singleLine = false,
                minLines = 8
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    modifier = Modifier.testTag("repair_notes_save_button"),
                    onClick = {
                        onDocumentationUpdated(documentation.withUserNotes(repair, noteText))
                    }
                ) {
                    Text("Zapisz notatki")
                }
            }
        }
    }
}

private fun nextInventoryId(parts: List<PartInventoryItem>): String =
    "repair-part-${System.currentTimeMillis()}-${parts.size}"

private fun PartInventoryItem.matchesShoppingItem(item: ShoppingListItem): Boolean {
    val inventoryNumbers = listOf(oemPartNumber, manufacturerPartNumber).filter { it.isNotBlank() }
    val shoppingNumbers = listOf(item.partNumber, item.manufacturerPartNumber).filter { it.isNotBlank() }
    return inventoryNumbers.any { inventoryNumber ->
        shoppingNumbers.any { shoppingNumber -> inventoryNumber == shoppingNumber }
    } || name.equals(item.name, ignoreCase = true)
}

private fun VehicleArea.accentColor(): androidx.compose.ui.graphics.Color = when (this) {
    VehicleArea.Engine -> AccentYellow
    VehicleArea.Body -> AccentGreen
    VehicleArea.Suspension -> AccentBlue
    VehicleArea.Electronics -> AccentPurple
    VehicleArea.Service -> AccentGreen
}

@Composable
private fun RepairDetailTile(
    title: String,
    subtitle: String,
    marker: String,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        maxLines = 4
                    )
                }
                Text(
                    text = marker,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private data class RealOemDiagram(
    val id: String,
    val title: String,
    val groupLabel: String,
    val url: String,
    val thumbnailUrl: String? = null,
)

private data class RealOemPart(
    val position: String,
    val name: String,
    val quantity: String,
    val partNumber: String,
    val notes: String,
)

private data class RealOemDiagramDetails(
    val title: String,
    val imageUrl: String?,
    val parts: List<RealOemPart>,
)

private data class RepairIndexedTisLink(
    val index: Int,
    val link: TisDocumentationLink,
)

private data class RepairIndexedYoutubeVideo(
    val index: Int,
    val video: YoutubeVideo,
)

@Composable
private fun RealOemSchematicsDialog(
    vehicle: Vehicle,
    repair: RepairProject,
    documentation: RepairDocumentation?,
    onAddShoppingItems: (List<ShoppingListItem>) -> Unit,
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var diagrams by remember { mutableStateOf(emptyList<RealOemDiagram>()) }
    var selectedDiagram by remember { mutableStateOf<RealOemDiagram?>(null) }
    var diagramDetails by remember { mutableStateOf<RealOemDiagramDetails?>(null) }
    var selectedPartNumbers by remember { mutableStateOf(setOf<String>()) }
    var partPendingLookup by remember { mutableStateOf<RealOemPart?>(null) }
    var isLoadingDiagrams by remember { mutableStateOf(false) }
    var isLoadingParts by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var isSavingTorqueDiagram by remember { mutableStateOf(false) }
    var diagramSearchQuery by remember { mutableStateOf("") }
    var diagramPendingTorqueMove by remember { mutableStateOf<RealOemDiagramDetails?>(null) }
    val filteredDiagrams = remember(diagrams, diagramSearchQuery) {
        val query = diagramSearchQuery.catalogKey()
        if (query.isBlank()) {
            diagrams
        } else {
            diagrams.filter { diagram ->
                diagram.title.catalogKey().contains(query) ||
                    diagram.groupLabel.catalogKey().contains(query)
            }
        }
    }

    fun loadDiagrams() {
        coroutineScope.launch {
            isLoadingDiagrams = true
            message = null
            diagrams = runCatching {
                fetchRealOemDiagrams(vehicle, repair.area)
            }.onFailure {
                message = "Nie udalo sie pobrac schematow z czescidobmw.pl. Sprawdz VIN w profilu auta."
            }.getOrDefault(emptyList())
            if (diagrams.isEmpty() && message == null) {
                message = "Brak schematow dla tej kategorii."
            }
            isLoadingDiagrams = false
        }
    }

    fun loadParts(diagram: RealOemDiagram) {
        coroutineScope.launch {
            selectedDiagram = diagram
            diagramDetails = null
            selectedPartNumbers = emptySet()
            isLoadingParts = true
            message = null
            diagramDetails = runCatching {
                fetchRealOemDiagramDetails(diagram)
            }.onFailure {
                message = "Nie udalo sie pobrac listy czesci ze schematu."
            }.getOrNull()
            isLoadingParts = false
        }
    }

    LaunchedEffect(vehicle.id, vehicle.vin, repair.area) {
        loadDiagrams()
    }

    fun addLookupPartToShoppingList(
        schemePart: RealOemPart,
        lookup: MockPartLookupResult,
        quantity: Int,
    ) {
        val diagram = selectedDiagram ?: return
        onAddShoppingItems(
            listOf(
                ShoppingListItem(
                    id = "czescidobmw-${lookup.manufacturerPartNumber}-${System.currentTimeMillis()}",
                    partNumber = lookup.oemPartNumber,
                    manufacturerPartNumber = lookup.manufacturerPartNumber,
                    name = lookup.name,
                    manufacturer = lookup.manufacturer,
                    repairTitle = repair.title,
                    repairId = repair.id,
                    area = repair.area,
                    quantity = quantity,
                    source = "czescidobmw.pl",
                    price = lookup.shopPrice,
                    imageUri = lookup.imageUrl,
                    shopUrl = lookup.shopUrl,
                    realOemUrl = lookup.realOemUrl.ifBlank { diagram.url }
                )
            )
        )
        selectedPartNumbers = selectedPartNumbers + schemePart.partNumber
        partPendingLookup = null
    }

    fun moveDiagramToTorqueSection(details: RealOemDiagramDetails) {
        val imageUrl = details.imageUrl ?: return
        coroutineScope.launch {
            isSavingTorqueDiagram = true
            message = "Zapisuje schemat lokalnie..."
            val baseDocumentation = documentation ?: RepairDocumentation(
                title = "Dokumentacja: ${repair.title}",
                area = repair.area,
                repairTitle = repair.title,
                repairId = repair.id,
                summary = "Dokumentacja powiazana z naprawa: ${repair.title}."
            )
            val currentTables = baseDocumentation.effectiveTorqueTables()
            val tableTitle = translateRealOemLabel(details.title)
                .ifBlank { "Schemat momentow ${currentTables.size + 1}" }
            val alreadyExists = currentTables.any { table ->
                table.diagramImageUri == imageUrl ||
                    table.title.catalogKey() == tableTitle.catalogKey()
            }
            if (alreadyExists) {
                onDocumentationUpdated(baseDocumentation.copy(torqueTables = currentTables))
                message = "Ten schemat jest juz w sekcji momenty."
            } else {
                val localImageUri = saveTorqueDiagramImageLocally(
                    context = context,
                    imageUrl = imageUrl,
                    repairId = repair.id,
                    tableTitle = tableTitle
                )
                if (localImageUri == null) {
                    message = "Nie udalo sie zapisac schematu lokalnie. Sprawdz polaczenie i sprobuj ponownie."
                } else {
                    val newTable = TorqueSpecTable(
                        id = "czescidobmw-torque-${System.currentTimeMillis()}",
                        title = tableTitle,
                        diagramImageUri = localImageUri,
                        torqueSpecs = emptyList(),
                        diagramAssignments = emptyList()
                    )
                    onDocumentationUpdated(baseDocumentation.copy(torqueTables = currentTables + newTable))
                    message = "Schemat zapisany lokalnie i przeniesiony do sekcji momenty."
                }
            }
            isSavingTorqueDiagram = false
            diagramPendingTorqueMove = null
        }
    }

    partPendingLookup?.let { part ->
        RealOemPartLookupDialog(
            part = part,
            onAddToShoppingList = { lookup, quantity ->
                addLookupPartToShoppingList(part, lookup, quantity)
            },
            onDismiss = { partPendingLookup = null }
        )
    }

    diagramPendingTorqueMove?.let { details ->
        MoveDiagramToTorqueDialog(
            diagramTitle = translateRealOemLabel(details.title),
            onConfirm = { moveDiagramToTorqueSection(details) },
            onDismiss = { diagramPendingTorqueMove = null }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (selectedDiagram == null) {
                    "Schematy czescidobmw.pl"
                } else {
                    translateRealOemLabel(diagramDetails?.title ?: selectedDiagram?.title.orEmpty())
                },
                fontSize = if (selectedDiagram == null) 22.sp else 18.sp,
                lineHeight = if (selectedDiagram == null) 28.sp else 22.sp
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (selectedDiagram == null) {
                    Text(
                        text = "${repair.area.label} / ${vehicle.displayName.ifBlank { "BMW" }}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                    if (isLoadingDiagrams) {
                        RealOemInfoRow("Pobieram schematy...")
                    }
                    message?.let { text ->
                        RealOemInfoRow(text)
                    }
                    if (diagrams.isNotEmpty()) {
                        GarageTextField(
                            value = diagramSearchQuery,
                            onValueChange = { diagramSearchQuery = it },
                            label = "Szukaj schematu",
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = "np. wtryskiwacz, turbo, EGR"
                        )
                        Text(
                            text = "Dostepne schematy (${filteredDiagrams.size})",
                            fontWeight = FontWeight.SemiBold
                        )
                        if (filteredDiagrams.isEmpty()) {
                            RealOemInfoRow("Brak schematow pasujacych do tej frazy.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 430.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(filteredDiagrams) { diagram ->
                                    RealOemDiagramRow(
                                        diagram = diagram,
                                        isSelected = false,
                                        onClick = { loadParts(diagram) }
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Text(
                        text = "‹ Wroc do schematow",
                        modifier = Modifier.clickable {
                            selectedDiagram = null
                            diagramDetails = null
                            selectedPartNumbers = emptySet()
                            message = null
                        },
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    message?.let { text ->
                        RealOemInfoRow(text)
                    }
                    if (isSavingTorqueDiagram) {
                        RealOemInfoRow("Zapis lokalnej kopii schematu...")
                    }
                    if (isLoadingParts) {
                        RealOemInfoRow("Pobieram schemat i liste czesci...")
                    }
                    diagramDetails?.let { details ->
                        details.imageUrl?.let { imageUrl ->
                            RealOemDiagramImage(
                                imageUrl = imageUrl,
                                maxImageHeight = 300,
                                onLongClick = { diagramPendingTorqueMove = details }
                            )
                        } ?: RealOemInfoRow("Nie znaleziono obrazu dla tego schematu.")
                        Text(
                            text = "Lista czesci",
                            fontWeight = FontWeight.SemiBold
                        )
                        if (details.parts.isEmpty()) {
                            RealOemInfoRow("Nie znaleziono listy czesci na tym schemacie.")
                        } else {
                            LazyColumn(
                                modifier = Modifier.heightIn(max = 430.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(details.parts) { part ->
                                    RealOemPartRow(
                                        part = part,
                                        isSelected = part.partNumber in selectedPartNumbers,
                                        onClick = { partPendingLookup = part }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
}

@Composable
private fun MoveDiagramToTorqueDialog(
    diagramTitle: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Schemat momentow") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = diagramTitle.ifBlank { "Wybrany schemat" },
                    fontWeight = FontWeight.SemiBold
                )
                GaragePanel(onClick = onConfirm) {
                    Text("Przenies schemat do sekcji momenty", fontWeight = FontWeight.SemiBold)
                    Text(
                        text = "Ten obraz stanie sie osobna sekcja momentow dokrecania dla tej naprawy.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
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

@Composable
private fun RealOemDiagramRow(
    diagram: RealOemDiagram,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RealOemDiagramThumbnail(diagram = diagram)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = translateRealOemLabel(diagram.title),
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = diagram.title,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.58f)
                )
                Text(
                    text = translateRealOemLabel(diagram.groupLabel),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RealOemDiagramThumbnail(diagram: RealOemDiagram) {
    val bitmap by produceState<Bitmap?>(initialValue = null, diagram.thumbnailUrl) {
        value = withContext(Dispatchers.IO) {
            diagram.thumbnailUrl?.let { url ->
                runCatching {
                    URL(url).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)
                    }
                }.getOrNull()
            }
        }
    }

    Surface(
        modifier = Modifier.width(82.dp),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Miniatura schematu czescidobmw.pl",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                contentScale = ContentScale.Fit
            )
        } else {
            Text(
                text = "Schemat",
                modifier = Modifier.padding(8.dp),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.52f)
            )
        }
    }
}

@Composable
private fun RealOemPartRow(
    part: RealOemPart,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
        } else {
            MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
        },
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = part.position.ifBlank { "-" },
                modifier = Modifier.width(30.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = translateRealOemLabel(part.name),
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 20.sp
                )
                if (translateRealOemLabel(part.name) != part.name) {
                    Text(
                        text = part.name,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                    )
                }
                Text(
                    text = "Ilosc: ${part.quantity.ifBlank { "1" }} / OEM: ${part.partNumber}",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                if (part.notes.isNotBlank()) {
                    Text(
                        text = part.notes,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                    )
                }
            }
            Surface(
                modifier = Modifier.size(38.dp),
                color = if (isSelected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.82f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = if (isSelected) "✓" else "▤",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun RealOemPartLookupDialog(
    part: RealOemPart,
    onAddToShoppingList: (MockPartLookupResult, Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var quantity by remember(part.partNumber) {
        mutableStateOf(part.quantity.defaultQuantityText())
    }
    var lookupResults by remember(part.partNumber) { mutableStateOf(emptyList<MockPartLookupResult>()) }
    var selectedResult by remember(part.partNumber) { mutableStateOf<MockPartLookupResult?>(null) }
    var isSearching by remember(part.partNumber) { mutableStateOf(false) }
    var searchError by remember(part.partNumber) { mutableStateOf<String?>(null) }
    val quantityValue = quantity.toIntOrNull()
    val canAdd = selectedResult != null && quantityValue != null && quantityValue > 0

    fun searchOffers() {
        coroutineScope.launch {
            isSearching = true
            searchError = null
            selectedResult = null
            lookupResults = runCatching {
                fetchCzescidobmwResults(part.partNumber)
            }.recoverCatching {
                listOf(mockPartLookup(part.partNumber))
            }.getOrDefault(emptyList())
            if (lookupResults.isEmpty()) {
                searchError = "Nie znaleziono dostepnych czesci dla OEM ${part.partNumber}."
            }
            isSearching = false
        }
    }

    LaunchedEffect(part.partNumber) {
        searchOffers()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = translateRealOemLabel(part.name),
                fontSize = 18.sp,
                lineHeight = 22.sp
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "OEM: ${part.partNumber} / ilosc ze schematu: ${part.quantity.ifBlank { "1" }}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontSize = 13.sp
                )
                GarageTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { character -> character.isDigit() } },
                    label = "Ilosc do listy zakupow",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = part.quantity.ifBlank { "1" },
                    keyboardType = KeyboardType.Number
                )
                if (isSearching) {
                    RealOemInfoRow("Szukam dostepnych czesci w sklepie...")
                }
                searchError?.let { error ->
                    RealOemInfoRow(error)
                }
                if (!isSearching && lookupResults.isEmpty() && searchError == null) {
                    RealOemInfoRow("Wyniki pojawia sie po pobraniu danych sklepu.")
                }
                lookupResults.forEach { lookup ->
                    LookupResultCard(
                        title = lookup.manufacturer,
                        subtitle = lookup.name,
                        primary = "Cena brutto: ${lookup.shopPrice}",
                        secondary = "OEM: ${lookup.oemPartNumber} / Producent: ${lookup.manufacturerPartNumber}",
                        source = lookup.shopUrl,
                        imageUrl = lookup.imageUrl,
                        imageSearchUrl = lookup.imageSearchUrl,
                        isSelected = selectedResult == lookup,
                        onClick = { selectedResult = lookup }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canAdd,
                onClick = {
                    val lookup = selectedResult ?: return@TextButton
                    onAddToShoppingList(lookup, quantityValue ?: 1)
                }
            ) {
                Text("Dodaj do zakupow")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    enabled = !isSearching,
                    onClick = { searchOffers() }
                ) {
                    Text("Odswiez")
                }
                TextButton(onClick = onDismiss) {
                    Text("Anuluj")
                }
            }
        }
    )
}

private fun String.defaultQuantityText(): String =
    Regex("\\d+").find(this)?.value ?: "1"

@Composable
private fun RealOemInfoRow(text: String) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(12.dp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f)
        )
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
private fun RealOemDiagramImage(
    imageUrl: String,
    maxImageHeight: Int = 300,
    onLongClick: (() -> Unit)? = null,
) {
    val bitmap by produceState<Bitmap?>(initialValue = null, imageUrl) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                URL(imageUrl).openStream().use { stream ->
                    BitmapFactory.decodeStream(stream)
                }
            }.getOrNull()
        }
    }
    var scale by remember(imageUrl) { mutableStateOf(1f) }
    var offset by remember(imageUrl) { mutableStateOf(Offset.Zero) }
    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val updatedScale = (scale * zoomChange).coerceIn(1f, 4f)
        scale = updatedScale
        offset = if (updatedScale > 1f) {
            Offset(
                x = (offset.x + panChange.x).coerceIn(-360f, 360f),
                y = (offset.y + panChange.y).coerceIn(-360f, 360f)
            )
        } else {
            Offset.Zero
        }
    }

    if (bitmap != null) {
        val aspectRatio = bitmap!!.width.toFloat() / bitmap!!.height.toFloat()
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = maxImageHeight.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.White)
                .combinedClickable(
                    onClick = {},
                    onLongClick = onLongClick
                )
                .transformable(transformState),
            contentAlignment = Alignment.TopCenter
        ) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = "Schemat czescidobmw.pl",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(aspectRatio)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        translationX = offset.x
                        translationY = offset.y
                    },
                contentScale = ContentScale.Fit
            )
        }
    } else {
        RealOemInfoRow("Schemat graficzny bedzie widoczny po pobraniu obrazu.")
    }
}

private suspend fun fetchRealOemDiagrams(
    vehicle: Vehicle,
    area: VehicleArea,
): List<RealOemDiagram> =
    withContext(Dispatchers.IO) {
        val catalogUrl = czescidobmwCatalogUrl(vehicle)
        val catalogHtml = fetchText(catalogUrl)
        val groups = parseCzescidobmwGroups(catalogHtml)
            .filter { group ->
                czescidobmwGroupNamesFor(area).any { expected ->
                    group.title.catalogKey() == expected.catalogKey()
                }
            }
            .ifEmpty { parseCzescidobmwGroups(catalogHtml) }

        groups.flatMap { group ->
            parseCzescidobmwDiagrams(
                html = fetchText(group.url),
                groupLabel = group.title
            )
        }.distinctBy { it.url }
    }

private suspend fun fetchRealOemDiagramDetails(diagram: RealOemDiagram): RealOemDiagramDetails =
    withContext(Dispatchers.IO) {
        parseRealOemDiagramDetails(fetchText(diagram.url), diagram)
    }

private fun czescidobmwCatalogUrl(vehicle: Vehicle): String {
    val vin = vehicle.vin.filter { it.isLetterOrDigit() }.uppercase()
    if (vin.length != 17) {
        if (vehicle.partsCatalogUrl.isNotBlank()) return vehicle.partsCatalogUrl
        error("VIN musi miec 17 znakow.")
    }
    val encodedVin = URLEncoder.encode(vin, "UTF-8")
    val lookupHtml = fetchText("https://czescidobmw.pl/api/laximo/car/$encodedVin")
    val lookup = JSONObject(lookupHtml)
    val root = lookup.optJSONObject("data") ?: lookup
    val vehicles = root.optJSONArray("vehicles") ?: lookup.optJSONArray("vehicles")
    val foundVehicle = vehicles?.optJSONObject(0)
    val vehicleId = foundVehicle?.optString("vehicleId").orEmpty()
    if (vehicleId.isBlank()) {
        if (vehicle.partsCatalogUrl.isNotBlank()) return vehicle.partsCatalogUrl
        error("Nie znaleziono katalogu dla VIN.")
    }
    return "https://czescidobmw.pl/wyszukiwarka-vin/$encodedVin/$vehicleId"
}

private fun parseCzescidobmwGroups(html: String): List<CzescidobmwGroup> {
    val linkRegex = Regex(
        pattern = "<a[^>]+href=\"([^\"]+)\"[^>]+title=\"([^\"]+)\"[^>]*>",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    return linkRegex.findAll(html).mapNotNull { match ->
        val href = decodeHtml(match.groupValues[1])
        val title = decodeHtml(match.groupValues[2]).trim()
        val isCatalogLink = href.contains("/vin/", ignoreCase = true) ||
            href.contains("/wyszukiwarka-vin/", ignoreCase = true)
        if (!isCatalogLink || title.isBlank()) return@mapNotNull null
        if (href.contains("szczegoly-unitu", ignoreCase = true)) return@mapNotNull null
        CzescidobmwGroup(
            title = title,
            url = absoluteCzescidobmwUrl(href)
        )
    }.distinctBy { it.url }.toList()
}

private fun parseCzescidobmwDiagrams(
    html: String,
    groupLabel: String,
): List<RealOemDiagram> =
    parseRealOemDiagrams(
        html = html,
        groupLabel = groupLabel
    )

private fun parseRealOemDiagrams(
    html: String,
    groupLabel: String,
): List<RealOemDiagram> {
    val linkRegex = Regex(
        pattern = "<a(?=[^>]*class=\"[^\"]*c-laximo-model__units__link[^\"]*\")(?=[^>]*href=\"([^\"]+)\")(?=[^>]*title=\"([^\"]*)\")[^>]*>(.*?)</a>",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    return linkRegex.findAll(html).mapNotNull { match ->
        val href = decodeHtml(match.groupValues[1])
        val title = decodeHtml(match.groupValues[2]).ifBlank {
            cleanHtml(match.groupValues[3])
        }
        val unitId = Regex("szczegoly-unitu/(\\d+)")
            .find(href)
            ?.groupValues
            ?.getOrNull(1)
            ?: href
        val thumbnailUrl = Regex("<img[^>]+src=\"([^\"]+)\"", RegexOption.IGNORE_CASE)
            .find(match.groupValues[3])
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::decodeHtml)
            ?.let(::absoluteCzescidobmwUrl)
        if (title.isBlank()) return@mapNotNull null
        RealOemDiagram(
            id = unitId,
            title = title,
            groupLabel = groupLabel,
            url = absoluteCzescidobmwUrl(href),
            thumbnailUrl = thumbnailUrl
        )
    }.toList()
}

private fun parseRealOemDiagramDetails(
    html: String,
    diagram: RealOemDiagram,
): RealOemDiagramDetails {
    val title = Regex("<h1[^>]*>(.*?)</h1>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        .find(html)
        ?.groupValues
        ?.getOrNull(1)
        ?.let(::cleanHtml)
        .orEmpty()
    return RealOemDiagramDetails(
        title = title.ifBlank { diagram.title },
        imageUrl = parseRealOemDiagramImageUrl(html),
        parts = parseRealOemParts(html)
    )
}

private fun parseRealOemDiagramImageUrl(html: String): String? {
    val imageCandidates = Regex(
        pattern = "<img[^>]+src=\"([^\"]+)\"[^>]*>",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    ).findAll(html).mapNotNull { match ->
        match.groupValues.getOrNull(1)?.let(::decodeHtml)
    }.toList()

    val diagramImage = imageCandidates.firstOrNull { src ->
        src.contains("img.altechopersys.com", ignoreCase = true) &&
            src.contains("/source/", ignoreCase = true)
    } ?: imageCandidates.firstOrNull { src ->
        src.contains("img.altechopersys.com", ignoreCase = true)
    } ?: imageCandidates.firstOrNull { src ->
        !src.contains("logo", ignoreCase = true) &&
            !src.contains("clicky", ignoreCase = true) &&
            !src.endsWith(".svg", ignoreCase = true)
    }

    return diagramImage?.let(::absoluteCzescidobmwUrl)
}

private fun parseRealOemParts(html: String): List<RealOemPart> {
    val itemBlockRegex = Regex(
        pattern = "<span[^>]+class=\"[^\"]*c-tab-content__item[\\s\\S]*?(?=<span[^>]+class=\"[^\"]*c-tab-content__item|<div id=\"hook_|</body>)",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val positionRegex = Regex("focusRect[^']*'([^']+)'", RegexOption.IGNORE_CASE)
    val linkRegex = Regex(
        pattern = "<a[^>]+href=\"([^\"]*lista-towarow/(\\d+)[^\"]*)\"[^>]*title=\"([^\"]*)\"[^>]*>(.*?)</a>",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    val titleLinkRegex = Regex(
        pattern = "<a[^>]*title=\"([^\"]*)\"[^>]*>(.*?)</a>",
        options = setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    return itemBlockRegex.findAll(html).mapNotNull { item ->
        val block = item.value
        val position = positionRegex.find(block)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::decodeHtml)
            .orEmpty()
        if (position.isBlank()) return@mapNotNull null
        val links = linkRegex.findAll(block).toList()
        val titleValues = titleLinkRegex.findAll(block)
            .map { match ->
                decodeHtml(match.groupValues[1]).ifBlank { cleanHtml(match.groupValues[2]) }
            }
            .filter { it.isNotBlank() }
            .toList()
        val partNumber = links
            .map { it.groupValues[2] }
            .lastOrNull { it.matches(Regex("\\d{7,12}")) }
            ?: titleValues.firstOrNull { it.matches(Regex("\\d{7,12}")) }
            ?: return@mapNotNull null
        val name = links
            .map { decodeHtml(it.groupValues[3]).ifBlank { cleanHtml(it.groupValues[4]) } }
            .ifEmpty { titleValues }
            .firstOrNull { value ->
                value.isNotBlank() &&
                    value != position &&
                    value != partNumber &&
                    !value.matches(Regex("\\d{1,3}"))
            }
            .orEmpty()
        val quantity = Regex("Quantity:[\\s\\S]*?<span>([^<]+)</span>", RegexOption.IGNORE_CASE)
            .find(block)
            ?.groupValues
            ?.getOrNull(1)
            ?.let(::cleanHtml)
            ?.substringBefore(" ")
            .orEmpty()
        val notes = Regex("comment\\d*:</span>\\s*<span>([^<]+)</span>", RegexOption.IGNORE_CASE)
            .findAll(block)
            .map { cleanHtml(it.groupValues[1]) }
            .filter { it.isNotBlank() }
            .joinToString(" / ")
        RealOemPart(
            position = position,
            name = name.ifBlank { "Czesc BMW $partNumber" },
            quantity = quantity.ifBlank { "1" },
            partNumber = partNumber,
            notes = notes
        )
    }.distinctBy { it.partNumber }.toList()
}

private data class CzescidobmwGroup(
    val title: String,
    val url: String,
)

private fun czescidobmwGroupNamesFor(area: VehicleArea): List<String> =
    when (area) {
        VehicleArea.Engine -> listOf(
            "Silnik",
            "Silnik- elektryka",
            "Mieszanka paliwowa",
            "Zasilanie paliwowe",
            "Chlodnica",
            "Uklad wydechowy",
            "Zawieszenie silnika/ skrzyni biegow"
        )
        VehicleArea.Suspension -> listOf(
            "Przednia os",
            "Tylna os",
            "Uklad kierowniczy",
            "Hamulce",
            "Kola"
        )
        VehicleArea.Body -> listOf(
            "Elementy blasz. karoserii",
            "Wyposazenie pojazdu",
            "Fotele",
            "Dach przesuwny / dach skladany"
        )
        VehicleArea.Electronics -> listOf(
            "Elektryka pojazdu",
            "Silnik- elektryka",
            "Instalacja oswietleniowa",
            "Audio, nawigacja, ukl. elektroniczne",
            "Systemy komunikacji",
            "Distance Systems, Cruise Control",
            "Przyrzady wskazujace, systemy pomiarowe"
        )
        VehicleArea.Service -> listOf(
            "Czesci i zestawy do serwisu i napraw",
            "Zakres przegladow i napraw",
            "Mat pomoc i plyny eksploat / ColorSystem",
            "Parts Repair Service"
        )
    }

private fun fetchText(url: String): String {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        connectTimeout = 8_000
        readTimeout = 8_000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "BmwGarageAssistant/0.1")
    }
    val stream = if (connection.responseCode >= 400) {
        connection.errorStream ?: connection.inputStream
    } else {
        connection.inputStream
    }
    return stream.bufferedReader().use { it.readText() }
}

private fun String.catalogKey(): String =
    lowercase()
        .replace("ą", "a")
        .replace("ć", "c")
        .replace("ę", "e")
        .replace("ł", "l")
        .replace("ń", "n")
        .replace("ó", "o")
        .replace("ś", "s")
        .replace("ź", "z")
        .replace("ż", "z")
        .replace(Regex("\\s+"), " ")
        .trim()

private fun cleanHtml(value: String): String =
    decodeHtml(value.replace(Regex("<[^>]+>"), " "))
        .replace(Regex("\\s+"), " ")
        .trim()

private fun decodeHtml(value: String): String =
    Html.fromHtml(value, Html.FROM_HTML_MODE_LEGACY).toString()

private fun translateRealOemLabel(value: String): String {
    val normalized = value.trim()
    if (normalized.isBlank()) return normalized

    realOemExactTranslations[normalized.uppercase()]?.let { return it }

    var translated = normalized.lowercase()
    realOemWordTranslations.forEach { (english, polish) ->
        translated = translated.replace(
            Regex("\\b${Regex.escape(english)}\\b", RegexOption.IGNORE_CASE),
            polish
        )
    }
    return translated.replaceFirstChar { character ->
        if (character.isLowerCase()) character.titlecase() else character.toString()
    }
}

private val realOemExactTranslations = mapOf(
    "ENGINE" to "Silnik",
    "SHORT ENGINE" to "Silnik kompletny / blok krotki",
    "ENGINE BLOCK" to "Blok silnika",
    "ENGINE BLOCK MOUNTING PARTS" to "Elementy montazowe bloku silnika",
    "LOWER TIMING CASE" to "Dolna obudowa rozrzadu",
    "OIL PAN/OIL LEVEL INDICATOR" to "Miska olejowa / czujnik poziomu oleju",
    "CYLINDER HEAD" to "Glowica cylindrow",
    "CYLINDER HEAD ATTACHED PARTS" to "Osprzet glowicy",
    "CYLINDER HEAD COVER" to "Pokrywa glowicy",
    "BELT DRIVE-VIBRATION DAMPER" to "Naped paskowy / tlumik drgan",
    "BELT DRIVE WATER PUMP/ALTERNATOR" to "Pasek pompy wody / alternatora",
    "CRANKSHAFT-PISTONS" to "Wal korbowy / tloki",
    "CRANKSHAFT WITH BEARING SHELLS" to "Wal korbowy z panewkami",
    "FLYWHEEL / TWIN MASS FLYWHEEL" to "Kolo zamachowe / dwumasa",
    "INTAKE MANIFOLD AGR WITH FLAP CONTROL" to "Kolektor dolotowy EGR ze sterowaniem klap",
    "INTAKE MANIFOLD-SUPERCHARG.AIR DUCT/AGR" to "Dolot / przewod powietrza doladowania / EGR",
    "VACUUM PUMP WITH TUBES" to "Pompa podcisnienia z przewodami",
    "VACUUM CONTROL-AGR" to "Sterowanie podcisnieniem EGR",
    "EXHAUST TURBOCHARGER WITH LUBRICATION" to "Turbosprezarka z przewodami oleju",
    "EXHAUST MANIFOLD-AGR" to "Kolektor wydechowy / EGR",
    "ENGINE ACOUSTICS" to "Oslony akustyczne silnika",
    "FRONT AXLE" to "Os przednia",
    "REAR AXLE" to "Os tylna",
    "BODYWORK" to "Nadwozie",
    "VEHICLE ELECTRICAL SYSTEM" to "Instalacja elektryczna pojazdu",
    "LIGHTING" to "Oswietlenie",
    "HEATER AND AIR CONDITIONING" to "Ogrzewanie i klimatyzacja",
    "AUDIO / NAVIGATION" to "Audio / nawigacja"
)

private val realOemWordTranslations = linkedMapOf(
    "rear" to "tylny",
    "front" to "przedni",
    "left" to "lewy",
    "right" to "prawy",
    "upper" to "gorny",
    "lower" to "dolny",
    "engine" to "silnik",
    "block" to "blok",
    "mounting" to "mocowanie",
    "parts" to "czesci",
    "part" to "czesc",
    "carrier" to "rama pomocnicza",
    "rubber" to "gumowy",
    "stopper" to "odbojnik",
    "bolt" to "sruba",
    "hex" to "szesciokatna",
    "washer" to "podkladka",
    "nut" to "nakretka",
    "screw" to "wkret",
    "seal" to "uszczelka",
    "gasket" to "uszczelka",
    "ring" to "pierscien",
    "cover" to "pokrywa",
    "hose" to "przewod",
    "pipe" to "rura",
    "support" to "wspornik",
    "bracket" to "uchwyt",
    "repair" to "naprawy",
    "required" to "wymagane",
    "installation" to "montaz",
    "tool" to "narzedzie",
    "push rod" to "drazek",
    "manifold" to "kolektor",
    "intake" to "dolotowy",
    "exhaust" to "wydechowy",
    "turbocharger" to "turbosprezarka",
    "vacuum" to "podcisnienie",
    "pump" to "pompa",
    "water" to "wody",
    "oil" to "oleju",
    "filter" to "filtr"
)

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun VehicleRepairListScreenPreview() {
    val vehicle = Vehicle(
        brand = "BMW",
        model = "E61 520d",
        generation = "E61",
        engine = "M47N2 2.0d",
        year = "2006",
        vin = "WBAXXXXXXXXXXXXXX",
        mileage = "285000",
        note = "Tylna zwrotnica lewa, zardzewiala sruba"
    )

    GarageTheme {
        VehicleRepairListScreen(
            vehicle = vehicle,
            repairs = sampleRepairsFor(vehicle),
            repairDocumentation = emptyList(),
            inventoryParts = emptyList(),
            shoppingList = emptyList(),
            onRepairAdded = { _, _ -> },
            onRepairUpdated = {},
            onOpenDocumentation = {},
            onDocumentationUpdated = {},
            onOpenShoppingList = {},
            onAddShoppingItems = {},
            onShoppingListUpdated = {},
            onInventoryPartAdded = {},
            onBack = {}
        )
    }
}
