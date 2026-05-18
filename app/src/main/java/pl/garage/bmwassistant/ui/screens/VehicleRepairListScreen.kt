package pl.garage.bmwassistant.ui.screens

import androidx.activity.compose.BackHandler
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.Html
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.produceState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.data.sampleRepairsFor
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.PersonalDocumentationItem
import pl.garage.bmwassistant.model.PersonalDocumentationItemType
import pl.garage.bmwassistant.model.RepairCheckpoint
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.TorqueDiagramAssignment
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.TorqueSpecTable
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
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
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

@Composable
fun VehicleRepairListScreen(
    vehicle: Vehicle,
    repairs: List<RepairProject>,
    repairDocumentation: List<RepairDocumentation>,
    inventoryParts: List<PartInventoryItem>,
    initialRepairTitle: String? = null,
    onRepairAdded: (RepairProject, RepairDocumentation) -> Unit,
    onRepairUpdated: (RepairProject) -> Unit,
    onOpenDocumentation: (RepairDocumentation) -> Unit,
    onOpenShoppingList: (RepairProject) -> Unit,
    onAddShoppingItems: (List<ShoppingListItem>) -> Unit,
    onInitialRepairClosed: () -> Unit = {},
    onBack: () -> Unit,
) {
    var expandedAreas by remember {
        mutableStateOf(
            repairs.map { it.area }.toSet().ifEmpty { setOf(VehicleArea.Engine) }
        )
    }
    var selectedFilter by remember { mutableStateOf("Aktywne") }
    var isChoosingRepairArea by remember { mutableStateOf(false) }
    var selectedAreaForNewRepair by remember { mutableStateOf<VehicleArea?>(null) }
    var selectedRepair by remember(initialRepairTitle, repairs) {
        mutableStateOf(
            initialRepairTitle?.let { repairTitle ->
                repairs.firstOrNull { it.title == repairTitle }
            }
        )
    }

    BackHandler(enabled = selectedRepair != null) {
        selectedRepair = null
        onInitialRepairClosed()
    }

    selectedRepair?.let { repair ->
        RepairDetailsScreen(
            vehicle = vehicle,
            repair = repair,
            documentation = repairDocumentation.firstOrNull { it.belongsToRepair(repair) },
            availableParts = inventoryParts.filter { it.belongsToRepair(repair) },
            onOpenDocumentation = onOpenDocumentation,
            onOpenShoppingList = onOpenShoppingList,
            onAddShoppingItems = onAddShoppingItems,
            onRepairUpdated = { updatedRepair ->
                selectedRepair = updatedRepair
                onRepairUpdated(updatedRepair)
            },
            onBack = {
                selectedRepair = null
                onInitialRepairClosed()
            }
        )
        return
    }

    if (isChoosingRepairArea) {
        RepairAreaPickerDialog(
            onAreaSelected = { area ->
                selectedAreaForNewRepair = area
                isChoosingRepairArea = false
            },
            onDismiss = { isChoosingRepairArea = false }
        )
    }

    selectedAreaForNewRepair?.let { area ->
        AddRepairDialog(
            vehicle = vehicle,
            area = area,
            onSave = { repair, documentation ->
                onRepairAdded(repair, documentation)
                expandedAreas = expandedAreas + area
                selectedAreaForNewRepair = null
                selectedRepair = repair
            },
            onDismiss = { selectedAreaForNewRepair = null }
        )
    }

    val filteredRepairs = remember(repairs, selectedFilter) {
        repairs.filter { repair ->
            when (selectedFilter) {
                "Zakonczone" -> repair.status.lowercase().contains("zakon")
                else -> !repair.status.lowercase().contains("zakon")
            }
        }
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
                            title = "Naprawy",
                            subtitle = vehicle.displayName.ifBlank { "Profil auta" }
                        )
                        AddRepairButton(onClick = { isChoosingRepairArea = true })
                    }
                }

                item {
                    SegmentTabs(
                        tabs = listOf("Aktywne", "Zakonczone"),
                        selectedTab = selectedFilter,
                        onSelect = { selectedFilter = it }
                    )
                }

                if (filteredRepairs.isEmpty()) {
                    item {
                        GaragePanel {
                            Text(
                                text = "Brak napraw w tej zakladce.",
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                            )
                        }
                    }
                } else {
                    items(filteredRepairs) { repair ->
                        RepairCard(
                            repair = repair,
                            documentation = repairDocumentation.firstOrNull { it.belongsToRepair(repair) },
                            partsCount = inventoryParts.count { it.belongsToRepair(repair) },
                            onClick = { selectedRepair = repair }
                        )
                    }
                }
            }
            BottomNavBar(
                items = bottomItems,
                selectedItem = "Naprawy",
                onSelect = { item ->
                    if (item != "Naprawy") onBack()
                },
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun RepairCard(
    repair: RepairProject,
    documentation: RepairDocumentation?,
    partsCount: Int,
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
            StatusBadge(repair.status.normalizedRepairStatus())
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
                text = "${documentation?.torqueSpecs?.size ?: 0} momentow",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                fontSize = 12.sp
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
    onSave: (RepairProject, RepairDocumentation) -> Unit,
    onDismiss: () -> Unit,
) {
    var repairTitle by remember { mutableStateOf("") }
    var repairNote by remember { mutableStateOf("") }
    val canSave = repairTitle.isNotBlank()

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
    onOpenDocumentation: (RepairDocumentation) -> Unit,
    onOpenShoppingList: (RepairProject) -> Unit,
    onAddShoppingItems: (List<ShoppingListItem>) -> Unit,
    onRepairUpdated: (RepairProject) -> Unit,
    onBack: () -> Unit,
) {
    var isCatalogVisible by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("Opis") }

    if (isCatalogVisible) {
        RealOemSchematicsDialog(
            vehicle = vehicle,
            repair = repair,
            onAddShoppingItems = onAddShoppingItems,
            onDismiss = { isCatalogVisible = false }
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
                    Text("‹ Naprawy")
                }
            }
            item {
                RepairDetailsHeader(repair)
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
                    RepairOverviewTab(
                        repair = repair,
                        onRepairUpdated = onRepairUpdated
                    )
                }
                "Czesci" -> item {
                    RepairPartsTab(
                        repair = repair,
                        availableParts = availableParts,
                        onOpenShoppingList = { onOpenShoppingList(repair) },
                        onOpenCatalog = { isCatalogVisible = true }
                    )
                }
                "Dokumenty" -> item {
                    RepairDocumentsTab(
                        documentation = documentation,
                        onOpenDocumentation = onOpenDocumentation
                    )
                }
                "Momenty" -> item { RepairTorqueTab(documentation) }
                "Notatki" -> item { RepairNotesTab(documentation) }
            }
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
            StatusBadge(repair.status.normalizedRepairStatus())
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
    val checkpoints = repair.effectiveCheckpoints()

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
                    modifier = Modifier.weight(1f),
                    placeholder = "Nowy checkpoint"
                )
                TextButton(
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
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
    }
}

@Composable
private fun RepairPartsTab(
    repair: RepairProject,
    availableParts: List<PartInventoryItem>,
    onOpenShoppingList: () -> Unit,
    onOpenCatalog: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GaragePanel(onClick = onOpenShoppingList) {
            Text("Czesci do ustalenia", fontWeight = FontWeight.SemiBold)
            if (repair.partsToIdentify.isEmpty()) {
                Text("Brak pozycji na liscie.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
            } else {
                repair.partsToIdentify.forEach { part ->
                    Text("• $part", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f))
                }
            }
            StatusBadge("Do kupienia", AccentYellow)
        }
        GaragePanel {
            Text("Na stanie", fontWeight = FontWeight.SemiBold)
            if (availableParts.isEmpty()) {
                Text("Brak czesci przypisanych do tej naprawy.", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f))
            } else {
                availableParts.forEach { part ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(part.name, modifier = Modifier.weight(1f))
                        Text("${part.quantity} szt.", color = AccentGreen)
                    }
                }
            }
        }
        GaragePanel(onClick = onOpenCatalog) {
            Text("Schematy czescidobmw.pl", fontWeight = FontWeight.SemiBold)
            Text(
                text = "Pobierz schematy po VIN i dodaj OEM-y do listy zakupow.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        }
    }
}

@Composable
private fun RepairDocumentsTab(
    documentation: RepairDocumentation?,
    onOpenDocumentation: (RepairDocumentation) -> Unit,
) {
    val tisLinks = documentation?.tisDocuments.orEmpty()
    val legacyTisLinks = documentation?.tisLinks.orEmpty()
    val files = documentation?.personalNotes.orEmpty().filter {
        it.type == PersonalDocumentationItemType.Document || it.type == PersonalDocumentationItemType.File
    }
    val media = documentation?.personalNotes.orEmpty().filter {
        it.type == PersonalDocumentationItemType.Photo || it.type == PersonalDocumentationItemType.Video
    }
    val videos = documentation?.youtubeVideos.orEmpty()

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        DocumentationSectionTitle("Linki TIS")
        GaragePanel(onClick = documentation?.let { doc -> { onOpenDocumentation(doc) } }) {
            if (tisLinks.isEmpty() && legacyTisLinks.isEmpty()) {
                EmptyDocumentationText("Brak linkow TIS.")
            } else {
                tisLinks.forEach { link ->
                    DocumentationLinkRow(
                        title = link.title,
                        subtitle = "BMW TIS",
                        marker = "↗",
                        accent = AccentBlue
                    )
                }
                legacyTisLinks.forEach { url ->
                    DocumentationLinkRow(
                        title = url,
                        subtitle = "BMW TIS",
                        marker = "↗",
                        accent = AccentBlue
                    )
                }
            }
        }

        DocumentationSectionTitle("Pliki i dokumenty")
        GaragePanel {
            if (files.isEmpty()) {
                EmptyDocumentationText("Brak plikow i dokumentow.")
            } else {
                files.forEach { file ->
                    DocumentationFileRow(file)
                }
            }
        }

        DocumentationSectionTitle("Youtube")
        GaragePanel {
            if (videos.isEmpty()) {
                EmptyDocumentationText("Brak filmow YouTube.")
            } else {
                videos.forEach { video ->
                    DocumentationYoutubeRow(
                        title = video.title,
                        subtitle = video.note.ifBlank { "YouTube" }
                    )
                }
            }
        }

        DocumentationSectionTitle("Zdjecia i filmy")
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
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        GaragePanel(onClick = documentation?.let { doc -> { onOpenDocumentation(doc) } }) {
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
private fun DocumentationSectionTitle(title: String) {
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onBackground,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )
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
private fun DocumentationLinkRow(
    title: String,
    subtitle: String,
    marker: String,
    accent: androidx.compose.ui.graphics.Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
private fun DocumentationYoutubeRow(
    title: String,
    subtitle: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
            Surface(
                color = AccentRed.copy(alpha = 0.92f),
                shape = RoundedCornerShape(6.dp)
            ) {
                Text(
                    text = "▶",
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
            Text(title, fontWeight = FontWeight.SemiBold, maxLines = 2)
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
private fun DocumentationFileRow(file: PersonalDocumentationItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(30.dp),
            color = AccentRed.copy(alpha = 0.18f),
            shape = RoundedCornerShape(6.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("PDF", color = AccentRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(file.title, fontWeight = FontWeight.SemiBold, maxLines = 1)
            Text(
                file.text.ifBlank { "Dokument" },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                fontSize = 12.sp,
                maxLines = 1
            )
        }
        Text("↓", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f), fontSize = 22.sp)
    }
}

@Composable
private fun DocumentationMediaTile(
    item: PersonalDocumentationItem,
    extraCount: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(82.dp)
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF2B3C46), Color(0xFF0E1821))
                ),
                RoundedCornerShape(8.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
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
        } else {
            Text(
                text = if (item.type == PersonalDocumentationItemType.Video) "FILM" else "IMG",
                color = AccentBlue,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
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
private fun RepairTorqueTab(documentation: RepairDocumentation?) {
    var mode by remember { mutableStateOf("Lista") }
    var selectedTorqueIndex by remember { mutableStateOf(0) }
    val tables = documentation?.effectiveTorqueTables().orEmpty()
    val activeTable = tables.firstOrNull()
    val specs = activeTable?.torqueSpecs.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SegmentTabs(
            tabs = listOf("Lista", "Szczegoly"),
            selectedTab = mode,
            onSelect = { mode = it }
        )
        if (mode == "Lista") {
            TorqueDiagramListView(
                table = activeTable,
                specs = specs,
                selectedTorqueIndex = selectedTorqueIndex.coerceIn(0, (specs.size - 1).coerceAtLeast(0)),
                onSelectTorque = { selectedTorqueIndex = it }
            )
        } else {
            TorqueDetailsTable(specs = specs)
        }
        GaragePanel {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("+", color = AccentBlue, fontSize = 24.sp, fontWeight = FontWeight.Medium)
                Text(
                    text = if (mode == "Lista") "Zmien schemat" else "Import ze screenshotu lub dodaj recznie",
                    color = AccentBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun TorqueDiagramListView(
    table: TorqueSpecTable?,
    specs: List<TorqueSpec>,
    selectedTorqueIndex: Int,
    onSelectTorque: (Int) -> Unit,
) {
    val assignments = table?.diagramAssignments.orEmpty()
        .filter { it.torqueSpecIndex in specs.indices }
        .ifEmpty { defaultTorqueAssignments(specs.size) }
    val selectedSpec = specs.getOrNull(selectedTorqueIndex)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        TorqueDiagramPreview(
            imageUri = table?.diagramImageUri,
            assignments = assignments,
            specs = specs,
            selectedTorqueIndex = selectedTorqueIndex,
            onSelectTorque = onSelectTorque
        )

        if (specs.isEmpty()) {
            GaragePanel {
                EmptyDocumentationText("Brak zapisanych momentow dla tej naprawy.")
            }
        } else {
            specs.take(4).forEachIndexed { index, spec ->
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
            Text(
                text = if (selectedSpec == null) {
                    "Momenty dokrecania zgodnie z TIS. Zawsze sprawdzaj aktualne dane techniczne."
                } else {
                    "${selectedSpec.component}: ${selectedSpec.torque}. ${selectedSpec.notes.ifBlank { "Sprawdz zrodlo przed montazem." }}"
                },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
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
) {
    val context = LocalContext.current
    val bitmap = remember(imageUri) {
        imageUri?.let { loadBitmapFromUri(context, Uri.parse(it)) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.96f))
    ) {
        val diagramWidth = maxWidth
        val diagramHeight = maxHeight
        if (bitmap != null) {
            Image(
                bitmap = bitmap.asImageBitmap(),
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
private fun TorqueDetailsTable(specs: List<TorqueSpec>) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GaragePanel {
            Text("Pelna tabela dokrecen", fontWeight = FontWeight.SemiBold)
            if (specs.isEmpty()) {
                EmptyDocumentationText("Brak zapisanych momentow.")
            } else {
                specs.forEachIndexed { index, spec ->
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${index + 1}. ${spec.component}",
                                modifier = Modifier.weight(1f),
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 2
                            )
                            Text(spec.torque, color = AccentBlue, fontWeight = FontWeight.SemiBold)
                        }
                        val details = listOf(
                            spec.type,
                            spec.thread,
                            spec.tighteningSpecifications,
                            spec.source,
                            spec.notes
                        ).filter { it.isNotBlank() }
                        if (details.isNotEmpty()) {
                            Text(
                                text = details.joinToString(" / "),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun RepairDocumentation.effectiveTorqueTables(): List<TorqueSpecTable> =
    torqueTables.ifEmpty {
        if (
            torqueSpecs.isEmpty() &&
            torqueDiagramImageUri == null &&
            torqueDiagramAssignments.isEmpty()
        ) {
            emptyList()
        } else {
            listOf(
                TorqueSpecTable(
                    id = "table-1",
                    title = "Tabela momentow 1",
                    torqueSpecs = torqueSpecs,
                    diagramImageUri = torqueDiagramImageUri,
                    diagramAssignments = torqueDiagramAssignments
                )
            )
        }
    }

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

@Composable
private fun RepairNotesTab(documentation: RepairDocumentation?) {
    val notes = documentation?.personalNotes.orEmpty()
    GaragePanel {
        Text("Notatki", fontWeight = FontWeight.SemiBold)
        if (notes.isEmpty()) {
            Text(
                text = "Brak notatek przypisanych do tej naprawy.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
            )
        } else {
            notes.forEach { note ->
                Text(note.title, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f))
            }
        }
    }
}

private fun String.normalizedRepairStatus(): String = when {
    lowercase().contains("zakon") -> "Zakonczona"
    lowercase().contains("plan") -> "Planowana"
    lowercase().contains("trak") -> "W trakcie"
    else -> this
}

private fun RepairDocumentation.belongsToRepair(repair: RepairProject): Boolean =
    repairId == repair.id || (repairId.isBlank() && repairTitle == repair.title && area == repair.area)

private fun PartInventoryItem.belongsToRepair(repair: RepairProject): Boolean =
    repairId == repair.id || (repairId.isNullOrBlank() && repairTitle == repair.title)

private fun RepairProject.effectiveCheckpoints(): List<RepairCheckpoint> =
    checkpoints.ifEmpty {
        checklist.mapIndexed { index, text ->
            RepairCheckpoint(
                id = "checkpoint-${index + 1}",
                text = text,
                isDone = false
            )
        }
    }

private fun RepairProject.withCheckpoints(updatedCheckpoints: List<RepairCheckpoint>): RepairProject =
    copy(
        checkpoints = updatedCheckpoints,
        checklist = updatedCheckpoints.map { it.text }
    )

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

@Composable
private fun RealOemSchematicsDialog(
    vehicle: Vehicle,
    repair: RepairProject,
    onAddShoppingItems: (List<ShoppingListItem>) -> Unit,
    onDismiss: () -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    var diagrams by remember { mutableStateOf(emptyList<RealOemDiagram>()) }
    var selectedDiagram by remember { mutableStateOf<RealOemDiagram?>(null) }
    var diagramDetails by remember { mutableStateOf<RealOemDiagramDetails?>(null) }
    var selectedPartNumbers by remember { mutableStateOf(setOf<String>()) }
    var isLoadingDiagrams by remember { mutableStateOf(false) }
    var isLoadingParts by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var diagramSearchQuery by remember { mutableStateOf("") }
    var enlargedImageUrl by remember { mutableStateOf<String?>(null) }
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

    enlargedImageUrl?.let { imageUrl ->
        EnlargedDiagramDialog(
            imageUrl = imageUrl,
            onDismiss = { enlargedImageUrl = null }
        )
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
            enlargedImageUrl = null
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

    fun addSelectedPartsToShoppingList() {
        val details = diagramDetails ?: return
        val diagram = selectedDiagram ?: return
        val items = details.parts
            .filter { it.partNumber in selectedPartNumbers }
            .mapIndexed { index, part ->
                ShoppingListItem(
                    id = "czescidobmw-${part.partNumber}-${System.currentTimeMillis()}-$index",
                    partNumber = part.partNumber,
                    manufacturerPartNumber = part.partNumber,
                    name = translateRealOemLabel(part.name),
                    manufacturer = "BMW / OEM",
                    repairTitle = repair.title,
                    repairId = repair.id,
                    area = repair.area,
                    quantity = part.quantity.toIntOrNull() ?: 1,
                    source = "czescidobmw.pl",
                    shopUrl = diagram.url,
                    realOemUrl = diagram.url
                )
            }
        onAddShoppingItems(items)
        onDismiss()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (selectedDiagram == null) {
                    "Schematy czescidobmw.pl"
                } else {
                    translateRealOemLabel(diagramDetails?.title ?: selectedDiagram?.title.orEmpty())
                }
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedDiagram == null) {
                    Text(
                        text = "${repair.area.label} / ${vehicle.displayName.ifBlank { "BMW" }}",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                    )
                    TextButton(
                        enabled = !isLoadingDiagrams,
                        onClick = { loadDiagrams() }
                    ) {
                        Text(if (isLoadingDiagrams) "Pobieram schematy..." else "Pobierz schematy")
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
                    TextButton(
                        onClick = {
                            selectedDiagram = null
                            diagramDetails = null
                            selectedPartNumbers = emptySet()
                            message = null
                        }
                    ) {
                        Text("Wroc do schematow")
                    }
                    message?.let { text ->
                        RealOemInfoRow(text)
                    }
                    if (isLoadingParts) {
                        RealOemInfoRow("Pobieram schemat i liste czesci...")
                    }
                    diagramDetails?.let { details ->
                        details.imageUrl?.let { imageUrl ->
                            RealOemDiagramImage(
                                imageUrl = imageUrl,
                                onClick = { enlargedImageUrl = imageUrl }
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
                                modifier = Modifier.heightIn(max = 360.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(details.parts) { part ->
                                    RealOemPartRow(
                                        part = part,
                                        isSelected = part.partNumber in selectedPartNumbers,
                                        onToggle = {
                                            selectedPartNumbers = if (part.partNumber in selectedPartNumbers) {
                                                selectedPartNumbers - part.partNumber
                                            } else {
                                                selectedPartNumbers + part.partNumber
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = diagramDetails?.parts.orEmpty().any { it.partNumber in selectedPartNumbers },
                onClick = { addSelectedPartsToShoppingList() }
            ) {
                Text("Dodaj zaznaczone")
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
    onToggle: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onToggle),
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
            Text(
                text = part.position.ifBlank { "-" },
                modifier = Modifier.width(34.dp),
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = translateRealOemLabel(part.name),
                    fontWeight = FontWeight.SemiBold
                )
                if (translateRealOemLabel(part.name) != part.name) {
                    Text(
                        text = part.name,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                    )
                }
                Text(
                    text = "Ilosc: ${part.quantity.ifBlank { "1" }} / OEM: ${part.partNumber}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                )
                if (part.notes.isNotBlank()) {
                    Text(
                        text = part.notes,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.56f)
                    )
                }
            }
            Text(
                text = if (isSelected) "Wybrane" else "Wybierz",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

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
private fun RealOemDiagramImage(
    imageUrl: String,
    onClick: () -> Unit,
    imageHeight: Int = 260,
    showHint: Boolean = true,
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

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = "Schemat czescidobmw.pl",
            modifier = Modifier
                .fillMaxWidth()
                .height(imageHeight.dp)
                .clickable(onClick = onClick),
            contentScale = ContentScale.Fit
        )
        if (showHint) {
            Text(
                text = "Kliknij schemat, aby powiekszyc",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        RealOemInfoRow("Schemat graficzny bedzie widoczny po pobraniu obrazu.")
    }
}

@Composable
private fun EnlargedDiagramDialog(
    imageUrl: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Podglad schematu") },
        text = {
            RealOemDiagramImage(
                imageUrl = imageUrl,
                onClick = {},
                imageHeight = 520,
                showHint = false
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Zamknij")
            }
        }
    )
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

private fun absoluteCzescidobmwUrl(href: String): String =
    when {
        href.startsWith("http://") || href.startsWith("https://") -> href
        href.startsWith("/") -> "https://czescidobmw.pl$href"
        else -> "https://czescidobmw.pl/$href"
    }.replace("&amp;", "&")

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
            onRepairAdded = { _, _ -> },
            onRepairUpdated = {},
            onOpenDocumentation = {},
            onOpenShoppingList = {},
            onAddShoppingItems = {},
            onBack = {}
        )
    }
}
