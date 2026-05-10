package pl.garage.bmwassistant.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.garage.bmwassistant.data.sampleRepairsFor
import pl.garage.bmwassistant.model.PartInventoryItem
import pl.garage.bmwassistant.model.RepairDocumentation
import pl.garage.bmwassistant.model.RepairProject
import pl.garage.bmwassistant.model.Vehicle
import pl.garage.bmwassistant.model.VehicleArea
import pl.garage.bmwassistant.ui.components.GarageTextField
import pl.garage.bmwassistant.ui.components.Header
import pl.garage.bmwassistant.ui.components.iconResource
import pl.garage.bmwassistant.ui.theme.GarageTheme

@Composable
fun VehicleRepairListScreen(
    vehicle: Vehicle,
    repairs: List<RepairProject>,
    repairDocumentation: List<RepairDocumentation>,
    inventoryParts: List<PartInventoryItem>,
    onRepairAdded: (RepairProject, RepairDocumentation) -> Unit,
    onBack: () -> Unit,
) {
    var expandedAreas by remember {
        mutableStateOf(
            repairs.map { it.area }.toSet().ifEmpty { setOf(VehicleArea.Engine) }
        )
    }
    var isChoosingRepairArea by remember { mutableStateOf(false) }
    var selectedAreaForNewRepair by remember { mutableStateOf<VehicleArea?>(null) }
    var selectedRepair by remember { mutableStateOf<RepairProject?>(null) }

    BackHandler(enabled = selectedRepair != null) {
        selectedRepair = null
    }

    selectedRepair?.let { repair ->
        RepairDetailsScreen(
            vehicle = vehicle,
            repair = repair,
            documentation = repairDocumentation.firstOrNull { it.repairTitle == repair.title },
            availableParts = inventoryParts.filter { it.repairTitle == repair.title },
            onBack = { selectedRepair = null }
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
    onBack: () -> Unit,
) {
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
                        marker = "${repair.partsToIdentify.size} pozycji"
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
                        title = "Dokumentacja",
                        subtitle = documentation?.summary
                            ?: "Dokumentacja zostanie utworzona automatycznie dla nowej naprawy.",
                        marker = documentation?.title ?: "Brak wpisu"
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
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            onBack = {}
        )
    }
}
