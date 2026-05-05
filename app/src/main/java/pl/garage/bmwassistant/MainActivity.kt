package pl.garage.bmwassistant

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GarageTheme {
                GarageApp()
            }
        }
    }
}

private data class Vehicle(
    val brand: String,
    val model: String,
    val generation: String,
    val engine: String,
    val year: String,
    val vin: String,
    val mileage: String,
    val note: String,
) {
    val displayName: String
        get() = listOf(brand, model, generation)
            .filter { it.isNotBlank() }
            .joinToString(" ")

    val technicalSummary: String
        get() = listOf(engine, yearLabel, mileageLabel)
            .filter { it.isNotBlank() }
            .joinToString(" / ")

    private val yearLabel: String
        get() = if (year.isBlank()) "" else "Rok $year"

    private val mileageLabel: String
        get() = if (mileage.isBlank()) "" else "$mileage km"
}

private data class GarageTask(
    val title: String,
    val vehicle: String,
    val status: String,
)

@Composable
private fun GarageTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Color(0xFF245B73),
            secondary = Color(0xFF6F4E2F),
            background = Color(0xFFF6F1E9),
            surface = Color(0xFFFFFCF7),
            onPrimary = Color.White,
            onSecondary = Color.White,
            onBackground = Color(0xFF211C18),
            onSurface = Color(0xFF211C18)
        ),
        content = content
    )
}

@Composable
private fun GarageApp() {
    val vehicles = remember { mutableStateListOf<Vehicle>() }
    var isAddingVehicle by rememberSaveable { mutableStateOf(false) }
    var vehiclePendingDeletion by remember { mutableStateOf<Vehicle?>(null) }

    vehiclePendingDeletion?.let { vehicle ->
        DeleteVehicleDialog(
            vehicle = vehicle,
            onConfirm = {
                vehicles.remove(vehicle)
                vehiclePendingDeletion = null
            },
            onDismiss = { vehiclePendingDeletion = null }
        )
    }

    when {
        vehicles.isEmpty() -> AddVehicleWizard(
            onVehicleCreated = { vehicle ->
                vehicles.add(vehicle)
                isAddingVehicle = false
            }
        )

        isAddingVehicle -> AddVehicleWizard(
            onVehicleCreated = { vehicle ->
                vehicles.add(vehicle)
                isAddingVehicle = false
            },
            onCancel = { isAddingVehicle = false }
        )

        else -> GarageDashboard(
            vehicles = vehicles,
            activeTasks = sampleTasksFor(vehicles.first()),
            onAddVehicle = { isAddingVehicle = true },
            onDeleteVehicle = { vehiclePendingDeletion = it }
        )
    }
}

@Composable
private fun AddVehicleWizard(
    onVehicleCreated: (Vehicle) -> Unit,
    onCancel: (() -> Unit)? = null,
) {
    var brand by rememberSaveable { mutableStateOf("BMW") }
    var model by rememberSaveable { mutableStateOf("") }
    var generation by rememberSaveable { mutableStateOf("") }
    var engine by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableStateOf("") }
    var vin by rememberSaveable { mutableStateOf("") }
    var mileage by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    val canSave = model.isNotBlank() && engine.isNotBlank()

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
                Header(
                    title = "Dodaj pierwsze auto",
                    subtitle = "Zaczynamy od profilu auta. Pozniej podlaczymy do niego naprawy, zdjecia, czesci i dokumenty."
                )
            }

            item {
                WizardCard {
                    GarageTextField(
                        value = brand,
                        onValueChange = { brand = it },
                        label = "Marka"
                    )
                    GarageTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = "Model",
                        placeholder = "np. E60 520d"
                    )
                    GarageTextField(
                        value = generation,
                        onValueChange = { generation = it },
                        label = "Generacja / nadwozie",
                        placeholder = "np. E60"
                    )
                    GarageTextField(
                        value = engine,
                        onValueChange = { engine = it },
                        label = "Silnik",
                        placeholder = "np. M47N2 2.0d"
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        GarageTextField(
                            value = year,
                            onValueChange = { year = it.filter(Char::isDigit).take(4) },
                            label = "Rok",
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Number
                        )
                        GarageTextField(
                            value = mileage,
                            onValueChange = { mileage = it.filter(Char::isDigit).take(7) },
                            label = "Przebieg",
                            modifier = Modifier.weight(1f),
                            keyboardType = KeyboardType.Number
                        )
                    }
                    GarageTextField(
                        value = vin,
                        onValueChange = { vin = it.uppercase().take(17) },
                        label = "VIN",
                        placeholder = "17 znakow"
                    )
                    GarageTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = "Notatka startowa",
                        placeholder = "np. tylna zwrotnica lewa, diagnostyka DDE",
                        singleLine = false,
                        minLines = 3
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    onCancel?.let {
                        TextButton(
                            onClick = it,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Anuluj")
                        }
                    }
                    Button(
                        onClick = {
                            onVehicleCreated(
                                Vehicle(
                                    brand = brand.trim(),
                                    model = model.trim(),
                                    generation = generation.trim(),
                                    engine = engine.trim(),
                                    year = year.trim(),
                                    vin = vin.trim(),
                                    mileage = mileage.trim(),
                                    note = note.trim()
                                )
                            )
                        },
                        enabled = canSave,
                        modifier = Modifier.weight(if (onCancel == null) 1f else 1.4f)
                    ) {
                        Text("Zapisz auto")
                    }
                }
            }
        }
    }
}

@Composable
private fun GarageDashboard(
    vehicles: List<Vehicle>,
    activeTasks: List<GarageTask>,
    onAddVehicle: () -> Unit,
    onDeleteVehicle: (Vehicle) -> Unit,
) {
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
                Header(
                    title = "BMW Garage Assistant",
                    subtitle = "Prywatne centrum napraw, czesci, zdjec i notatek dla Twoich BMW."
                )
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SectionTitle("Moje auta")
                    TextButton(onClick = onAddVehicle) {
                        Text("Dodaj")
                    }
                }
            }

            items(vehicles) { vehicle ->
                VehicleCard(
                    vehicle = vehicle,
                    onDelete = { onDeleteVehicle(vehicle) }
                )
            }

            item {
                SectionTitle("Aktywne zadania")
            }

            items(activeTasks) { task ->
                TaskRow(task)
            }

            item {
                SectionTitle("Pierwszy modul")
            }

            item {
                FirstModuleCard()
            }
        }
    }
}

@Composable
private fun DeleteVehicleDialog(
    vehicle: Vehicle,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Usunac profil auta?") },
        text = {
            Text(
                text = "Profil ${vehicle.displayName.ifBlank { "BMW" }} zostanie usuniety z aktualnej listy. W kolejnym kroku podepniemy to pod lokalna baze danych."
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Usun")
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
private fun Header(
    title: String,
    subtitle: String,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = subtitle,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun WizardCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content
        )
    }
}

@Composable
private fun GarageTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder.isBlank()) null else {
            { Text(placeholder) }
        },
        singleLine = singleLine,
        minLines = minLines,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = keyboardType
        )
    )
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier.padding(top = 8.dp),
        fontSize = 20.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun VehicleCard(
    vehicle: Vehicle,
    onDelete: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                        text = vehicle.displayName.ifBlank { "BMW" },
                        fontSize = 19.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = vehicle.technicalSummary.ifBlank { "Dane techniczne do uzupelnienia" },
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.68f)
                    )
                }
                StatusPill("Profil auta")
            }

            if (vehicle.vin.isNotBlank()) {
                Text(
                    text = "VIN: ${vehicle.vin}",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.74f)
                )
            }

            Text(
                text = vehicle.note.ifBlank { "Brak notatki startowej." },
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDelete) {
                    Text("Usun profil")
                }
            }
        }
    }
}

@Composable
private fun StatusPill(text: String) {
    Box(
        modifier = Modifier
            .background(
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                shape = RoundedCornerShape(50)
            )
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun TaskRow(task: GarageTask) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = task.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(onClick = {}, label = { Text(task.vehicle) })
                AssistChip(onClick = {}, label = { Text(task.status) })
            }
        }
    }
}

@Composable
private fun FirstModuleCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Projekt naprawy: pierwsze auto",
                fontSize = 19.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                text = "Nastepnie dodamy lokalna baze, projekty napraw, zdjecia i liste czesci.",
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.86f)
            )
        }
    }
}

private fun sampleTasksFor(vehicle: Vehicle): List<GarageTask> {
    val vehicleName = vehicle.model.ifBlank { vehicle.displayName.ifBlank { "BMW" } }
    return listOf(
        GarageTask(
            title = "Uzupelnic szczegoly profilu auta",
            vehicle = vehicleName,
            status = "Profil"
        ),
        GarageTask(
            title = "Dodac pierwszy projekt naprawy",
            vehicle = vehicleName,
            status = "Nastepny krok"
        ),
        GarageTask(
            title = "Przygotowac sekcje czesci i dokumentow",
            vehicle = vehicleName,
            status = "Planowanie"
        )
    )
}

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun AddVehicleWizardPreview() {
    GarageTheme {
        AddVehicleWizard(onVehicleCreated = {})
    }
}

@Preview(showBackground = true, widthDp = 430)
@Composable
private fun GarageDashboardPreview() {
    GarageTheme {
        GarageDashboard(
            vehicles = listOf(
                Vehicle(
                    brand = "BMW",
                    model = "E60 520d",
                    generation = "E60",
                    engine = "M47N2 2.0d",
                    year = "2006",
                    vin = "WBAXXXXXXXXXXXXXX",
                    mileage = "285000",
                    note = "Tylna zwrotnica lewa, zardzewiala sruba"
                )
            ),
            activeTasks = sampleTasksFor(
                Vehicle(
                    brand = "BMW",
                    model = "E60 520d",
                    generation = "E60",
                    engine = "M47N2 2.0d",
                    year = "2006",
                    vin = "",
                    mileage = "",
                    note = ""
                )
            ),
            onAddVehicle = {},
            onDeleteVehicle = {}
        )
    }
}
