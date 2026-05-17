package pl.garage.bmwassistant.ui.screens

import androidx.activity.compose.BackHandler
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Html
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.data.sampleRepairsFor
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.ShoppingListItem
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.ui.components.GarageTextField
import pl.garage.bmwassistant.ui.components.Header
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
            documentation = repairDocumentation.firstOrNull { it.repairTitle == repair.title },
            availableParts = inventoryParts.filter { it.repairTitle == repair.title },
            onOpenDocumentation = onOpenDocumentation,
            onOpenShoppingList = onOpenShoppingList,
            onAddShoppingItems = onAddShoppingItems,
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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Header(
                            title = "Lista napraw",
                            subtitle = vehicle.displayName.ifBlank { "Profil auta" }
                        )
                    }
                    AddRepairButton(onClick = { isChoosingRepairArea = true })
                }
            }

            items(VehicleArea.entries) { area ->
                val areaRepairs = repairs.filter { it.area == area }
                RepairAreaSection(
                    area = area,
                    repairs = areaRepairs,
                    isExpanded = area in expandedAreas,
                    onRepairClick = { repair -> selectedRepair = repair },
                    onToggle = {
                        expandedAreas = if (area in expandedAreas) {
                            expandedAreas - area
                        } else {
                            expandedAreas + area
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AddRepairButton(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .height(48.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "+",
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
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
    onBack: () -> Unit,
) {
    var isCatalogVisible by remember { mutableStateOf(false) }

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
                    Text("Wroc do listy")
                }
            }
            item {
                Header(
                    title = repair.title,
                    subtitle = "${vehicle.displayName.ifBlank { "BMW" }} / ${repair.area.label}"
                )
            }
            item {
                Text(
                    text = repair.problemDescription,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    RepairDetailTile(
                        title = "Lista zakupow",
                        subtitle = "Tu zbierzemy elementy potrzebne do wykonania tej naprawy.",
                        marker = "${repair.partsToIdentify.size} pozycji",
                        onClick = { onOpenShoppingList(repair) }
                    )
                    RepairDetailTile(
                        title = "Dostepne czesci",
                        subtitle = if (availableParts.isEmpty()) {
                            "Brak czesci przypisanych do tej naprawy."
                        } else {
                            availableParts.joinToString { "${it.name} x${it.quantity}" }
                        },
                        marker = "${availableParts.size} w magazynie"
                    )
                    RepairDetailTile(
                        title = "Schematy czescidobmw.pl",
                        subtitle = "Pobierz schematy po VIN z kategorii ${repair.area.label}, wybierz diagram i dodaj OEM-y do listy zakupow.",
                        marker = "Pobierz",
                        onClick = { isCatalogVisible = true }
                    )
                    RepairDetailTile(
                        title = "Dokumentacja",
                        subtitle = documentation?.summary
                            ?: "Dokumentacja zostanie utworzona automatycznie dla nowej naprawy.",
                        marker = documentation?.title ?: "Brak wpisu",
                        onClick = documentation?.let { doc ->
                            { onOpenDocumentation(doc) }
                        }
                    )
                    RepairDetailTile(
                        title = "Notatki",
                        subtitle = "Szybkie uwagi z pracy, pomiary, obserwacje i decyzje.",
                        marker = "Do rozbudowy"
                    )
                    RepairDetailTile(
                        title = "Odczyt bledow",
                        subtitle = "Pozniej podepniemy tutaj odczyt z BimmerTool i przypisanie bledow do kategorii.",
                        marker = "BimmerTool pozniej"
                    )
                }
            }
        }
    }
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
            onOpenDocumentation = {},
            onOpenShoppingList = {},
            onAddShoppingItems = {},
            onBack = {}
        )
    }
}
