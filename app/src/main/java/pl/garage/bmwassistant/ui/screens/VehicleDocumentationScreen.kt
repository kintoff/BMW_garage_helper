package pl.garage.bmwassistant.ui.screens

import android.content.Context
import android.content.ClipData
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.FileProvider
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.data.sampleRepairDocumentationFor
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
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
import pl.garage.bmwassistant.ui.components.AccentBlue
import pl.garage.bmwassistant.ui.components.GarageTextField
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.SegmentTabs
import pl.garage.bmwassistant.ui.components.StatusBadge
import pl.garage.bmwassistant.ui.components.iconResource
import pl.garage.bmwassistant.ui.theme.GarageTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.Text as MlKitText
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.net.URL
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

@Composable
fun VehicleDocumentationScreen(
    vehicle: Vehicle,
    repairDocumentation: List<RepairDocumentation>,
    repairProjects: List<RepairProject> = emptyList(),
    shoppingList: List<ShoppingListItem> = emptyList(),
    initialRepairTitle: String? = null,
    returnToPreviousModuleOnBack: Boolean = false,
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
    onBack: () -> Unit,
) {
    var isGeneralExpanded by rememberSaveable(vehicle.id) { mutableStateOf(true) }
    var isRepairDocsExpanded by rememberSaveable(vehicle.id) { mutableStateOf(true) }
    var selectedDocumentationId by rememberSaveable(vehicle.id, initialRepairTitle) {
        mutableStateOf(
            initialRepairTitle?.let { repairTitle ->
                repairDocumentation.firstOrNull { it.repairTitle == repairTitle }?.repairId
            }
        )
    }
    val finishedRepairProjects = remember(repairProjects) {
        repairProjects.filter { it.status.isFinishedRepairStatus() }
    }
    val archivedRepairDocumentation = remember(repairDocumentation, finishedRepairProjects) {
        if (repairProjects.isEmpty()) {
            repairDocumentation
        } else {
            repairDocumentation.filter { documentation ->
                finishedRepairProjects.any { repair -> documentation.belongsToRepair(repair) }
            }
        }
    }
    var expandedRepairAreas by remember(archivedRepairDocumentation) {
        mutableStateOf(
            archivedRepairDocumentation.map { it.area }.toSet().ifEmpty { setOf(VehicleArea.Engine) }
        )
    }
    val documentationByArea = remember(archivedRepairDocumentation) {
        archivedRepairDocumentation.groupBy { it.area }
    }
    val selectedDocumentation = remember(selectedDocumentationId, repairDocumentation) {
        selectedDocumentationId?.let { repairId ->
            repairDocumentation.firstOrNull { it.repairId == repairId }
        }
    }

    LaunchedEffect(initialRepairTitle, repairDocumentation) {
        if (selectedDocumentationId == null) {
            selectedDocumentationId = initialRepairTitle?.let { repairTitle ->
                repairDocumentation.firstOrNull { it.repairTitle == repairTitle }?.repairId
            }
        }
    }

    fun closeDocumentationDetails() {
        if (returnToPreviousModuleOnBack) {
            onBack()
        } else {
            selectedDocumentationId = null
        }
    }

    BackHandler(enabled = selectedDocumentation != null) {
        closeDocumentationDetails()
    }

    selectedDocumentation?.let { documentation ->
        val repair = repairProjects.firstOrNull { documentation.belongsToRepair(it) }
        RepairDocumentationDetailsScreen(
            vehicle = vehicle,
            documentation = documentation,
            repair = repair,
            archivedShoppingList = documentation.archivedShoppingList.ifEmpty {
                shoppingList.filter { item -> repair != null && item.belongsToRepair(repair) }
            },
            onDocumentationUpdated = { updatedDocumentation ->
                selectedDocumentationId = updatedDocumentation.repairId
                onDocumentationUpdated(updatedDocumentation)
            },
            onBack = { closeDocumentationDetails() }
        )
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text("Wroc do auta")
                }
            }

            item {
                Header(
                    title = "Notatki / Dokumentacja",
                    subtitle = vehicle.displayName.ifBlank { "Profil auta" }
                )
            }

            item {
                DocumentationSection(
                    title = "Dokumentacja ogolna",
                    subtitle = "Stale materialy auta: VIN, instrukcje, linki, PDF-y, schematy i notatki.",
                    countLabel = "0 wpisow",
                    isExpanded = isGeneralExpanded,
                    onToggle = { isGeneralExpanded = !isGeneralExpanded }
                ) {
                    EmptyDocumentationRow("Tutaj trafia dokumentacja niezalezna od konkretnej naprawy.")
                }
            }

            item {
                DocumentationSection(
                    title = "Dokumentacja do napraw",
                    subtitle = "Archiwum zakonczonych napraw. Zawiera opis, dokumenty i nieaktywna historie zakupow.",
                    countLabel = "${archivedRepairDocumentation.size} wpisow",
                    isExpanded = isRepairDocsExpanded,
                    onToggle = { isRepairDocsExpanded = !isRepairDocsExpanded }
                ) {
                    if (archivedRepairDocumentation.isEmpty()) {
                        EmptyDocumentationRow("Brak zakonczonych napraw w archiwum dokumentacji.")
                    } else {
                        VehicleArea.entries.forEach { area ->
                            val areaDocumentation = documentationByArea[area].orEmpty()
                            RepairDocumentationAreaGroup(
                                area = area,
                                documentation = areaDocumentation,
                                repairProjects = finishedRepairProjects,
                                isExpanded = area in expandedRepairAreas,
                                onDocumentationClick = { documentation ->
                                    selectedDocumentationId = documentation.repairId
                                },
                                onToggle = {
                                    expandedRepairAreas = if (area in expandedRepairAreas) {
                                        expandedRepairAreas - area
                                    } else {
                                        expandedRepairAreas + area
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentationSection(
    title: String,
    subtitle: String,
    countLabel: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable () -> Unit,
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = subtitle,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.66f),
                        maxLines = 3
                    )
                    Text(
                        text = countLabel,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = if (isExpanded) "Zwin" else "Rozwin",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isExpanded) {
                content()
            }
        }
    }
}

@Composable
private fun RepairDocumentationAreaGroup(
    area: VehicleArea,
    documentation: List<RepairDocumentation>,
    repairProjects: List<RepairProject>,
    isExpanded: Boolean,
    onDocumentationClick: (RepairDocumentation) -> Unit,
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(area.iconResource()),
                    contentDescription = area.label,
                    modifier = Modifier.height(30.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = area.label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${documentation.size} wpisow",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = if (isExpanded) "Zwin" else "Rozwin",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isExpanded) {
                if (documentation.isEmpty()) {
                    EmptyDocumentationRow("Brak dokumentacji w tej kategorii.")
                } else {
                    documentation
                        .sortedBy { it.repairTitle.lowercase() }
                        .forEach { item ->
                            RepairDocumentationRow(
                                documentation = item,
                                repair = repairProjects.firstOrNull { repair -> item.belongsToRepair(repair) },
                                onClick = { onDocumentationClick(item) }
                            )
                        }
                }
            }
        }
    }
}

@Composable
private fun EmptyDocumentationRow(text: String) {
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
private fun RepairDocumentationRow(
    documentation: RepairDocumentation,
    repair: RepairProject?,
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Image(
                painter = painterResource(documentation.area.iconResource()),
                contentDescription = documentation.area.label,
                modifier = Modifier.height(30.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Text(
                    text = repair?.title ?: documentation.repairTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Dokumentacja: ${documentation.title}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
                repair?.let {
                    StatusBadge(it.status.normalizedRepairStatusLabel())
                }
                Text(
                    text = documentation.summary,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
                    maxLines = 4
                )
                Text(
                    text = "Otworz dokumentacje",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.82f),
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun RepairDocumentationDetailsScreen(
    vehicle: Vehicle,
    documentation: RepairDocumentation,
    repair: RepairProject?,
    archivedShoppingList: List<ShoppingListItem>,
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var selectedTab by remember(documentation.repairId, documentation.repairTitle) { mutableStateOf("Opis") }
    var isAddingTisLink by remember { mutableStateOf(false) }
    var tisLinkPendingEdit by remember { mutableStateOf<IndexedTisDocumentationLink?>(null) }
    var isAddingYoutubeLink by remember { mutableStateOf(false) }
    var youtubeVideoPendingEdit by remember { mutableStateOf<IndexedYoutubeVideo?>(null) }
    var personalNotePendingEdit by remember { mutableStateOf<IndexedPersonalNote?>(null) }
    var personalNoteDialogType by remember { mutableStateOf<PersonalDocumentationItemType?>(null) }
    var pendingPersonalFileType by remember { mutableStateOf<PersonalDocumentationItemType?>(null) }
    var importExportStatus by remember { mutableStateOf<String?>(null) }
    var isLinksExpanded by remember { mutableStateOf(true) }
    var isTorqueExpanded by remember { mutableStateOf(true) }
    var isYoutubeExpanded by remember { mutableStateOf(true) }
    var isPersonalNotesExpanded by remember { mutableStateOf(true) }
    var isAddingTorqueTable by remember { mutableStateOf(false) }
    var torqueSpecPendingAddTableId by remember { mutableStateOf<String?>(null) }
    var torqueSpecPendingEdit by remember { mutableStateOf<TorqueSpecEditRequest?>(null) }
    var torqueImportStatus by remember { mutableStateOf<String?>(null) }
    var activeTorqueImportTableId by remember { mutableStateOf<String?>(null) }
    var activeDiagramImportTableId by remember { mutableStateOf<String?>(null) }
    var selectedDiagramTorqueByTable by remember { mutableStateOf<Map<String, Int?>>(emptyMap()) }
    var expandedTorqueTableIds by remember(documentation.effectiveTorqueTables()) {
        mutableStateOf(
            documentation.effectiveTorqueTables().map { it.id }.toSet()
        )
    }

    fun updateTorqueTables(tables: List<TorqueSpecTable>) {
        onDocumentationUpdated(
            documentation.copy(
                torqueTables = tables,
                torqueSpecs = tables.firstOrNull()?.torqueSpecs.orEmpty(),
                torqueDiagramImageUri = tables.firstOrNull()?.diagramImageUri,
                torqueDiagramAssignments = tables.firstOrNull()?.diagramAssignments.orEmpty()
            )
        )
    }

    fun updateTorqueTable(tableId: String, transform: (TorqueSpecTable) -> TorqueSpecTable) {
        updateTorqueTables(
            documentation.effectiveTorqueTables().map { table ->
                if (table.id == tableId) transform(table) else table
            }
        )
    }

    fun importTorqueScreenshot(tableId: String, uri: Uri) {
        val bitmap = loadDocumentationBitmapFromUri(context, uri)
        if (bitmap == null) {
            torqueImportStatus = "Nie udalo sie wczytac screenshotu."
            return
        }
        torqueImportStatus = "Odczytuje screenshot TIS..."
        recognizeTorqueSpecsFromBitmap(
            bitmap = bitmap,
            onResult = { torqueSpecs ->
                if (torqueSpecs.isEmpty()) {
                    torqueImportStatus = "Nie udalo sie rozpoznac wierszy tabeli. Sprobuj przyciac screenshot blizej tabeli."
                } else {
                    var addedCount = 0
                    updateTorqueTable(tableId) { table ->
                        val mergedTorqueSpecs = table.torqueSpecs.replaceOcrTorqueSpecs(torqueSpecs)
                        addedCount = mergedTorqueSpecs.size - table.torqueSpecs.size
                        table.copy(torqueSpecs = mergedTorqueSpecs)
                    }
                    torqueImportStatus = if (addedCount == 0) {
                        "OCR rozpoznal wpisy, ale wszystkie byly juz w tabeli."
                    } else {
                        "Dodano $addedCount wpisow z OCR."
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
            activeTorqueImportTableId?.let { tableId ->
                importTorqueScreenshot(tableId, uri)
            }
        }
        activeTorqueImportTableId = null
    }

    val torqueDiagramLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            activeDiagramImportTableId?.let { tableId ->
                val localDiagramUri = copyTorqueDiagramToAppStorage(
                    context = context,
                    sourceUri = uri,
                    repairId = documentation.repairId,
                    tableId = tableId
                ) ?: uri.toString()
                val currentTables = documentation.effectiveTorqueTables()
                val currentTable = currentTables.firstOrNull { it.id == tableId }
                val shouldCreateNewTable = currentTable != null &&
                    (
                        currentTable.diagramImageUri != null ||
                            currentTable.torqueSpecs.isNotEmpty() ||
                            currentTable.diagramAssignments.isNotEmpty()
                    )
                if (shouldCreateNewTable) {
                    val newTable = TorqueSpecTable(
                        id = "table-${System.currentTimeMillis()}",
                        title = "${currentTable.title} - nowy schemat",
                        diagramImageUri = localDiagramUri
                    )
                    updateTorqueTables(currentTables + newTable)
                    expandedTorqueTableIds = expandedTorqueTableIds + newTable.id
                    selectedDiagramTorqueByTable = selectedDiagramTorqueByTable + (newTable.id to null)
                } else {
                    updateTorqueTable(tableId) { table ->
                        table.copy(
                            diagramImageUri = localDiagramUri,
                            diagramAssignments = emptyList()
                        )
                    }
                }
            }
        }
        activeDiagramImportTableId = null
    }

    val personalFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        val type = pendingPersonalFileType
        if (uri != null && type != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onDocumentationUpdated(
                documentation.withAddedPersonalNote(
                    PersonalDocumentationItem(
                        id = "personal-${System.currentTimeMillis()}",
                        type = type,
                        title = type.defaultPersonalTitle(),
                        uri = uri.toString()
                    )
                )
            )
        }
        pendingPersonalFileType = null
    }

    val documentationExportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            importExportStatus = if (exportRepairDocumentationPackage(context, documentation, uri)) {
                "Eksport dokumentacji zakonczony."
            } else {
                "Nie udalo sie wyeksportowac dokumentacji."
            }
        }
    }

    val documentationImportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            val importedDocumentation = importRepairDocumentationPackage(context, uri, documentation)
            if (importedDocumentation != null) {
                onDocumentationUpdated(importedDocumentation)
                importExportStatus = "Import dokumentacji zakonczony."
            } else {
                importExportStatus = "Nie udalo sie zaimportowac dokumentacji."
            }
        }
    }

    if (isAddingTisLink) {
        AddTisLinkDialog(
            onDismiss = { isAddingTisLink = false },
            onSave = { link ->
                onDocumentationUpdated(
                    documentation.withAddedDocumentationTisLink(link)
                )
                isAddingTisLink = false
            }
        )
    }

    tisLinkPendingEdit?.let { indexedLink ->
        AddTisLinkDialog(
            initialLink = indexedLink.link,
            onDismiss = { tisLinkPendingEdit = null },
            onDelete = {
                onDocumentationUpdated(
                    documentation.withRemovedDocumentationTisLink(indexedLink.index)
                )
                tisLinkPendingEdit = null
            },
            onSave = { link ->
                onDocumentationUpdated(
                    documentation.withUpdatedDocumentationTisLink(indexedLink.index, link)
                )
                tisLinkPendingEdit = null
            }
        )
    }

    if (isAddingYoutubeLink) {
        AddYoutubeLinkDialog(
            onDismiss = { isAddingYoutubeLink = false },
            onSave = { video ->
                onDocumentationUpdated(
                    documentation.withAddedDocumentationYoutubeVideo(video)
                )
                isAddingYoutubeLink = false
            }
        )
    }

    youtubeVideoPendingEdit?.let { indexedVideo ->
        AddYoutubeLinkDialog(
            initialVideo = indexedVideo.video,
            onDismiss = { youtubeVideoPendingEdit = null },
            onDelete = {
                onDocumentationUpdated(
                    documentation.withRemovedDocumentationYoutubeVideo(indexedVideo.index)
                )
                youtubeVideoPendingEdit = null
            },
            onSave = { video ->
                onDocumentationUpdated(
                    documentation.withUpdatedDocumentationYoutubeVideo(indexedVideo.index, video)
                )
                youtubeVideoPendingEdit = null
            }
        )
    }

    personalNoteDialogType?.let { type ->
        AddPersonalNoteDialog(
            type = type,
            onDismiss = { personalNoteDialogType = null },
            onSave = { item ->
                onDocumentationUpdated(
                    documentation.withAddedPersonalNote(item)
                )
                personalNoteDialogType = null
            }
        )
    }

    personalNotePendingEdit?.let { indexedItem ->
        AddPersonalNoteDialog(
            type = indexedItem.item.type,
            initialItem = indexedItem.item,
            onDismiss = { personalNotePendingEdit = null },
            onDelete = {
                onDocumentationUpdated(
                    documentation.withRemovedPersonalNote(indexedItem.index)
                )
                personalNotePendingEdit = null
            },
            onSave = { item ->
                onDocumentationUpdated(
                    documentation.withUpdatedPersonalNote(indexedItem.index, item)
                )
                personalNotePendingEdit = null
            }
        )
    }

    if (isAddingTorqueTable) {
        AddTorqueTableDialog(
            tableNumber = documentation.effectiveTorqueTables().size + 1,
            onDismiss = { isAddingTorqueTable = false },
            onSave = { title ->
                val newTable = TorqueSpecTable(
                    id = "table-${System.currentTimeMillis()}",
                    title = title,
                )
                updateTorqueTables(documentation.effectiveTorqueTables() + newTable)
                expandedTorqueTableIds = expandedTorqueTableIds + newTable.id
                selectedDiagramTorqueByTable = selectedDiagramTorqueByTable + (newTable.id to null)
                isAddingTorqueTable = false
            }
        )
    }

    torqueSpecPendingAddTableId?.let { tableId ->
        AddTorqueSpecDialog(
            onDismiss = { torqueSpecPendingAddTableId = null },
            onSave = { spec ->
                updateTorqueTable(tableId) { table ->
                    table.copy(torqueSpecs = table.torqueSpecs + spec)
                }
                torqueSpecPendingAddTableId = null
            }
        )
    }

    torqueSpecPendingEdit?.let { indexedSpec ->
        AddTorqueSpecDialog(
            initialSpec = indexedSpec.spec,
            onDismiss = { torqueSpecPendingEdit = null },
            onDelete = {
                updateTorqueTable(indexedSpec.tableId) { table ->
                    table.copy(
                        torqueSpecs = table.torqueSpecs.filterIndexed { index, _ ->
                            index != indexedSpec.index
                        },
                        diagramAssignments = table.diagramAssignments
                            .afterTorqueSpecRemoved(indexedSpec.index)
                    )
                }
                torqueSpecPendingEdit = null
            },
            onSave = { spec ->
                updateTorqueTable(indexedSpec.tableId) { table ->
                    table.copy(
                        torqueSpecs = table.torqueSpecs.mapIndexed { index, currentSpec ->
                            if (index == indexedSpec.index) spec else currentSpec
                        }
                    )
                }
                torqueSpecPendingEdit = null
            }
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                TextButton(onClick = onBack) {
                    Text("Wroc do dokumentacji")
                }
            }
            item {
                DocumentationRepairHeader(
                    vehicle = vehicle,
                    documentation = documentation,
                    repair = repair
                )
            }
            item {
                SegmentTabs(
                    tabs = listOf("Opis", "Czesci", "Dokumenty", "Momenty", "Notatki"),
                    selectedTab = selectedTab,
                    onSelect = { selectedTab = it }
                )
            }
            when (selectedTab) {
                "Opis" -> item {
                    ArchiveRepairOverviewTab(
                        documentation = documentation,
                        repair = repair
                    )
                }
                "Czesci" -> item {
                    ArchivedRepairPartsTab(shoppingList = archivedShoppingList)
                }
                "Dokumenty" -> item {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        DocumentationTransferActions(
                            status = importExportStatus,
                            onExport = {
                                documentationExportLauncher.launch(
                                    "${documentation.repairTitle.safeExportFileName()}-dokumentacja.bmwdoc.zip"
                                )
                            },
                            onImport = {
                                documentationImportLauncher.launch(
                                    arrayOf(
                                        "application/zip",
                                        "application/octet-stream",
                                        "application/x-zip-compressed"
                                    )
                                )
                            }
                        )
                        DocumentationLinksTile(
                            title = "Schematy",
                            subtitle = "Linki do procedur TIS i schematow powiazanych z ta naprawa.",
                            marker = "${documentation.effectiveDocumentationTisDocuments().size} linkow",
                            links = documentation.effectiveDocumentationTisDocuments(),
                            isExpanded = isLinksExpanded,
                            onToggle = { isLinksExpanded = !isLinksExpanded },
                            onAddLink = { isAddingTisLink = true },
                            onEditLink = { index, link ->
                                tisLinkPendingEdit = IndexedTisDocumentationLink(index, link)
                            }
                        )
                        YoutubeLinksTile(
                            videos = documentation.effectiveDocumentationYoutubeVideos(),
                            isExpanded = isYoutubeExpanded,
                            onToggle = { isYoutubeExpanded = !isYoutubeExpanded },
                            onAddLink = { isAddingYoutubeLink = true },
                            onEditVideo = { index, video ->
                                youtubeVideoPendingEdit = IndexedYoutubeVideo(index, video)
                            }
                        )
                    }
                }
                "Momenty" -> item {
                    TorqueTablesTile(
                        tables = documentation.effectiveTorqueTables(),
                        importStatus = torqueImportStatus,
                        isExpanded = isTorqueExpanded,
                        onToggle = { isTorqueExpanded = !isTorqueExpanded },
                        expandedTableIds = expandedTorqueTableIds,
                        selectedTorqueByTable = selectedDiagramTorqueByTable,
                        onToggleTable = { tableId ->
                            expandedTorqueTableIds = if (tableId in expandedTorqueTableIds) {
                                expandedTorqueTableIds - tableId
                            } else {
                                expandedTorqueTableIds + tableId
                            }
                        },
                        onAddTable = { isAddingTorqueTable = true },
                        onDeleteTable = { tableId ->
                            updateTorqueTables(
                                documentation.effectiveTorqueTables()
                                    .filterNot { it.id == tableId }
                            )
                            expandedTorqueTableIds = expandedTorqueTableIds - tableId
                            selectedDiagramTorqueByTable = selectedDiagramTorqueByTable - tableId
                        },
                        onAddTorqueSpec = { tableId -> torqueSpecPendingAddTableId = tableId },
                        onImportScreenshot = { tableId ->
                            activeTorqueImportTableId = tableId
                            torqueScreenshotLauncher.launch("image/*")
                        },
                        onEditTorqueSpec = { tableId, index, spec ->
                            torqueSpecPendingEdit = TorqueSpecEditRequest(tableId, index, spec)
                        },
                        onSelectedTorqueIndexChanged = { tableId, index ->
                            selectedDiagramTorqueByTable = selectedDiagramTorqueByTable + (tableId to index)
                        },
                        onImportDiagram = { tableId ->
                            activeDiagramImportTableId = tableId
                            torqueDiagramLauncher.launch(arrayOf("image/*"))
                        },
                        onAssignmentAdded = { tableId, assignment ->
                            updateTorqueTable(tableId) { table ->
                                table.copy(
                                    diagramAssignments = table.diagramAssignments.upsertAssignment(assignment)
                                )
                            }
                        },
                        onAssignmentRemoved = { tableId, torqueSpecIndex ->
                            updateTorqueTable(tableId) { table ->
                                table.copy(
                                    diagramAssignments = table.diagramAssignments
                                        .filterNot { it.torqueSpecIndex == torqueSpecIndex }
                                )
                            }
                        },
                        onClearDiagram = { tableId ->
                            updateTorqueTable(tableId) { table ->
                                table.copy(
                                    diagramImageUri = null,
                                    diagramAssignments = emptyList()
                                )
                            }
                        }
                    )
                }
                "Notatki" -> item {
                    PersonalNotesTile(
                        items = documentation.personalNotes,
                        isExpanded = isPersonalNotesExpanded,
                        onToggle = { isPersonalNotesExpanded = !isPersonalNotesExpanded },
                        onAddText = { personalNoteDialogType = PersonalDocumentationItemType.Text },
                        onAddLink = { personalNoteDialogType = PersonalDocumentationItemType.Link },
                        onAddPhoto = {
                            pendingPersonalFileType = PersonalDocumentationItemType.Photo
                            personalFileLauncher.launch(arrayOf("image/*"))
                        },
                        onAddVideo = {
                            pendingPersonalFileType = PersonalDocumentationItemType.Video
                            personalFileLauncher.launch(arrayOf("video/*"))
                        },
                        onAddDocument = {
                            pendingPersonalFileType = PersonalDocumentationItemType.Document
                            personalFileLauncher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "application/msword",
                                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                                    "text/plain"
                                )
                            )
                        },
                        onAddFile = {
                            pendingPersonalFileType = PersonalDocumentationItemType.File
                            personalFileLauncher.launch(arrayOf("*/*"))
                        },
                        onEditItem = { index, item ->
                            personalNotePendingEdit = IndexedPersonalNote(index, item)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DocumentationRepairHeader(
    vehicle: Vehicle,
    documentation: RepairDocumentation,
    repair: RepairProject?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = repair?.title ?: documentation.repairTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${vehicle.displayName.ifBlank { "BMW" }} / ${documentation.area.label}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
            }
            repair?.let {
                StatusBadge(it.status.normalizedRepairStatusLabel())
            }
        }
    }
}

@Composable
private fun ArchiveRepairOverviewTab(
    documentation: RepairDocumentation,
    repair: RepairProject?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Opis", fontWeight = FontWeight.SemiBold)
                Text(
                    text = repair?.problemDescription?.ifBlank { documentation.summary } ?: documentation.summary,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Status archiwum", fontWeight = FontWeight.SemiBold)
                Text(
                    text = "Ten wpis jest dokumentacja zakonczonej naprawy. Dane z listy zakupow sa tutaj tylko historia i nie aktywuja zakupow w programie.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ArchivedRepairPartsTab(shoppingList: List<ShoppingListItem>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Archiwalna lista zakupow",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                if (shoppingList.isEmpty()) {
                    Text(
                        text = "Brak zapisanych pozycji z listy zakupow dla tej dokumentacji.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                } else {
                    shoppingList.forEach { item ->
                        ArchivedShoppingListRow(item)
                    }
                }
            }
        }
    }
}

@Composable
private fun ArchivedShoppingListRow(item: ShoppingListItem) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(46.dp),
            color = MaterialTheme.colorScheme.background.copy(alpha = 0.62f),
            shape = RoundedCornerShape(8.dp)
        ) {
            PartPhotoContent(photoUri = item.imageUri, height = 46.dp)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = item.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = item.manufacturerPartNumber.ifBlank { item.partNumber.ifBlank { item.source } },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.64f),
                fontSize = 13.sp,
                maxLines = 1
            )
        }
        Text(
            text = "${item.quantity} szt.",
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f),
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        )
        Surface(
            color = AccentBlue.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(
                text = "Historia",
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                color = AccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun DocumentationTransferActions(
    status: String?,
    onExport: () -> Unit,
    onImport: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onExport) {
                    Text("Eksportuj")
                }
                TextButton(onClick = onImport) {
                    Text("Importuj")
                }
            }
            if (!status.isNullOrBlank()) {
                Text(
                    text = status,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun AddTisLinkDialog(
    initialLink: TisDocumentationLink? = null,
    onDismiss: () -> Unit,
    onDelete: (() -> Unit)? = null,
    onSave: (TisDocumentationLink) -> Unit,
) {
    var title by remember(initialLink) { mutableStateOf(initialLink?.title.orEmpty()) }
    var link by remember(initialLink) { mutableStateOf(initialLink?.url.orEmpty()) }
    val normalizedTitle = title.trim()
    val normalizedLink = link.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialLink == null) "Dodaj link TIS" else "Edytuj link TIS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Dodaj nazwe i link do procedury lub schematu. Bedzie widoczny w dokumentacji tej naprawy.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                GarageTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Nazwa",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("documentation_tis_title_input"),
                    placeholder = "Np. Demontaz kolektora / TIS"
                )
                GarageTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = "Link",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("documentation_tis_link_input"),
                    placeholder = "https://www.newtis.info/..."
                )
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("documentation_tis_save_button"),
                enabled = normalizedLink.isNotBlank(),
                onClick = {
                    onSave(
                        TisDocumentationLink(
                            title = normalizedTitle.ifBlank { "Link TIS" },
                            url = normalizedLink.withDocumentationHttpsPrefix()
                        )
                    )
                }
            ) {
                Text(if (initialLink == null) "Dodaj" else "Zapisz")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(
                        modifier = Modifier.testTag("documentation_tis_delete_button"),
                        onClick = onDelete
                    ) {
                        Text("Usun")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Anuluj")
                }
            }
        }
    )
}

@Composable
private fun AddYoutubeLinkDialog(
    initialVideo: YoutubeVideo? = null,
    onDismiss: () -> Unit,
    onSave: (YoutubeVideo) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember(initialVideo) { mutableStateOf(initialVideo?.title.orEmpty()) }
    var link by remember(initialVideo) { mutableStateOf(initialVideo?.url.orEmpty()) }
    var note by remember(initialVideo) { mutableStateOf(initialVideo?.note.orEmpty()) }
    var titleWasEdited by remember(initialVideo) { mutableStateOf(initialVideo != null) }
    var titleLookupStatus by remember(initialVideo) { mutableStateOf<String?>(null) }
    val normalizedLink = link.trim()
    val normalizedTitle = title.trim()
    val isEditing = initialVideo != null

    LaunchedEffect(normalizedLink) {
        if (normalizedLink.isBlank() || titleWasEdited) {
            return@LaunchedEffect
        }
        titleLookupStatus = "Pobieram tytul filmu..."
        val fetchedTitle = fetchYoutubeTitle(normalizedLink.withDocumentationHttpsPrefix())
        if (fetchedTitle.isNullOrBlank()) {
            titleLookupStatus = "Nie udalo sie pobrac tytulu. Mozesz wpisac go recznie."
        } else {
            title = fetchedTitle
            titleLookupStatus = "Tytul pobrany automatycznie."
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edytuj film YouTube" else "Dodaj film YouTube") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Dodaj tytul, link i krotka notatke, zeby pozniej latwo znalezc film przy naprawie.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                GarageTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        titleWasEdited = true
                    },
                    label = "Tytul filmu",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("documentation_youtube_title_input"),
                    placeholder = "np. Demontaz tylnej zwrotnicy E60"
                )
                titleLookupStatus?.let { status ->
                    Text(
                        text = status,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                GarageTextField(
                    value = link,
                    onValueChange = {
                        link = it
                        if (!titleWasEdited) {
                            title = ""
                        }
                    },
                    label = "Link YouTube",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("documentation_youtube_link_input"),
                    placeholder = "https://www.youtube.com/watch?v=..."
                )
                GarageTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = "Notatka",
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("documentation_youtube_note_input"),
                    placeholder = "np. pokazuje uklad wahaczy od 08:30",
                    singleLine = false,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                modifier = Modifier.testTag("documentation_youtube_save_button"),
                enabled = normalizedLink.isNotBlank(),
                onClick = {
                    val normalizedUrl = normalizedLink.withDocumentationHttpsPrefix()
                    onSave(
                        YoutubeVideo(
                            title = normalizedTitle.ifBlank {
                                normalizedUrl.youtubeVideoId()?.let { "Film YouTube $it" } ?: "Film YouTube"
                            },
                            url = normalizedUrl,
                            note = note.trim()
                        )
                    )
                }
            ) {
                Text(if (isEditing) "Zapisz" else "Dodaj")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(
                        modifier = Modifier.testTag("documentation_youtube_delete_button"),
                        onClick = onDelete
                    ) {
                        Text("Usun")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Anuluj")
                }
            }
        }
    )
}

@Composable
private fun AddPersonalNoteDialog(
    type: PersonalDocumentationItemType,
    initialItem: PersonalDocumentationItem? = null,
    onDismiss: () -> Unit,
    onSave: (PersonalDocumentationItem) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var title by remember(initialItem, type) {
        mutableStateOf(initialItem?.title ?: type.defaultPersonalTitle())
    }
    var text by remember(initialItem) { mutableStateOf(initialItem?.text.orEmpty()) }
    var url by remember(initialItem) { mutableStateOf(initialItem?.url.orEmpty()) }
    val normalizedTitle = title.trim()
    val canSave = normalizedTitle.isNotBlank() &&
        when (type) {
            PersonalDocumentationItemType.Link -> url.trim().isNotBlank()
            else -> text.trim().isNotBlank()
        }
    val isEditing = initialItem != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edytuj wpis" else "Dodaj ${type.personalLabel()}") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GarageTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Tytul",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = type.defaultPersonalTitle()
                )
                if (type == PersonalDocumentationItemType.Link) {
                    GarageTextField(
                        value = url,
                        onValueChange = { url = it },
                        label = "Link",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "https://..."
                    )
                    GarageTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = "Notatka",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Dlaczego ten link jest wazny?",
                        singleLine = false,
                        minLines = 3
                    )
                } else {
                    GarageTextField(
                        value = text,
                        onValueChange = { text = it },
                        label = "Tresc",
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = "Zapisz obserwacje, decyzje albo rzeczy do sprawdzenia.",
                        singleLine = false,
                        minLines = 5
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        PersonalDocumentationItem(
                            id = initialItem?.id ?: "personal-${System.currentTimeMillis()}",
                            type = type,
                            title = normalizedTitle,
                            text = text.trim(),
                            uri = initialItem?.uri,
                            url = url.trim().takeIf { it.isNotBlank() }?.withDocumentationHttpsPrefix()
                        )
                    )
                }
            ) {
                Text(if (isEditing) "Zapisz" else "Dodaj")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Usun")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Anuluj")
                }
            }
        }
    )
}

@Composable
private fun DocumentationLinksTile(
    title: String,
    subtitle: String,
    marker: String,
    links: List<TisDocumentationLink>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddLink: () -> Unit,
    onEditLink: (Int, TisDocumentationLink) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
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
                    text = if (isExpanded) "Zwin" else marker,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isExpanded) {
                return@Column
            }

            if (links.isEmpty()) {
                Text(
                    text = "Brak dodanych linkow.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            } else {
                links.forEachIndexed { index, link ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { uriHandler.openUri(link.url) },
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Text(
                                    text = link.title.ifBlank { "TIS ${index + 1}" },
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = link.url,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 12.sp,
                                    maxLines = 2
                                )
                            }
                            TextButton(onClick = { onEditLink(index, link) }) {
                                Text("Edytuj")
                            }
                        }
                    }
                }
            }

            TextButton(
                modifier = Modifier.testTag("documentation_add_tis_button"),
                onClick = onAddLink
            ) {
                Text("Dodaj link TIS")
            }
        }
    }
}

@Composable
private fun AddTorqueTableDialog(
    tableNumber: Int,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var title by remember { mutableStateOf("Tabela momentow $tableNumber") }
    val normalizedTitle = title.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj tabele momentow") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Kazda tabela moze miec wlasny import momentow, wlasny schemat i wlasne punkty na obrazie.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                GarageTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = "Nazwa tabeli",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. Tylna zwrotnica - wahacze"
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalizedTitle.isNotBlank(),
                onClick = { onSave(normalizedTitle) }
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
fun AddTorqueSpecDialog(
    initialSpec: TorqueSpec? = null,
    onDismiss: () -> Unit,
    onSave: (TorqueSpec) -> Unit,
    onDelete: (() -> Unit)? = null,
) {
    var component by remember(initialSpec) { mutableStateOf(initialSpec?.component.orEmpty()) }
    var type by remember(initialSpec) { mutableStateOf(initialSpec?.type.orEmpty()) }
    var thread by remember(initialSpec) { mutableStateOf(initialSpec?.thread.orEmpty()) }
    var tighteningSpecifications by remember(initialSpec) {
        mutableStateOf(initialSpec?.tighteningSpecifications.orEmpty())
    }
    var torque by remember(initialSpec) { mutableStateOf(initialSpec?.torque.orEmpty()) }
    var source by remember(initialSpec) { mutableStateOf(initialSpec?.source ?: "TIS") }
    var notes by remember(initialSpec) { mutableStateOf(initialSpec?.notes.orEmpty()) }
    val canSave = component.isNotBlank() && torque.isNotBlank()
    val isEditing = initialSpec != null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isEditing) "Edytuj moment dokrecenia" else "Dodaj moment dokrecenia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isEditing) {
                        "Popraw dane odczytane z TIS albo uzupelnij brakujace kolumny."
                    } else {
                        "Wpis reczny uzupelnia te same kolumny, ktore wypelnia import ze screenshotu."
                    },
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                GarageTextField(
                    value = component,
                    onValueChange = { component = it },
                    label = "Element / sruba",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. Swieca zarowa"
                )
                GarageTextField(
                    value = torque,
                    onValueChange = { torque = it },
                    label = "Moment",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. 13 Nm"
                )
                GarageTextField(
                    value = type,
                    onValueChange = { type = it },
                    label = "Typ",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. M47 / M47TU"
                )
                GarageTextField(
                    value = thread,
                    onValueChange = { thread = it },
                    label = "Gwint",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. M10"
                )
                GarageTextField(
                    value = tighteningSpecifications,
                    onValueChange = { tighteningSpecifications = it },
                    label = "Specyfikacja dokrecania",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. nowa sruba / kat dokrecania"
                )
                GarageTextField(
                    value = source,
                    onValueChange = { source = it },
                    label = "Zrodlo",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "TIS / Czescidobmw / wlasne"
                )
                GarageTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = "Uwagi",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "np. zimny/cieply silnik, nowa sruba"
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    onSave(
                        TorqueSpec(
                            component = component.trim(),
                            type = type.trim(),
                            thread = thread.trim(),
                            tighteningSpecifications = tighteningSpecifications.trim(),
                            torque = torque.trim(),
                            source = source.trim().ifBlank { "TIS" },
                            notes = notes.trim()
                        )
                    )
                }
            ) {
                Text(if (isEditing) "Zapisz" else "Dodaj")
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) {
                        Text("Usun")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Anuluj")
                }
            }
        }
    )
}

@Composable
private fun TorqueTablesTile(
    tables: List<TorqueSpecTable>,
    importStatus: String?,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    expandedTableIds: Set<String>,
    selectedTorqueByTable: Map<String, Int?>,
    onToggleTable: (String) -> Unit,
    onAddTable: () -> Unit,
    onDeleteTable: (String) -> Unit,
    onAddTorqueSpec: (String) -> Unit,
    onImportScreenshot: (String) -> Unit,
    onEditTorqueSpec: (String, Int, TorqueSpec) -> Unit,
    onSelectedTorqueIndexChanged: (String, Int) -> Unit,
    onImportDiagram: (String) -> Unit,
    onAssignmentAdded: (String, TorqueDiagramAssignment) -> Unit,
    onAssignmentRemoved: (String, Int) -> Unit,
    onClearDiagram: (String) -> Unit,
) {
    val totalTorqueSpecs = tables.sumOf { it.torqueSpecs.size }
    val totalAssignments = tables.sumOf { table ->
        table.diagramAssignments.count { it.torqueSpecIndex in table.torqueSpecs.indices }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Tabele momentow dokrecen",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Dodawaj osobne tabele dla roznych schematow tej samej naprawy. Kazda tabela ma wlasny OCR, schemat i punkty.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        maxLines = 4
                    )
                }
                Text(
                    text = if (isExpanded) "Zwin" else "${tables.size} tabel / $totalTorqueSpecs wpisow",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isExpanded) {
                return@Column
            }

            if (tables.isEmpty()) {
                Text(
                    text = "Brak tabel momentow. Dodaj pierwsza tabele, a potem wgraj screenshot TIS i schemat.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            } else {
                Text(
                    text = "$totalAssignments punktow przypisanych do schematow",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
                tables.forEachIndexed { index, table ->
                    TorqueTableSection(
                        table = table,
                        tableNumber = index + 1,
                        isExpanded = table.id in expandedTableIds,
                        selectedTorqueIndex = selectedTorqueByTable[table.id]
                            ?: table.torqueSpecs.indices.firstOrNull(),
                        onToggle = { onToggleTable(table.id) },
                        onDeleteTable = { onDeleteTable(table.id) },
                        onAddTorqueSpec = { onAddTorqueSpec(table.id) },
                        onImportScreenshot = { onImportScreenshot(table.id) },
                        onEditTorqueSpec = { specIndex, spec ->
                            onEditTorqueSpec(table.id, specIndex, spec)
                        },
                        onSelectedTorqueIndexChanged = { specIndex ->
                            onSelectedTorqueIndexChanged(table.id, specIndex)
                        },
                        onImportDiagram = { onImportDiagram(table.id) },
                        onAssignmentAdded = { assignment ->
                            onAssignmentAdded(table.id, assignment)
                        },
                        onAssignmentRemoved = { specIndex ->
                            onAssignmentRemoved(table.id, specIndex)
                        },
                        onClearDiagram = { onClearDiagram(table.id) }
                    )
                }
            }

            importStatus?.let { status ->
                Text(
                    text = status,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onAddTable) {
                    Text("Dodaj tabele")
                }
            }
        }
    }
}

@Composable
private fun TorqueTableSection(
    table: TorqueSpecTable,
    tableNumber: Int,
    isExpanded: Boolean,
    selectedTorqueIndex: Int?,
    onToggle: () -> Unit,
    onDeleteTable: () -> Unit,
    onAddTorqueSpec: () -> Unit,
    onImportScreenshot: () -> Unit,
    onEditTorqueSpec: (Int, TorqueSpec) -> Unit,
    onSelectedTorqueIndexChanged: (Int) -> Unit,
    onImportDiagram: () -> Unit,
    onAssignmentAdded: (TorqueDiagramAssignment) -> Unit,
    onAssignmentRemoved: (Int) -> Unit,
    onClearDiagram: () -> Unit,
) {
    val validAssignments = table.diagramAssignments.count { it.torqueSpecIndex in table.torqueSpecs.indices }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TorqueNumberBadge(
                    label = tableNumber.toString(),
                    selected = isExpanded,
                    modifier = Modifier.size(34.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = table.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2
                    )
                    Text(
                        text = "${table.torqueSpecs.size} wpisow / $validAssignments punktow",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(
                    text = if (isExpanded) "Zwin" else "Rozwin",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (isExpanded) {
                TorqueSpecsMiniTile(
                    torqueSpecs = table.torqueSpecs,
                    onAddTorqueSpec = onAddTorqueSpec,
                    onImportScreenshot = onImportScreenshot,
                    onEditTorqueSpec = onEditTorqueSpec
                )
                TorqueDiagramTile(
                    torqueSpecs = table.torqueSpecs,
                    imageUri = table.diagramImageUri,
                    assignments = table.diagramAssignments,
                    selectedTorqueIndex = selectedTorqueIndex,
                    onSelectedTorqueIndexChanged = onSelectedTorqueIndexChanged,
                    onImportDiagram = onImportDiagram,
                    onAssignmentAdded = onAssignmentAdded,
                    onAssignmentRemoved = onAssignmentRemoved,
                    onClearDiagram = onClearDiagram
                )
                TextButton(onClick = onDeleteTable) {
                    Text("Usun te tabele")
                }
            }
        }
    }
}

@Composable
private fun TorqueSpecsMiniTile(
    torqueSpecs: List<TorqueSpec>,
    onAddTorqueSpec: () -> Unit,
    onImportScreenshot: () -> Unit,
    onEditTorqueSpec: (Int, TorqueSpec) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (torqueSpecs.isEmpty()) {
            Text(
                text = "Brak dodanych momentow w tej tabeli.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp
            )
        } else {
            TorqueSpecsTable(
                torqueSpecs = torqueSpecs,
                onEditTorqueSpec = onEditTorqueSpec
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onAddTorqueSpec) {
                Text("Dodaj moment")
            }
            TextButton(onClick = onImportScreenshot) {
                Text("Import ze screenshotu")
            }
        }
    }
}

@Composable
private fun TorqueDiagramTile(
    torqueSpecs: List<TorqueSpec>,
    imageUri: String?,
    assignments: List<TorqueDiagramAssignment>,
    selectedTorqueIndex: Int?,
    onSelectedTorqueIndexChanged: (Int) -> Unit,
    onImportDiagram: () -> Unit,
    onAssignmentAdded: (TorqueDiagramAssignment) -> Unit,
    onAssignmentRemoved: (Int) -> Unit,
    onClearDiagram: () -> Unit,
) {
    val context = LocalContext.current
    var isDiagramPreviewOpen by remember { mutableStateOf(false) }
    var isDiagramEditMode by remember { mutableStateOf(false) }
    val diagramBitmap = remember(imageUri) {
        imageUri?.let { loadDocumentationBitmapFromUri(context, Uri.parse(it)) }
    }
    val validAssignments = assignments.filter { it.torqueSpecIndex in torqueSpecs.indices }
    val selectedSpec = selectedTorqueIndex
        ?.takeIf { it in torqueSpecs.indices }
        ?.let { torqueSpecs[it] }

    if (isDiagramPreviewOpen && diagramBitmap != null) {
        TorqueDiagramPreviewDialog(
            bitmap = diagramBitmap,
            torqueSpecs = torqueSpecs,
            assignments = validAssignments,
            selectedTorqueIndex = selectedTorqueIndex,
            selectedSpec = selectedSpec,
            isEditMode = isDiagramEditMode,
            onSelectedTorqueIndexChanged = onSelectedTorqueIndexChanged,
            onStartEditing = { isDiagramEditMode = true },
            onStopEditing = { isDiagramEditMode = false },
            onDismiss = {
                isDiagramPreviewOpen = false
                isDiagramEditMode = false
            },
            onAssignmentAdded = onAssignmentAdded,
            onAssignmentRemoved = onAssignmentRemoved
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Schemat momentow",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Kliknij schemat, zeby otworzyc wiekszy podglad. Punkty mozna nanosic tylko w osobnym trybie edycji.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        maxLines = 4
                    )
                }
                Text(
                    text = "${validAssignments.size} punktow",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (torqueSpecs.isEmpty()) {
                Text(
                    text = "Najpierw dodaj albo zaimportuj momenty dokrecen.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            } else {
                TorqueSpecSelector(
                    torqueSpecs = torqueSpecs,
                    selectedTorqueIndex = selectedTorqueIndex,
                    onSelectedTorqueIndexChanged = onSelectedTorqueIndexChanged
                )
            }

            if (diagramBitmap == null) {
                Text(
                    text = "Brak wgranego schematu dla tej tabeli.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            } else {
                TorqueDiagramImage(
                    bitmap = diagramBitmap,
                    assignments = validAssignments,
                    torqueSpecs = torqueSpecs,
                    selectedTorqueIndex = selectedTorqueIndex,
                    isEditMode = false,
                    onDiagramTap = { isDiagramPreviewOpen = true }
                )

                if (selectedSpec != null) {
                    Text(
                        text = "Aktywny rekord: ${selectedSpec.component} / ${selectedSpec.torque}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (validAssignments.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        validAssignments
                            .sortedBy { it.torqueSpecIndex }
                            .forEach { assignment ->
                                val spec = torqueSpecs[assignment.torqueSpecIndex]
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        TorqueNumberBadge(
                                            label = "${assignment.torqueSpecIndex + 1}",
                                            selected = assignment.torqueSpecIndex == selectedTorqueIndex,
                                            modifier = Modifier.size(34.dp)
                                        )
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Text(
                                                text = spec.component,
                                                fontWeight = FontWeight.SemiBold,
                                                maxLines = 1
                                            )
                                            Text(
                                                text = spec.torque,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onImportDiagram) {
                    Text(if (imageUri == null) "Wgraj schemat" else "Zmien schemat")
                }
                if (imageUri != null) {
                    TextButton(onClick = { isDiagramPreviewOpen = true }) {
                        Text("Otworz schemat")
                    }
                    TextButton(onClick = onClearDiagram) {
                        Text("Usun schemat")
                    }
                }
            }
        }
    }
}

@Composable
private fun TorqueDiagramPreviewDialog(
    bitmap: Bitmap,
    torqueSpecs: List<TorqueSpec>,
    assignments: List<TorqueDiagramAssignment>,
    selectedTorqueIndex: Int?,
    selectedSpec: TorqueSpec?,
    isEditMode: Boolean,
    onSelectedTorqueIndexChanged: (Int) -> Unit,
    onStartEditing: () -> Unit,
    onStopEditing: () -> Unit,
    onDismiss: () -> Unit,
    onAssignmentAdded: (TorqueDiagramAssignment) -> Unit,
    onAssignmentRemoved: (Int) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(if (isEditMode) "Nanoszenie momentow" else "Podglad schematu")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEditMode) {
                    Text(
                        text = "Wybierz rekord i kliknij miejsce na schemacie. Dopiero w tym trybie mozna zmieniac polozenie punktow.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                    TorqueSpecSelector(
                        torqueSpecs = torqueSpecs,
                        selectedTorqueIndex = selectedTorqueIndex,
                        onSelectedTorqueIndexChanged = onSelectedTorqueIndexChanged
                    )
                } else {
                    Text(
                        text = "To jest bezpieczny podglad. Klikniecie schematu nie zmienia punktow.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                }

                TorqueDiagramImage(
                    bitmap = bitmap,
                    assignments = assignments,
                    torqueSpecs = torqueSpecs,
                    selectedTorqueIndex = selectedTorqueIndex,
                    isEditMode = isEditMode,
                    onDiagramTap = { tap ->
                        if (!isEditMode) return@TorqueDiagramImage
                        val specIndex = selectedTorqueIndex ?: return@TorqueDiagramImage
                        if (specIndex !in torqueSpecs.indices) return@TorqueDiagramImage
                        onAssignmentAdded(
                            TorqueDiagramAssignment(
                                torqueSpecIndex = specIndex,
                                xRatio = tap.x.coerceIn(0f, 1f),
                                yRatio = tap.y.coerceIn(0f, 1f)
                            )
                        )
                    }
                )

                if (selectedSpec != null) {
                    Text(
                        text = "Aktywny rekord: ${selectedSpec.component} / ${selectedSpec.torque}",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (assignments.isNotEmpty()) {
                    TorqueAssignmentList(
                        assignments = assignments,
                        torqueSpecs = torqueSpecs,
                        selectedTorqueIndex = selectedTorqueIndex,
                        isEditMode = isEditMode,
                        onAssignmentRemoved = onAssignmentRemoved
                    )
                }
            }
        },
        confirmButton = {
            if (isEditMode) {
                TextButton(onClick = onStopEditing) {
                    Text("Zakoncz nanoszenie")
                }
            } else {
                TextButton(
                    enabled = torqueSpecs.isNotEmpty(),
                    onClick = onStartEditing
                ) {
                    Text("Nanies momenty")
                }
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
private fun TorqueAssignmentList(
    assignments: List<TorqueDiagramAssignment>,
    torqueSpecs: List<TorqueSpec>,
    selectedTorqueIndex: Int?,
    isEditMode: Boolean,
    onAssignmentRemoved: (Int) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        assignments
            .sortedBy { it.torqueSpecIndex }
            .forEach { assignment ->
                val spec = torqueSpecs[assignment.torqueSpecIndex]
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TorqueNumberBadge(
                            label = "${assignment.torqueSpecIndex + 1}",
                            selected = assignment.torqueSpecIndex == selectedTorqueIndex,
                            modifier = Modifier.size(34.dp)
                        )
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = spec.component,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1
                            )
                            Text(
                                text = spec.torque,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                                fontSize = 12.sp
                            )
                        }
                        if (isEditMode) {
                            TextButton(
                                onClick = { onAssignmentRemoved(assignment.torqueSpecIndex) }
                            ) {
                                Text("Usun punkt")
                            }
                        }
                    }
                }
            }
    }
}

@Composable
private fun TorqueSpecSelector(
    torqueSpecs: List<TorqueSpec>,
    selectedTorqueIndex: Int?,
    onSelectedTorqueIndexChanged: (Int) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        torqueSpecs.forEachIndexed { index, spec ->
            val selected = selectedTorqueIndex == index
            Surface(
                modifier = Modifier.clickable { onSelectedTorqueIndexChanged(index) },
                color = if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                } else {
                    MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
                },
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .width(190.dp)
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "${index + 1}. ${spec.component}",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        maxLines = 2
                    )
                    Text(
                        text = spec.torque,
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun TorqueDiagramImage(
    bitmap: Bitmap,
    assignments: List<TorqueDiagramAssignment>,
    torqueSpecs: List<TorqueSpec>,
    selectedTorqueIndex: Int?,
    isEditMode: Boolean,
    onDiagramTap: (Offset) -> Unit,
) {
    val aspectRatio = bitmap.width.toFloat() / bitmap.height.toFloat()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.32f),
                shape = RoundedCornerShape(8.dp)
            )
            .pointerInput(selectedTorqueIndex, bitmap, isEditMode) {
                detectTapGestures { offset ->
                    val width = size.width.toFloat().coerceAtLeast(1f)
                    val height = size.height.toFloat().coerceAtLeast(1f)
                    onDiagramTap(
                        Offset(
                            x = offset.x / width,
                            y = offset.y / height
                        )
                    )
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
                contentDescription = "Schemat z przypisanymi momentami dokrecen",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillBounds
            )

            assignments.forEach { assignment ->
                val spec = torqueSpecs.getOrNull(assignment.torqueSpecIndex)
                val selected = assignment.torqueSpecIndex == selectedTorqueIndex
                TorqueDiagramMarker(
                    label = "${assignment.torqueSpecIndex + 1}",
                    torque = spec?.torque.orEmpty(),
                    selected = selected,
                    modifier = Modifier.offset(
                        x = (diagramWidth * assignment.xRatio) - 13.dp,
                        y = (diagramHeight * assignment.yRatio) - 13.dp
                    )
                )
            }
        }
    }
}

@Composable
private fun TorqueDiagramMarker(
    label: String,
    torque: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        TorqueNumberBadge(
            label = label,
            selected = selected,
            modifier = Modifier.size(if (selected) 38.dp else 34.dp)
        )
        if (selected && torque.isNotBlank()) {
            Surface(
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = torque,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun TorqueNumberBadge(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = if (selected) 5.dp else 3.dp,
                shape = CircleShape,
                clip = false
            )
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .border(
                width = if (selected) 4.dp else 2.dp,
                color = if (selected) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.surface
                },
                shape = CircleShape
            )
            .padding(2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
private fun TorqueSpecsTable(
    torqueSpecs: List<TorqueSpec>,
    onEditTorqueSpec: (Int, TorqueSpec) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TorqueSpecTableRow(
            component = "Element",
            type = "Typ",
            thread = "Gwint",
            tighteningSpecifications = "Specyfikacja",
            torque = "Moment",
            source = "Zrodlo",
            notes = "Uwagi",
            isHeader = true
        )
        torqueSpecs
            .mapIndexed { index, spec -> IndexedTorqueSpec(index, spec) }
            .sortedBy { it.component.lowercase() }
            .forEach { indexedSpec ->
                TorqueSpecTableRow(
                    component = indexedSpec.spec.component,
                    type = indexedSpec.spec.type,
                    thread = indexedSpec.spec.thread,
                    tighteningSpecifications = indexedSpec.spec.tighteningSpecifications,
                    torque = indexedSpec.spec.torque,
                    source = indexedSpec.spec.source,
                    notes = indexedSpec.spec.notes,
                    isHeader = false,
                    onClick = {
                        onEditTorqueSpec(indexedSpec.index, indexedSpec.spec)
                    }
                )
            }
    }
}

private data class IndexedTorqueSpec(
    val index: Int,
    val spec: TorqueSpec,
) {
    val component: String
        get() = spec.component
}

private data class TorqueSpecEditRequest(
    val tableId: String,
    val index: Int,
    val spec: TorqueSpec,
)

private data class IndexedTisDocumentationLink(
    val index: Int,
    val link: TisDocumentationLink,
)

private data class IndexedYoutubeVideo(
    val index: Int,
    val video: YoutubeVideo,
)

private data class IndexedPersonalNote(
    val index: Int,
    val item: PersonalDocumentationItem,
)

@Composable
private fun TorqueSpecTableRow(
    component: String,
    type: String,
    thread: String,
    tighteningSpecifications: String,
    torque: String,
    source: String,
    notes: String,
    isHeader: Boolean,
    onClick: (() -> Unit)? = null,
) {
    val rowColor = if (isHeader) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
    }
    val weight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal

    Row(
        modifier = Modifier
            .width(1336.dp)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TorqueSpecCell(text = component, width = 220.dp, color = rowColor, fontWeight = weight)
        TorqueSpecCell(text = type.ifBlank { "-" }, width = 180.dp, color = rowColor, fontWeight = weight)
        TorqueSpecCell(text = thread.ifBlank { "-" }, width = 120.dp, color = rowColor, fontWeight = weight)
        TorqueSpecCell(
            text = tighteningSpecifications.ifBlank { "-" },
            width = 190.dp,
            color = rowColor,
            fontWeight = weight
        )
        TorqueSpecCell(text = torque, width = 120.dp, color = rowColor, fontWeight = weight)
        TorqueSpecCell(text = source, width = 130.dp, color = rowColor, fontWeight = weight)
        TorqueSpecCell(text = notes.ifBlank { "-" }, width = 270.dp, color = rowColor, fontWeight = weight)
        TorqueSpecCell(
            text = if (isHeader) "Akcja" else "Edytuj",
            width = 90.dp,
            color = rowColor,
            fontWeight = weight
        )
    }
}

@Composable
private fun TorqueSpecCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: androidx.compose.ui.graphics.Color,
    fontWeight: FontWeight,
) {
    Surface(
        modifier = Modifier.width(width),
        color = color,
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp),
            fontSize = 12.sp,
            fontWeight = fontWeight,
            maxLines = 3
        )
    }
}

@Composable
private fun YoutubeLinksTile(
    videos: List<YoutubeVideo>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddLink: () -> Unit,
    onEditVideo: (Int, YoutubeVideo) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "YouTube",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Filmy pokazujace podobna naprawe, demontaz, diagnostyke albo konkretne objawy.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        maxLines = 4
                    )
                }
                Text(
                    text = if (isExpanded) "Zwin" else "${videos.size} filmow",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isExpanded) {
                return@Column
            }

            if (videos.isEmpty()) {
                Text(
                    text = "Brak dodanych filmow.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            } else {
                videos.forEachIndexed { index, video ->
                    YoutubeLinkRow(
                        video = video,
                        onOpen = { uriHandler.openUri(video.url) },
                        onEdit = { onEditVideo(index, video) }
                    )
                }
            }

            TextButton(
                modifier = Modifier.testTag("documentation_add_youtube_button"),
                onClick = onAddLink
            ) {
                Text("Dodaj film YouTube")
            }
        }
    }
}

@Composable
private fun YoutubeLinkRow(
    video: YoutubeVideo,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
) {
    val videoId = remember(video.url) { video.url.youtubeVideoId() }
    val thumbnailUrl = remember(videoId) {
        videoId?.let { "https://img.youtube.com/vi/$it/hqdefault.jpg" }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            YoutubeThumbnail(
                thumbnailUrl = thumbnailUrl,
                modifier = Modifier
                    .width(112.dp)
                    .height(64.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Text(
                    text = video.url,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    maxLines = 2
                )
                if (video.note.isNotBlank()) {
                    Text(
                        text = video.note,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        maxLines = 3
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TextButton(onClick = onOpen) {
                    Text("Otworz")
                }
                TextButton(onClick = onEdit) {
                    Text("Edytuj")
                }
            }
        }
    }
}

@Composable
private fun YoutubeThumbnail(
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    var bitmap by remember(thumbnailUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(thumbnailUrl) {
        bitmap = thumbnailUrl?.let { loadBitmapFromUrl(it) }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        shape = RoundedCornerShape(6.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
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
}

@Composable
private fun PersonalNotesTile(
    items: List<PersonalDocumentationItem>,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    onAddText: () -> Unit,
    onAddPhoto: () -> Unit,
    onAddVideo: () -> Unit,
    onAddDocument: () -> Unit,
    onAddLink: () -> Unit,
    onAddFile: () -> Unit,
    onEditItem: (Int, PersonalDocumentationItem) -> Unit,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var previewItem by remember { mutableStateOf<PersonalDocumentationItem?>(null) }

    previewItem?.let { item ->
        PersonalMediaPreviewDialog(
            item = item,
            onDismiss = { previewItem = null },
            onOpen = {
                openPersonalDocumentationItem(context, uriHandler, item)
            }
        )
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Notatki wlasne",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Twoje obserwacje, zdjecia, filmy, dokumenty, linki i pliki powiazane z ta naprawa.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        maxLines = 4
                    )
                }
                Text(
                    text = if (isExpanded) "Zwin" else "${items.size} wpisow",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (!isExpanded) {
                return@Column
            }

            if (items.isEmpty()) {
                Text(
                    text = "Brak wlasnych materialow dla tej naprawy.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            } else {
                PersonalDocumentationItemType.entries.forEach { type ->
                    val typedItems = items
                        .mapIndexed { index, item -> IndexedPersonalNote(index, item) }
                        .filter { it.item.type == type }
                    if (typedItems.isNotEmpty()) {
                        PersonalNoteCategory(
                            type = type,
                            count = typedItems.size
                        ) {
                            typedItems.forEach { indexedItem ->
                                PersonalNoteRow(
                                    item = indexedItem.item,
                                    onOpen = {
                                        openPersonalDocumentationItem(context, uriHandler, indexedItem.item)
                                    },
                                    onPreview = {
                                        previewItem = indexedItem.item
                                    },
                                    onEdit = { onEditItem(indexedItem.index, indexedItem.item) }
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onAddText) { Text("Notatka") }
                TextButton(onClick = onAddPhoto) { Text("Zdjecie") }
                TextButton(onClick = onAddVideo) { Text("Film") }
                TextButton(onClick = onAddDocument) { Text("Dokument") }
                TextButton(onClick = onAddLink) { Text("Link") }
                TextButton(onClick = onAddFile) { Text("Plik") }
            }
        }
    }
}

@Composable
private fun PersonalNoteCategory(
    type: PersonalDocumentationItemType,
    count: Int,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = type.categoryLabel(),
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$count",
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        content()
    }
}

@Composable
private fun PersonalNoteRow(
    item: PersonalDocumentationItem,
    onOpen: () -> Unit,
    onPreview: () -> Unit,
    onEdit: () -> Unit,
) {
    val canPreview = item.type == PersonalDocumentationItemType.Photo ||
        item.type == PersonalDocumentationItemType.Video

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PersonalItemThumbnail(
                item = item,
                modifier = Modifier
                    .width(82.dp)
                    .height(62.dp)
                    .then(if (canPreview) Modifier.clickable(onClick = onPreview) else Modifier)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = item.title,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2
                )
                Text(
                    text = item.type.personalLabel(),
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
                val detail = item.text
                if (detail.isNotBlank()) {
                    Text(
                        text = detail,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        fontSize = 12.sp,
                        maxLines = 3
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (canPreview) {
                    TextButton(onClick = onPreview) {
                        Text("Podglad")
                    }
                }
                if (item.url != null || item.uri != null) {
                    TextButton(onClick = onOpen) {
                        Text("Otworz")
                    }
                }
                TextButton(onClick = onEdit) {
                    Text("Edytuj")
                }
            }
        }
    }
}

@Composable
private fun PersonalItemThumbnail(
    item: PersonalDocumentationItem,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var bitmap by remember(item.uri, item.type) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.uri, item.type) {
        bitmap = when (item.type) {
            PersonalDocumentationItemType.Photo -> item.uri?.let {
                loadDocumentationBitmapFromUri(context, Uri.parse(it))
            }
            PersonalDocumentationItemType.Video -> item.uri?.let {
                loadVideoThumbnail(context, Uri.parse(it))
            }
            else -> null
        }
    }

    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
        shape = RoundedCornerShape(8.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = item.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                if (item.type == PersonalDocumentationItemType.Video) {
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
                }
            } else {
                Text(
                    text = item.type.shortLabel(),
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun PersonalMediaPreviewDialog(
    item: PersonalDocumentationItem,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
) {
    val context = LocalContext.current
    var bitmap by remember(item.uri, item.type) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(item.uri, item.type) {
        bitmap = when (item.type) {
            PersonalDocumentationItemType.Photo -> item.uri?.let {
                loadDocumentationBitmapFromUri(context, Uri.parse(it))
            }
            PersonalDocumentationItemType.Video -> item.uri?.let {
                loadVideoThumbnail(context, Uri.parse(it))
            }
            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(item.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp),
                    color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap!!.asImageBitmap(),
                                contentDescription = item.title,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Text(
                                text = item.type.personalLabel(),
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
                if (item.text.isNotBlank()) {
                    Text(
                        text = item.text,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                }
            }
        },
        confirmButton = {
            if (item.type == PersonalDocumentationItemType.Video) {
                TextButton(onClick = onOpen) {
                    Text("Otworz film")
                }
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
private fun DocumentationMaterialTile(
    title: String,
    subtitle: String,
    marker: String,
) {
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
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
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

private fun String.withDocumentationHttpsPrefix(): String =
    if (startsWith("http://") || startsWith("https://")) {
        this
    } else {
        "https://$this"
    }

private fun exportRepairDocumentationPackage(
    context: Context,
    documentation: RepairDocumentation,
    destinationUri: Uri,
): Boolean =
    runCatching {
        context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
            ZipOutputStream(outputStream).use { zip ->
                var assetIndex = 0
                fun writeAsset(rawUri: String?): String? {
                    if (rawUri.isNullOrBlank()) return null
                    val sourceUri = Uri.parse(rawUri)
                    val entryName = "assets/asset-${assetIndex++}${sourceUri.exportExtension(context)}"
                    val copied = runCatching {
                        zip.putNextEntry(ZipEntry(entryName))
                        context.openAssetInputStream(sourceUri)?.use { input ->
                            input.copyTo(zip)
                        } ?: error("Missing asset")
                        zip.closeEntry()
                    }.isSuccess
                    return if (copied) "package://$entryName" else rawUri
                }

                val manifest = JSONObject()
                    .put("format", "bmw-garage-repair-documentation")
                    .put("version", 1)
                    .put("documentation", documentation.toExportJson(::writeAsset))

                zip.putNextEntry(ZipEntry("manifest.json"))
                zip.write(manifest.toString(2).toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        } ?: return@runCatching false
        true
    }.getOrDefault(false)

private fun importRepairDocumentationPackage(
    context: Context,
    packageUri: Uri,
    currentDocumentation: RepairDocumentation,
): RepairDocumentation? =
    runCatching {
        val cacheFile = File(context.cacheDir, "repair-doc-import-${System.currentTimeMillis()}.zip")
        context.contentResolver.openInputStream(packageUri)?.use { input ->
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        } ?: return@runCatching null

        ZipFile(cacheFile).use { zip ->
            val manifestEntry = zip.getEntry("manifest.json") ?: return@runCatching null
            val manifest = zip.getInputStream(manifestEntry).bufferedReader().use { it.readText() }
            val documentationJson = JSONObject(manifest).optJSONObject("documentation") ?: return@runCatching null
            val importDirectory = File(context.filesDir, "imported_repair_docs/${System.currentTimeMillis()}")

            fun resolveAsset(rawUri: String?): String? {
                if (rawUri.isNullOrBlank() || !rawUri.startsWith("package://")) return rawUri
                val entryName = rawUri.removePrefix("package://")
                val entry = zip.getEntry(entryName) ?: return null
                importDirectory.mkdirs()
                val destination = File(importDirectory, entryName.substringAfterLast('/'))
                zip.getInputStream(entry).use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
                return Uri.fromFile(destination).toString()
            }

            documentationJson.toImportedDocumentation(
                currentDocumentation = currentDocumentation,
                resolveAsset = ::resolveAsset
            )
        }.also {
            cacheFile.delete()
        }
    }.getOrNull()

private fun RepairDocumentation.toExportJson(
    writeAsset: (String?) -> String?,
): JSONObject =
    JSONObject()
        .put("title", title)
        .put("summary", summary)
        .put("archivedShoppingList", JSONArray().apply {
            archivedShoppingList.forEach { item ->
                put(item.toExportJson())
            }
        })
        .put("tisDocuments", JSONArray().apply {
            effectiveDocumentationTisDocuments().forEach { link ->
                put(JSONObject().put("title", link.title).put("url", link.url))
            }
        })
        .put("torqueTables", JSONArray().apply {
            effectiveTorqueTables().forEach { table ->
                put(
                    JSONObject()
                        .put("id", table.id)
                        .put("title", table.title)
                        .put("torqueSpecs", JSONArray().apply {
                            table.torqueSpecs.forEach { spec -> put(spec.toJson()) }
                        })
                        .put("diagramImageUri", writeAsset(table.diagramImageUri))
                        .put("diagramAssignments", JSONArray().apply {
                            table.diagramAssignments.forEach { assignment ->
                                put(
                                    JSONObject()
                                        .put("torqueSpecIndex", assignment.torqueSpecIndex)
                                        .put("xRatio", assignment.xRatio.toDouble())
                                        .put("yRatio", assignment.yRatio.toDouble())
                                )
                            }
                        })
                )
            }
        })
        .put("youtubeVideos", JSONArray().apply {
            effectiveDocumentationYoutubeVideos().forEach { video ->
                put(
                    JSONObject()
                        .put("title", video.title)
                        .put("url", video.url)
                        .put("note", video.note)
                )
            }
        })
        .put("personalNotes", JSONArray().apply {
            personalNotes.forEach { item ->
                put(
                    JSONObject()
                        .put("id", item.id)
                        .put("type", item.type.name)
                        .put("title", item.title)
                        .put("text", item.text)
                        .put("uri", writeAsset(item.uri))
                        .put("url", item.url)
                )
            }
        })

private fun ShoppingListItem.toExportJson(): JSONObject =
    JSONObject()
        .put("id", id)
        .put("partNumber", partNumber)
        .put("manufacturerPartNumber", manufacturerPartNumber)
        .put("name", name)
        .put("manufacturer", manufacturer)
        .put("repairTitle", repairTitle)
        .put("repairId", repairId)
        .put("area", area.name)
        .put("quantity", quantity)
        .put("source", source)
        .put("price", price)
        .put("imageUri", imageUri)
        .put("shopUrl", shopUrl)
        .put("realOemUrl", realOemUrl)

private fun TorqueSpec.toJson(): JSONObject =
    JSONObject()
        .put("component", component)
        .put("type", type)
        .put("thread", thread)
        .put("tighteningSpecifications", tighteningSpecifications)
        .put("torque", torque)
        .put("source", source)
        .put("notes", notes)

private fun Context.openAssetInputStream(uri: Uri) =
    if (uri.scheme == "file") {
        uri.path?.let { File(it).inputStream() }
    } else {
        contentResolver.openInputStream(uri)
    }

private fun Uri.exportExtension(context: Context): String {
    val pathExtension = lastPathSegment
        ?.substringAfterLast('.', missingDelimiterValue = "")
        ?.takeIf { it.length in 1..8 }
    if (!pathExtension.isNullOrBlank()) return ".$pathExtension"
    return when (context.contentResolver.getType(this)) {
        "image/jpeg" -> ".jpg"
        "image/png" -> ".png"
        "image/webp" -> ".webp"
        "video/mp4" -> ".mp4"
        "application/pdf" -> ".pdf"
        else -> ".bin"
    }
}

private fun String.safeExportFileName(): String =
    replace(Regex("[^A-Za-z0-9_-]+"), "-")
        .trim('-')
        .ifBlank { "naprawa" }
        .take(48)

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

private suspend fun loadBitmapFromUrl(url: String): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            URL(url).openStream().use(BitmapFactory::decodeStream)
        }.getOrNull()
    }

private suspend fun fetchYoutubeTitle(videoUrl: String): String? =
    withContext(Dispatchers.IO) {
        runCatching {
            val encodedUrl = URLEncoder.encode(videoUrl, "UTF-8")
            val rawJson = URL("https://www.youtube.com/oembed?url=$encodedUrl&format=json")
                .readText()
            JSONObject(rawJson).optString("title").trim().ifBlank { null }
        }.getOrNull()
    }

fun List<TorqueSpec>.mergeTorqueSpecs(importedSpecs: List<TorqueSpec>): List<TorqueSpec> {
    val existingKeys = mutableSetOf<String>()
    return (this + importedSpecs)
        .collapseOcrContinuationSpecs()
        .filter { spec -> existingKeys.add(spec.stableTorqueKey()) }
}

fun List<TorqueSpec>.replaceOcrTorqueSpecs(importedSpecs: List<TorqueSpec>): List<TorqueSpec> {
    val manualSpecs = filterNot { it.source.equals("TIS screenshot", ignoreCase = true) }
    return manualSpecs.mergeTorqueSpecs(importedSpecs)
}

private fun List<TorqueSpec>.collapseOcrContinuationSpecs(): List<TorqueSpec> {
    val collapsed = mutableListOf<TorqueSpec>()
    var activeBlock = mutableListOf<TorqueSpec>()

    fun flushBlock() {
        if (activeBlock.isNotEmpty()) {
            collapsed += activeBlock.mergeOcrBlock()
            activeBlock = mutableListOf()
        }
    }

    forEach { spec ->
        val isOcr = spec.source.equals("TIS screenshot", ignoreCase = true)
        when {
            isOcr && spec.component.containsAzComponent() -> {
                flushBlock()
                activeBlock += spec
            }
            isOcr && activeBlock.isNotEmpty() -> {
                activeBlock += spec
            }
            else -> {
                flushBlock()
                collapsed += spec
            }
        }
    }
    flushBlock()

    return collapsed
}

private fun List<TorqueSpec>.mergeOcrBlock(): TorqueSpec {
    val firstSpec = first()
    if (size == 1) return firstSpec
    return firstSpec.copy(
        type = flatMap { spec -> listOf(spec.type, spec.component.takeUnless { it.containsAzComponent() }.orEmpty()) }
            .map { it.normalizeTorqueCell() }
            .filter { it.isMeaningfulTorqueDetail() }
            .distinct()
            .joinToString(" / "),
        thread = map { it.thread.normalizeTorqueCell() }
            .filter { it.isMeaningfulTorqueDetail() }
            .distinct()
            .joinToString(" / "),
        tighteningSpecifications = map { it.tighteningSpecifications.normalizeTorqueCell() }
            .filter { it.isMeaningfulTorqueDetail() }
            .distinct()
            .joinToString(" / "),
        torque = flatMap { spec ->
            torqueRegex.findAll(spec.torque)
                .map { it.value.standardizeTorqueText() }
                .toList()
        }.distinct().joinToString(" / ")
    )
}

private fun PersonalDocumentationItemType.shortLabel(): String =
    when (this) {
        PersonalDocumentationItemType.Text -> "TXT"
        PersonalDocumentationItemType.Photo -> "IMG"
        PersonalDocumentationItemType.Video -> "VID"
        PersonalDocumentationItemType.Document -> "DOC"
        PersonalDocumentationItemType.Link -> "URL"
        PersonalDocumentationItemType.File -> "FILE"
    }

private fun openPersonalDocumentationItem(
    context: Context,
    uriHandler: UriHandler,
    item: PersonalDocumentationItem,
) {
    item.url?.let { url ->
        uriHandler.openUri(url)
        return
    }

    val rawUri = item.uri ?: return
    val uri = Uri.parse(rawUri)
    val sharedUri = if (uri.scheme == "file") {
        uri.path?.let { path ->
            FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                File(path)
            )
        } ?: uri
    } else {
        uri
    }
    val resolverMimeType = context.contentResolver.getType(sharedUri)
    val fallbackMimeType = when (item.type) {
        PersonalDocumentationItemType.Photo -> "image/*"
        PersonalDocumentationItemType.Video -> "video/*"
        PersonalDocumentationItemType.Document -> "application/pdf"
        PersonalDocumentationItemType.File -> "*/*"
        PersonalDocumentationItemType.Text -> "text/plain"
        PersonalDocumentationItemType.Link -> "text/plain"
    }
    val mimeType = resolverMimeType ?: fallbackMimeType
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(sharedUri, mimeType)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    intent.clipData = ClipData.newUri(context.contentResolver, item.title, sharedUri)

    runCatching {
        context.startActivity(Intent.createChooser(intent, "Otworz plik"))
    }
}

private suspend fun loadVideoThumbnail(context: Context, uri: Uri): Bitmap? =
    withContext(Dispatchers.IO) {
        runCatching {
            MediaMetadataRetriever().use { retriever ->
                retriever.setDataSource(context, uri)
                retriever.getFrameAtTime(0)
            }
        }.getOrNull()
    }

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

fun recognizeTorqueSpecsFromBitmap(
    bitmap: Bitmap,
    onResult: (List<TorqueSpec>) -> Unit,
    onError: (String) -> Unit,
) {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { recognizedText ->
            onResult(parseTorqueSpecsFromRecognizedText(recognizedText))
        }
        .addOnFailureListener {
            onError("OCR nie odczytal tabeli momentow. Sprobuj wybrac wyrazniejszy screenshot.")
        }
}

fun loadDocumentationBitmapFromUri(context: Context, uri: Uri): Bitmap? =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()

private fun copyTorqueDiagramToAppStorage(
    context: Context,
    sourceUri: Uri,
    repairId: String,
    tableId: String,
): String? =
    runCatching {
        val directory = File(context.filesDir, "torque_diagrams/$repairId")
        directory.mkdirs()
        val destination = File(directory, "${System.currentTimeMillis()}-$tableId.jpg")
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            destination.outputStream().use { output ->
                input.copyTo(output)
            }
        } ?: return@runCatching null
        Uri.fromFile(destination).toString()
    }.getOrNull()

private fun parseTorqueSpecsFromRecognizedText(recognizedText: MlKitText): List<TorqueSpec> {
    val spatialSpecs = parseTorqueSpecsFromPositionedLines(recognizedText)
    return spatialSpecs.ifEmpty { parseTorqueSpecsFromText(recognizedText.text) }
}

private fun parseTorqueSpecsFromPositionedLines(recognizedText: MlKitText): List<TorqueSpec> {
    val lines = recognizedText.textBlocks
        .flatMap { it.lines }
        .mapNotNull { line ->
            val box = line.boundingBox ?: return@mapNotNull null
            OcrTorqueLine(
                text = line.text.replace(Regex("\\s+"), " ").trim(),
                left = box.left,
                top = box.top,
                right = box.right,
                bottom = box.bottom
            )
        }
        .filter { it.text.isNotBlank() }

    if (lines.none { it.text.containsAzComponent() } || lines.none { torqueRegex.containsMatchIn(it.text) }) {
        return emptyList()
    }

    val headerLines = lines.filter { it.text.isTorqueTableHeaderLine() }
    val tableLines = lines
        .filterNot { it.text.isTorqueTableHeaderLine() }
        .filterNot { it.text.isSponsoredOrFooterLine() }
        .sortedWith(compareBy<OcrTorqueLine> { it.centerY }.thenBy { it.left })

    if (tableLines.isEmpty()) return emptyList()

    val columnGuide = TorqueColumnGuide.from(headerLines, tableLines)
    val visualRows = tableLines.groupIntoVisualRows()
        .map { row ->
            OcrTorqueRow(
                columns = row.toTorqueColumns(columnGuide),
                rawText = row.joinToString(" ") { it.text }.normalizeTorqueCell()
            )
        }
        .filter { row ->
            row.rawText.isNotBlank() ||
                listOf(
                    row.columns.component,
                    row.columns.type,
                    row.columns.thread,
                    row.columns.tighteningSpecifications,
                    row.columns.torque
                ).any { it.isNotBlank() }
        }

    val blocks = buildList {
        var currentBlock = mutableListOf<OcrTorqueRow>()
        visualRows.forEach { row ->
            if (row.rawText.containsAzMarker()) {
                if (currentBlock.isNotEmpty()) {
                    add(currentBlock.toList())
                }
                currentBlock = mutableListOf(row)
            } else if (currentBlock.isNotEmpty()) {
                currentBlock.add(row)
            }
        }
        if (currentBlock.isNotEmpty()) {
            add(currentBlock.toList())
        }
    }

    return blocks.mapNotNull { block -> block.toTorqueSpecFromOcrRows() }
        .distinctBy { it.stableTorqueKey() }
}

private fun parseTorqueSpecsFromText(rawText: String): List<TorqueSpec> {
    val lines = rawText
        .replace("\r", "\n")
        .lines()
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .filter { it.isNotBlank() }
        .filterNot { it.isTorqueTableHeaderLine() }

    val blocks = buildList {
        var currentBlock = mutableListOf<String>()
        lines.forEach { line ->
            if (line.containsAzMarker()) {
                if (currentBlock.isNotEmpty()) {
                    add(currentBlock.toList())
                }
                currentBlock = mutableListOf(line)
            } else if (currentBlock.isNotEmpty()) {
                currentBlock.add(line)
            }
        }
        if (currentBlock.isNotEmpty()) {
            add(currentBlock.toList())
        }
    }

    return blocks.mapNotNull(::parseTorqueBlock)
}

private fun parseTorqueBlock(block: List<String>): TorqueSpec? {
    if (block.isEmpty()) return null

    val firstLine = block.first()
    val component = extractTorqueComponent(firstLine)
    if (component.isBlank()) return null

    val details = block.joinToString(" ") { line ->
        line.removePrefix(component)
            .replace(torqueRegex, "")
            .normalizeTorqueCell()
    }.normalizeTorqueCell()

    val torques = block.flatMap { line ->
        torqueRegex.findAll(line)
            .map { match -> match.value.standardizeTorqueText() }
            .toList()
    }.distinct()

    if (torques.isEmpty()) return null

    return TorqueSpec(
        component = component,
        type = details,
        torque = torques.joinToString(" / "),
        source = "TIS screenshot",
        notes = ""
    )
}

private val torqueRegex = Regex("\\b\\d+(?:[,.]\\d+)?\\s*N\\s*m\\b", RegexOption.IGNORE_CASE)
private val threadRegex = Regex("\\b(?:M\\d+\\s*x\\s*\\d+|M\\d+|Banjo bolt\\s*M\\d+\\s*x?\\s*\\d*)\\b", RegexOption.IGNORE_CASE)

private data class OcrTorqueLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
) {
    val centerY: Int
        get() = (top + bottom) / 2

    val height: Int
        get() = bottom - top
}

private data class TorqueColumnGuide(
    val typeStart: Int,
    val threadStart: Int,
    val tighteningStart: Int,
    val torqueStart: Int,
) {
    companion object {
        fun from(headerLines: List<OcrTorqueLine>, tableLines: List<OcrTorqueLine>): TorqueColumnGuide {
            val minLeft = tableLines.minOf { it.left }
            val maxRight = tableLines.maxOf { it.right }
            val width = (maxRight - minLeft).coerceAtLeast(1)

            val typeStart = headerLines.findLeft("type")
                ?: (minLeft + width * 0.36f).toInt()
            val threadStart = headerLines.findLeft("thread")
                ?: (minLeft + width * 0.52f).toInt()
            val tighteningStart = headerLines.findLeft("tightening specifications")
                ?: headerLines.findLeft("specifications")
                ?: (minLeft + width * 0.66f).toInt()
            val torqueStart = headerLines.findLeft("torque")
                ?: (minLeft + width * 0.88f).toInt()

            return TorqueColumnGuide(
                typeStart = typeStart,
                threadStart = threadStart,
                tighteningStart = tighteningStart,
                torqueStart = torqueStart
            )
        }
    }
}

private data class TorqueRowColumns(
    val component: String,
    val type: String,
    val thread: String,
    val tighteningSpecifications: String,
    val torque: String,
)

private data class OcrTorqueRow(
    val columns: TorqueRowColumns,
    val rawText: String,
)

private data class TorqueDetailRow(
    val type: String,
    val thread: String,
    val tighteningSpecifications: String,
    val torque: String,
)

private fun parseTorqueDetailRowFromText(
    rawText: String,
    component: String,
    fallbackType: String,
    fallbackThread: String,
    fallbackTightening: String,
): TorqueDetailRow {
    val rowWithoutComponent = rawText
        .removePrefix(component)
        .replace(torqueRegex, "")
        .normalizeTorqueCell()
    val source = rowWithoutComponent.ifBlank {
        listOf(fallbackType, fallbackThread, fallbackTightening)
            .joinToString(" ")
            .normalizeTorqueCell()
    }
    val threadMatch = threadRegex.find(source)
    val type = if (threadMatch == null) {
        fallbackType.normalizeTorqueCell().ifBlank { source }
    } else {
        source.substring(0, threadMatch.range.first).normalizeTorqueCell()
    }
    val thread = threadMatch?.value?.normalizeTorqueCell()
        ?: fallbackThread.normalizeTorqueCell()
    val tighteningFromThread = if (threadMatch == null) {
        fallbackTightening.replace(torqueRegex, "").normalizeTorqueCell()
    } else {
        source.substring(threadMatch.range.last + 1)
            .replace(torqueRegex, "")
            .normalizeTorqueCell()
            .ifBlank { fallbackTightening.replace(torqueRegex, "").normalizeTorqueCell() }
    }
    val splitType = type.splitTypeAndTighteningLeak()
    val tightening = listOf(splitType.second, tighteningFromThread)
        .filter { it.isMeaningfulTorqueDetail() }
        .joinToString(" ")
        .normalizeTorqueCell()

    return TorqueDetailRow(
        type = splitType.first,
        thread = thread,
        tighteningSpecifications = tightening,
        torque = ""
    )
}

private fun String.splitTypeAndTighteningLeak(): Pair<String, String> {
    val markers = listOf(
        "Blue union screw",
        "connection in steel",
        "connection in aluminium",
        "brake hose",
        "hydraulic control unit"
    )
    val markerIndex = markers
        .mapNotNull { marker ->
            indexOf(marker, ignoreCase = true).takeIf { it >= 0 }
        }
        .minOrNull()
    if (markerIndex == null) return normalizeTorqueCell() to ""
    return substring(0, markerIndex).normalizeTorqueCell() to
        substring(markerIndex).normalizeTorqueCell()
}

private fun List<TorqueDetailRow>.toTorqueDetailNotes(): String =
    if (isEmpty()) {
        ""
    } else {
        "OCR_ROWS\n" + JSONArray().apply {
            forEach { row ->
                put(
                    JSONObject()
                        .put("type", row.type)
                        .put("thread", row.thread)
                        .put("tighteningSpecifications", row.tighteningSpecifications)
                        .put("torque", row.torque)
                )
            }
        }.toString()
    }

private fun List<OcrTorqueRow>.toTorqueSpecFromOcrRows(): TorqueSpec? {
    if (isEmpty()) return null
    val firstRow = first()
    val component = extractTorqueComponent(
        firstRow.columns.component.ifBlank { firstRow.rawText }
    ).normalizeTorqueCell()
    if (!component.containsAzComponent()) return null

    val detailRows = mutableListOf<TorqueDetailRow>()
    forEach { row ->
        val columns = row.columns
        val torqueValues = torqueRegex.findAll(columns.torque)
            .map { match -> match.value.standardizeTorqueText() }
            .toList()
            .ifEmpty {
                torqueRegex.findAll(row.rawText)
                    .map { match -> match.value.standardizeTorqueText() }
                    .toList()
            }

        val parsedRow = parseTorqueDetailRowFromText(
            rawText = row.rawText,
            component = component,
            fallbackType = columns.type,
            fallbackThread = columns.thread,
            fallbackTightening = columns.tighteningSpecifications
        )

        if (torqueValues.isNotEmpty()) {
            detailRows += TorqueDetailRow(
                type = parsedRow.type,
                thread = parsedRow.thread,
                tighteningSpecifications = parsedRow.tighteningSpecifications,
                torque = torqueValues.joinToString(" / ")
            )
        } else if (detailRows.isNotEmpty()) {
            val last = detailRows.removeAt(detailRows.lastIndex)
            detailRows += last.copy(
                type = listOf(last.type, parsedRow.type)
                    .filter { it.isMeaningfulTorqueDetail() }
                    .joinToString(" ")
                    .normalizeTorqueCell(),
                tighteningSpecifications = listOf(last.tighteningSpecifications, parsedRow.tighteningSpecifications)
                    .filter { it.isMeaningfulTorqueDetail() }
                    .joinToString(" ")
                    .normalizeTorqueCell()
            )
        }
    }

    val cleanedRows = detailRows
        .filter { it.torque.isNotBlank() }
        .distinct()

    if (cleanedRows.isEmpty()) return null

    return TorqueSpec(
        component = component,
        type = cleanedRows.map { it.type }
            .filter { it.isMeaningfulTorqueDetail() }
            .distinct()
            .joinToString(" / "),
        thread = cleanedRows.map { it.thread }
            .filter { it.isMeaningfulTorqueDetail() }
            .distinct()
            .joinToString(" / "),
        tighteningSpecifications = cleanedRows.map { it.tighteningSpecifications }
            .filter { it.isMeaningfulTorqueDetail() }
            .distinct()
            .joinToString(" / "),
        torque = cleanedRows.map { it.torque }.distinct().joinToString(" / "),
        source = "TIS screenshot",
        notes = cleanedRows.toTorqueDetailNotes()
    )
}

private fun List<OcrTorqueLine>.groupIntoVisualRows(): List<List<OcrTorqueLine>> {
    if (isEmpty()) return emptyList()
    val rowTolerance = (map { it.height }.sorted().getOrNull(size / 2) ?: 24)
        .coerceAtLeast(18)

    return buildList {
        var currentRow = mutableListOf<OcrTorqueLine>()
        for (line in this@groupIntoVisualRows) {
            val currentCenter = currentRow.map { it.centerY }.average().takeIf { !it.isNaN() } ?: line.centerY.toDouble()
            if (currentRow.isEmpty() || kotlin.math.abs(line.centerY - currentCenter) <= rowTolerance) {
                currentRow.add(line)
            } else {
                add(currentRow.sortedBy { it.left })
                currentRow = mutableListOf(line)
            }
        }
        if (currentRow.isNotEmpty()) {
            add(currentRow.sortedBy { it.left })
        }
    }
}

private fun List<OcrTorqueLine>.toTorqueColumns(guide: TorqueColumnGuide): TorqueRowColumns {
    val componentLines = mutableListOf<String>()
    val typeLines = mutableListOf<String>()
    val threadLines = mutableListOf<String>()
    val tighteningLines = mutableListOf<String>()
    val torqueLines = mutableListOf<String>()

    forEach { line ->
        when {
            line.left >= guide.torqueStart -> torqueLines.add(line.text)
            line.left >= guide.tighteningStart -> tighteningLines.add(line.text)
            line.left >= guide.threadStart -> threadLines.add(line.text)
            line.left >= guide.typeStart -> typeLines.add(line.text)
            else -> componentLines.add(line.text)
        }
    }

    val allText = joinToString(" ") { it.text }
    val component = componentLines.joinToString(" ")
        .ifBlank { extractTorqueComponent(allText).takeIf { it.containsAzComponent() }.orEmpty() }
        .replace(torqueRegex, "")
        .normalizeTorqueCell()

    return TorqueRowColumns(
        component = component,
        type = typeLines.joinToString(" ").replace(torqueRegex, ""),
        thread = threadLines.joinToString(" ").replace(torqueRegex, ""),
        tighteningSpecifications = tighteningLines.joinToString(" ").replace(torqueRegex, ""),
        torque = torqueLines.joinToString(" ")
    )
}

private fun List<OcrTorqueLine>.findLeft(label: String): Int? {
    val normalizedLabel = label.lowercase()
    return firstOrNull { line ->
        val normalizedText = line.text.lowercase()
        val wordCount = normalizedText.split(" ").count { it.isNotBlank() }
        normalizedText.contains(normalizedLabel) && wordCount <= normalizedLabel.split(" ").size + 1
    }?.left
}

private fun String.containsAzComponent(): Boolean =
    Regex("^\\d+\\s*AZ\\b", RegexOption.IGNORE_CASE).containsMatchIn(this)

private fun String.containsAzMarker(): Boolean =
    Regex("\\b\\d+\\s*AZ\\b", RegexOption.IGNORE_CASE).containsMatchIn(this)

private fun String.isTorqueTableHeaderLine(): Boolean {
    val normalized = uppercase()
    return normalized.contains("TIGHTENING TORQUES") ||
        normalized.contains("GLOW ELEMENTS") ||
        normalized.matches(Regex("\\d+\\s+\\d+\\s+.*", RegexOption.IGNORE_CASE)) ||
        normalized == "TYPE" ||
        normalized == "THREAD" ||
        normalized == "TORQUE" ||
        normalized == "TIGHTENING" ||
        normalized == "SPECIFICATIONS" ||
        normalized.contains("TIGHTENING SPECIFICATIONS")
}

private fun String.isSponsoredOrFooterLine(): Boolean {
    val normalized = lowercase()
    return normalized.contains("sponsored") ||
        normalized.contains("links") ||
        normalized.contains("privacy") ||
        normalized.contains("cookie")
}

private fun extractTorqueComponent(line: String): String {
    val withoutTorque = line.replace(torqueRegex, "").trim()
    val detailStart = listOfNotNull(
        Regex("\\bM\\d", RegexOption.IGNORE_CASE).find(withoutTorque)?.range?.first,
        Regex("\\b[EGF]\\d{2}\\b", RegexOption.IGNORE_CASE).find(withoutTorque)?.range?.first
    ).minOrNull()
    return if (detailStart == null) {
        withoutTorque.trim()
    } else {
        withoutTorque.substring(0, detailStart).trim()
    }
}

private fun String.isMeaningfulTorqueDetail(): Boolean {
    val normalized = trim()
    if (normalized.isBlank()) return false
    if (normalized.equals("type", ignoreCase = true)) return false
    if (normalized.equals("thread", ignoreCase = true)) return false
    if (normalized.equals("torque", ignoreCase = true)) return false
    return true
}

private fun String.standardizeTorqueText(): String =
    replace(Regex("\\s+"), " ")
        .replace(Regex("N\\s*m", RegexOption.IGNORE_CASE), "Nm")
        .trim()

private fun String.normalizeTorqueCell(): String =
    replace(Regex("\\s+"), " ")
        .replace(Regex("\\s*/\\s*"), " / ")
        .trim()

private fun String.normalizeForTorqueKey(): String =
    normalizeTorqueCell().lowercase()

private fun TorqueSpec.stableTorqueKey(): String =
    listOf(
        component.normalizeForTorqueKey(),
        type.normalizeForTorqueKey(),
        thread.normalizeForTorqueKey(),
        tighteningSpecifications.normalizeForTorqueKey(),
        torque.normalizeForTorqueKey()
    ).joinToString("|")

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun VehicleDocumentationScreenPreview() {
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
        VehicleDocumentationScreen(
            vehicle = vehicle,
            repairDocumentation = sampleRepairDocumentationFor(vehicle),
            onDocumentationUpdated = {},
            onBack = {}
        )
    }
}
