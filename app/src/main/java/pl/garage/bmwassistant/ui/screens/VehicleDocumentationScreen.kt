package pl.garage.bmwassistant.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.data.sampleRepairDocumentationFor
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.TorqueSpec
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.ui.components.GarageTextField
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.iconResource
import pl.garage.bmwassistant.ui.theme.GarageTheme
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

@Composable
fun VehicleDocumentationScreen(
    vehicle: Vehicle,
    repairDocumentation: List<RepairDocumentation>,
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
    onBack: () -> Unit,
) {
    var isGeneralExpanded by remember { mutableStateOf(true) }
    var isRepairDocsExpanded by remember { mutableStateOf(true) }
    var selectedDocumentation by remember { mutableStateOf<RepairDocumentation?>(null) }
    var expandedRepairAreas by remember(repairDocumentation) {
        mutableStateOf(
            repairDocumentation.map { it.area }.toSet().ifEmpty { setOf(VehicleArea.Engine) }
        )
    }
    val documentationByArea = remember(repairDocumentation) {
        repairDocumentation.groupBy { it.area }
    }

    BackHandler(enabled = selectedDocumentation != null) {
        selectedDocumentation = null
    }

    selectedDocumentation?.let { documentation ->
        RepairDocumentationDetailsScreen(
            vehicle = vehicle,
            documentation = documentation,
            onDocumentationUpdated = { updatedDocumentation ->
                selectedDocumentation = updatedDocumentation
                onDocumentationUpdated(updatedDocumentation)
            },
            onBack = { selectedDocumentation = null }
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
                    subtitle = "Automatyczne foldery powiazane z naprawami. Zostaja w historii nawet po zamknieciu naprawy.",
                    countLabel = "${repairDocumentation.size} wpisow",
                    isExpanded = isRepairDocsExpanded,
                    onToggle = { isRepairDocsExpanded = !isRepairDocsExpanded }
                ) {
                    if (repairDocumentation.isEmpty()) {
                        EmptyDocumentationRow("Brak dokumentacji powiazanej z naprawami.")
                    } else {
                        VehicleArea.entries.forEach { area ->
                            val areaDocumentation = documentationByArea[area].orEmpty()
                            RepairDocumentationAreaGroup(
                                area = area,
                                documentation = areaDocumentation,
                                isExpanded = area in expandedRepairAreas,
                                onDocumentationClick = { documentation ->
                                    selectedDocumentation = documentation
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
                    text = documentation.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "Powiazana naprawa: ${documentation.repairTitle}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
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
    onDocumentationUpdated: (RepairDocumentation) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    var isAddingTisLink by remember { mutableStateOf(false) }
    var isAddingTorqueSpec by remember { mutableStateOf(false) }
    var torqueImportStatus by remember { mutableStateOf<String?>(null) }

    fun importTorqueScreenshot(uri: Uri) {
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
                    onDocumentationUpdated(
                        documentation.copy(torqueSpecs = documentation.torqueSpecs + torqueSpecs)
                    )
                    torqueImportStatus = "Dodano ${torqueSpecs.size} wpisow z OCR."
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

    if (isAddingTisLink) {
        AddTisLinkDialog(
            onDismiss = { isAddingTisLink = false },
            onSave = { link ->
                onDocumentationUpdated(
                    documentation.copy(tisLinks = documentation.tisLinks + link)
                )
                isAddingTisLink = false
            }
        )
    }

    if (isAddingTorqueSpec) {
        AddTorqueSpecDialog(
            onDismiss = { isAddingTorqueSpec = false },
            onSave = { spec ->
                onDocumentationUpdated(
                    documentation.copy(torqueSpecs = documentation.torqueSpecs + spec)
                )
                isAddingTorqueSpec = false
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
                Header(
                    title = documentation.repairTitle,
                    subtitle = "${vehicle.displayName.ifBlank { "BMW" }} / ${documentation.area.label}"
                )
            }
            item {
                Text(
                    text = documentation.summary,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DocumentationLinksTile(
                        title = "Schematy",
                        subtitle = "Linki do procedur TIS i schematow powiazanych z ta naprawa.",
                        marker = "${documentation.tisLinks.size} linkow",
                        links = documentation.tisLinks,
                        onAddLink = { isAddingTisLink = true }
                    )
                    TorqueSpecsTile(
                        torqueSpecs = documentation.torqueSpecs,
                        importStatus = torqueImportStatus,
                        onAddTorqueSpec = { isAddingTorqueSpec = true },
                        onImportScreenshot = { torqueScreenshotLauncher.launch("image/*") }
                    )
                    DocumentationMaterialTile(
                        title = "YouTube",
                        subtitle = "Linki do filmow pokazujacych podobna naprawe, demontaz albo diagnostyke.",
                        marker = "Linki"
                    )
                    DocumentationMaterialTile(
                        title = "Notatki wlasne",
                        subtitle = "Twoje obserwacje, zdjecia, objawy, decyzje i rzeczy do sprawdzenia przy tej naprawie.",
                        marker = "Notatki"
                    )
                }
            }
        }
    }
}

@Composable
private fun AddTisLinkDialog(
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
) {
    var link by remember { mutableStateOf("") }
    val normalizedLink = link.trim()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj link TIS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Wklej link do procedury lub schematu. Bedzie widoczny w dokumentacji tej naprawy.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f)
                )
                GarageTextField(
                    value = link,
                    onValueChange = { link = it },
                    label = "Link",
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = "https://www.newtis.info/..."
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = normalizedLink.isNotBlank(),
                onClick = { onSave(normalizedLink.withHttpsPrefix()) }
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
private fun DocumentationLinksTile(
    title: String,
    subtitle: String,
    marker: String,
    links: List<String>,
    onAddLink: () -> Unit,
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
                modifier = Modifier.fillMaxWidth(),
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

            if (links.isEmpty()) {
                Text(
                    text = "Brak dodanych linkow.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            } else {
                links.forEachIndexed { index, link ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { uriHandler.openUri(link) },
                        color = MaterialTheme.colorScheme.background.copy(alpha = 0.42f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "TIS ${index + 1}",
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = link,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 12.sp,
                                maxLines = 2
                            )
                        }
                    }
                }
            }

            TextButton(onClick = onAddLink) {
                Text("Dodaj link TIS")
            }
        }
    }
}

@Composable
private fun AddTorqueSpecDialog(
    onDismiss: () -> Unit,
    onSave: (TorqueSpec) -> Unit,
) {
    var component by remember { mutableStateOf("") }
    var type by remember { mutableStateOf("") }
    var thread by remember { mutableStateOf("") }
    var tighteningSpecifications by remember { mutableStateOf("") }
    var torque by remember { mutableStateOf("") }
    var source by remember { mutableStateOf("TIS") }
    var notes by remember { mutableStateOf("") }
    val canSave = component.isNotBlank() && torque.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Dodaj moment dokrecenia") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Na razie wpis reczny. Import ze screenshotu bedzie uzupelnial te same kolumny.",
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
private fun TorqueSpecsTile(
    torqueSpecs: List<TorqueSpec>,
    importStatus: String?,
    onAddTorqueSpec: () -> Unit,
    onImportScreenshot: () -> Unit,
) {
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
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = "Momenty dokrecen",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Tabela momentow z TIS. Pozniej podepniemy import ze screenshotu i przypisanie do srub ze schematow.",
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f),
                        maxLines = 4
                    )
                }
                Text(
                    text = "${torqueSpecs.size} wpisow",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            if (torqueSpecs.isEmpty()) {
                Text(
                    text = "Brak dodanych momentow dokrecen.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.62f),
                    fontSize = 12.sp
                )
            } else {
                TorqueSpecsTable(torqueSpecs = torqueSpecs)
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
                TextButton(onClick = onAddTorqueSpec) {
                    Text("Dodaj moment")
                }
                TextButton(onClick = onImportScreenshot) {
                    Text("Import ze screenshotu")
                }
            }
        }
    }
}

@Composable
private fun TorqueSpecsTable(torqueSpecs: List<TorqueSpec>) {
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
            .sortedBy { it.component.lowercase() }
            .forEach { spec ->
                TorqueSpecTableRow(
                    component = spec.component,
                    type = spec.type,
                    thread = spec.thread,
                    tighteningSpecifications = spec.tighteningSpecifications,
                    torque = spec.torque,
                    source = spec.source,
                    notes = spec.notes,
                    isHeader = false
                )
            }
    }
}

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
) {
    val rowColor = if (isHeader) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
    } else {
        MaterialTheme.colorScheme.background.copy(alpha = 0.42f)
    }
    val weight = if (isHeader) FontWeight.SemiBold else FontWeight.Normal

    Row(
        modifier = Modifier.width(1242.dp),
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

private fun String.withHttpsPrefix(): String =
    if (startsWith("http://") || startsWith("https://")) {
        this
    } else {
        "https://$this"
    }

private fun recognizeTorqueSpecsFromBitmap(
    bitmap: Bitmap,
    onResult: (List<TorqueSpec>) -> Unit,
    onError: (String) -> Unit,
) {
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        .process(InputImage.fromBitmap(bitmap, 0))
        .addOnSuccessListener { recognizedText ->
            onResult(parseTorqueSpecsFromText(recognizedText.text))
        }
        .addOnFailureListener {
            onError("OCR nie odczytal tabeli momentow. Sprobuj wybrac wyrazniejszy screenshot.")
        }
}

private fun loadDocumentationBitmapFromUri(context: Context, uri: Uri): Bitmap? =
    runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
        } else {
            @Suppress("DEPRECATION")
            MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
        }
    }.getOrNull()

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
            if (line.containsAzComponent()) {
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

    return blocks.flatMap(::parseTorqueBlock)
}

private fun parseTorqueBlock(block: List<String>): List<TorqueSpec> {
    if (block.isEmpty()) return emptyList()

    val firstLine = block.first()
    val component = extractTorqueComponent(firstLine)
    val firstLineRemainder = firstLine
        .removePrefix(component)
        .trim()

    val pendingTypeLines = mutableListOf<String>()
    val specs = mutableListOf<TorqueSpec>()

    listOf(firstLineRemainder)
        .plus(block.drop(1))
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .forEach { line ->
            val torqueValues = torqueRegex.findAll(line)
                .map { it.value.standardizeTorqueText() }
                .toList()
            val lineWithoutTorque = line
                .replace(torqueRegex, "")
                .replace(Regex("\\s+"), " ")
                .trim()

            if (torqueValues.isEmpty()) {
                if (lineWithoutTorque.isMeaningfulTorqueDetail()) {
                    pendingTypeLines.add(lineWithoutTorque)
                }
            } else {
                if (lineWithoutTorque.isMeaningfulTorqueDetail()) {
                    pendingTypeLines.add(lineWithoutTorque)
                }
                val type = pendingTypeLines.joinToString(" ")
                    .replace(Regex("\\s*/\\s*"), " / ")
                    .trim()
                torqueValues.forEach { torque ->
                    specs.add(
                        TorqueSpec(
                            component = component,
                            type = type,
                            torque = torque,
                            source = "TIS screenshot",
                            notes = ""
                        )
                    )
                }
                pendingTypeLines.clear()
            }
        }

    return specs
}

private val torqueRegex = Regex("\\b\\d+(?:[,.]\\d+)?\\s*N\\s*m\\b", RegexOption.IGNORE_CASE)

private fun String.containsAzComponent(): Boolean =
    Regex("^\\d+\\s*AZ\\b", RegexOption.IGNORE_CASE).containsMatchIn(this)

private fun String.isTorqueTableHeaderLine(): Boolean {
    val normalized = uppercase()
    return normalized.contains("TIGHTENING TORQUES") ||
        normalized == "TYPE" ||
        normalized == "THREAD" ||
        normalized == "TORQUE" ||
        normalized == "TIGHTENING" ||
        normalized == "SPECIFICATIONS" ||
        normalized.contains("TIGHTENING SPECIFICATIONS")
}

private fun extractTorqueComponent(line: String): String {
    val withoutTorque = line.replace(torqueRegex, "").trim()
    val typeStart = Regex("\\bM\\d", RegexOption.IGNORE_CASE).find(withoutTorque)?.range?.first
    return if (typeStart == null) {
        withoutTorque.trim()
    } else {
        withoutTorque.substring(0, typeStart).trim()
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
